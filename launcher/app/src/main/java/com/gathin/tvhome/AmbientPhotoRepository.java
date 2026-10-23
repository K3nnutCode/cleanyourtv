package com.gathin.tvhome;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
        // App-private storage is reliable on Android TV 11 and cannot be read by
        // unrelated apps. SAF imports copy selected bytes here; no storage grants.
        File result = new File(context.getFilesDir(), "ambient_photos");
        if (!result.isDirectory() && !result.mkdirs() && !result.isDirectory()) {
            throw new IllegalStateException("Unable to create Ambient photo directory");
        }
        return result;
    }

    static List<File> photos(Context context) {
        return WallpaperRotation.images(directory(context));
    }

    /**
     * Delete only a photo that is currently in this app's private collection.
     * Do not accept arbitrary paths, URIs or names from a caller.
     */
    static synchronized boolean deletePhoto(Context context, String name) {
        if (name == null || name.isEmpty() || name.contains("/") || name.contains("\\"))
            return false;
        for (File image : photos(context)) {
            if (image.getName().equals(name)) return image.delete();
        }
        return false;
    }

    /** Explicit user-confirmed bulk delete. Never removes the directory or preferences. */
    static synchronized int deleteAllPhotos(Context context) {
        int deleted = 0;
        for (File image : photos(context)) {
            if (image.delete()) deleted++;
        }
        return deleted;
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


    /**
     * Import a single browser-uploaded image. Network handler supplies a strict length,
     * known MIME type and a bounded InputStream. Filename and EXIF are never served.
     * Storage and validation are performed on the uploader worker, never on Android's UI.
     */
    static void importLocalStream(Context context, InputStream input, long length,
                                  String mime) throws IOException {
        String extension = AmbientUploadProtocol.extension(mime);
        if (extension == null || length < 12
                || length > AmbientUploadProtocol.MAX_UPLOAD_BYTES) {
            throw new IOException("Unsupported photo type or size (maximum 10 MB)");
        }
        File destination = directory(context);
        if (photos(context).size() >= MAX_PHOTOS)
            throw new IOException("100-photo limit reached");

        File partial = File.createTempFile("local-", ".partial", destination);
        try {
            byte[] buffer = new byte[32768];
            byte[] signature = new byte[12];
            int signatureBytes = 0;
            long remaining = length;
            try (FileOutputStream output = new FileOutputStream(partial)) {
                while (remaining > 0) {
                    int read = input.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                    if (read < 0) throw new IOException("Incomplete photo upload");
                    if (read == 0) continue;
                    int copy = Math.min(signature.length - signatureBytes, read);
                    if (copy > 0) {
                        System.arraycopy(buffer, 0, signature, signatureBytes, copy);
                        signatureBytes += copy;
                    }
                    output.write(buffer, 0, read);
                    remaining -= read;
                }
            }
            if (!AmbientUploadProtocol.permittedImageSignature(signature, mime))
                throw new IOException("File content does not match its image type");
            android.graphics.BitmapFactory.Options bounds =
                    new android.graphics.BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            android.graphics.BitmapFactory.decodeFile(partial.getAbsolutePath(), bounds);
            long pixels = (long) bounds.outWidth * bounds.outHeight;
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0 || pixels > 80_000_000L
                    || bounds.outWidth > 12000 || bounds.outHeight > 12000) {
                throw new IOException("Damaged or oversized image dimensions");
            }
            File finalPhoto = uniqueFile(destination, extension);
            if (!partial.renameTo(finalPhoto)) throw new IOException("Could not save photo");
        } finally {
            if (partial.exists()) partial.delete();
        }
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
