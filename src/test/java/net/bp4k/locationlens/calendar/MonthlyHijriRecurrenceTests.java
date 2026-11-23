package net.bp4k.locationlens.calendar;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MonthlyHijriRecurrenceTests {
    // Expected Gregorian dates were checked with .NET's UmAlQuraCalendar.
    @Test
    void clampsDayThirtyThenRestoresItInTheNextLongMonth() {
        assertEquals(List.of(LocalDate.of(2023, 11, 14), LocalDate.of(2023, 12, 13),
                        LocalDate.of(2024, 1, 12)),
                MonthlyHijriRecurrence.occurrences(1445, 4, 30, 3));
    }

    @Test
    void preservesAnOrdinaryDayAcrossTheHijriNewYear() {
        assertEquals(List.of(LocalDate.of(2024, 6, 21), LocalDate.of(2024, 7, 21)),
                MonthlyHijriRecurrence.occurrences(1445, 12, 15, 2));
    }

    @Test
    void countOneReturnsOnlyTheStartDate() {
        assertEquals(List.of(LocalDate.of(2023, 12, 13)),
                MonthlyHijriRecurrence.occurrences(1445, 5, 29, 1));
    }

    @Test
    void rejectsAnInvalidStartingDay() {
        assertThrows(DateTimeException.class,
                () -> MonthlyHijriRecurrence.occurrences(1445, 5, 30, 2));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejectsNonpositiveCounts(int count) {
        assertThrows(IllegalArgumentException.class,
                () -> MonthlyHijriRecurrence.occurrences(1445, 4, 30, count));
    }
}
