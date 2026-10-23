package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.EnumMap;
import java.util.Map;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

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

        // The previous bottom instruction banner obscured the prayer-time panel.
        // Keep the photograph and prayer information entirely unobstructed.

        // Always reachable with D-pad: photo management is an intentional action
        // separate from the automatic, non-interactive DreamService.
        managePhotos = new Button(this);
        managePhotos.setText("Manage photos");
        managePhotos.setTextSize(18);
        managePhotos.setAllCaps(false);
        managePhotos.setTextColor(0xFFFFFFFF);
        managePhotos.setPadding(dp(22), dp(8), dp(22), dp(8));
        managePhotos.setBackground(focusBackground(0xC914202C, 0xFF243C56, true));
        managePhotos.setOnFocusChangeListener((view, focused) ->
                view.setScaleX(focused ? 1.05f : 1f));

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
            // A full-screen slideshow has no focusable photo tiles. Make
            // Manage Photos the focused TV remote target immediately.
            managePhotos.requestFocus();
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
    }


    /** Dark, TV-remote-friendly gallery. Only explicit, confirmed deletions. */
    private void showPhotoManager() {
        showPhotoManager(0);
    }

    private void showPhotoManager(int focusPosition) {
        if (closed || photoDialog != null || uploadDialog != null) return;
        List<File> files = AmbientPhotoRepository.photos(this);
        int count = files.size();

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(22), dp(12), dp(22), dp(20));
        content.setBackgroundColor(0xFF141B24);

        TextView subtitle = new TextView(this);
        subtitle.setText(count + (count == 1 ? " photo" : " photos")
                + "  •  Saved privately on this TV");
        subtitle.setTextColor(0xFFB9C6D7);
        subtitle.setTextSize(18);
        subtitle.setPadding(dp(8), 0, 0, dp(12));
        content.addView(subtitle);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setVerticalScrollBarEnabled(true);
        LinearLayout.LayoutParams scrollSize = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1);
        content.addView(scroll, scrollSize);

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(3);
        grid.setPadding(dp(4), dp(4), dp(4), dp(20));
        scroll.addView(grid, new ScrollView.LayoutParams(-1, -2));

        Button[] cardButtons = new Button[count];
        if (files.isEmpty()) {
            TextView empty = infoLine("No photos yet. Add photos over Wi-Fi to begin.",
                    20, 0xFFD3DFEC);
            GridLayout.LayoutParams emptyParams = new GridLayout.LayoutParams();
            emptyParams.columnSpec = GridLayout.spec(0, 3);
            emptyParams.width = dp(750);
            emptyParams.height = dp(175);
            grid.addView(empty, emptyParams);
        }
        for (int index = 0; index < count; index++) {
            File file = files.get(index);
            final String savedName = file.getName();
            final int cardIndex = index;
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(10), dp(10), dp(10), dp(10));
            card.setBackground(roundSurface(0xFF202B37, 14, 0xFF344252, 1));
            GridLayout.LayoutParams cardParams = new GridLayout.LayoutParams();
            cardParams.rowSpec = GridLayout.spec(index / 3);
            cardParams.columnSpec = GridLayout.spec(index % 3);
            cardParams.width = dp(228);
            cardParams.height = dp(205);
            cardParams.setMargins(dp(7), dp(7), dp(7), dp(7));
            grid.addView(card, cardParams);

            ImageView thumbnail = new ImageView(this);
            thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
            thumbnail.setBackground(roundSurface(0xFF303A48, 8, 0, 0));
            thumbnail.setClipToOutline(true);
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), bounds);
            int sample = 1;
            while (bounds.outWidth / sample > 360 || bounds.outHeight / sample > 240) {
                sample *= 2;
            }
            bounds.inJustDecodeBounds = false;
            bounds.inSampleSize = sample;
            thumbnail.setImageBitmap(BitmapFactory.decodeFile(file.getAbsolutePath(), bounds));
            card.addView(thumbnail, new LinearLayout.LayoutParams(-1, dp(120)));

            TextView caption = new TextView(this);
            caption.setText("Photo " + (index + 1));
            caption.setTextSize(17);
            caption.setTextColor(0xFFE8EEF5);
            caption.setPadding(dp(4), dp(6), 0, 0);
            card.addView(caption);

            Button remove = actionButton("Delete", true);
            remove.setContentDescription("Delete photo " + (index + 1));
            remove.setOnFocusChangeListener((view, focused) -> {
                card.setBackground(roundSurface(focused ? 0xFF263D50 : 0xFF202B37,
                        14, focused ? 0xFFB8E9FF : 0xFF344252, focused ? 3 : 1));
                card.setScaleX(focused ? 1.018f : 1f);
                card.setScaleY(focused ? 1.018f : 1f);
            });
            remove.setOnClickListener(v -> new AlertDialog.Builder(this)
                    .setTitle("Delete photo " + (cardIndex + 1) + "?")
                    .setMessage("This removes the photo from your screensaver. "
                            + "You cannot undo this.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete", (dialog, which) -> {
                        boolean deleted = AmbientPhotoRepository.deletePhoto(this, savedName);
                        if (!deleted) {
                            Toast.makeText(this, "Could not delete photo",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            slideshow.refresh();
                            updateEmptyState();
                        }
                        refreshPhotoManager(cardIndex);
                    })
                    .show());
            LinearLayout.LayoutParams removeSize = new LinearLayout.LayoutParams(-1, dp(43));
            removeSize.topMargin = dp(3);
            card.addView(remove, removeSize);
            cardButtons[index] = remove;
        }

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        actions.setPadding(dp(4), dp(16), dp(4), 0);
        content.addView(actions);

        Button add = actionButton("Add photos over Wi-Fi", false);
        actions.addView(add, new LinearLayout.LayoutParams(0, dp(57), 1));
        add.setOnClickListener(v -> {
            dismissPhotoManager();
            showUploader();
        });
        if (count > 0) {
            Button clear = actionButton("Delete all", true);
            LinearLayout.LayoutParams clearParams =
                    new LinearLayout.LayoutParams(dp(160), dp(57));
            clearParams.leftMargin = dp(12);
            actions.addView(clear, clearParams);
            clear.setOnClickListener(v -> confirmDeleteAll());
        }
        Button close = actionButton("Close", false);
        LinearLayout.LayoutParams closeParams =
                new LinearLayout.LayoutParams(dp(140), dp(57));
        closeParams.leftMargin = dp(12);
        actions.addView(close, closeParams);
        close.setOnClickListener(v -> dismissPhotoManager());

        photoDialog = new AlertDialog.Builder(this)
                .setTitle("Screensaver photo library")
                .setView(content)
                .create();
        AlertDialog current = photoDialog;
        current.setOnDismissListener(dialog -> {
            // During a deletion we immediately replace this dialog. The old
            // dismiss callback must not steal focus from the new photo card.
            if (photoDialog == current) {
                photoDialog = null;
                if (!closed) managePhotos.requestFocus();
            }
        });
        current.show();
        android.view.Window window = current.getWindow();
        if (window != null) {
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            int screenHeight = getResources().getDisplayMetrics().heightPixels;
            window.setLayout(Math.min(dp(840), Math.round(screenWidth * .94f)),
                    Math.min(dp(620), Math.round(screenHeight * .88f)));
        }
        if (count > 0) cardButtons[Math.min(Math.max(0, focusPosition), count - 1)].requestFocus();
        else add.requestFocus();
    }

    private GradientDrawable roundSurface(int fill, int cornerDp, int stroke, int strokeDp) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(fill);
        shape.setCornerRadius(dp(cornerDp));
        if (strokeDp > 0) shape.setStroke(dp(strokeDp), stroke);
        return shape;
    }

    private StateListDrawable focusBackground(int ordinary, int focused, boolean outline) {
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_focused},
                roundSurface(focused, 12, 0xFFB8E9FF, outline ? 3 : 2));
        states.addState(new int[]{android.R.attr.state_pressed},
                roundSurface(focused, 12, 0xFFB8E9FF, 2));
        states.addState(new int[]{}, roundSurface(ordinary, 12, 0xFF415263, 1));
        return states;
    }

    private Button actionButton(String title, boolean destructive) {
        Button button = new Button(this);
        button.setText(title);
        button.setAllCaps(false);
        button.setTextSize(16);
        button.setTextColor(0xFFFFFFFF);
        button.setPadding(dp(6), dp(1), dp(6), dp(1));
        button.setBackground(focusBackground(destructive ? 0xFF503039 : 0xFF304255,
                destructive ? 0xFF8E4551 : 0xFF376784, true));
        return button;
    }

    private void dismissPhotoManager() {
        AlertDialog dialog = photoDialog;
        photoDialog = null;
        if (dialog != null) dialog.dismiss();
    }

    private void refreshPhotoManager(int focusPosition) {
        dismissPhotoManager();
        showPhotoManager(focusPosition);
    }

    private void confirmDeleteAll() {
        new AlertDialog.Builder(this)
                .setTitle("Delete all screensaver photos?")
                .setMessage("This permanently removes the photos stored in this version "
                        + "of the screensaver. The action cannot be undone.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete all photos", (dialog, which) -> {
                    int deleted = AmbientPhotoRepository.deleteAllPhotos(this);
                    slideshow.refresh();
                    updateEmptyState();
                    Toast.makeText(this, deleted + " photos deleted", Toast.LENGTH_SHORT).show();
                    refreshPhotoManager(0);
                }).show();
    }

    /**
     * Encode a session URL entirely on-device. The code lives in a URL
     * fragment (not an HTTP request); the local upload page consumes it once.
     */
    private static Bitmap qrBitmap(String link) throws WriterException {
        int pixels = 480;
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.MARGIN, 2);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        BitMatrix bits = new QRCodeWriter().encode(
                link, BarcodeFormat.QR_CODE, pixels, pixels, hints);
        int width = bits.getWidth();
        int height = bits.getHeight();
        int[] rgba = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                rgba[y * width + x] = bits.get(x, y) ? 0xFF101820 : 0xFFFFFFFF;
            }
        }
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        result.setPixels(rgba, 0, width, 0, 0, width, height);
        return result;
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

        content.addView(infoLine("Scan with your phone camera", 25, 0xFFFFFFFF));
        content.addView(infoLine("Connect your phone to the same home Wi-Fi as the TV.",
                17, 0xFFD0DCE9));
        try {
            // QR carries the ephemeral upload URL and access code in its fragment.
            // The fragment is never transmitted in the browser's HTTP GET request.
            String pairingUrl = AmbientQrPairing.link(uploader.address(), uploader.accessCode());
            ImageView qr = new ImageView(this);
            qr.setImageBitmap(qrBitmap(pairingUrl));
            qr.setScaleType(ImageView.ScaleType.FIT_CENTER);
            qr.setBackgroundColor(0xFFFFFFFF);
            qr.setPadding(dp(12), dp(12), dp(12), dp(12));
            LinearLayout.LayoutParams qrParams = new LinearLayout.LayoutParams(
                    dp(285), dp(285));
            qrParams.gravity = Gravity.CENTER_HORIZONTAL;
            qrParams.topMargin = dp(8);
            qrParams.bottomMargin = dp(8);
            content.addView(qr, qrParams);
        } catch (WriterException failure) {
            // The manual URL and code remain visible if QR generation fails.
            android.util.Log.e("AmbientUpload", "Unable to create upload QR code", failure);
            content.addView(infoLine("QR unavailable. Use the address and code below.",
                    18, 0xFFFFE9AE));
        }
        content.addView(infoLine("Scan → choose photos → upload. No typing required.",
                18, 0xFFECF3FA));

        content.addView(infoLine("Manual fallback: address", 15, 0xFFAEBFCC));
        TextView url = infoLine(uploader.address(), 17, 0xFFE5EEF8);
        url.setTypeface(android.graphics.Typeface.MONOSPACE);
        content.addView(url);
        TextView pin = infoLine("Access code: " + uploader.accessCode(), 19, 0xFFFFE9AE);
        pin.setTypeface(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD);
        content.addView(pin);

        uploadStatus = infoLine("JPG, PNG or WebP • 10 MB each • Up to 100 photos",
                17, 0xFFCFDAE6);
        content.addView(uploadStatus);
        content.addView(infoLine("Press OK or Back on the TV remote when finished.",
                17, 0xFFFFFFFF));
        content.addView(infoLine("Trusted home network only • Local HTTP is not encrypted"
                + " • Session closes after 10 minutes.", 14, 0xFFB6BEC8));

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
        android.view.Window window = uploadDialog.getWindow();
        if (window != null) {
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            int screenHeight = getResources().getDisplayMetrics().heightPixels;
            window.setLayout(Math.min(dp(660), Math.round(screenWidth * .90f)),
                    Math.min(dp(850), Math.round(screenHeight * .92f)));
        }
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
        if (!closed && slideshow != null) {
            updateEmptyState();
            // Closing the upload modal returns the D-pad to Manage Photos.
            if (managePhotos != null) managePhotos.requestFocus();
        }
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
        // Remote shortcut when the slideshow occupies the screen:
        // open the photo gallery, never the uploader. Empty collections keep
        // their original focused Wi-Fi upload button.
        if (event.getAction() == KeyEvent.ACTION_UP && uploadDialog == null
                && photoDialog == null && emptyPanel.getVisibility() == View.GONE
                && (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER
                    || event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
            showPhotoManager();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }
}
