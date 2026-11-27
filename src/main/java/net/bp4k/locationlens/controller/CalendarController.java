package net.bp4k.locationlens.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import net.bp4k.locationlens.dto.CalendarResponse;
import net.bp4k.locationlens.service.CalendarService;

@RestController
@RequestMapping("/api/v1/calendars")
public class CalendarController {

    private final CalendarService calendarService;

    public CalendarController(CalendarService calendarService) {
        this.calendarService = calendarService;
    }

    @GetMapping
    public List<CalendarResponse> getCalendars(
            @AuthenticationPrincipal Jwt jwt) {
        return calendarService.getUserCalendars(jwt.getSubject());
    }
}
