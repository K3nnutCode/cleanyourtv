package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;

/** Opt-in preview-only, location-free Open-Meteo weather for Bandar Seri Begawan. */
final class AmbientWeatherClient {
    interface Listener {
        void update(String temperature, String condition, String provenance);
    }

    private static final String URL_WEATHER = "https://api.open-meteo.com/v1/forecast"
            + "?latitude=4.9031&longitude=114.9398"
            + "&current=temperature_2m,weather_code"
            + "&timezone=Asia%2FBrunei";
    private static final long REFRESH_MS = 30L * 60_000L;
    private static final long RETRY_MS = 5L * 60_000L;
    private static final long MAX_CACHE_AGE_MS = 6L * 60L * 60_000L;
    private static final String PREFS = "customAmbientWeather";

    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final SharedPreferences cache;
    private final Listener listener;
    private boolean active;
    private boolean closed;
    private long generation;
    private final Runnable refresh = this::fetch;

    AmbientWeatherClient(Context context, Listener listener) {
        cache = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        this.listener = listener;
    }

    void start() {
        if (active || closed) return;
        active = true;
        generation++;
        showCache();
        fetch();
    }

    void stop() {
        active = false;
        generation++;
        ui.removeCallbacks(refresh);
    }

    void close() {
        stop();
        closed = true;
        io.shutdownNow();
    }

    private void showCache() {
        long age = System.currentTimeMillis() - cache.getLong("updated", 0);
        if (!cache.contains("temperature") || age < 0 || age > MAX_CACHE_AGE_MS) {
            listener.update("--°", "Weather unavailable", "Open-Meteo · Brunei-Muara");
            return;
        }
        String when = age < 60_000L ? "just now" : age < 60L * 60_000L
                ? (age / 60_000L) + "m ago" : (age / (60L * 60_000L)) + "h ago";
        listener.update(String.format(Locale.US, "%.0f°", cache.getFloat("temperature", 0f)),
                label(cache.getInt("code", -1)),
                "Open-Meteo · saved " + when);
    }

    private void fetch() {
        if (!active || closed) return;
        final long token = generation;
        io.execute(() -> {
            Double temperature = null;
            int code = -1;
            HttpsURLConnection connection = null;
            try {
                connection = (HttpsURLConnection) new URL(URL_WEATHER).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10_000);
                connection.setReadTimeout(12_000);
                connection.setUseCaches(false);
                if (connection.getResponseCode() != 200) {
                    throw new java.io.IOException("Open-Meteo HTTP " + connection.getResponseCode());
                }
                StringBuilder body = new StringBuilder();
                try (InputStream input = connection.getInputStream();
                     Reader reader = new InputStreamReader(input, java.nio.charset.StandardCharsets.UTF_8)) {
                    char[] buffer = new char[2048];
                    int read;
                    while ((read = reader.read(buffer)) != -1) {
                        body.append(buffer, 0, read);
                        if (body.length() > 32768) throw new java.io.IOException("Weather response too large");
                    }
                }
                JSONObject current = new JSONObject(body.toString()).getJSONObject("current");
                double observed = current.getDouble("temperature_2m");
                if (Double.isNaN(observed) || Double.isInfinite(observed)
                        || observed < -100 || observed > 100) {
                    throw new java.io.IOException("Invalid temperature in weather result");
                }
                temperature = observed;
                code = current.getInt("weather_code");
            } catch (Exception e) {
                Log.w("AmbientWeather", "Weather unavailable; retaining cache", e);
            } finally {
                if (connection != null) connection.disconnect();
            }
            final Double result = temperature;
            final int resultCode = code;
            ui.post(() -> {
                if (!active || closed || token != generation) return;
                if (result != null) {
                    cache.edit().putFloat("temperature", result.floatValue())
                            .putInt("code", resultCode)
                            .putLong("updated", System.currentTimeMillis()).apply();
                }
                showCache();
                ui.postDelayed(refresh, result != null ? REFRESH_MS : RETRY_MS);
            });
        });
    }

    static String label(int code) {
        if (code == 0) return "Clear sky";
        if (code >= 1 && code <= 3) return "Cloudy";
        if (code == 45 || code == 48) return "Fog";
        if (code >= 51 && code <= 57) return "Drizzle";
        if (code >= 61 && code <= 67) return "Rain";
        if (code >= 71 && code <= 77) return "Snow";
        if (code >= 80 && code <= 82) return "Showers";
        if (code >= 85 && code <= 86) return "Snow showers";
        if (code >= 95 && code <= 99) return "Thunderstorms";
        return "Weather";
    }
}
