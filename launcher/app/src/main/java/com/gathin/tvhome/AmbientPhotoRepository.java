package com.gathin.tvhome;

import android.content.Context;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Local, explicit photo collection for the experimental Ambient preview.
 *
 * User-selected images are copied to app-specific storage. No network requests,
 * MediaStore sweep, bundled photos, or cloud photo collection. Android removes
 * these copies when this PREVIEW application is uninstalled.
 */
final class AmbientPhotoRepository {
    private static final int MAX_PHOTOS = 100;
    private static final long MAX_BYTES = 10L * 1024 * 1024;

    private AmbientPhotoRepository() { }

    static File directory(Context context) {
        File external = context.getExternalFilesDir("ambient_photos");
        File result = external != null ? external : new File(context.getFilesDir(), "ambient_photos");
        if (!result.isDirectory() && !result.mkdirs() && !result.isDirectory()) {
            throw new IllegalStateException("Unable to create Ambient photo directory");
        }
        return result;
    }

    static List<File> photos(Context context) {
        return WallpaperRotation.images(directory(context));
    }

    /** Copies approved picker URIs into this app's own directory. Called from a background executor. */
    static int importSelected(Context context, List<Uri> uris) {
        File destination = directory(context);
        int remaining = Math.max(0, MAX_PHOTOS - WallpaperRotation.images(destination).size());
        int imported = 0;
        if (uris == null) return 0;
        for (Uri uri : uris) {
            if (remaining == 0) break;
            String extension = extension(context, uri);
            if (extension == null) continue;
            File temporary = null;
            try {
                temporary = File.createTempFile("ambient-", ".partial", destination);
                long total = 0;
                try (InputStream in = context.getContentResolver().openInputStream(uri);
                     FileOutputStream out = new FileOutputStream(temporary)) {
                    if (in == null) throw new IOException("Image provider returned no data");
                    byte[] buffer = new byte[32768];
                    int count;
                    while ((count = in.read(buffer)) != -1) {
                        total += count;
                        if (total > MAX_BYTES) throw new IOException("Photo over 10 MB limit");
                        out.write(buffer, 0, count);
                    }
                }
                if (total == 0) continue;
                File target = uniqueFile(destination, extension);
                if (!temporary.renameTo(target)) throw new IOException("Could not save photo");
                temporary = null;
                remaining--;
                imported++;
            } catch (Exception failure) {
                android.util.Log.w("AmbientPhotos", "Skipping photo that could not be imported", failure);
            } finally {
                if (temporary != null) temporary.delete();
            }
        }
        return imported;
    }

    private static File uniqueFile(File destination, String extension) throws IOException {
        for (int i = 0; i < 1000; i++) {
            File file = new File(destination, "photo-" + System.currentTimeMillis()
                    + "-" + i + "." + extension);
            if (!file.exists()) return file;
        }
        throw new IOException("Unable to allocate a unique image filename");
    }

    private static String extension(Context context, Uri uri) {
        String mime = context.getContentResolver().getType(uri);
        if (mime == null) return null; // no silent assumptions about unknown files
        switch (mime.toLowerCase(Locale.ROOT)) {
            case "image/jpeg": return "jpg";
            case "image/png": return "png";
            case "image/webp": return "webp";
            default: return null;
        }
    }
}
