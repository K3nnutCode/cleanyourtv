# Ambient Concept B v3 — Local Wi-Fi Photo Import

**Status:** Prototype feature, pending physical TCL browser-upload validation.
The original CleanYourTV Home dock stays untouched, and the independent v3
application is **not** a HOME launcher or Android DreamService.

## How to use (trusted home network only)

1. Install the **v3 side-by-side preview APK** from the passing
   [Ambient Concept B v3 workflow](https://github.com/K3nnutCode/cleanyourtv/actions/workflows/ambient-concept-b-v3.yml).
   The package is `com.k3nnut.tvhome.ambient.preview.v3`, separate from v2.
2. Launch the preview using the TCL's app tile or explicit ADB launch.
3. With an empty photo set, focus **Upload photos over Wi-Fi** and press OK.
   With existing photos, press OK once to open the uploader dialog.
4. The TV displays a temporary private IPv4 address with an ephemeral port
   (for example `http://192.168.1.100:45329/`) and an eight-character access
   code. **Use the address and code currently shown, not this example.**
5. On a PC or phone **on the same trusted home network**, open that exact URL
   in a browser. Type the TV code, choose JPG/PNG/WebP photos up to 10 MB each,
   and click **Send photos**. The browser sends each file directly to this TV.
6. The TV should report updated photo count and show the newest collection
   while retaining Concept B time/weather/prayers. Press **Stop uploading**
   on TV to close the port, or leave it to expire after 10 minutes.
7. Add no more than 100 photos total. No file manager, Internet cloud account,
   third-party application, port forwarding, or ADB file copy is required.
   Internet is still required separately for optional Open-Meteo weather.

## Privacy and security details

- Photos are uploaded only from the local browser to the TV. They are
  stored in **app-private internal storage**, never fetched by the server.
- No download/photo-list endpoint exists. Only `GET /` serves the static
  upload page and `POST /upload` can add a file.
- The HTTP listener binds only to a detected private IPv4 Wi-Fi/Ethernet
  interface. It starts ONLY when the user opens the uploader dialog.
- Eight-character randomly generated access code, constant-time comparison,
  and eight-attempt invalid-code cutoff. Access code is not embedded in the
  URL or persisted; it is typed into the browser and submitted only to the TV.
- Server accepts strict HTTP/1.1 framing with at most 8 KiB headers, no
  duplicate headers, no chunked uploads, a required Content-Length, strict
  same-origin POST, and validated actual image signatures and dimensions.
- Each image <=10 MB. Total collection <=100. Imported images use generated
  local filenames. Temporary partial files are removed after failed uploads.
- The server uses **unencrypted local HTTP**, not HTTPS. It is suitable only
  for your own trusted home LAN; anyone able to sniff that LAN could potentially
  intercept photo bytes or the access code. Don't use it on shared/guest/public
  Wi-Fi, a Wi-Fi with untrusted clients, or while routing traffic through a VPN.
  It is not marketed as a fully encrypted photo-transfer method.
- The session shuts off on Stop, activity pause/exit, too many bad codes, or
  10-minute expiry. Never leave the port exposed in the background.
- Do not forward the displayed port through your router or place the TV in
  a publicly accessible network segment.
- Uninstalling the v3 preview deletes its own imported photos. The photos
  already stored in v2 remain untouched, because v3 is another app package.

## Testing on the TCL

- Verify TV Home remains the Google/TCL original.
- After launching v3, click upload and note the temporary address and code.
- Open the URL from your Windows PC (do not share URL/code in a public screenshot).
- Import two new photos, verify count and slideshow transition after ~60sec.
- Confirm weather/prayers still render independently of the upload.
- Press Stop on TV and refresh the browser page: it should refuse to connect.
- Exit with Back then Home; no launcher settings or system packages altered.

## Outstanding validation

- The v3 Wi-Fi upload UI and request path need **a first real TCL test**.
  A green Gradle test proves compilation and protocol helpers, but does NOT
  prove a phone/PC can reach the TV on your particular network.
- Security review and better code/UI tests are needed before considering the
  photo manager production-ready. The actual selectable Android `DreamService`
  integration remains a separate later milestone.
