package com.officedesk.repository;

import com.officedesk.entity.Ticket;
import com.officedesk.entity.TicketRating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TicketRatingRepository extends JpaRepository<TicketRating, Long> {

    Optional<TicketRating> findByTicket(Ticket ticket);

    Optional<TicketRating> findByTicketId(Long ticketId);
}
