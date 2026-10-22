package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.SystemClock;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Standalone photo-only screensaver preview, deliberately separate from Home's wallpaper.
 * The repository accepts only the owner's imported photos. Images are decoded off-main-thread.
 */
final class AmbientSlideshow extends FrameLayout {
    private static final long PHOTO_INTERVAL_MS = 60_000L;
    private static final long FADE_MS = 1_000L;

    private final ExecutorService decoder = Executors.newSingleThreadExecutor();
    private final List<File> photos = new ArrayList<>();
    private ImageView foreground;
    private ImageView backgroundLayer;
    private Bitmap shown;
    private Bitmap arriving;
    private int index = -1;
    private long generation;
    private boolean active;
    private boolean destroyed;
    private boolean loading;
    private Runnable fadeFrame;
    private final Runnable nextPhoto = this::loadNext;

    AmbientSlideshow(Context context) {
        super(context);
        setBackgroundColor(0xFF000000);
        foreground = makeImage(context);
        backgroundLayer = makeImage(context);
        addView(foreground, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        addView(backgroundLayer, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private static ImageView makeImage(Context context) {
        ImageView image = new ImageView(context);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        return image;
    }

    void start() {
        if (active || destroyed) return;
        active = true;
        photos.clear();
        photos.addAll(AmbientPhotoRepository.photos(getContext()));
        index = -1;
        if (!photos.isEmpty()) loadNext();
    }

    void stop() {
        active = false;
        generation++;
        loading = false;
        removeCallbacks(nextPhoto);
        if (fadeFrame != null) {
            removeCallbacks(fadeFrame);
            fadeFrame = null;
        }
        foreground.setImageDrawable(null);
        backgroundLayer.setImageDrawable(null);
        if (arriving != null && arriving != shown) arriving.recycle();
        arriving = null;
        if (shown != null) shown.recycle();
        shown = null;
        foreground.setAlpha(1f);
        backgroundLayer.setAlpha(1f);
    }

    void refresh() {
        stop();
        start();
    }

    void close() {
        stop();
        destroyed = true;
        decoder.shutdownNow();
    }

    private void loadNext() {
        removeCallbacks(nextPhoto);
        if (!active || destroyed || loading || photos.isEmpty()) return;
        final int nextIndex = (index + 1) % photos.size();
        final File file = photos.get(nextIndex);
        final long token = ++generation;
        loading = true;
        decoder.execute(() -> {
            Bitmap decoded = decode(file);
            post(() -> {
                if (!active || destroyed || token != generation) {
                    if (decoded != null) decoded.recycle();
                    return;
                }
                loading = false;
                if (decoded == null) {
                    // A corrupt/removed file is never replaced with online or bundled art.
                    photos.remove(file);
                    if (photos.isEmpty()) return;
                    if (nextIndex <= index) index--;
                    loadNext();
                    return;
                }
                index = nextIndex;
                display(decoded);
            });
        });
    }

    private void display(Bitmap image) {
        arriving = image;
        backgroundLayer.bringToFront();
        backgroundLayer.setImageBitmap(image);
        if (shown == null) {
            backgroundLayer.setAlpha(1f);
            foreground.setImageDrawable(null);
            ImageView old = foreground;
            foreground = backgroundLayer;
            backgroundLayer = old;
            shown = image;
            arriving = null;
            schedule();
            return;
        }
        backgroundLayer.setAlpha(0f);
        final long start = SystemClock.uptimeMillis();
        fadeFrame = new Runnable() {
            @Override
            public void run() {
                if (!active || arriving != image) return;
                float fraction = Math.min(1f,
                        (SystemClock.uptimeMillis() - start) / (float) FADE_MS);
                float eased = fraction * fraction * (3f - 2f * fraction);
                backgroundLayer.setAlpha(eased);
                if (fraction < 1f) {
                    postOnAnimation(this);
                } else {
                    fadeFrame = null;
                    foreground.setImageDrawable(null);
                    Bitmap oldBitmap = shown;
                    shown = image;
                    arriving = null;
                    ImageView oldFront = foreground;
                    foreground = backgroundLayer;
                    backgroundLayer = oldFront;
                    if (oldBitmap != null) oldBitmap.recycle();
                    schedule();
                }
            }
        };
        postOnAnimation(fadeFrame);
    }

    private void schedule() {
        if (active && photos.size() > 1) postDelayed(nextPhoto, PHOTO_INTERVAL_MS);
    }

    private Bitmap decode(File file) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), options);
            if (options.outWidth <= 0 || options.outHeight <= 0) return null;
            int width = Math.max(1, getResources().getDisplayMetrics().widthPixels);
            int height = Math.max(1, getResources().getDisplayMetrics().heightPixels);
            int sample = WallpaperSize.sampleSize(
                    options.outWidth, options.outHeight, width, height);
            if (!WallpaperSize.accepted(options.outWidth / sample,
                    options.outHeight / sample)) return null;
            int[] scale = WallpaperSize.coverScale(
                    options.outWidth / sample, options.outHeight / sample, width, height);
            options.inJustDecodeBounds = false;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            options.inSampleSize = sample;
            if (scale != null) {
                options.inScaled = true;
                options.inDensity = scale[0];
                options.inTargetDensity = scale[1];
            }
            Bitmap decoded = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
            if (decoded == null) return null;
            Bitmap texture;
            try {
                texture = decoded.copy(Bitmap.Config.HARDWARE, false);
            } catch (Exception | OutOfMemoryError failure) {
                texture = null;
            }
            if (texture == null) return decoded;
            decoded.recycle();
            return texture;
        } catch (Exception | OutOfMemoryError failure) {
            android.util.Log.w("AmbientSlideshow", "Skipping unreadable local photo", failure);
            return null;
        }
    }
}
