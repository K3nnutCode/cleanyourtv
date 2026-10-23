package com.gathin.tvhome;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Enumeration;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A manual, private-LAN-only, short-lived one-way browser photo uploader.
 *
 * The socket ONLY exists while the owner has the TV upload dialog open.
 * No cloud, no photo retrieval, no permanent service, no remote settings.
 * HTTP on a trusted LAN is not encrypted; do not use on public/guest Wi-Fi.
 */
final class AmbientLocalUploadServer implements AutoCloseable {
    interface Listener {
        void onPhotoAdded(int count);
        void onStopped(String reason);
    }

    static final long SESSION_MS = 10L * 60_000L;
    static final int MAX_WRONG_CODES = 8;

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final Context app;
    private final Listener listener;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final ServerSocket listenerSocket;
    private final String address;
    private final String code;
    private final long deadline;
    private volatile boolean running = true;
    private int wrongCodes;

    AmbientLocalUploadServer(Context context, Listener listener) throws IOException {
        this.app = context.getApplicationContext();
        this.listener = listener;
        Inet4Address ip = localPrivateIpv4();
        if (ip == null) throw new IOException("Connect the TV to a local Wi-Fi or Ethernet network");
        listenerSocket = new ServerSocket(0, 4, ip);
        listenerSocket.setSoTimeout(800);
        address = "http://" + ip.getHostAddress() + ":" + listenerSocket.getLocalPort() + "/";
        code = randomCode();
        deadline = SystemClock.elapsedRealtime() + SESSION_MS;
        io.execute(this::serve);
    }

    String address() { return address; }
    String accessCode() { return code; }
    boolean isRunning() { return running; }

    private static Inet4Address localPrivateIpv4() throws IOException {
        Inet4Address alternate = null;
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        if (interfaces == null) return null;
        while (interfaces.hasMoreElements()) {
            NetworkInterface network = interfaces.nextElement();
            if (!network.isUp() || network.isLoopback() || network.isVirtual()) continue;
            Enumeration<InetAddress> ips = network.getInetAddresses();
            while (ips.hasMoreElements()) {
                InetAddress a = ips.nextElement();
                if (!(a instanceof Inet4Address) || !a.isSiteLocalAddress()) continue;
                String name = network.getName().toLowerCase(Locale.ROOT);
                if (name.startsWith("wlan") || name.startsWith("eth")) return (Inet4Address) a;
                if (alternate == null) alternate = (Inet4Address) a;
            }
        }
        return alternate;
    }

    private static String randomCode() {
        SecureRandom secure = new SecureRandom();
        StringBuilder result = new StringBuilder();
        for (int n = 0; n < 8; n++)
            result.append(ALPHABET.charAt(secure.nextInt(ALPHABET.length())));
        return result.toString();
    }

    private void serve() {
        String reason = "Upload session ended";
        try {
            while (running) {
                if (SystemClock.elapsedRealtime() >= deadline) {
                    reason = "Upload session expired after 10 minutes";
                    break;
                }
                try (Socket socket = listenerSocket.accept()) {
                    socket.setSoTimeout(12_000);
                    socket.setTcpNoDelay(true);
                    serveOne(socket);
                    if (wrongCodes >= MAX_WRONG_CODES) {
                        reason = "Too many invalid access codes; reopen Wi-Fi upload";
                        break;
                    }
                } catch (SocketTimeoutException ignored) {
                    // Poll the expiry timer while there are no browser connections.
                } catch (IOException problem) {
                    if (running) Log.w("AmbientUpload", "Local upload request failed", problem);
                }
            }
        } catch (Exception e) {
            if (running) Log.w("AmbientUpload", "Upload server exited", e);
        } finally {
            shutdownSocket();
            String endReason = reason;
            ui.post(() -> listener.onStopped(endReason));
        }
    }

