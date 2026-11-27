package net.bp4k.locationlens.dto;

import net.bp4k.locationlens.enums.CalendarRole;
import net.bp4k.locationlens.enums.CalendarType;

public record CalendarResponse(
        String id,
        String name,
        CalendarType type,
        String color,
        CalendarRole currentUserRole) {
}
