package com.gathin.tvhome;

/** Session-only QR link builder. The code fragment is NOT part of the HTTP GET request. */
final class AmbientQrPairing {
    private AmbientQrPairing() { }

    /** Keep the square code fully inside the phone-scan dialog on smaller TVs. */
    static int recommendedQrSizePx(int screenHeightPx, float density) {
        int min = Math.round(150f * density);
        int max = Math.round(215f * density);
        return Math.min(max, Math.max(min, Math.round(screenHeightPx * 0.35f)));
    }

    static String link(String address, String code) {
        if (address == null || !address.startsWith("http://") || !address.endsWith("/")
                || code == null || !code.matches("[A-HJ-NP-Z2-9]{8}")) {
            throw new IllegalArgumentException("Invalid local upload session");
        }
        return address + "#code=" + code;
    }
}