    private void serveOne(Socket socket) throws IOException {
        InputStream input = socket.getInputStream();
        OutputStream output = socket.getOutputStream();
        AmbientUploadProtocol.Request request;
        try {
            request = AmbientUploadProtocol.parse(input);
        } catch (IOException badRequest) {
            respond(output, 400, "Invalid request", false);
            return;
        }

        // Prevent DNS rebinding (or accidental traffic via a different host).
        String expectedHost = address.substring("http://".length(), address.length() - 1);
        if (!expectedHost.equals(request.host)) {
            respond(output, 403, "Incorrect host", false);
            return;
        }
        if ("GET".equals(request.method) && "/".equals(request.path)) {
            respond(output, 200, PAGE, true);
            return;
        }
        if (!"POST".equals(request.method) || !"/upload".equals(request.path)) {
            respond(output, 404, "Not found", false);
            return;
        }
        // Only requests originating in the same browser page; no cross-origin uploads.
        if (!("http://" + expectedHost).equals(request.origin)) {
            respond(output, 403, "Open the upload page on your TV's displayed address", false);
            return;
        }
        if (!AmbientUploadProtocol.constantTimeMatches(request.uploadCode, code)) {
            wrongCodes++;
            respond(output, 403, "Incorrect access code", false);
            return;
        }
        if (request.chunked || request.length < 12) {
            respond(output, 411, "A supported photo with Content-Length is required", false);
            return;
        }
        if (request.length > AmbientUploadProtocol.MAX_UPLOAD_BYTES) {
            respond(output, 413, "Photos must be 10 MB or smaller", false);
            return;
        }
        if (AmbientUploadProtocol.extension(request.contentType) == null) {
            respond(output, 415, "Choose a JPG, PNG or WebP photo", false);
            return;
        }
        try {
            AmbientPhotoRepository.importLocalStream(app, input, request.length, request.contentType);
            int count = AmbientPhotoRepository.photos(app).size();
            ui.post(() -> listener.onPhotoAdded(count));
            respond(output, 200, "Photo saved on the TV", false);
        } catch (IOException rejected) {
            respond(output, 422, rejected.getMessage(), false);
        }
    }

    private static void respond(OutputStream out, int status, String content,
                                boolean html) throws IOException {
        String label;
        switch (status) {
            case 200: label = "OK"; break;
            case 400: label = "Bad Request"; break;
            case 403: label = "Forbidden"; break;
            case 404: label = "Not Found"; break;
            case 411: label = "Length Required"; break;
            case 413: label = "Payload Too Large"; break;
            case 415: label = "Unsupported Media Type"; break;
            default: label = "Unprocessable Entity"; break;
        }
        byte[] body = content.getBytes(StandardCharsets.UTF_8);
        String headers = "HTTP/1.1 " + status + " " + label + "\r\n"
                + "Content-Type: " + (html ? "text/html" : "text/plain") + "; charset=utf-8\r\n"
                + "Content-Length: " + body.length + "\r\n"
                + "Cache-Control: no-store\r\n"
                + "Referrer-Policy: no-referrer\r\n"
                + "X-Content-Type-Options: nosniff\r\n"
                + "X-Frame-Options: DENY\r\n"
                + "Content-Security-Policy: default-src 'none'; style-src 'unsafe-inline'; "
                + "script-src 'unsafe-inline'; connect-src 'self'; form-action 'none'; frame-ancestors 'none'\r\n"
                + "Connection: close\r\n\r\n";
        out.write(headers.getBytes(StandardCharsets.US_ASCII));
        out.write(body);
        out.flush();
    }

    private void shutdownSocket() {
        running = false;
        try { listenerSocket.close(); } catch (IOException ignored) { }
    }

    @Override public void close() {
        shutdownSocket();
        io.shutdownNow();
    }

