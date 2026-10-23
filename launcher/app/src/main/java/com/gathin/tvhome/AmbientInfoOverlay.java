package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Locale;

/**
 * Concept B information overlay for the manual Ambient photo preview.
 * Clock/date top-left, area-level weather top-right, five daily prayers below.
 * Clear photograph center; no playback, dock, recommendations or app artwork.
 */
final class AmbientInfoOverlay extends FrameLayout {
    private static final String ZONE = "Asia/Brunei";
    private static final int[] PRAYER_INDEXES = {0, 2, 3, 4, 5};
    private static final String[] NAMES = {"Subuh", "Zohor", "Asar", "Maghrib", "Isyak"};
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final TextView temperature;
    private final TextView weatherCondition;
    private final TextView weatherStatus;
    private final TextView prayerStatus;
    private final LinearLayout prayerRow;
    private final TextView[] names = new TextView[5];
    private final TextView[] times = new TextView[5];
    private final LinearLayout[] prayerCells = new LinearLayout[5];
    private final AmbientWeatherClient weather;
    private boolean active;
    private final Runnable minute = new Runnable() {
        @Override public void run() {
            if (!active) return;
            updatePrayers();
            ui.postDelayed(this, 60_000L);
        }
    };

    AmbientInfoOverlay(Context context) {
        super(context);
        setFocusable(false);
        setClickable(false);
        float scale = Math.max(0.68f,
                context.getResources().getDisplayMetrics().widthPixels / 1920f);

        // Only the top and bottom of the photo are shaded for typography.
        View topShade = new View(context);
        topShade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xCE071018, 0x61071016, 0x00071016}));
        LayoutParams shadeTop = new LayoutParams(-1, size(390, scale), Gravity.TOP);
        addView(topShade, shadeTop);

        View bottomShade = new View(context);
        bottomShade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x00071016, 0xB2081018, 0xF6081018}));
        LayoutParams shadeBottom = new LayoutParams(-1, size(370, scale), Gravity.BOTTOM);
        addView(bottomShade, shadeBottom);

        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.TOP);
        LayoutParams headerParams = new LayoutParams(-1, -2, Gravity.TOP);
        headerParams.setMargins(size(104, scale), size(68, scale),
                size(104, scale), 0);
        addView(header, headerParams);

        LinearLayout timeColumn = new LinearLayout(context);
        timeColumn.setOrientation(LinearLayout.VERTICAL);
        timeColumn.setGravity(Gravity.START);
        header.addView(timeColumn, new LinearLayout.LayoutParams(0, -2, 1));

        TextClock date = new TextClock(context);
        date.setTimeZone(ZONE);
        date.setFormat12Hour("EEEE, d MMMM yyyy");
        date.setFormat24Hour("EEEE, d MMMM yyyy");
        date.setTextColor(0xFFF2F5FA);
        date.setShadowLayer(3f, 0f, 2f, 0xAA000000);
        date.setTextSize(TypedValue.COMPLEX_UNIT_PX, size(27, scale));
        date.setLetterSpacing(0.03f);
        timeColumn.addView(date);

        TextClock clock = new TextClock(context);
        clock.setTimeZone(ZONE);
        clock.setFormat12Hour("h:mm a");
        clock.setFormat24Hour("HH:mm");
        clock.setTextColor(Color.WHITE);
        clock.setShadowLayer(5f, 0f, 3f, 0xB8000000);
        clock.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
        clock.setTextSize(TypedValue.COMPLEX_UNIT_PX, size(146, scale));
        clock.setIncludeFontPadding(false);
        LinearLayout.LayoutParams clockParams = new LinearLayout.LayoutParams(-2, -2);
        clockParams.topMargin = size(15, scale);
        timeColumn.addView(clock, clockParams);

        TextView area = label(context, "BRUNEI DARUSSALAM", 22, 0xE1EDF3F9, scale);
        area.setLetterSpacing(0.16f);
        timeColumn.addView(area);

        LinearLayout conditions = new LinearLayout(context);
        conditions.setOrientation(LinearLayout.VERTICAL);
        conditions.setGravity(Gravity.END);
        header.addView(conditions, new LinearLayout.LayoutParams(0, -2, 1));

        temperature = label(context, "--°", 88, Color.WHITE, scale);
        temperature.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        temperature.setGravity(Gravity.END);
        conditions.addView(temperature);
        weatherCondition = label(context, "Weather unavailable", 29, 0xFFF0F4F7, scale);
        weatherCondition.setGravity(Gravity.END);
        conditions.addView(weatherCondition);
        TextView city = label(context, "BANDAR SERI BEGAWAN", 20, 0xE2DAE3EB, scale);
        city.setGravity(Gravity.END);
        city.setLetterSpacing(0.1f);
        conditions.addView(city);
        weatherStatus = label(context, "Open-Meteo · Brunei-Muara", 17, 0xC2E6EBEE, scale);
        weatherStatus.setGravity(Gravity.END);
        LinearLayout.LayoutParams metadata = new LinearLayout.LayoutParams(-2, -2);
        metadata.topMargin = size(9, scale);
        conditions.addView(weatherStatus, metadata);

        LinearLayout footer = new LinearLayout(context);
        footer.setOrientation(LinearLayout.VERTICAL);
        LayoutParams footerParams = new LayoutParams(-1, -2, Gravity.BOTTOM);
        footerParams.setMargins(size(104, scale), 0,
                size(104, scale), size(42, scale));
        addView(footer, footerParams);

        LinearLayout footerHead = new LinearLayout(context);
        footerHead.setOrientation(LinearLayout.HORIZONTAL);
        footerHead.setGravity(Gravity.CENTER_VERTICAL);
        footer.addView(footerHead, new LinearLayout.LayoutParams(-1, size(54, scale)));
        TextView title = label(context, "WAKTU SOLAT · BRUNEI-MUARA", 22, 0xFFE6EDF3, scale);
        title.setTypeface(null, Typeface.BOLD);
        title.setLetterSpacing(0.1f);
        footerHead.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        prayerStatus = label(context, "PDI 2026 · Offline", 19, 0xFFE3D6B3, scale);
        prayerStatus.setGravity(Gravity.END);
        footerHead.addView(prayerStatus);

        View rule = new View(context);
        rule.setBackgroundColor(0x4DFFFFFF);
        footer.addView(rule, new LinearLayout.LayoutParams(-1, Math.max(1, size(1, scale))));

        prayerRow = new LinearLayout(context);
        prayerRow.setOrientation(LinearLayout.HORIZONTAL);
        prayerRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams prayerParams = new LinearLayout.LayoutParams(-1, size(123, scale));
        prayerParams.topMargin = size(10, scale);
        footer.addView(prayerRow, prayerParams);

        for (int i = 0; i < 5; i++) {
            LinearLayout cell = new LinearLayout(context);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
            cell.setPadding(size(16, scale), 0, size(8, scale), 0);
            LinearLayout.LayoutParams cellParams = new LinearLayout.LayoutParams(0, -1, 1);
            cellParams.rightMargin = i < 4 ? size(7, scale) : 0;
            prayerRow.addView(cell, cellParams);
            prayerCells[i] = cell;
            names[i] = label(context, NAMES[i], 23, 0xDAE8ECF3, scale);
            times[i] = label(context, "–:––", 47, Color.WHITE, scale);
            times[i].setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
            cell.addView(names[i]);
            cell.addView(times[i]);
        }

        weather = new AmbientWeatherClient(context, (temp, condition, provenance) -> {
            temperature.setText(temp);
            weatherCondition.setText(condition);
            weatherStatus.setText(provenance);
        });
        updatePrayers();
    }

    private static TextView label(Context context, String text, int px, int color, float scale) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX, size(px, scale));
        view.setTextColor(color);
        view.setShadowLayer(2.5f, 0f, 1.5f, 0x9A000000);
        view.setIncludeFontPadding(false);
        view.setSingleLine(true);
        view.setFocusable(false);
        return view;
    }

    private static int size(float px, float scale) {
        return Math.round(px * scale);
    }

    void start() {
        if (active) return;
        active = true;
        updatePrayers();
        weather.start();
        ui.postDelayed(minute, 60_000L);
    }

    void stop() {
        active = false;
        ui.removeCallbacks(minute);
        weather.stop();
    }

    void close() {
        stop();
        weather.close();
    }

    static int nextPrayer(int[] today, int minutesNow) {
        if (today == null || today.length != 6) return -1;
        for (int i = 0; i < PRAYER_INDEXES.length; i++) {
            if (minutesNow < today[PRAYER_INDEXES[i]]) return i;
        }
        return -1;
    }

    /** Only highlight tomorrow when its date-specific timetable actually exists. */
    static boolean nextIsTomorrowSubuh(int[] today, int[] tomorrow, int minutesNow) {
        return today != null && today.length == 6
                && tomorrow != null && tomorrow.length == 6
                && nextPrayer(today, minutesNow) == -1;
    }

    private void updatePrayers() {
        Calendar brunei = Calendar.getInstance(AmbientPrayerSchedule2026.ZONE);
        int[] today = AmbientPrayerSchedule2026.get(brunei);
        if (today == null) {
            prayerRow.setVisibility(View.GONE);
            prayerStatus.setText("Prayer times unavailable · outside verified coverage");
            return;
        }
        prayerRow.setVisibility(View.VISIBLE);
        int minutesNow = brunei.get(Calendar.HOUR_OF_DAY) * 60 + brunei.get(Calendar.MINUTE);
        int next = nextPrayer(today, minutesNow);
        int[] tomorrow = null;
        if (next < 0) {
            tomorrow = AmbientPrayerSchedule2026.get(AmbientPrayerSchedule2026.nextDay(brunei));
        }
        boolean highlightTomorrow = nextIsTomorrowSubuh(today, tomorrow, minutesNow);
        String nextLabel = next >= 0 ? NAMES[next]
                : highlightTomorrow ? "Subuh tomorrow" : null;
        prayerStatus.setText(nextLabel == null
                ? "PDI 2026 · Next prayer unavailable"
                : "Next: " + nextLabel + " · PDI 2026");
        for (int i = 0; i < 5; i++) {
            boolean tomorrowColumn = highlightTomorrow && i == 0;
            // Explicitly label the day switch; never highlight today's elapsed Subuh.
            names[i].setText(tomorrowColumn ? "Subuh · tomorrow" : NAMES[i]);
            times[i].setText(AmbientPrayerSchedule2026.format(tomorrowColumn
                    ? tomorrow[0] : today[PRAYER_INDEXES[i]]));
            boolean highlight = i == next || tomorrowColumn;
            names[i].setTextColor(highlight ? 0xFFFFE7A8 : 0xDAE8ECF3);
            times[i].setTextColor(highlight ? 0xFFFFF2D1 : Color.WHITE);
            if (highlight) {
                GradientDrawable shade = new GradientDrawable();
                shade.setColor(0x60423624);
                shade.setCornerRadius(size(18, Math.max(0.68f,
                        getResources().getDisplayMetrics().widthPixels / 1920f)));
                shade.setStroke(Math.max(1, size(2, Math.max(0.68f,
                        getResources().getDisplayMetrics().widthPixels / 1920f))), 0xE8F2DCA7);
                prayerCells[i].setBackground(shade);
            } else {
                prayerCells[i].setBackground(null);
            }
        }
    }
}
