package com.officedesk.repository;

import com.officedesk.entity.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Page<Ticket> findByRaisedById(Long userId, Pageable pageable);

    Page<Ticket> findByDepartmentId(Long deptId, Pageable pageable);

    Page<Ticket> findByAssignedToId(Long userId, Pageable pageable);

    Page<Ticket> findByDepartmentIdAndStatus(Long deptId, com.officedesk.enums.TicketStatus status, Pageable pageable);

    Page<Ticket> findByStatusIn(List<com.officedesk.enums.TicketStatus> statuses, Pageable pageable);

    boolean existsByTicketNo(String ticketNo);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.assignedTo.id = :agentId AND t.status = :status")
    long countByAssignedToIdAndStatus(@Param("agentId") Long agentId, @Param("status") com.officedesk.enums.TicketStatus status);

    List<Ticket> findBySlaDeadlineBeforeAndStatusNotIn(LocalDateTime now, List<com.officedesk.enums.TicketStatus> statuses);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.department.id = :deptId AND t.status = :status")
    long countByDepartmentIdAndStatus(@Param("deptId") Long deptId, @Param("status") com.officedesk.enums.TicketStatus status);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.department.id = :deptId AND t.slaBreached = true")
    long countByDepartmentIdAndSlaBreachedTrue(@Param("deptId") Long deptId);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.department.id = :deptId")
    long countByDepartmentId(@Param("deptId") Long deptId);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.department.id = :deptId AND t.status NOT IN ('RESOLVED', 'CLOSED')")
    long countOpenByDepartmentId(@Param("deptId") Long deptId);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.department.id = :deptId AND t.status = 'IN_PROGRESS'")
    long countInProgressByDepartmentId(@Param("deptId") Long deptId);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.department.id = :deptId AND t.status = 'RESOLVED'")
    long countResolvedByDepartmentId(@Param("deptId") Long deptId);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.department.id = :deptId AND t.status = 'CLOSED'")
    long countClosedByDepartmentId(@Param("deptId") Long deptId);

    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM TicketRating r WHERE r.ticket.department.id = :deptId")
    double avgRatingByDeptId(@Param("deptId") Long deptId);

    List<Ticket> findByAssignedToIdAndStatus(Long agentId, com.officedesk.enums.TicketStatus status);

    @Query(value = "SELECT t.* FROM tickets t WHERE t.raised_by = CAST(:userId AS bigint) " +
           "AND (CAST(:status AS varchar) IS NULL OR t.status = CAST(:status AS varchar)) " +
           "AND (CAST(:priority AS varchar) IS NULL OR t.priority = CAST(:priority AS varchar)) " +
           "AND (CAST(:search AS varchar) IS NULL OR lower(t.title) LIKE lower('%' || CAST(:search AS varchar) || '%') OR lower(t.ticket_no) LIKE lower('%' || CAST(:search AS varchar) || '%')) " +
           "ORDER BY t.created_at DESC",
           countQuery = "SELECT COUNT(*) FROM tickets t WHERE t.raised_by = CAST(:userId AS bigint) " +
           "AND (CAST(:status AS varchar) IS NULL OR t.status = CAST(:status AS varchar)) " +
           "AND (CAST(:priority AS varchar) IS NULL OR t.priority = CAST(:priority AS varchar)) " +
           "AND (CAST(:search AS varchar) IS NULL OR lower(t.title) LIKE lower('%' || CAST(:search AS varchar) || '%') OR lower(t.ticket_no) LIKE lower('%' || CAST(:search AS varchar) || '%'))",
           nativeQuery = true)
    Page<Ticket> findByRaisedByIdWithFilters(@Param("userId") Long userId, @Param("status") String status,
                                              @Param("priority") String priority, @Param("search") String search, Pageable pageable);

    @Query(value = "SELECT t.* FROM tickets t WHERE t.assigned_to = CAST(:agentId AS bigint) " +
           "AND (CAST(:status AS varchar) IS NULL OR t.status = CAST(:status AS varchar)) " +
           "AND (CAST(:priority AS varchar) IS NULL OR t.priority = CAST(:priority AS varchar)) " +
           "AND (CAST(:search AS varchar) IS NULL OR lower(t.title) LIKE lower('%' || CAST(:search AS varchar) || '%') OR lower(t.ticket_no) LIKE lower('%' || CAST(:search AS varchar) || '%')) " +
           "ORDER BY t.created_at DESC",
           countQuery = "SELECT COUNT(*) FROM tickets t WHERE t.assigned_to = CAST(:agentId AS bigint) " +
           "AND (CAST(:status AS varchar) IS NULL OR t.status = CAST(:status AS varchar)) " +
           "AND (CAST(:priority AS varchar) IS NULL OR t.priority = CAST(:priority AS varchar)) " +
           "AND (CAST(:search AS varchar) IS NULL OR lower(t.title) LIKE lower('%' || CAST(:search AS varchar) || '%') OR lower(t.ticket_no) LIKE lower('%' || CAST(:search AS varchar) || '%'))",
           nativeQuery = true)
    Page<Ticket> findByAssignedToIdWithFilters(@Param("agentId") Long agentId, @Param("status") String status,
                                               @Param("priority") String priority, @Param("search") String search, Pageable pageable);

    @Query(value = "SELECT t.* FROM tickets t WHERE t.department_id = CAST(:deptId AS bigint) " +
           "AND (CAST(:status AS varchar) IS NULL OR t.status = CAST(:status AS varchar)) " +
           "AND (CAST(:priority AS varchar) IS NULL OR t.priority = CAST(:priority AS varchar)) " +
           "AND (CAST(:search AS varchar) IS NULL OR lower(t.title) LIKE lower('%' || CAST(:search AS varchar) || '%') OR lower(t.ticket_no) LIKE lower('%' || CAST(:search AS varchar) || '%')) " +
           "ORDER BY t.created_at DESC",
           countQuery = "SELECT COUNT(*) FROM tickets t WHERE t.department_id = CAST(:deptId AS bigint) " +
           "AND (CAST(:status AS varchar) IS NULL OR t.status = CAST(:status AS varchar)) " +
           "AND (CAST(:priority AS varchar) IS NULL OR t.priority = CAST(:priority AS varchar)) " +
           "AND (CAST(:search AS varchar) IS NULL OR lower(t.title) LIKE lower('%' || CAST(:search AS varchar) || '%') OR lower(t.ticket_no) LIKE lower('%' || CAST(:search AS varchar) || '%'))",
           nativeQuery = true)
    Page<Ticket> findByDepartmentIdWithFilters(@Param("deptId") Long deptId, @Param("status") String status,
                                               @Param("priority") String priority, @Param("search") String search, Pageable pageable);

    @Query(value = "SELECT t.* FROM tickets t " +
           "WHERE (CAST(:status AS varchar) IS NULL OR t.status = CAST(:status AS varchar)) " +
           "AND (CAST(:priority AS varchar) IS NULL OR t.priority = CAST(:priority AS varchar)) " +
           "AND (CAST(:search AS varchar) IS NULL OR lower(t.title) LIKE lower('%' || CAST(:search AS varchar) || '%') OR lower(t.ticket_no) LIKE lower('%' || CAST(:search AS varchar) || '%')) " +
           "ORDER BY t.created_at DESC",
           countQuery = "SELECT COUNT(*) FROM tickets t " +
           "WHERE (CAST(:status AS varchar) IS NULL OR t.status = CAST(:status AS varchar)) " +
           "AND (CAST(:priority AS varchar) IS NULL OR t.priority = CAST(:priority AS varchar)) " +
           "AND (CAST(:search AS varchar) IS NULL OR lower(t.title) LIKE lower('%' || CAST(:search AS varchar) || '%') OR lower(t.ticket_no) LIKE lower('%' || CAST(:search AS varchar) || '%'))",
           nativeQuery = true)
    Page<Ticket> findAllWithFilters(@Param("status") String status, @Param("priority") String priority,
                                      @Param("search") String search, Pageable pageable);

    @Query(value = "SELECT t.* FROM tickets t INNER JOIN ticket_ratings r ON t.id = r.ticket_id ORDER BY r.created_at DESC",
           countQuery = "SELECT COUNT(*) FROM tickets t INNER JOIN ticket_ratings r ON t.id = r.ticket_id",
           nativeQuery = true)
    Page<Ticket> findRatedTickets(Pageable pageable);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.raisedBy.id = :userId")
    long countByRaisedById(@Param("userId") Long userId);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.raisedBy.id = :userId AND t.status NOT IN ('RESOLVED', 'CLOSED')")
    long countOpenByRaisedById(@Param("userId") Long userId);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.raisedBy.id = :userId AND t.status = 'IN_PROGRESS'")
    long countInProgressByRaisedById(@Param("userId") Long userId);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.raisedBy.id = :userId AND t.status = 'RESOLVED'")
    long countResolvedByRaisedById(@Param("userId") Long userId);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.raisedBy.id = :userId AND t.status = 'CLOSED'")
    long countClosedByRaisedById(@Param("userId") Long userId);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.raisedBy.id = :userId AND t.slaBreached = true")
    long countBreachedByRaisedById(@Param("userId") Long userId);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.assignedTo.id = :agentId")
    long countByAssignedToId(@Param("agentId") Long agentId);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.assignedTo.id = :agentId AND t.status NOT IN ('RESOLVED', 'CLOSED')")
    long countOpenByAssignedToId(@Param("agentId") Long agentId);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.assignedTo.id = :agentId AND t.status = 'IN_PROGRESS'")
    long countInProgressByAssignedToId(@Param("agentId") Long agentId);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.assignedTo.id = :agentId AND t.status = 'RESOLVED'")
    long countResolvedByAssignedToId(@Param("agentId") Long agentId);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.assignedTo.id = :agentId AND t.status = 'CLOSED'")
    long countClosedByAssignedToId(@Param("agentId") Long agentId);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.assignedTo.id = :agentId AND t.slaBreached = true")
    long countBreachedByAssignedToId(@Param("agentId") Long agentId);

    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM TicketRating r WHERE r.ticket.assignedTo.id = :agentId")
    double avgRatingByAgentId(@Param("agentId") Long agentId);

    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM TicketRating r")
    double avgRatingAll();

    @Query("SELECT COUNT(t) FROM Ticket t")
    long countAll();

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.status NOT IN ('RESOLVED', 'CLOSED')")
    long countOpenAll();

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.status = 'IN_PROGRESS'")
    long countInProgressAll();

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.status = 'RESOLVED'")
    long countResolvedAll();

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.status = 'CLOSED'")
    long countClosedAll();

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.slaBreached = true")
    long countBreachedAll();
}
