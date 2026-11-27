package net.bp4k.locationlens.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import net.bp4k.locationlens.dto.CalendarResponse;
import net.bp4k.locationlens.dto.CreateCalendarRequest;
import net.bp4k.locationlens.entity.CalendarEntity;
import net.bp4k.locationlens.entity.CalendarMembershipEntity;
import net.bp4k.locationlens.enums.CalendarRole;
import net.bp4k.locationlens.enums.CalendarType;
import net.bp4k.locationlens.repository.CalendarMembershipRepository;
import net.bp4k.locationlens.repository.CalendarRepository;

@Service
public class CalendarService {

    private static final String DEFAULT_COLOR = "#4285F4";
    private static final int MAX_NAME_LENGTH = 255;
    private static final Pattern HEX_COLOR = Pattern.compile(
            "#[0-9a-fA-F]{6}");

    private final CalendarRepository calendarRepository;
    private final CalendarMembershipRepository membershipRepository;

    public CalendarService(
            CalendarRepository calendarRepository,
            CalendarMembershipRepository membershipRepository) {
        this.calendarRepository = calendarRepository;
        this.membershipRepository = membershipRepository;
    }

    @Transactional(readOnly = true)
    public List<CalendarResponse> getUserCalendars(String userId) {
        requireUserId(userId);

        List<CalendarResponse> calendars = new ArrayList<>();
        for (CalendarEntity calendar : calendarRepository
                .findAllByOwnerIdAndDeletedAtIsNullOrderByCreatedAtAsc(userId)) {
            calendars.add(toResponse(calendar, CalendarRole.OWNER));
        }

        for (CalendarMembershipEntity membership : membershipRepository
                .findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtAsc(userId)) {
            CalendarEntity calendar = membership.getCalendar();
            if (calendar.isDeleted()
                    || userId.equals(calendar.getOwnerId())) {
                continue;
            }
            calendars.add(toResponse(calendar, membership.getRole()));
        }

        return calendars;
    }

    @Transactional
    public CalendarResponse createCalendar(
            String userId, CreateCalendarRequest request) {
        requireUserId(userId);
        if (request == null) {
            throw badRequest("Calendar request is required");
        }

        String name = validateName(request.name());
        if (request.type() == null) {
            throw badRequest("Calendar type is required");
        }
        String color = request.color() == null
                ? DEFAULT_COLOR
                : validateColor(request.color());

        if (request.type() == CalendarType.PERSONAL
                && calendarRepository.existsByOwnerIdAndTypeAndDeletedAtIsNull(
                        userId, CalendarType.PERSONAL)) {
            throw conflict("A personal calendar already exists");
        }

        CalendarEntity calendar = new CalendarEntity(
                userId, name, request.type(), color);
        try {
            // saveAndFlush so a concurrent duplicate caught only by the
            // database's unique index still surfaces as a 409 here.
            calendar = calendarRepository.saveAndFlush(calendar);
        } catch (DataIntegrityViolationException exception) {
            throw conflict("Calendar conflicts with an existing record");
        }
        return toResponse(calendar, CalendarRole.OWNER);
    }

    private static CalendarResponse toResponse(
            CalendarEntity calendar, CalendarRole role) {
        return new CalendarResponse(
                calendar.getId(),
                calendar.getName(),
                calendar.getType(),
                calendar.getColor(),
                role);
    }

    private static String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw badRequest("Calendar name must not be blank");
        }
        String normalizedName = name.trim();
        if (normalizedName.length() > MAX_NAME_LENGTH) {
            throw badRequest("Calendar name must be at most 255 characters");
        }
        return normalizedName;
    }

    private static String validateColor(String color) {
        if (color == null || !HEX_COLOR.matcher(color).matches()) {
            throw badRequest(
                    "Color must be a six-digit hex value such as #4285F4");
        }
        return color;
    }

    private static void requireUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Authenticated user is required");
        }
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private static ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
