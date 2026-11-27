package net.bp4k.locationlens.controller;

import static org.springframework.security.test.web.servlet.request
        .SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders
        .get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers
        .jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers
        .status;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import net.bp4k.locationlens.entity.CalendarEntity;
import net.bp4k.locationlens.entity.CalendarMembershipEntity;
import net.bp4k.locationlens.enums.CalendarRole;
import net.bp4k.locationlens.enums.CalendarType;
import net.bp4k.locationlens.repository.CalendarMembershipRepository;
import net.bp4k.locationlens.repository.CalendarRepository;

@SpringBootTest
@AutoConfigureMockMvc
class CalendarControllerTest {

    private static final String USER_ID = "user-1";
    private static final String CALENDAR_PATH = "/api/v1/calendars";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CalendarRepository calendarRepository;

    @Autowired
    private CalendarMembershipRepository membershipRepository;

    @BeforeEach
    void clearDatabase() {
        membershipRepository.deleteAllInBatch();
        calendarRepository.deleteAllInBatch();
    }

    @Test
    void listCalendarsRequiresBearerAuthentication() throws Exception {
        mockMvc.perform(get(CALENDAR_PATH))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get(CALENDAR_PATH)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void emptyCalendarListReturnsAnEmptyArray() throws Exception {
        mockMvc.perform(get(CALENDAR_PATH).with(jwt().jwt(
                        token -> token.subject(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void listIncludesOwnedAndSharedCalendarsButIgnoresDeletedMemberships()
            throws Exception {
        CalendarEntity owned = saveCalendar(USER_ID, CalendarType.PERSONAL);
        CalendarEntity shared = saveCalendar("owner-2", CalendarType.GROUP);
        CalendarEntity revoked = saveCalendar("owner-3", CalendarType.GROUP);
        membershipRepository.saveAndFlush(new CalendarMembershipEntity(
                shared, USER_ID, CalendarRole.EDITOR));
        CalendarMembershipEntity deletedMembership =
                new CalendarMembershipEntity(
                        revoked, USER_ID, CalendarRole.VIEWER);
        deletedMembership.setDeletedAt(Instant.now());
        membershipRepository.saveAndFlush(deletedMembership);

        mockMvc.perform(get(CALENDAR_PATH).with(jwt().jwt(
                        token -> token.subject(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(owned.getId()))
                .andExpect(jsonPath("$[0].currentUserRole").value("OWNER"))
                .andExpect(jsonPath("$[1].id").value(shared.getId()))
                .andExpect(jsonPath("$[1].currentUserRole").value("EDITOR"));
    }

    @Test
    void listExcludesSoftDeletedCalendarsSharedWithMembers() throws Exception {
        CalendarEntity calendar = saveCalendar("owner-2", CalendarType.GROUP);
        membershipRepository.saveAndFlush(new CalendarMembershipEntity(
                calendar, USER_ID, CalendarRole.EDITOR));
        calendar.setDeletedAt(Instant.now());
        calendarRepository.saveAndFlush(calendar);

        mockMvc.perform(get(CALENDAR_PATH).with(jwt().jwt(
                        token -> token.subject(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    private CalendarEntity saveCalendar(String ownerId, CalendarType type) {
        return calendarRepository.saveAndFlush(new CalendarEntity(
                ownerId, "Test calendar", type, "#123456"));
    }
}
