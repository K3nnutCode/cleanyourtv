package com.gathin.tvhome;

/** Session-only QR link builder. The code fragment is NOT part of the HTTP GET request. */
final class AmbientQrPairing {
    private AmbientQrPairing() { }

    static String link(String address, String code) {
        if (address == null || !address.startsWith("http://") || !address.endsWith("/")
                || code == null || !code.matches("[A-HJ-NP-Z2-9]{8}")) {
            throw new IllegalArgumentException("Invalid local upload session");
        }
        return address + "#code=" + code;
    }
}
