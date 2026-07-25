package com.officedesk.controller;

import com.officedesk.dto.ticket.*;
import com.officedesk.entity.User;
import com.officedesk.enums.Priority;
import com.officedesk.enums.TicketStatus;
import com.officedesk.security.SecurityUtils;
import com.officedesk.security.JwtAuthDetails;
import com.officedesk.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    public ResponseEntity<TicketResponse> create(@Valid @RequestBody TicketCreateRequest req) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.createTicket(req, userId));
    }

    @GetMapping("/my")
    public ResponseEntity<Page<TicketResponse>> myTickets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) String search) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ticketService.getMyTickets(userId, PageRequest.of(page, size), status, priority, search));
    }

    @GetMapping("/dashboard/stats")
    public ResponseEntity<DashboardStats> dashboardStats() {
        JwtAuthDetails details = (JwtAuthDetails) SecurityContextHolder.getContext().getAuthentication().getDetails();
        return ResponseEntity.ok(ticketService.getDashboardStats(details.getUserId(), details.getRole()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> getById(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ticketService.getTicketById(id, userId));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<TicketResponse> updateStatus(@PathVariable Long id,
                                                       @Valid @RequestBody TicketStatusUpdateRequest req) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ticketService.updateStatus(id, req, userId));
    }

    @PutMapping("/{id}/assign")
    public ResponseEntity<TicketResponse> assign(@PathVariable Long id,
                                                 @Valid @RequestBody TicketAssignRequest req) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ticketService.assignTicket(id, req, userId));
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<CommentResponse> addComment(@PathVariable Long id,
                                                      @Valid @RequestBody CommentRequest req) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.addComment(id, req, userId));
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<List<CommentResponse>> getComments(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ticketService.getComments(id, userId));
    }

    @GetMapping("/{id}/activities")
    public ResponseEntity<List<ActivityResponse>> getActivities(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ticketService.getActivities(id, userId));
    }

    @PostMapping("/{id}/reopen")
    public ResponseEntity<TicketResponse> reopen(@PathVariable Long id,
                                                 @Valid @RequestBody ReopenRequest req) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ticketService.reopenTicket(id, req, userId));
    }

    @PostMapping("/{id}/rate")
    public ResponseEntity<Void> rate(@PathVariable Long id,
                                     @Valid @RequestBody RatingRequest req) {
        Long userId = SecurityUtils.getCurrentUserId();
        ticketService.rateTicket(id, req, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/dept/{deptId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPT_HEAD', 'AGENT')")
    public ResponseEntity<Page<TicketResponse>> deptTickets(
            @PathVariable Long deptId,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ticketService.getDeptTickets(deptId, status, priority, search, PageRequest.of(page, size)));
    }

    @GetMapping("/agent/{agentId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPT_HEAD', 'AGENT')")
    public ResponseEntity<Page<TicketResponse>> agentTickets(
            @PathVariable Long agentId,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ticketService.getAgentTickets(agentId, status, priority, search, PageRequest.of(page, size)));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPT_HEAD')")
    public ResponseEntity<Page<TicketResponse>> allTickets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(ticketService.getAllTickets(status, priority, search, PageRequest.of(page, size)));
    }

    @GetMapping("/dept/{deptId}/agents")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DEPT_HEAD')")
    public ResponseEntity<List<Map<String, Object>>> deptAgents(@PathVariable Long deptId) {
        List<User> agents = ticketService.getDeptAgents(deptId);
        List<Map<String, Object>> result = agents.stream().map(a -> Map.<String, Object>of(
                "id", a.getId(),
                "name", a.getName(),
                "email", a.getEmail()
        )).toList();
        return ResponseEntity.ok(result);
    }
}
