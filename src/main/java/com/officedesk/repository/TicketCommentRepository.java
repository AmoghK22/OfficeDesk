package com.officedesk.repository;

import com.officedesk.entity.Ticket;
import com.officedesk.entity.TicketComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketCommentRepository extends JpaRepository<TicketComment, Long> {

    List<TicketComment> findByTicketOrderByCreatedAtAsc(Ticket ticket);

    List<TicketComment> findByTicketAndIsInternalFalseOrderByCreatedAtAsc(Ticket ticket);
}
