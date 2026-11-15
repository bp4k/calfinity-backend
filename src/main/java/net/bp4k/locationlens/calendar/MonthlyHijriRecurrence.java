package net.bp4k.locationlens.calendar;

import java.time.LocalDate;
import java.time.chrono.HijrahDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public final class MonthlyHijriRecurrence {
    private MonthlyHijriRecurrence() {}

    /**
     * Expands an all-day Umm al-Qura event into Gregorian dates, counting the start
     * as the first occurrence. Short months use their last day.
     */
    public static List<LocalDate> occurrences(int year, int month, int day, int count) {
        if (count < 1) {
            throw new IllegalArgumentException("Occurrence count must be positive");
        }

        HijrahDate start = HijrahDate.of(year, month, day);
        List<LocalDate> dates = new ArrayList<>();
        for (int offset = 0; offset < count; offset++) {
            // Anchor each month to the start so a short month cannot shift later dates.
            dates.add(LocalDate.from(start.plus(offset, ChronoUnit.MONTHS)));
        }
        return List.copyOf(dates);
    }
}
