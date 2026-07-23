package com.officedesk.service;

import com.officedesk.entity.Ticket;
import com.officedesk.enums.TicketStatus;
import com.officedesk.repository.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class EscalationService {

    private static final Logger log = LoggerFactory.getLogger(EscalationService.class);

    private final TicketRepository ticketRepo;

    public EscalationService(TicketRepository ticketRepo) {
        this.ticketRepo = ticketRepo;
    }

    @Scheduled(fixedRate = 900000)
    public void checkAndEscalateBreachedTickets() {
        List<TicketStatus> closedStatuses = List.of(TicketStatus.RESOLVED, TicketStatus.CLOSED);
        List<Ticket> breachedTickets = ticketRepo
                .findBySlaDeadlineBeforeAndStatusNotIn(LocalDateTime.now(), closedStatuses);

        for (Ticket ticket : breachedTickets) {
            if (!ticket.isEscalated()) {
                ticket.setSlaBreached(true);
                ticket.setEscalated(true);
                ticketRepo.save(ticket);
                log.info("Ticket {} breached SLA — escalated to dept head of {}",
                        ticket.getTicketNo(), ticket.getDepartment().getName());
            }
        }
    }
}
