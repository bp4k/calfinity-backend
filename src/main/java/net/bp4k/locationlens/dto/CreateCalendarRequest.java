package net.bp4k.locationlens.dto;

import net.bp4k.locationlens.enums.CalendarType;

public record CreateCalendarRequest(
        String name,
        CalendarType type,
        String color) {
}
