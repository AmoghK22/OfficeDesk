package com.officedesk.service;

import com.officedesk.dto.ticket.*;
import com.officedesk.entity.*;
import com.officedesk.enums.Priority;
import com.officedesk.enums.Role;
import com.officedesk.enums.TicketStatus;
import com.officedesk.exception.*;
import com.officedesk.repository.*;
import com.officedesk.util.CategoryDeptMapping;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;

@Service
public class TicketService {

    private final TicketRepository ticketRepo;
    private final UserRepository userRepo;
    private final DepartmentRepository deptRepo;
    private final SlaConfigRepository slaConfigRepo;
    private final TicketCommentRepository commentRepo;
    private final TicketRatingRepository ratingRepo;
    private final TicketActivityRepository activityRepo;

    public TicketService(TicketRepository ticketRepo, UserRepository userRepo,
                         DepartmentRepository deptRepo, SlaConfigRepository slaConfigRepo,
                         TicketCommentRepository commentRepo, TicketRatingRepository ratingRepo,
                         TicketActivityRepository activityRepo) {
        this.ticketRepo = ticketRepo;
        this.userRepo = userRepo;
        this.deptRepo = deptRepo;
        this.slaConfigRepo = slaConfigRepo;
        this.commentRepo = commentRepo;
        this.ratingRepo = ratingRepo;
        this.activityRepo = activityRepo;
    }

    @Transactional
    public TicketResponse createTicket(TicketCreateRequest req, Long userId) {
        User employee = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Department dept = deptRepo.findByName(CategoryDeptMapping.getDept(req.getCategory()))
                .orElseThrow(() -> new ResourceNotFoundException("Department not found for category"));

        User agent = findLeastLoadedAgent(dept);
        LocalDateTime slaDeadline = calculateSlaDeadline(dept, req.getPriority());
        String ticketNo = generateTicketNo();

        Ticket ticket = Ticket.builder()
                .ticketNo(ticketNo)
                .title(req.getTitle())
                .description(req.getDescription())
                .status(TicketStatus.ASSIGNED)
                .priority(req.getPriority())
                .category(req.getCategory())
                .department(dept)
                .raisedBy(employee)
                .assignedTo(agent)
                .slaDeadline(slaDeadline)
                .slaBreached(false)
                .escalated(false)
                .build();

        ticket = ticketRepo.save(ticket);
        logActivity(ticket, "CREATED", "Ticket created and assigned to " + agent.getName(), employee);
        logActivity(ticket, "ASSIGNED", "Auto-assigned to " + agent.getName() + " (" + dept.getName().name() + ")", employee);
        return mapToResponse(ticket);
    }

    public Page<TicketResponse> getMyTickets(Long userId, Pageable pageable, TicketStatus status, Priority priority, String search) {
        return ticketRepo.findByRaisedByIdWithFilters(userId, status != null ? status.name() : null, priority != null ? priority.name() : null, search, pageable).map(this::mapToResponse);
    }

