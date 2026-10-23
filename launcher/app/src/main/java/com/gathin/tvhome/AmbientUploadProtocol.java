package com.gathin.tvhome;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Strict, small HTTP/1.1 request parser for the *temporary local-only* upload service. */
final class AmbientUploadProtocol {
    static final long MAX_UPLOAD_BYTES = 10L * 1024 * 1024;
    private static final int MAX_HEADER_BYTES = 8192;
    private AmbientUploadProtocol() { }

    static final class Request {
        final String method, path, host, origin, contentType, uploadCode, deleteConfirm;
        final long length;
        final boolean chunked;
        Request(String method, String path, Map<String, String> headers, long length) {
            this.method = method;
            this.path = path;
            this.host = headers.getOrDefault("host", "");
            this.origin = headers.getOrDefault("origin", "");
            this.contentType = headers.getOrDefault("content-type", "").split(";", 2)[0]
                    .trim().toLowerCase(Locale.ROOT);
            this.uploadCode = headers.getOrDefault("x-upload-code", "");
            this.deleteConfirm = headers.getOrDefault("x-delete-confirm", "");
            this.length = length;
            this.chunked = headers.containsKey("transfer-encoding");
        }
    }

    static Request parse(InputStream stream) throws IOException {
        int[] remaining = {MAX_HEADER_BYTES};
        String first = line(stream, remaining);
        if (first == null) throw new IOException("Empty request");
        String[] parts = first.split(" ", -1);
        if (parts.length != 3 || !parts[2].equals("HTTP/1.1")
                || !(parts[0].equals("GET") || parts[0].equals("POST"))
                || !(parts[1].equals("/") || parts[1].equals("/upload")
                    || parts[1].equals("/photos") || parts[1].equals("/delete-all")
                    || parts[1].startsWith("/thumb/")
                    || parts[1].startsWith("/delete/"))) {
            throw new IOException("Unsupported request line");
        }
        Map<String, String> headers = new HashMap<>();
        for (int i = 0; i < 40; i++) {
            String entry = line(stream, remaining);
            if (entry == null) throw new IOException("Truncated header");
            if (entry.isEmpty()) break;
            int separator = entry.indexOf(':');
            if (separator <= 0) throw new IOException("Malformed header");
            String name = entry.substring(0, separator).toLowerCase(Locale.ROOT);
            if (!name.matches("[a-z0-9-]+") || headers.containsKey(name))
                throw new IOException("Duplicate/invalid header");
            headers.put(name, entry.substring(separator + 1).trim());
            if (i == 39) throw new IOException("Too many headers");
        }
        if (headers.containsKey("expect")) throw new IOException("Expect unsupported");
        long length = -1;
        if (headers.containsKey("content-length")) {
            String raw = headers.get("content-length");
            if (!raw.matches("[0-9]{1,9}")) throw new IOException("Invalid Content-Length");
            try {
                length = Long.parseLong(raw);
            } catch (NumberFormatException e) {
                throw new IOException("Invalid Content-Length", e);
            }
        }
        return new Request(parts[0], parts[1], headers, length);
    }

    private static String line(InputStream in, int[] remaining) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int previous = -1;
        while (remaining[0]-- > 0) {
            int c = in.read();
            if (c == -1) return null;
            if (c == 0) throw new IOException("Null in header");
            if (c == '\n') {
                if (previous != '\r') throw new IOException("Expected CRLF");
                byte[] data = bytes.toByteArray();
                return new String(data, 0, data.length - 1, StandardCharsets.US_ASCII);
            }
            if (c < 32 && c != '\r' && c != '\t')
                throw new IOException("Control character in header");
            bytes.write(c);
            previous = c;
        }
        throw new IOException("Request header too large");
    }

    static String extension(String mime) {
        if ("image/jpeg".equals(mime)) return "jpg";
        if ("image/png".equals(mime)) return "png";
        if ("image/webp".equals(mime)) return "webp";
        return null;
    }

    static boolean constantTimeMatches(String supplied, String secret) {
        if (supplied == null || secret == null) return false;
        return MessageDigest.isEqual(
                supplied.getBytes(StandardCharsets.US_ASCII),
                secret.getBytes(StandardCharsets.US_ASCII));
    }

    static boolean permittedImageSignature(byte[] prefix, String mime) {
        if (prefix == null || prefix.length < 12) return false;
        if ("image/jpeg".equals(mime))
            return (prefix[0] & 255) == 0xFF && (prefix[1] & 255) == 0xD8
                    && (prefix[2] & 255) == 0xFF;
        if ("image/png".equals(mime))
            return (prefix[0] & 255) == 0x89 && prefix[1] == 'P'
                    && prefix[2] == 'N' && prefix[3] == 'G'
                    && (prefix[4] & 255) == 13 && (prefix[5] & 255) == 10
                    && (prefix[6] & 255) == 26 && (prefix[7] & 255) == 10;
        if ("image/webp".equals(mime))
            return prefix[0] == 'R' && prefix[1] == 'I' && prefix[2] == 'F'
                    && prefix[3] == 'F' && prefix[8] == 'W'
                    && prefix[9] == 'E' && prefix[10] == 'B' && prefix[11] == 'P';
        return false;
    }
}
