package com.gathin.tvhome;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.google.zxing.qrcode.QRCodeWriter;

import org.junit.Test;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class AmbientQrPairingTest {
    @Test public void pairingLinkKeepsSecretOutsideHttpRequest() throws Exception {
        String link = AmbientQrPairing.link("http://192.168.1.50:38911/", "ABCD2345");
        URI uri = new URI(link);
        assertEquals("http", uri.getScheme());
        assertEquals("/", uri.getPath());
        assertNull(uri.getQuery());
        assertEquals("code=ABCD2345", uri.getFragment());
        assertEquals("http://192.168.1.50:38911/", link.split("#", 2)[0]);
    }

    @Test public void rejectMalformedAccessCode() {
        try {
            AmbientQrPairing.link("http://192.168.1.50:38911/", "INVALID1");
            fail("invalid code allowed");
        } catch (IllegalArgumentException expected) { }
    }

    @Test public void qrCodeEncodesAReadableSessionLink() throws Exception {
        String payload = AmbientQrPairing.link("http://192.168.1.50:38911/", "ABCD2345");
        BitMatrix qr = new QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, 480, 480);
        int w = qr.getWidth();
        int h = qr.getHeight();
        int[] pixels = new int[w*h];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++)
            pixels[y*w+x] = qr.get(x, y) ? 0xFF000000 : 0xFFFFFFFF;
        RGBLuminanceSource source = new RGBLuminanceSource(w, h, pixels);
        String result = new QRCodeReader().decode(
                new BinaryBitmap(new HybridBinarizer(source))).getText();
        assertEquals(payload, result);
    }
}
