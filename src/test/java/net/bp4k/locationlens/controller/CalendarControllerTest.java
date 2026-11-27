package net.bp4k.locationlens.controller;

import static org.springframework.security.test.web.servlet.request
        .SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders
        .get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders
        .post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers
        .header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers
        .jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers
        .status;

import static org.hamcrest.Matchers.containsString;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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

    @Test
    void createCalendarRequiresBearerAuthentication() throws Exception {
        mockMvc.perform(post(CALENDAR_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Personal","type":"PERSONAL"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createDefaultsColorAndReturnsCreatedOwnerResponse() throws Exception {
        mockMvc.perform(post(CALENDAR_PATH)
                        .with(jwt().jwt(token -> token.subject(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"  Personal  ","type":"PERSONAL"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location", containsString(CALENDAR_PATH + "/")))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Personal"))
                .andExpect(jsonPath("$.type").value("PERSONAL"))
                .andExpect(jsonPath("$.color").value("#4285F4"))
                .andExpect(jsonPath("$.currentUserRole").value("OWNER"));

        mockMvc.perform(get(CALENDAR_PATH).with(jwt().jwt(
                        token -> token.subject(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void createAcceptsAnExplicitColor() throws Exception {
        mockMvc.perform(post(CALENDAR_PATH)
                        .with(jwt().jwt(token -> token.subject(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Work","type":"GROUP","color":"#AaBbCc"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.color").value("#AaBbCc"));
    }

    @Test
    void createRejectsInvalidRequests() throws Exception {
        String[] invalidBodies = {
                // blank name
                "{\"name\":\"   \",\"type\":\"GROUP\"}",
                // missing name
                "{\"type\":\"GROUP\"}",
                // name longer than 255 characters
                "{\"name\":\"" + "x".repeat(256) + "\",\"type\":\"GROUP\"}",
                // missing type
                "{\"name\":\"Work\"}",
                // unknown type
                "{\"name\":\"Work\",\"type\":\"TEAM\"}",
                // malformed colors
                "{\"name\":\"Work\",\"type\":\"GROUP\",\"color\":\"red\"}",
                "{\"name\":\"Work\",\"type\":\"GROUP\",\"color\":\"#12345\"}",
                "{\"name\":\"Work\",\"type\":\"GROUP\",\"color\":\"123456\"}",
        };

        for (String body : invalidBodies) {
            mockMvc.perform(post(CALENDAR_PATH)
                            .with(jwt().jwt(token -> token.subject(USER_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        mockMvc.perform(post(CALENDAR_PATH)
                        .with(jwt().jwt(token -> token.subject(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void onlyOneActivePersonalCalendarIsAllowedPerOwner() throws Exception {
        saveCalendar(USER_ID, CalendarType.PERSONAL);

        mockMvc.perform(post(CALENDAR_PATH)
                        .with(jwt().jwt(token -> token.subject(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Second","type":"PERSONAL"}
                                """))
                .andExpect(status().isConflict());

        // Another user is unaffected by this owner's personal calendar.
        mockMvc.perform(post(CALENDAR_PATH)
                        .with(jwt().jwt(token -> token.subject("user-2")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Personal","type":"PERSONAL"}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void deletedPersonalCalendarDoesNotBlockCreatingANewOne()
            throws Exception {
        CalendarEntity calendar = saveCalendar(
                USER_ID, CalendarType.PERSONAL);
        calendar.setDeletedAt(Instant.now());
        calendarRepository.saveAndFlush(calendar);

        mockMvc.perform(post(CALENDAR_PATH)
                        .with(jwt().jwt(token -> token.subject(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Replacement","type":"PERSONAL"}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void multipleGroupCalendarsAreAllowedPerOwner() throws Exception {
        for (String name : new String[] {"Team A", "Team B"}) {
            mockMvc.perform(post(CALENDAR_PATH)
                            .with(jwt().jwt(token -> token.subject(USER_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"" + name
                                    + "\",\"type\":\"GROUP\"}"))
                    .andExpect(status().isCreated());
        }
    }
}
