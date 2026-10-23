package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Manually launched Concept B Ambient preview — never an Android HOME activity.
 *
 * On this TCL model there is no system Documents photo picker. Instead the
 * owner starts a short-lived local Wi-Fi upload session using the TV remote.
 * The server stops on exit/Back, expiry, or app lifecycle pause.
 */
public final class AmbientPhotoActivity extends Activity {
    private AmbientSlideshow slideshow;
    private AmbientInfoOverlay information;
    private LinearLayout emptyPanel;
    private TextView status;
    private TextView hint;
    private AlertDialog uploadDialog;
    private AlertDialog photoDialog;
    private Button managePhotos;
    private AmbientLocalUploadServer uploader;
    private TextView uploadStatus;
    private int uploadGeneration;
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
        information = new AmbientInfoOverlay(this);
        screen.addView(information, new FrameLayout.LayoutParams(
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
        status.setText("Add your own photos directly from your PC or phone on the same home Wi-Fi.");
        status.setTextSize(20);
        status.setTextColor(0xFFC8CFD9);
        status.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams copy = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        copy.topMargin = dp(18);
        emptyPanel.addView(status, copy);

        Button wifi = new Button(this);
        wifi.setText("Upload photos over Wi-Fi");
        wifi.setTextSize(22);
        wifi.setOnClickListener(v -> showUploader());
        LinearLayout.LayoutParams button = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        button.topMargin = dp(28);
        emptyPanel.addView(wifi, button);

        FrameLayout.LayoutParams center = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER);
        screen.addView(emptyPanel, center);

        hint = new TextView(this);
        hint.setText("Preview only  ·  OK: upload photos  ·  Back: exit");
        hint.setTextColor(0xFFE5E9F0);
        hint.setTextSize(17);
        hint.setPadding(dp(24), dp(15), dp(24), dp(15));
        hint.setBackgroundColor(0xAA0E1821);
        FrameLayout.LayoutParams bottom = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
        bottom.bottomMargin = dp(44);
        screen.addView(hint, bottom);

        // Always reachable with D-pad: photo management is an intentional action
        // separate from the automatic, non-interactive DreamService.
        managePhotos = new Button(this);
        managePhotos.setText("Manage photos");
        managePhotos.setTextSize(18);
        managePhotos.setOnClickListener(v -> showPhotoManager());
        FrameLayout.LayoutParams managePosition = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.RIGHT);
        managePosition.setMargins(dp(18), dp(18), dp(24), 0);
        screen.addView(managePhotos, managePosition);

