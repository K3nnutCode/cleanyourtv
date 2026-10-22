package com.gathin.tvhome;

import org.junit.Test;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

/** Pure JVM validation for the TV's short-lived Wi-Fi upload HTTP entry point. */
public final class AmbientUploadProtocolTest {
    private static AmbientUploadProtocol.Request request(String http) throws IOException {
        return AmbientUploadProtocol.parse(new ByteArrayInputStream(
                http.getBytes(StandardCharsets.US_ASCII)));
    }

    @Test public void acceptsOnlyMinimalExpectedRequests() throws Exception {
        AmbientUploadProtocol.Request homepage = request(
                "GET / HTTP/1.1\r\nHost: 192.168.1.22:12345\r\n\r\n");
        assertEquals("GET", homepage.method);
        assertEquals("/", homepage.path);
        AmbientUploadProtocol.Request upload = request(
                "POST /upload HTTP/1.1\r\nHost: 192.168.1.22:12345\r\n"
                + "Origin: http://192.168.1.22:12345\r\n"
                + "Content-Type: image/jpeg\r\nContent-Length: 2048\r\n"
                + "X-Upload-Code: 56ZK239M\r\n\r\n");
        assertEquals(2048, upload.length);
        assertEquals("image/jpeg", upload.contentType);
        assertEquals("56ZK239M", upload.uploadCode);
        assertEquals("http://192.168.1.22:12345", upload.origin);
        assertFalse(upload.chunked);
    }

    @Test public void rejectsSmugglingAndUnwantedEndpoints() throws Exception {
        bad("POST /upload HTTP/1.1\r\nHost: a\r\nContent-Length: 5\r\nContent-Length: 1\r\n\r\n");
        bad("POST /upload HTTP/1.1\r\nHost: a\r\nContent-Length: 99,100\r\n\r\n");
        bad("POST /delete HTTP/1.1\r\nHost: a\r\n\r\n");
        bad("GET /../../photos HTTP/1.1\r\nHost: a\r\n\r\n");
        bad("PUT /upload HTTP/1.1\r\nHost: a\r\n\r\n");
        bad("GET / HTTP/1.0\r\nHost: a\r\n\r\n");
        bad("GET / HTTP/1.1\nHost: a\n\n");
        bad("GET / HTTP/1.1\r\nHost: a\r\nHost: b\r\n\r\n");
    }

    private static void bad(String request) throws Exception {
        try {
            request(request);
            fail("Expected rejected upload request");
        } catch (IOException expected) {
            // Intentionally rejected before handling body or writing any private files.
        }
    }

    @Test public void checksCodesAndMagicBytes() {
        assertTrue(AmbientUploadProtocol.constantTimeMatches("AB23CD45", "AB23CD45"));
        assertFalse(AmbientUploadProtocol.constantTimeMatches("AB23CD4X", "AB23CD45"));
        assertFalse(AmbientUploadProtocol.constantTimeMatches("", "AB23CD45"));
        assertEquals("jpg", AmbientUploadProtocol.extension("image/jpeg"));
        assertEquals("png", AmbientUploadProtocol.extension("image/png"));
        assertEquals("webp", AmbientUploadProtocol.extension("image/webp"));
        assertNull(AmbientUploadProtocol.extension("application/octet-stream"));
        assertTrue(AmbientUploadProtocol.permittedImageSignature(
                new byte[]{-1, -40, -1, -32, 0, 0, 0, 0, 0, 0, 0, 0}, "image/jpeg"));
        assertFalse(AmbientUploadProtocol.permittedImageSignature(
                new byte[]{'<', 's', 'c', 'r', 'i', 'p', 't', '>', 0, 0, 0, 0}, "image/jpeg"));
        assertTrue(AmbientUploadProtocol.permittedImageSignature(
                new byte[]{(byte)137, 'P','N','G',13,10,26,10,0,0,0,0}, "image/png"));
        assertTrue(AmbientUploadProtocol.permittedImageSignature(
                new byte[]{'R','I','F','F',2,0,0,0,'W','E','B','P'}, "image/webp"));
    }
}
