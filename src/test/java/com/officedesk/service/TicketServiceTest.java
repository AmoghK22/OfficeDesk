package com.officedesk.service;

import com.officedesk.dto.ticket.*;
import com.officedesk.entity.*;
import com.officedesk.enums.*;
import com.officedesk.exception.*;
import com.officedesk.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TicketServiceTest {

    @Mock private TicketRepository ticketRepo;
    @Mock private UserRepository userRepo;
    @Mock private DepartmentRepository deptRepo;
    @Mock private SlaConfigRepository slaConfigRepo;
    @Mock private TicketCommentRepository commentRepo;
    @Mock private TicketRatingRepository ratingRepo;
    @Mock private TicketActivityRepository activityRepo;

    @InjectMocks private TicketService ticketService;

    private Department dept;
    private User employee;
    private User agent;
    private User deptHead;
    private User admin;
    private Ticket ticket;
    private SlaConfig slaConfig;

    @BeforeEach
    void setUp() {
        dept = Department.builder().id(1L).name(DepartmentName.IT).build();
        employee = User.builder().id(1L).name("Rahul").email("rahul@test.com").role(Role.EMPLOYEE).department(dept).isActive(true).build();
        agent = User.builder().id(2L).name("Vikram").email("vikram@test.com").role(Role.AGENT).department(dept).isActive(true).build();
        deptHead = User.builder().id(3L).name("Deepak").email("deepak@test.com").role(Role.DEPT_HEAD).department(dept).isActive(true).build();
        admin = User.builder().id(4L).name("Admin").email("admin@test.com").role(Role.SUPER_ADMIN).department(null).isActive(true).build();
        slaConfig = SlaConfig.builder().id(1L).department(dept).priority(Priority.MEDIUM).resolutionHours(48).build();
        ticket = Ticket.builder().id(1L).ticketNo("TKT-2026-0001").title("Test").description("Desc")
                .status(TicketStatus.ASSIGNED).priority(Priority.MEDIUM).category("Software")
                .department(dept).raisedBy(employee).assignedTo(agent)
                .slaDeadline(LocalDateTime.now().plusHours(48)).slaBreached(false).escalated(false).build();
    }

    // --- createTicket ---

    @Test
    @DisplayName("createTicket - success, auto-assigns least loaded agent")
    void createTicket_success() {
        TicketCreateRequest req = TicketCreateRequest.builder().title("Laptop issue").description("Screen broken").priority(Priority.HIGH).category("Hardware").build();

        when(userRepo.findById(1L)).thenReturn(Optional.of(employee));
        when(deptRepo.findByName(DepartmentName.IT)).thenReturn(Optional.of(dept));
        when(userRepo.findByDepartmentIdAndRoleAndIsActive(1L, Role.AGENT, true)).thenReturn(List.of(agent));
        when(ticketRepo.countByAssignedToIdAndStatus(2L, TicketStatus.IN_PROGRESS)).thenReturn(0L);
        when(slaConfigRepo.findByDepartmentIdAndPriority(1L, Priority.HIGH)).thenReturn(Optional.of(slaConfig));
        when(ticketRepo.count()).thenReturn(0L);
        when(ticketRepo.save(any(Ticket.class))).thenAnswer(inv -> {
            Ticket t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        TicketResponse res = ticketService.createTicket(req, 1L);

        assertThat(res.getTitle()).isEqualTo("Laptop issue");
        assertThat(res.getStatus()).isEqualTo("ASSIGNED");
        assertThat(res.getAssignedToName()).isEqualTo("Vikram");
        verify(activityRepo, times(2)).save(any(TicketActivity.class));
    }

    @Test
    @DisplayName("createTicket - employee not found throws")
    void createTicket_userNotFound_throws() {
        TicketCreateRequest req = TicketCreateRequest.builder().title("T").description("D").priority(Priority.LOW).category("Software").build();
        when(userRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.createTicket(req, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("createTicket - no agents in dept throws")
    void createTicket_noAgents_throws() {
        TicketCreateRequest req = TicketCreateRequest.builder().title("T").description("D").priority(Priority.LOW).category("Software").build();
        when(userRepo.findById(1L)).thenReturn(Optional.of(employee));
        when(deptRepo.findByName(DepartmentName.IT)).thenReturn(Optional.of(dept));
        when(userRepo.findByDepartmentIdAndRoleAndIsActive(1L, Role.AGENT, true)).thenReturn(List.of());

        assertThatThrownBy(() -> ticketService.createTicket(req, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No active agents");
    }

    // --- getTicketById ---

    @Test
    @DisplayName("getTicketById - owner can access their ticket")
    void getTicketById_ownerCanAccess() {
        when(ticketRepo.findById(1L)).thenReturn(Optional.of(ticket));
        when(userRepo.findById(1L)).thenReturn(Optional.of(employee));

        TicketResponse res = ticketService.getTicketById(1L, 1L);
        assertThat(res.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getTicketById - assigned agent can access")
    void getTicketById_agentCanAccess() {
        when(ticketRepo.findById(1L)).thenReturn(Optional.of(ticket));
        when(userRepo.findById(2L)).thenReturn(Optional.of(agent));

        TicketResponse res = ticketService.getTicketById(1L, 2L);
        assertThat(res.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getTicketById - admin can access any ticket")
    void getTicketById_adminCanAccess() {
        when(ticketRepo.findById(1L)).thenReturn(Optional.of(ticket));
        when(userRepo.findById(4L)).thenReturn(Optional.of(admin));

        TicketResponse res = ticketService.getTicketById(1L, 4L);
        assertThat(res.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getTicketById - unauthorized user throws")
    void getTicketById_unauthorized_throws() {
        User stranger = User.builder().id(5L).name("Stranger").role(Role.EMPLOYEE).department(dept).isActive(true).build();
        when(ticketRepo.findById(1L)).thenReturn(Optional.of(ticket));
        when(userRepo.findById(5L)).thenReturn(Optional.of(stranger));

        assertThatThrownBy(() -> ticketService.getTicketById(1L, 5L))
                .isInstanceOf(UnauthorizedException.class);
    }

    // --- updateStatus ---

    @Test
    @DisplayName("updateStatus - ASSIGNED -> IN_PROGRESS")
    void updateStatus_assignedToInProgress() {
        when(ticketRepo.findById(1L)).thenReturn(Optional.of(ticket));
        when(userRepo.findById(2L)).thenReturn(Optional.of(agent));
        when(ticketRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TicketStatusUpdateRequest req = TicketStatusUpdateRequest.builder().status(TicketStatus.IN_PROGRESS).build();
        TicketResponse res = ticketService.updateStatus(1L, req, 2L);

        assertThat(res.getStatus()).isEqualTo("IN_PROGRESS");
    }

    @Test
    @DisplayName("updateStatus - IN_PROGRESS -> RESOLVED with note")
    void updateStatus_inProgressToResolved() {
        ticket.setStatus(TicketStatus.IN_PROGRESS);
        when(ticketRepo.findById(1L)).thenReturn(Optional.of(ticket));
        when(userRepo.findById(2L)).thenReturn(Optional.of(agent));
        when(ticketRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TicketStatusUpdateRequest req = TicketStatusUpdateRequest.builder().status(TicketStatus.RESOLVED).resolutionNote("Fixed").build();
        TicketResponse res = ticketService.updateStatus(1L, req, 2L);

        assertThat(res.getStatus()).isEqualTo("RESOLVED");
        assertThat(res.getResolutionNote()).isEqualTo("Fixed");
    }

    @Test
    @DisplayName("updateStatus - invalid transition throws InvalidStatusTransitionException")
    void updateStatus_invalidTransition_throws() {
        when(ticketRepo.findById(1L)).thenReturn(Optional.of(ticket));
        when(userRepo.findById(2L)).thenReturn(Optional.of(agent));

        TicketStatusUpdateRequest req = TicketStatusUpdateRequest.builder().status(TicketStatus.CLOSED).build();
        assertThatThrownBy(() -> ticketService.updateStatus(1L, req, 2L))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    @DisplayName("updateStatus - CLOSED terminal, no transitions allowed")
    void updateStatus_closedIsTerminal_throws() {
        ticket.setStatus(TicketStatus.CLOSED);
        when(ticketRepo.findById(1L)).thenReturn(Optional.of(ticket));
        when(userRepo.findById(2L)).thenReturn(Optional.of(agent));

        TicketStatusUpdateRequest req = TicketStatusUpdateRequest.builder().status(TicketStatus.IN_PROGRESS).build();
        assertThatThrownBy(() -> ticketService.updateStatus(1L, req, 2L))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    @DisplayName("updateStatus - unauthorized agent throws")
    void updateStatus_unauthorized_throws() {
        User stranger = User.builder().id(5L).name("Stranger").role(Role.EMPLOYEE).department(dept).isActive(true).build();
        when(ticketRepo.findById(1L)).thenReturn(Optional.of(ticket));
        when(userRepo.findById(5L)).thenReturn(Optional.of(stranger));

        TicketStatusUpdateRequest req = TicketStatusUpdateRequest.builder().status(TicketStatus.IN_PROGRESS).build();
        assertThatThrownBy(() -> ticketService.updateStatus(1L, req, 5L))
                .isInstanceOf(UnauthorizedException.class);
    }

    // --- addComment ---

    @Test
    @DisplayName("addComment - employee cannot post internal comments")
    void addComment_employeeForcesNotInternal() {
        when(ticketRepo.findById(1L)).thenReturn(Optional.of(ticket));
        when(userRepo.findById(1L)).thenReturn(Optional.of(employee));
        when(commentRepo.save(any())).thenAnswer(inv -> {
            TicketComment c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });

        CommentRequest req = CommentRequest.builder().comment("Hello").isInternal(true).build();
        CommentResponse res = ticketService.addComment(1L, req, 1L);

        assertThat(res.getComment()).isEqualTo("Hello");
        assertThat(res.isInternal()).isFalse();
    }

    @Test
    @DisplayName("addComment - agent can post internal comments")
    void addComment_agentCanPostInternal() {
        when(ticketRepo.findById(1L)).thenReturn(Optional.of(ticket));
        when(userRepo.findById(2L)).thenReturn(Optional.of(agent));
        when(commentRepo.save(any())).thenAnswer(inv -> {
            TicketComment c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });

        CommentRequest req = CommentRequest.builder().comment("Internal note").isInternal(true).build();
        CommentResponse res = ticketService.addComment(1L, req, 2L);

        assertThat(res.isInternal()).isTrue();
    }

    // --- reopenTicket ---

    @Test
    @DisplayName("reopenTicket - owner can reopen a RESOLVED ticket")
    void reopenTicket_success() {
        ticket.setStatus(TicketStatus.RESOLVED);
        when(ticketRepo.findById(1L)).thenReturn(Optional.of(ticket));
        when(userRepo.findById(1L)).thenReturn(Optional.of(employee));
        when(slaConfigRepo.findByDepartmentIdAndPriority(1L, Priority.MEDIUM)).thenReturn(Optional.of(slaConfig));
        when(ticketRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReopenRequest req = ReopenRequest.builder().reason("Still broken").build();
        TicketResponse res = ticketService.reopenTicket(1L, req, 1L);

        assertThat(res.getStatus()).isEqualTo("REOPENED");
    }

    @Test
    @DisplayName("reopenTicket - non-owner cannot reopen")
    void reopenTicket_notOwner_throws() {
        ticket.setStatus(TicketStatus.RESOLVED);
        when(ticketRepo.findById(1L)).thenReturn(Optional.of(ticket));
        when(userRepo.findById(2L)).thenReturn(Optional.of(agent));

        ReopenRequest req = ReopenRequest.builder().reason("reason").build();
        assertThatThrownBy(() -> ticketService.reopenTicket(1L, req, 2L))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("reopenTicket - cannot reopen non-RESOLVED ticket")
    void reopenTicket_notResolved_throws() {
        when(ticketRepo.findById(1L)).thenReturn(Optional.of(ticket));
        when(userRepo.findById(1L)).thenReturn(Optional.of(employee));

        ReopenRequest req = ReopenRequest.builder().reason("reason").build();
        assertThatThrownBy(() -> ticketService.reopenTicket(1L, req, 1L))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    // --- rateTicket ---

    @Test
    @DisplayName("rateTicket - owner rates a CLOSED ticket")
    void rateTicket_success() {
        ticket.setStatus(TicketStatus.CLOSED);
        when(ticketRepo.findById(1L)).thenReturn(Optional.of(ticket));
        when(userRepo.findById(1L)).thenReturn(Optional.of(employee));
        when(ratingRepo.findByTicket(ticket)).thenReturn(Optional.empty());

        RatingRequest req = RatingRequest.builder().rating(5).feedback("Great job").build();
        ticketService.rateTicket(1L, req, 1L);

        verify(ratingRepo).save(any(TicketRating.class));
        verify(activityRepo).save(any(TicketActivity.class));
    }

    @Test
    @DisplayName("rateTicket - cannot rate non-CLOSED ticket")
    void rateTicket_notClosed_throws() {
        when(ticketRepo.findById(1L)).thenReturn(Optional.of(ticket));
        when(userRepo.findById(1L)).thenReturn(Optional.of(employee));

        RatingRequest req = RatingRequest.builder().rating(5).build();
        assertThatThrownBy(() -> ticketService.rateTicket(1L, req, 1L))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    @DisplayName("rateTicket - cannot rate twice")
    void rateTicket_alreadyRated_throws() {
        ticket.setStatus(TicketStatus.CLOSED);
        when(ticketRepo.findById(1L)).thenReturn(Optional.of(ticket));
        when(userRepo.findById(1L)).thenReturn(Optional.of(employee));
        when(ratingRepo.findByTicket(ticket)).thenReturn(Optional.of(new TicketRating()));

        RatingRequest req = RatingRequest.builder().rating(5).build();
        assertThatThrownBy(() -> ticketService.rateTicket(1L, req, 1L))
                .isInstanceOf(TicketAlreadyRatedException.class);
    }

    @Test
    @DisplayName("rateTicket - non-owner cannot rate")
    void rateTicket_notOwner_throws() {
        ticket.setStatus(TicketStatus.CLOSED);
        when(ticketRepo.findById(1L)).thenReturn(Optional.of(ticket));
        when(userRepo.findById(2L)).thenReturn(Optional.of(agent));

        RatingRequest req = RatingRequest.builder().rating(5).build();
        assertThatThrownBy(() -> ticketService.rateTicket(1L, req, 2L))
                .isInstanceOf(UnauthorizedException.class);
    }

    // --- assignTicket ---

    @Test
    @DisplayName("assignTicket - dept head can reassign within dept")
    void assignTicket_deptHeadSuccess() {
        User otherAgent = User.builder().id(6L).name("Other").role(Role.AGENT).department(dept).isActive(true).build();
        when(ticketRepo.findById(1L)).thenReturn(Optional.of(ticket));
        when(userRepo.findById(3L)).thenReturn(Optional.of(deptHead));
        when(userRepo.findById(6L)).thenReturn(Optional.of(otherAgent));
        when(ticketRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TicketAssignRequest req = TicketAssignRequest.builder().agentId(6L).build();
        TicketResponse res = ticketService.assignTicket(1L, req, 3L);

        assertThat(res.getAssignedToName()).isEqualTo("Other");
    }

    @Test
    @DisplayName("assignTicket - employee cannot assign")
    void assignTicket_employeeCannotAssign_throws() {
        when(ticketRepo.findById(1L)).thenReturn(Optional.of(ticket));
        when(userRepo.findById(1L)).thenReturn(Optional.of(employee));

        TicketAssignRequest req = TicketAssignRequest.builder().agentId(2L).build();
        assertThatThrownBy(() -> ticketService.assignTicket(1L, req, 1L))
                .isInstanceOf(UnauthorizedException.class);
    }

    // --- Full ticket lifecycle ---

    @Test
    @DisplayName("Full lifecycle: ASSIGNED -> IN_PROGRESS -> RESOLVED -> CLOSED")
    void fullLifecycle() {
        // ASSIGNED -> IN_PROGRESS
        when(ticketRepo.findById(1L)).thenReturn(Optional.of(ticket));
        when(userRepo.findById(2L)).thenReturn(Optional.of(agent));
        when(ticketRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        ticketService.updateStatus(1L, TicketStatusUpdateRequest.builder().status(TicketStatus.IN_PROGRESS).build(), 2L);

        // IN_PROGRESS -> RESOLVED
        ticket.setStatus(TicketStatus.IN_PROGRESS);
        ticketService.updateStatus(1L, TicketStatusUpdateRequest.builder().status(TicketStatus.RESOLVED).resolutionNote("Done").build(), 2L);

        // RESOLVED -> CLOSED (employee)
        ticket.setStatus(TicketStatus.RESOLVED);
        when(userRepo.findById(1L)).thenReturn(Optional.of(employee));
        ticketService.updateStatus(1L, TicketStatusUpdateRequest.builder().status(TicketStatus.CLOSED).build(), 1L);

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CLOSED);
        assertThat(ticket.getClosedAt()).isNotNull();
    }
}