    private static final String PAGE = """
            <!doctype html>
            <html lang="en"><head><meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <title>Send photos to TV</title>
            <style>
              :root{color-scheme:dark;font-family:system-ui,Segoe UI,Arial,sans-serif}
              body{background:#0b0d13;color:#f5f6f9;margin:0;padding:32px 16px}
              main{margin:auto;max-width:520px}
              h1{font-size:30px;font-weight:500} p{color:#b9c2cd;line-height:1.5}
              input,button{box-sizing:border-box;width:100%;font-size:18px;border-radius:12px;padding:15px;margin:9px 0}
              input{color:#fff;background:#222733;border:1px solid #5d6573}
              input[type=file]{font-size:16px}
              button{background:#e6e9f1;color:#12151d;border:0;font-weight:650;cursor:pointer}
              button:disabled{opacity:.45}
              #status{white-space:pre-wrap;min-height:54px}
              small{color:#99a4b3}
            </style></head>
            <body><main>
            <h1>Send photos to your TV</h1>
            <p>Scanned the TV QR? The access code is already filled in. Your photos go directly to this TV, not a cloud.</p>
            <label for="code">Access code (auto-filled when scanned)</label>
            <input id="code" type="text" placeholder="8-character access code"
              maxlength="8" autocapitalize="characters" autocomplete="off" spellcheck="false">
            <label for="photos">Choose photos (JPG, PNG or WebP, up to 10 MB each)</label>
            <input id="photos" type="file" accept="image/jpeg,image/png,image/webp" multiple>
            <button id="send">Send photos</button>
            <p id="status" role="status" aria-live="polite"></p>
            <small>Up to 100 total photos. Keep the Wi-Fi upload screen open on your TV.
              The uploader shuts off after 10 minutes or when you close it.
              Use only on a trusted home network (local HTTP is not encrypted).</small>
            </main><script>
            const code = document.getElementById('code');
            const photos = document.getElementById('photos');
            const status = document.getElementById('status');
            const button = document.getElementById('send');
            // The camera's QR payload is /#code=XXXXXXXX. URL fragments never
            // reach the HTTP server; remove it from browser history immediately.
            const scanned = new URLSearchParams(location.hash.slice(1)).get('code');
            if (scanned && /^[A-Z2-9]{8}$/.test(scanned.toUpperCase())) {
              code.value = scanned.toUpperCase();
              status.textContent = 'QR connected. Choose photos and tap Send photos.';
            }
            if (location.hash) {
              try { history.replaceState(null, '', location.pathname); }
              catch (ignored) { /* Some embedded QR browsers disable history. */ }
            }
            button.addEventListener('click', async () => {
              const password = code.value.trim().toUpperCase();
              const selected = [...photos.files];
              if (password.length !== 8) { status.textContent = 'Enter the TV access code.'; return; }
              if (!selected.length) { status.textContent = 'Choose at least one photo.'; return; }
              button.disabled = true;
              let uploaded = 0;
              for (const file of selected) {
                const ext = file.name.toLowerCase().split('.').pop();
                const mime = ({jpg:'image/jpeg',jpeg:'image/jpeg',png:'image/png',webp:'image/webp'})[ext];
                if (!mime || file.size < 12 || file.size > 10*1024*1024) {
                  status.textContent += '\\nSkipped unsupported or oversized photo.';
                  continue;
                }
                status.textContent = 'Sending ' + (uploaded + 1) + ' of ' + selected.length + '…';
                try {
                  const response = await fetch('/upload', {
                    method:'POST', headers:{'X-Upload-Code':password,'Content-Type':mime},
                    body:file, cache:'no-store', credentials:'omit'
                  });
                  const message = await response.text();
                  if (!response.ok) { status.textContent = message; break; }
                  uploaded++;
                  status.textContent = uploaded + ' photo(s) saved on the TV.';
                } catch (error) {
                  status.textContent = 'Connection lost. Reopen Wi-Fi upload on the TV.';
                  break;
                }
              }
              button.disabled = false;
            });
            </script></body></html>
            """;
}
