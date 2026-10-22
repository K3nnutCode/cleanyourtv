package com.gathin.tvhome;

import org.junit.Test;
import java.util.Calendar;
import static org.junit.Assert.*;

/** Guard against shipping invented or stale prayer times in the Ambient preview. */
public final class AmbientPrayerSchedule2026Test {
    private static void expect(int month, int day, String... values) {
        int[] times = AmbientPrayerSchedule2026.get(2026, month, day);
        assertNotNull(times);
        assertEquals(values.length, times.length);
        for (int i = 0; i < times.length; i++) {
            assertEquals("2026-" + month + "-" + day + " at " + i,
                    values[i], AmbientPrayerSchedule2026.format(times[i]));
        }
    }

    @Test public void referenceDaysAndBoundaries() {
        expect(10, 1, "04:51", "06:08", "12:11", "15:23", "18:12", "19:21");
        expect(10, 22, "04:47", "06:05", "12:06", "15:24", "18:04", "19:14");
        expect(11, 1, "04:46", "06:05", "12:05", "15:25", "18:02", "19:13");
        expect(11, 30, "04:50", "06:13", "12:10", "15:32", "18:05", "19:18");
        expect(12, 31, "05:04", "06:28", "12:24", "15:46", "18:18", "19:33");
        assertNull(AmbientPrayerSchedule2026.get(2026, 9, 30));
        assertNull(AmbientPrayerSchedule2026.get(2026, 10, 32));
        assertNull(AmbientPrayerSchedule2026.get(2026, 11, 31));
        assertNull(AmbientPrayerSchedule2026.get(2027, 1, 1));
    }

    @Test public void all92DatesOrderedAndNextPrayerBoundaries() {
        int count = 0;
        for (int m = 10; m <= 12; m++) {
            int days = m == 11 ? 30 : 31;
            for (int day = 1; day <= days; day++) {
                int[] times = AmbientPrayerSchedule2026.get(2026, m, day);
                assertNotNull(times);
                for (int i = 1; i < times.length; i++) assertTrue(times[i] > times[i-1]);
                count++;
            }
        }
        assertEquals(92, count);
        int[] day = AmbientPrayerSchedule2026.get(2026, 10, 22);
        assertEquals(0, AmbientInfoOverlay.nextPrayer(day, 200));
        assertEquals(1, AmbientInfoOverlay.nextPrayer(day, 300));
        assertEquals(2, AmbientInfoOverlay.nextPrayer(day, 725));
        assertEquals(3, AmbientInfoOverlay.nextPrayer(day, 1000));
        assertEquals(4, AmbientInfoOverlay.nextPrayer(day, 1090));
        assertEquals(-1, AmbientInfoOverlay.nextPrayer(day, 1200));
        assertEquals(-1, AmbientInfoOverlay.nextPrayer(null, 800));
    }

    @Test public void bruneiMidnightRoll() {
        Calendar brunei = Calendar.getInstance(AmbientPrayerSchedule2026.ZONE);
        brunei.clear();
        brunei.set(2026, Calendar.DECEMBER, 31, 23, 59, 0);
        Calendar tomorrow = AmbientPrayerSchedule2026.nextDay(brunei);
        assertEquals(2027, tomorrow.get(Calendar.YEAR));
        assertNull(AmbientPrayerSchedule2026.get(tomorrow));
        assertEquals("Asia/Brunei", AmbientPrayerSchedule2026.ZONE.getID());
    }
}