    public TicketResponse getTicketById(Long ticketId, Long userId) {
        Ticket ticket = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketId));

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!canAccessTicket(ticket, user)) {
            throw new UnauthorizedException("You do not have access to this ticket");
        }
        return mapToResponse(ticket);
    }

    @Transactional
    public TicketResponse updateStatus(Long ticketId, TicketStatusUpdateRequest req, Long agentId) {
        Ticket ticket = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        User agent = userRepo.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!canModifyTicket(ticket, agent)) {
            throw new UnauthorizedException("You do not have permission to modify this ticket");
        }

        // Employees may only CLOSE a resolved ticket via this endpoint
        if (agent.getRole() == Role.EMPLOYEE && req.getStatus() != TicketStatus.CLOSED) {
            throw new UnauthorizedException("Employees can only close a resolved ticket");
        }

        validateStatusTransition(ticket.getStatus(), req.getStatus());

        TicketStatus oldStatus = ticket.getStatus();
        ticket.setStatus(req.getStatus());

        if (req.getStatus() == TicketStatus.RESOLVED && req.getResolutionNote() != null) {
            ticket.setResolutionNote(req.getResolutionNote());
        }
        if (req.getStatus() == TicketStatus.CLOSED) {
            ticket.setClosedAt(LocalDateTime.now());
        }

        ticket = ticketRepo.save(ticket);
        String desc = oldStatus + " -> " + req.getStatus();
        if (req.getStatus() == TicketStatus.RESOLVED && req.getResolutionNote() != null) {
            desc += " | Note: " + req.getResolutionNote();
        }
        logActivity(ticket, "STATUS_CHANGED", desc, agent);
        return mapToResponse(ticket);
    }

    @Transactional
    public TicketResponse assignTicket(Long ticketId, TicketAssignRequest req, Long headId) {
        Ticket ticket = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        User head = userRepo.findById(headId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (head.getRole() != Role.DEPT_HEAD && head.getRole() != Role.SUPER_ADMIN) {
            throw new UnauthorizedException("Only department head or admin can assign tickets");
        }

        if (head.getRole() == Role.DEPT_HEAD) {
            Long headDeptId = head.getDepartment() != null ? head.getDepartment().getId() : null;
            if (!ticket.getDepartment().getId().equals(headDeptId)) {
                throw new UnauthorizedException("Cannot assign tickets from other departments");
            }
        }

        User agent = userRepo.findById(req.getAgentId())
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found"));

        if (agent.getRole() != Role.AGENT && agent.getRole() != Role.DEPT_HEAD) {
            throw new UnauthorizedException("Can only assign to agents or department heads");
        }
        if (head.getRole() == Role.DEPT_HEAD) {
            Long agentDeptId = agent.getDepartment() != null ? agent.getDepartment().getId() : null;
            if (!ticket.getDepartment().getId().equals(agentDeptId)) {
                throw new UnauthorizedException("Cannot assign agents from other departments");
            }
        }

        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new InvalidStatusTransitionException("Cannot reassign a CLOSED ticket. Reopen it first.");
        }

        String oldAgent = ticket.getAssignedTo() != null ? ticket.getAssignedTo().getName() : "Unassigned";
        ticket.setAssignedTo(agent);

        if (ticket.getStatus() != TicketStatus.IN_PROGRESS) {
            ticket.setStatus(TicketStatus.ASSIGNED);
        }

        ticket = ticketRepo.save(ticket);
        logActivity(ticket, "ASSIGNED", "Reassigned from " + oldAgent + " to " + agent.getName(), head);
        return mapToResponse(ticket);
    }

    @Transactional
    public CommentResponse addComment(Long ticketId, CommentRequest req, Long userId) {
        Ticket ticket = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!canAccessTicket(ticket, user)) {
            throw new UnauthorizedException("You do not have access to this ticket");
        }

        boolean internal = req.isInternal() && (user.getRole() == Role.AGENT || user.getRole() == Role.DEPT_HEAD || user.getRole() == Role.SUPER_ADMIN);

        TicketComment comment = TicketComment.builder()
                .ticket(ticket)
                .postedBy(user)
                .comment(req.getComment())
                .isInternal(internal)
                .build();

        comment = commentRepo.save(comment);
        String visibility = internal ? " (internal)" : "";
        logActivity(ticket, "COMMENT_ADDED", "Comment by " + user.getName() + visibility, user);
        return mapToResponse(comment);
    }

    public List<CommentResponse> getComments(Long ticketId, Long userId) {
        Ticket ticket = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<TicketComment> comments;
        if (user.getRole() == Role.EMPLOYEE) {
            comments = commentRepo.findByTicketAndIsInternalFalseOrderByCreatedAtAsc(ticket);
        } else {
            comments = commentRepo.findByTicketOrderByCreatedAtAsc(ticket);
        }
        return comments.stream().map(this::mapToResponse).toList();
    }

    public List<ActivityResponse> getActivities(Long ticketId, Long userId) {
        Ticket ticket = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() == Role.EMPLOYEE && !ticket.getRaisedBy().getId().equals(userId)) {
            throw new UnauthorizedException("You do not have access to this ticket's activity");
        }

        return activityRepo.findByTicketIdOrderByCreatedAtAsc(ticketId)
                .stream().map(this::mapToResponse).toList();
    }

    @Transactional
    public TicketResponse reopenTicket(Long ticketId, ReopenRequest req, Long userId) {
        Ticket ticket = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        if (!ticket.getRaisedBy().getId().equals(userId)) {
            throw new UnauthorizedException("Only the ticket owner can reopen");
        }
        if (ticket.getStatus() != TicketStatus.RESOLVED) {
            throw new InvalidStatusTransitionException("Can only reopen a RESOLVED ticket");
        }

        User user = userRepo.findById(userId).orElseThrow();
        ticket.setStatus(TicketStatus.REOPENED);
        ticket.setSlaBreached(false);
        ticket.setEscalated(false);
        ticket.setSlaDeadline(LocalDateTime.now().plusHours(
                slaConfigRepo.findByDepartmentIdAndPriority(ticket.getDepartment().getId(), ticket.getPriority())
                        .map(c -> c.getResolutionHours())
                        .orElse(48)
        ));

        ticket = ticketRepo.save(ticket);
        logActivity(ticket, "REOPENED", "Reason: " + req.getReason(), user);
        return mapToResponse(ticket);
    }

    @Transactional
    public void rateTicket(Long ticketId, RatingRequest req, Long userId) {
        Ticket ticket = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        if (ticket.getStatus() != TicketStatus.CLOSED) {
            throw new InvalidStatusTransitionException("Can only rate a CLOSED ticket");
        }
        if (!ticket.getRaisedBy().getId().equals(userId)) {
            throw new UnauthorizedException("Only the ticket owner can rate");
        }
        if (ratingRepo.findByTicket(ticket).isPresent()) {
            throw new TicketAlreadyRatedException("Ticket has already been rated");
        }

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        TicketRating rating = TicketRating.builder()
                .ticket(ticket)
                .ratedBy(user)
                .rating(req.getRating())
                .feedback(req.getFeedback())
                .build();

        ratingRepo.save(rating);
        logActivity(ticket, "RATED", "Rated " + req.getRating() + "/5" + (req.getFeedback() != null ? " - " + req.getFeedback() : ""), user);
    }

    public Page<TicketResponse> getDeptTickets(Long deptId, TicketStatus status, Priority priority, String search, Pageable pageable) {
        return ticketRepo.findByDepartmentIdWithFilters(deptId, status != null ? status.name() : null, priority != null ? priority.name() : null, search, pageable).map(this::mapToResponse);
    }

    public Page<TicketResponse> getAgentTickets(Long agentId, TicketStatus status, Priority priority, String search, Pageable pageable) {
        return ticketRepo.findByAssignedToIdWithFilters(agentId, status != null ? status.name() : null, priority != null ? priority.name() : null, search, pageable).map(this::mapToResponse);
    }

    public Page<TicketResponse> getAllTickets(TicketStatus status, Priority priority, String search, Pageable pageable) {
        return ticketRepo.findAllWithFilters(status != null ? status.name() : null, priority != null ? priority.name() : null, search, pageable).map(this::mapToResponse);
    }

    public List<User> getDeptAgents(Long deptId) {
        return userRepo.findByDepartmentIdAndRoleAndIsActive(deptId, Role.AGENT, true);
    }

    public DashboardStats getDashboardStats(Long userId, String role) {
        long total, open, inProgress, resolved, closed, breached;
        double avgRating;

        if ("EMPLOYEE".equals(role)) {
            total = ticketRepo.countByRaisedById(userId);
            open = ticketRepo.countOpenByRaisedById(userId);
            inProgress = ticketRepo.countInProgressByRaisedById(userId);
            resolved = ticketRepo.countResolvedByRaisedById(userId);
            closed = ticketRepo.countClosedByRaisedById(userId);
            breached = ticketRepo.countBreachedByRaisedById(userId);
            avgRating = 0;
        } else if ("AGENT".equals(role)) {
            total = ticketRepo.countByAssignedToId(userId);
            open = ticketRepo.countOpenByAssignedToId(userId);
            inProgress = ticketRepo.countInProgressByAssignedToId(userId);
            resolved = ticketRepo.countResolvedByAssignedToId(userId);
            closed = ticketRepo.countClosedByAssignedToId(userId);
            breached = ticketRepo.countBreachedByAssignedToId(userId);
            avgRating = ticketRepo.avgRatingByAgentId(userId);
        } else if ("DEPT_HEAD".equals(role)) {
            User user = userRepo.findById(userId).orElse(null);
            Long deptId = user != null && user.getDepartment() != null ? user.getDepartment().getId() : null;
            if (deptId != null) {
                total = ticketRepo.countByDepartmentId(deptId);
                open = ticketRepo.countOpenByDepartmentId(deptId);
                inProgress = ticketRepo.countInProgressByDepartmentId(deptId);
                resolved = ticketRepo.countResolvedByDepartmentId(deptId);
                closed = ticketRepo.countClosedByDepartmentId(deptId);
                breached = ticketRepo.countByDepartmentIdAndSlaBreachedTrue(deptId);
                avgRating = ticketRepo.avgRatingByDeptId(deptId);
            } else {
                total = 0; open = 0; inProgress = 0; resolved = 0; closed = 0; breached = 0; avgRating = 0;
            }
        } else {
            total = ticketRepo.countAll();
            open = ticketRepo.countOpenAll();
            inProgress = ticketRepo.countInProgressAll();
            resolved = ticketRepo.countResolvedAll();
            closed = ticketRepo.countClosedAll();
            breached = ticketRepo.countBreachedAll();
            avgRating = ticketRepo.avgRatingAll();
        }

        return DashboardStats.builder()
                .total(total).open(open).inProgress(inProgress)
                .resolved(resolved).closed(closed).breached(breached)
                .avgRating(Math.round(avgRating * 10.0) / 10.0)
                .build();
    }

    public Page<TicketResponse> getRatedTickets(Pageable pageable) {
        return ticketRepo.findRatedTickets(pageable).map(this::mapToResponse);
    }

    // ---- Activity ----

    private void logActivity(Ticket ticket, String action, String description, User performedBy) {
        activityRepo.save(TicketActivity.builder()
                .ticket(ticket)
                .action(action)
                .description(description)
                .performedBy(performedBy)
                .build());
    }

    // ---- Private helpers ----

    private boolean canModifyTicket(Ticket ticket, User user) {
        if (user.getRole() == Role.SUPER_ADMIN) return true;
        if (user.getRole() == Role.DEPT_HEAD &&
            ticket.getDepartment().getId().equals(user.getDepartment() != null ? user.getDepartment().getId() : null)) return true;
        if (ticket.getAssignedTo() != null && ticket.getAssignedTo().getId().equals(user.getId())) return true;
        if (ticket.getRaisedBy().getId().equals(user.getId())) return true;
        return false;
    }

    private boolean canAccessTicket(Ticket ticket, User user) {
        if (user.getRole() == Role.SUPER_ADMIN) return true;
        if (user.getRole() == Role.DEPT_HEAD &&
            ticket.getDepartment().getId().equals(user.getDepartment() != null ? user.getDepartment().getId() : null)) return true;
        if (ticket.getRaisedBy().getId().equals(user.getId())) return true;
        if (ticket.getAssignedTo() != null && ticket.getAssignedTo().getId().equals(user.getId())) return true;
        return false;
    }

    private User findLeastLoadedAgent(Department dept) {
        List<User> agents = userRepo.findByDepartmentIdAndRoleAndIsActive(dept.getId(), Role.AGENT, true);
        if (agents.isEmpty()) {
            throw new ResourceNotFoundException("No active agents found in department: " + dept.getName());
        }
        return agents.stream()
                .min((a, b) -> Long.compare(
                        ticketRepo.countByAssignedToIdAndStatus(a.getId(), TicketStatus.IN_PROGRESS),
                        ticketRepo.countByAssignedToIdAndStatus(b.getId(), TicketStatus.IN_PROGRESS)))
                .orElse(agents.get(0));
    }

    private LocalDateTime calculateSlaDeadline(Department dept, Priority priority) {
        return slaConfigRepo.findByDepartmentIdAndPriority(dept.getId(), priority)
                .map(config -> LocalDateTime.now().plusHours(config.getResolutionHours()))
                .orElse(LocalDateTime.now().plusHours(48));
    }

    private synchronized String generateTicketNo() {
        int year = Year.now().getValue();
        long count = ticketRepo.countByYear(year) + 1;
        String ticketNo = String.format("TKT-%d-%04d", year, count);
        int safety = 0;
        while (safety < 100) {
            try {
                if (!ticketRepo.existsByTicketNo(ticketNo)) {
                    return ticketNo;
                }
            } catch (DataIntegrityViolationException ignored) {
            }
            count++;
            ticketNo = String.format("TKT-%d-%04d", year, count);
            safety++;
        }
        return ticketNo;
    }

    private void validateStatusTransition(TicketStatus current, TicketStatus next) {
        boolean valid = switch (current) {
            case RAISED -> next == TicketStatus.ASSIGNED;
            case ASSIGNED -> next == TicketStatus.IN_PROGRESS;
            case IN_PROGRESS -> next == TicketStatus.RESOLVED;
            case RESOLVED -> next == TicketStatus.CLOSED || next == TicketStatus.REOPENED;
            case REOPENED -> next == TicketStatus.ASSIGNED || next == TicketStatus.IN_PROGRESS;
            case CLOSED -> false;
        };
        if (!valid) {
            throw new InvalidStatusTransitionException(
                    "Cannot transition from " + current + " to " + next);
        }
    }

    private TicketResponse mapToResponse(Ticket t) {
        long slaHours = 24;
        if (t.getSlaDeadline() != null && t.getCreatedAt() != null) {
            long h = Duration.between(t.getCreatedAt(), t.getSlaDeadline()).toHours();
            if (h > 0) slaHours = h;
        }
        return TicketResponse.builder()
                .id(t.getId())
                .ticketNo(t.getTicketNo())
                .title(t.getTitle())
                .description(t.getDescription())
                .status(t.getStatus().name())
                .priority(t.getPriority().name())
                .category(t.getCategory())
                .departmentName(t.getDepartment().getName().name())
                .departmentId(t.getDepartment().getId())
                .raisedByName(t.getRaisedBy().getName())
                .raisedById(t.getRaisedBy().getId())
                .assignedToName(t.getAssignedTo() != null ? t.getAssignedTo().getName() : null)
                .assignedToId(t.getAssignedTo() != null ? t.getAssignedTo().getId() : null)
                .slaDeadline(t.getSlaDeadline())
                .slaBreached(t.isSlaBreached())
                .escalated(t.isEscalated())
                .resolutionNote(t.getResolutionNote())
                .createdAt(t.getCreatedAt())
                .closedAt(t.getClosedAt())
                .slaHours(slaHours)
                .build();
    }

    private CommentResponse mapToResponse(TicketComment c) {
        return CommentResponse.builder()
                .id(c.getId())
                .postedByName(c.getPostedBy().getName())
                .comment(c.getComment())
                .isInternal(c.isInternal())
                .createdAt(c.getCreatedAt())
                .build();
    }

    private ActivityResponse mapToResponse(TicketActivity a) {
        return ActivityResponse.builder()
                .id(a.getId())
                .action(a.getAction())
                .description(a.getDescription())
                .performedByName(a.getPerformedBy().getName())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
