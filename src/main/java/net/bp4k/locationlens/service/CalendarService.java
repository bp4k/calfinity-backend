package net.bp4k.locationlens.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import net.bp4k.locationlens.dto.CalendarResponse;
import net.bp4k.locationlens.entity.CalendarEntity;
import net.bp4k.locationlens.entity.CalendarMembershipEntity;
import net.bp4k.locationlens.enums.CalendarRole;
import net.bp4k.locationlens.repository.CalendarMembershipRepository;
import net.bp4k.locationlens.repository.CalendarRepository;

@Service
public class CalendarService {

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

    private static CalendarResponse toResponse(
            CalendarEntity calendar, CalendarRole role) {
        return new CalendarResponse(
                calendar.getId(),
                calendar.getName(),
                calendar.getType(),
                calendar.getColor(),
                role);
    }

    private static void requireUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Authenticated user is required");
        }
    }
}
