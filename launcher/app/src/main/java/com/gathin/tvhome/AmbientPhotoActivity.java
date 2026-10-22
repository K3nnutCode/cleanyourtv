package com.gathin.tvhome;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manual, standalone preview of the Ambient photos feature.
 *
 * Only enabled in -PambientPreview=true builds, whose regular Home activity
 * is disabled. Not a DreamService yet: real system screensaver hookup is a
 * separate phase requiring owner approval and TCL validation.
 */
public final class AmbientPhotoActivity extends Activity {
    private static final int PICK_PHOTOS = 41;
    private final ExecutorService importer = Executors.newSingleThreadExecutor();
    private AmbientSlideshow slideshow;
    private LinearLayout emptyPanel;
    private TextView status;
    private TextView hint;
    private boolean closed;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        FrameLayout screen = new FrameLayout(this);
        screen.setBackgroundColor(0xFF000000);
        screen.setFocusableInTouchMode(true);
        slideshow = new AmbientSlideshow(this);
        screen.addView(slideshow, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        emptyPanel = new LinearLayout(this);
        emptyPanel.setOrientation(LinearLayout.VERTICAL);
        emptyPanel.setGravity(Gravity.CENTER);
        emptyPanel.setPadding(dp(40), dp(40), dp(40), dp(40));

        TextView heading = new TextView(this);
        heading.setText("Your photos, your screensaver");
        heading.setTextSize(34);
        heading.setTextColor(0xFFF4F6F8);
        heading.setGravity(Gravity.CENTER);
        emptyPanel.addView(heading);

        status = new TextView(this);
        status.setText("Choose your photos to begin. No stock photos or online content.");
        status.setTextSize(20);
        status.setTextColor(0xFFC8CFD9);
        status.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams copy = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        copy.topMargin = dp(18);
        emptyPanel.addView(status, copy);

        Button pick = new Button(this);
        pick.setText("Choose local photos");
        pick.setTextSize(20);
        pick.setOnClickListener(v -> openPicker());
        LinearLayout.LayoutParams button = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        button.topMargin = dp(28);
        emptyPanel.addView(pick, button);

        FrameLayout.LayoutParams center = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        screen.addView(emptyPanel, center);

        hint = new TextView(this);
        hint.setText("Preview only  ·  OK: add photos  ·  Back: exit");
        hint.setTextColor(0xFFE5E9F0);
        hint.setTextSize(17);
        hint.setPadding(dp(24), dp(15), dp(24), dp(15));
        hint.setBackgroundColor(0xAA0E1821);
        FrameLayout.LayoutParams bottom = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
        bottom.bottomMargin = dp(44);
        screen.addView(hint, bottom);

        setContentView(screen);
        screen.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        updateEmptyState();
        if (emptyPanel.getVisibility() == View.VISIBLE) {
            pick.requestFocus();
        } else {
            hint.postDelayed(() -> {
                if (!closed) hint.setVisibility(View.GONE);
            }, 8000L);
        }
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void updateEmptyState() {
        boolean empty = AmbientPhotoRepository.photos(this).isEmpty();
        emptyPanel.setVisibility(empty ? View.VISIBLE : View.GONE);
        hint.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void openPicker() {
        Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        picker.setType("image/*");
        picker.addCategory(Intent.CATEGORY_OPENABLE);
        picker.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        picker.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivityForResult(picker, PICK_PHOTOS);
        } catch (ActivityNotFoundException noPicker) {
            status.setText("This TV has no compatible photo picker. Photos can also be copied"
                    + " into this preview app's ambient_photos folder over ADB.");
            emptyPanel.setVisibility(View.VISIBLE);
            hint.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_PHOTOS || resultCode != RESULT_OK || data == null) return;
        List<Uri> selected = new ArrayList<>();
        ClipData clip = data.getClipData();
        if (clip != null) {
            for (int i = 0; i < clip.getItemCount() && i < 100; i++) {
                selected.add(clip.getItemAt(i).getUri());
            }
        } else if (data.getData() != null) {
            selected.add(data.getData());
        }
        if (selected.isEmpty()) return;
        status.setText("Importing selected photos…");
        emptyPanel.setVisibility(View.VISIBLE);
        hint.setVisibility(View.GONE);
        importer.execute(() -> {
            int count = AmbientPhotoRepository.importSelected(this, selected);
            runOnUiThread(() -> {
                if (closed) return;
                slideshow.refresh();
                updateEmptyState();
                if (emptyPanel.getVisibility() == View.VISIBLE) {
                    status.setText("No supported photo could be imported. Choose JPG, PNG or WebP,"
                            + " up to 10 MB each.");
                } else {
                    hint.setText(count + " photo(s) imported  ·  OK: add more  ·  Back: exit");
                    hint.postDelayed(() -> {
                        if (!closed) hint.setVisibility(View.GONE);
                    }, 8000L);
                }
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (slideshow != null) slideshow.start();
    }

    @Override
    protected void onPause() {
        if (slideshow != null) slideshow.stop();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        closed = true;
        if (slideshow != null) slideshow.close();
        importer.shutdownNow();
        super.onDestroy();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_UP
                && emptyPanel.getVisibility() == View.GONE
                && (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER
                    || event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
            openPicker();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }
}