        setContentView(screen);
        screen.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        updateEmptyState();
        if (emptyPanel.getVisibility() == View.VISIBLE) {
            wifi.requestFocus();
        } else {
            hint.postDelayed(() -> {
                if (!closed && uploader == null) hint.setVisibility(View.GONE);
            }, 8000);
        }
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private TextView infoLine(String message, int sp, int color) {
        TextView view = new TextView(this);
        view.setText(message);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(12), dp(8), dp(12), dp(8));
        // Selectable TextViews can capture D-pad focus and swallow the TV remote OK key.
        // The address and PIN are intended to be read from the TV, not selected.
        view.setTextIsSelectable(false);
        return view;
    }

    private void updateEmptyState() {
        boolean empty = AmbientPhotoRepository.photos(this).isEmpty();
        emptyPanel.setVisibility(empty ? View.VISIBLE : View.GONE);
        information.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (empty) information.stop();
        else if (hasWindowFocus()) information.start();
        hint.setVisibility(empty || uploader != null ? View.GONE : View.VISIBLE);
    }


    /** TV-remote gallery. Deletions are explicit, confirmed and app-private. */
    private void showPhotoManager() {
        if (closed || photoDialog != null || uploadDialog != null) return;
        List<File> files = AmbientPhotoRepository.photos(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(20), dp(8), dp(20), dp(8));

        if (files.isEmpty()) {
            list.addView(infoLine("No saved photos yet. Choose Add photos to start.", 19,
                    0xFFCDD4DF));
        }
        int index = 0;
        for (File file : files) {
            final String safeName = file.getName();
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(8), dp(7), dp(8), dp(7));

            ImageView preview = new ImageView(this);
            preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), options);
            int sample = 1;
            while (options.outWidth / sample > 280 || options.outHeight / sample > 170) {
                sample *= 2;
            }
            options.inJustDecodeBounds = false;
            options.inSampleSize = sample;
            preview.setImageBitmap(BitmapFactory.decodeFile(file.getAbsolutePath(), options));
            LinearLayout.LayoutParams imageSize = new LinearLayout.LayoutParams(
                    dp(170), dp(96));
            imageSize.rightMargin = dp(18);
            row.addView(preview, imageSize);

            TextView name = new TextView(this);
            name.setText("Photo " + (++index));
            name.setTextSize(19);
            name.setTextColor(0xFFFFFFFF);
            row.addView(name, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1));

            Button delete = new Button(this);
            delete.setText("Delete");
            delete.setContentDescription("Delete photo " + index);
            delete.setOnClickListener(v -> new AlertDialog.Builder(this)
                    .setTitle("Delete this photo?")
                    .setMessage("This removes the photo from the screensaver. It cannot be undone.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete photo", (dialog, which) -> {
                        boolean deleted = AmbientPhotoRepository.deletePhoto(this, safeName);
                        if (!deleted) Toast.makeText(this, "Could not delete photo",
                                Toast.LENGTH_SHORT).show();
                        else {
                            slideshow.refresh();
                            updateEmptyState();
                        }
                        refreshPhotoManager();
                    })
                    .show());
            row.addView(delete);
            list.addView(row);
        }

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(list);
        scroll.setLayoutParams(new ViewGroup.LayoutParams(dp(780), dp(460)));
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Screensaver photos (" + files.size() + ")")
                .setView(scroll)
                .setNegativeButton("Close", null)
                .setPositiveButton("Add photos over Wi-Fi", (dialog, which) -> showUploader());
        if (!files.isEmpty()) {
            builder.setNeutralButton("Delete all", (dialog, which) -> confirmDeleteAll());
        }
        photoDialog = builder.create();
        AlertDialog current = photoDialog;
        current.setOnDismissListener(dialog -> {
            if (photoDialog == current) photoDialog = null;
        });
        current.show();
    }

    private void refreshPhotoManager() {
        if (photoDialog != null) {
            AlertDialog previous = photoDialog;
            photoDialog = null;
            previous.dismiss();
        }
        showPhotoManager();
    }

    private void confirmDeleteAll() {
        // The gallery dialog closes when its neutral action is pressed.
        new AlertDialog.Builder(this)
                .setTitle("Delete every screensaver photo?")
                .setMessage("All locally stored screensaver photos will be removed. "
                        + "This cannot be undone. You can upload new photos afterwards.")
                .setNegativeButton("Cancel", (dialog, which) -> showPhotoManager())
                .setPositiveButton("Delete all photos", (dialog, which) -> {
                    int deleted = AmbientPhotoRepository.deleteAllPhotos(this);
                    slideshow.refresh();
                    updateEmptyState();
                    Toast.makeText(this, deleted + " photos deleted", Toast.LENGTH_SHORT).show();
                    showPhotoManager();
                }).show();
    }

    private void showUploader() {
        if (closed || uploadDialog != null) return;
        final int generation = ++uploadGeneration;
        try {
            uploader = new AmbientLocalUploadServer(this, new AmbientLocalUploadServer.Listener() {
                @Override public void onPhotoAdded(int count) {
                    if (closed || generation != uploadGeneration) return;
                    slideshow.refresh();
                    updateEmptyState();
                    if (uploadStatus != null) {
                        uploadStatus.setText(count + " photo(s) on TV. Add more from the browser,"
                                + " or press OK / Back on your remote to return to the slideshow.");
                    }
                }
                @Override public void onStopped(String reason) {
                    if (closed || generation != uploadGeneration) return;
                    if (uploadStatus != null) uploadStatus.setText(reason);
                }
            });
        } catch (IOException failure) {
            uploader = null;
            new AlertDialog.Builder(this)
                    .setTitle("Wi-Fi upload unavailable")
                    .setMessage("Connect this TV and your PC/phone to the same trusted home"
                            + " Wi-Fi or Ethernet network. Then try again.\n\n"
                            + failure.getMessage())
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        content.setPadding(dp(24), dp(12), dp(24), dp(14));

        content.addView(infoLine("On your PC or phone, open this address:", 20, 0xFFCDD4DF));
        TextView url = infoLine(uploader.address(), 27, 0xFFFFFFFF);
        url.setTypeface(android.graphics.Typeface.MONOSPACE);
        content.addView(url);
        content.addView(infoLine("Enter this 8-character code in the browser:", 19, 0xFFCDD4DF));
        TextView pin = infoLine(uploader.accessCode(), 35, 0xFFFFE9AE);
        pin.setTypeface(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD);
        content.addView(pin);

        uploadStatus = infoLine("Choose JPG, PNG or WebP photos in your browser."
                + " 10 MB each, up to 100 on this TV.", 19, 0xFFCFDAE6);
        content.addView(uploadStatus);
        content.addView(infoLine("Press OK or Back on the TV remote when finished.",
                20, 0xFFFFFFFF));
        content.addView(infoLine("Uploads work only on this local network. The address uses"
                + " unencrypted HTTP: use trusted home Wi-Fi only. Automatically closes after"
                + " 10 minutes.", 15, 0xFFB6BEC8));

        uploadDialog = new AlertDialog.Builder(this)
                .setTitle("Upload photos over Wi-Fi")
                .setView(content)
                .setNegativeButton("Done / Stop uploading", (dialog, which) -> stopUploader())
                .create();
        // Dialogs receive keys before their child view; otherwise D-pad OK may be
        // swallowed by the dialog's text or stay in the dialog rather than closing it.
        uploadDialog.setOnKeyListener((dialog, keyCode, event) -> {
            boolean closeKey = keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                    || keyCode == KeyEvent.KEYCODE_ENTER
                    || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER
                    || keyCode == KeyEvent.KEYCODE_BACK;
            if (!closeKey) return false;
            if (event.getAction() == KeyEvent.ACTION_UP) stopUploader();
            // Consume both key-down and key-up, so OK cannot reopen the uploader.
            return true;
        });
        uploadDialog.setOnDismissListener(dialog -> stopUploader());
        uploadDialog.show();
        uploadDialog.getButton(AlertDialog.BUTTON_NEGATIVE).requestFocus();
    }

    private void stopUploader() {
        // Safe for repeated dismiss callbacks and Activity.onPause.
        if (uploader == null && uploadDialog == null) return;
        uploadGeneration++;
        if (uploader != null) {
            uploader.close();
            uploader = null;
        }
        AlertDialog dialog = uploadDialog;
        uploadDialog = null;
        uploadStatus = null;
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
        if (!closed && slideshow != null) updateEmptyState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (slideshow != null) slideshow.start();
        if (information != null && !AmbientPhotoRepository.photos(this).isEmpty())
            information.start();
    }

    @Override
    protected void onPause() {
        if (photoDialog != null) {
            AlertDialog dialog = photoDialog;
            photoDialog = null;
            dialog.dismiss();
        }
        stopUploader(); // No background/photo upload listener after Home or another app.
        if (slideshow != null) slideshow.stop();
        if (information != null) information.stop();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        closed = true;
        stopUploader();
        if (slideshow != null) slideshow.close();
        if (information != null) information.close();
        super.onDestroy();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_UP && uploadDialog == null
                && photoDialog == null && getCurrentFocus() != managePhotos
                && emptyPanel.getVisibility() == View.GONE
                && (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER
                    || event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
            showUploader();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }
}
