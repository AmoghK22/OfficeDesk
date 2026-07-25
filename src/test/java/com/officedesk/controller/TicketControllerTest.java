package com.officedesk.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.officedesk.BaseIntegrationTest;
import com.officedesk.dto.auth.RegisterRequest;
import com.officedesk.dto.ticket.*;
import com.officedesk.entity.Department;
import com.officedesk.entity.User;
import com.officedesk.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TicketControllerTest extends BaseIntegrationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private Department dept;
    private User employee;
    private User agent;
    private User deptHead;
    private String empToken;
    private String agentToken;
    private String headToken;

    @BeforeEach
    void setUp() {
        dept = deptRepo.findByName(DepartmentName.IT).orElseGet(
                () -> deptRepo.save(Department.builder().name(DepartmentName.IT).build()));

        employee = userRepo.save(User.builder()
                .name("Rahul").email("rahul@test.com").password("encoded")
                .role(Role.EMPLOYEE).department(dept).isActive(true).build());
        agent = userRepo.save(User.builder()
                .name("Vikram").email("vikram@test.com").password("encoded")
                .role(Role.AGENT).department(dept).isActive(true).build());
        deptHead = userRepo.save(User.builder()
                .name("Deepak").email("deepak@test.com").password("encoded")
                .role(Role.DEPT_HEAD).department(dept).isActive(true).build());

        empToken = tokenFor(employee);
        agentToken = tokenFor(agent);
        headToken = tokenFor(deptHead);
    }

    private String createTicket(String title) throws Exception {
        TicketCreateRequest req = TicketCreateRequest.builder()
                .title(title).description("Description").priority(Priority.MEDIUM).category("Software").build();
        MvcResult result = mockMvc.perform(post("/api/tickets")
                        .header("Authorization", "Bearer " + empToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticketNo").isNotEmpty())
                .andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    // --- Create Ticket ---

    @Test
    @DisplayName("POST /api/tickets - employee creates ticket, auto-assigned")
    void createTicket_success() throws Exception {
        TicketCreateRequest req = TicketCreateRequest.builder()
                .title("Laptop broken").description("Screen cracked").priority(Priority.HIGH).category("Hardware").build();

        mockMvc.perform(post("/api/tickets")
                        .header("Authorization", "Bearer " + empToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Laptop broken"))
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.assignedToName").value("Vikram"))
                .andExpect(jsonPath("$.departmentName").value("IT"));
    }

    @Test
    @DisplayName("POST /api/tickets - unauthenticated returns 403")
    void createTicket_unauthenticated() throws Exception {
        TicketCreateRequest req = TicketCreateRequest.builder()
                .title("Test").description("Desc").priority(Priority.LOW).category("Software").build();

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // --- Get Ticket ---

    @Test
    @DisplayName("GET /api/tickets/{id} - owner can view")
    void getById_ownerCanView() throws Exception {
        String id = createTicket("View test");

        mockMvc.perform(get("/api/tickets/" + id)
                        .header("Authorization", "Bearer " + empToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("View test"));
    }

    @Test
    @DisplayName("GET /api/tickets/{id} - assigned agent can view")
    void getById_agentCanView() throws Exception {
        String id = createTicket("Agent view test");

        mockMvc.perform(get("/api/tickets/" + id)
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/tickets/{id} - dept head can view dept tickets")
    void getById_deptHeadCanView() throws Exception {
        String id = createTicket("Head view test");

        mockMvc.perform(get("/api/tickets/" + id)
                        .header("Authorization", "Bearer " + headToken))
                .andExpect(status().isOk());
    }

    // --- My Tickets ---

    @Test
    @DisplayName("GET /api/tickets/my - returns employee's tickets")
    void myTickets() throws Exception {
        createTicket("My ticket 1");
        createTicket("My ticket 2");

        mockMvc.perform(get("/api/tickets/my")
                        .header("Authorization", "Bearer " + empToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    // --- Agent Tickets ---

    @Test
    @DisplayName("GET /api/tickets/agent/{id} - returns agent's assigned tickets")
    void agentTickets() throws Exception {
        createTicket("Agent ticket 1");

        mockMvc.perform(get("/api/tickets/agent/" + agent.getId())
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(1)));
    }

    // --- Full Lifecycle ---

    @Test
    @DisplayName("Full lifecycle: create -> start progress -> resolve -> close -> rate")
    void fullLifecycle() throws Exception {
        // 1. Create ticket
        String ticketId = createTicket("Lifecycle test");

        // 2. ASSIGNED -> IN_PROGRESS (agent)
        mockMvc.perform(put("/api/tickets/" + ticketId + "/status")
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(
                                TicketStatusUpdateRequest.builder().status(TicketStatus.IN_PROGRESS).build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        // 3. IN_PROGRESS -> RESOLVED (agent with resolution note)
        mockMvc.perform(put("/api/tickets/" + ticketId + "/status")
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(
                                TicketStatusUpdateRequest.builder().status(TicketStatus.RESOLVED).resolutionNote("Fixed the issue").build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.resolutionNote").value("Fixed the issue"));

        // 4. RESOLVED -> CLOSED (employee)
        mockMvc.perform(put("/api/tickets/" + ticketId + "/status")
                        .header("Authorization", "Bearer " + empToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(
                                TicketStatusUpdateRequest.builder().status(TicketStatus.CLOSED).build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        // 5. Rate the ticket (employee)
        mockMvc.perform(post("/api/tickets/" + ticketId + "/rate")
                        .header("Authorization", "Bearer " + empToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(
                                RatingRequest.builder().rating(5).feedback("Excellent service").build())))
                .andExpect(status().isOk());

        // 6. Verify rating prevents re-rating
        mockMvc.perform(post("/api/tickets/" + ticketId + "/rate")
                        .header("Authorization", "Bearer " + empToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(
                                RatingRequest.builder().rating(4).build())))
                .andExpect(status().isConflict());
    }

    // --- Reopen flow ---

    @Test
    @DisplayName("Reopen flow: resolve -> reopen -> reassign -> progress -> resolve -> close")
    void reopenFlow() throws Exception {
        String ticketId = createTicket("Reopen test");

        // ASSIGNED -> IN_PROGRESS
        mockMvc.perform(put("/api/tickets/" + ticketId + "/status")
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(
                                TicketStatusUpdateRequest.builder().status(TicketStatus.IN_PROGRESS).build())))
                .andExpect(status().isOk());

        // IN_PROGRESS -> RESOLVED
        mockMvc.perform(put("/api/tickets/" + ticketId + "/status")
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(
                                TicketStatusUpdateRequest.builder().status(TicketStatus.RESOLVED).resolutionNote("Fixed").build())))
                .andExpect(status().isOk());

        // Reopen (employee)
        mockMvc.perform(post("/api/tickets/" + ticketId + "/reopen")
                        .header("Authorization", "Bearer " + empToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(
                                ReopenRequest.builder().reason("Still not working").build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REOPENED"));

        // REOPENED -> IN_PROGRESS (agent)
        mockMvc.perform(put("/api/tickets/" + ticketId + "/status")
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(
                                TicketStatusUpdateRequest.builder().status(TicketStatus.IN_PROGRESS).build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        // IN_PROGRESS -> RESOLVED
        mockMvc.perform(put("/api/tickets/" + ticketId + "/status")
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(
                                TicketStatusUpdateRequest.builder().status(TicketStatus.RESOLVED).resolutionNote("Really fixed").build())))
                .andExpect(status().isOk());

        // RESOLVED -> CLOSED
        mockMvc.perform(put("/api/tickets/" + ticketId + "/status")
                        .header("Authorization", "Bearer " + empToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(
                                TicketStatusUpdateRequest.builder().status(TicketStatus.CLOSED).build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    // --- Comments ---

    @Test
    @DisplayName("Comments: employee posts public, agent posts internal")
    void comments_flow() throws Exception {
        String ticketId = createTicket("Comment test");

        // Employee posts public comment
        mockMvc.perform(post("/api/tickets/" + ticketId + "/comments")
                        .header("Authorization", "Bearer " + empToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(
                                CommentRequest.builder().comment("Hello").isInternal(false).build())))
                .andExpect(status().isCreated());

        // Employee tries internal comment - forced to public
        mockMvc.perform(post("/api/tickets/" + ticketId + "/comments")
                        .header("Authorization", "Bearer " + empToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(
                                CommentRequest.builder().comment("Internal try").isInternal(true).build())))
                .andExpect(status().isCreated());

        // Agent posts internal comment
        mockMvc.perform(post("/api/tickets/" + ticketId + "/comments")
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(
                                CommentRequest.builder().comment("Agent note").isInternal(true).build())))
                .andExpect(status().isCreated());

        // Employee sees 2 public comments (internal hidden)
        mockMvc.perform(get("/api/tickets/" + ticketId + "/comments")
                        .header("Authorization", "Bearer " + empToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        // Agent sees all 3 comments
        mockMvc.perform(get("/api/tickets/" + ticketId + "/comments")
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    // --- Invalid transitions ---

    @Test
    @DisplayName("Invalid transitions: ASSIGNED -> CLOSED should fail")
    void invalidTransition() throws Exception {
        String ticketId = createTicket("Invalid transition test");

        mockMvc.perform(put("/api/tickets/" + ticketId + "/status")
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(
                                TicketStatusUpdateRequest.builder().status(TicketStatus.CLOSED).build())))
                .andExpect(status().isBadRequest());
    }

    // --- Assign ---

    @Test
    @DisplayName("PUT /api/tickets/{id}/assign - dept head reassigns ticket")
    void assignTicket() throws Exception {
        String ticketId = createTicket("Assign test");

        mockMvc.perform(put("/api/tickets/" + ticketId + "/assign")
                        .header("Authorization", "Bearer " + headToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(
                                TicketAssignRequest.builder().agentId(agent.getId()).build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedToName").value("Vikram"));
    }

    // --- Filter/search ---

    @Test
    @DisplayName("GET /api/tickets/my?status= filters correctly")
    void filterByStatus() throws Exception {
        createTicket("Filter test");

        mockMvc.perform(get("/api/tickets/my")
                        .header("Authorization", "Bearer " + empToken)
                        .param("status", "ASSIGNED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/api/tickets/my")
                        .header("Authorization", "Bearer " + empToken)
                        .param("status", "CLOSED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    @DisplayName("GET /api/tickets/my?search= filters by title")
    void filterBySearch() throws Exception {
        createTicket("Unique search term XYZ");

        mockMvc.perform(get("/api/tickets/my")
                        .header("Authorization", "Bearer " + empToken)
                        .param("search", "Unique search term XYZ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        mockMvc.perform(get("/api/tickets/my")
                        .header("Authorization", "Bearer " + empToken)
                        .param("search", "nonexistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    // --- Activities ---

    @Test
    @DisplayName("GET /api/tickets/{id}/activities - owner can view")
    void activities_ownerCanView() throws Exception {
        String ticketId = createTicket("Activity test");

        mockMvc.perform(get("/api/tickets/" + ticketId + "/activities")
                        .header("Authorization", "Bearer " + empToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)));
    }

    // --- Dept head cannot assign outside dept ---

    @Test
    @DisplayName("Dept head cannot assign ticket from another department")
    void assignTicket_otherDept_throws() throws Exception {
        Department otherDept = deptRepo.save(Department.builder().name(DepartmentName.HR).build());
        User otherHead = userRepo.save(User.builder()
                .name("OtherHead").email("other@test.com").password("encoded")
                .role(Role.DEPT_HEAD).department(otherDept).isActive(true).build());

        String ticketId = createTicket("Dept boundary test");

        mockMvc.perform(put("/api/tickets/" + ticketId + "/assign")
                        .header("Authorization", "Bearer " + tokenFor(otherHead))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(
                                TicketAssignRequest.builder().agentId(agent.getId()).build())))
                .andExpect(status().isForbidden());
    }
}
