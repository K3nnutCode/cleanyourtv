package com.gathin.tvhome;

import android.service.dreams.DreamService;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * Concept B photo-only Android TV screensaver.
 *
 * Opt-in build only. The TV must select this service in its native screen saver
 * settings; simply installing the APK never changes system idle settings.
 *
 * Shares the same private photo folder, slideshow, weather and PDI prayer
 * overlay with AmbientPhotoActivity *within this application package*.
 * Never starts the LAN upload listener inside a screensaver.
 */
public final class AmbientPhotoDream extends DreamService {
    private AmbientSlideshow slideshow;
    private AmbientInfoOverlay information;

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false); // TV remote key dismisses the dream, not the uploader.
        setFullscreen(true);
        // Like the upstream ClockDream: don't insist on full panel brightness.
        setScreenBright(false);

        FrameLayout scene = new FrameLayout(this);
        scene.setBackgroundColor(0xFF000000);
        slideshow = new AmbientSlideshow(this);
        scene.addView(slideshow, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        information = new AmbientInfoOverlay(this);
        scene.addView(information, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(scene);
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        // Never silently substitute online/stock photos or display a blank slideshow.
        // Owner can import photos by opening the standalone preview app first.
        if (AmbientPhotoRepository.photos(this).isEmpty()) {
            finish();
            return;
        }
        if (slideshow != null) slideshow.start();
        if (information != null) information.start();
    }

    @Override
    public void onDreamingStopped() {
        if (slideshow != null) slideshow.stop();
        if (information != null) information.stop();
        super.onDreamingStopped();
    }

    @Override
    public void onDetachedFromWindow() {
        if (slideshow != null) {
            slideshow.close();
            slideshow = null;
        }
        if (information != null) {
            information.close();
            information = null;
        }
        super.onDetachedFromWindow();
    }
}
