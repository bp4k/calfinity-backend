package net.bp4k.locationlens.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import net.bp4k.locationlens.dto.CalendarResponse;
import net.bp4k.locationlens.dto.CreateCalendarRequest;
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

    @PostMapping
    public ResponseEntity<CalendarResponse> createCalendar(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody CreateCalendarRequest request) {
        CalendarResponse calendar = calendarService.createCalendar(
                jwt.getSubject(), request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{calendarId}")
                .buildAndExpand(calendar.id())
                .toUri();
        return ResponseEntity.created(location).body(calendar);
    }
}
