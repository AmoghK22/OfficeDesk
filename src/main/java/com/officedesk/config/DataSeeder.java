package com.officedesk.config;

import com.officedesk.entity.*;
import com.officedesk.enums.*;
import com.officedesk.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Profile("!test")
public class DataSeeder implements CommandLineRunner {

    private final DepartmentRepository deptRepo;
    private final UserRepository userRepo;
    private final SlaConfigRepository slaConfigRepo;
    private final TicketRepository ticketRepo;
    private final TicketCommentRepository commentRepo;
    private final TicketRatingRepository ratingRepo;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(DepartmentRepository deptRepo, UserRepository userRepo,
                      SlaConfigRepository slaConfigRepo, TicketRepository ticketRepo,
                      TicketCommentRepository commentRepo, TicketRatingRepository ratingRepo,
                      PasswordEncoder passwordEncoder) {
        this.deptRepo = deptRepo;
        this.userRepo = userRepo;
        this.slaConfigRepo = slaConfigRepo;
        this.ticketRepo = ticketRepo;
        this.commentRepo = commentRepo;
        this.ratingRepo = ratingRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (deptRepo.count() > 0) return;

        // Departments
        Department it = deptRepo.save(Department.builder().name(DepartmentName.IT).build());
        Department hr = deptRepo.save(Department.builder().name(DepartmentName.HR).build());
        Department finance = deptRepo.save(Department.builder().name(DepartmentName.FINANCE).build());
        Department facilities = deptRepo.save(Department.builder().name(DepartmentName.FACILITIES).build());

        // Users
        User admin = userRepo.save(User.builder()
                .name("Admin").email("admin@officedesk.com")
                .password(passwordEncoder.encode("admin123"))
                .role(Role.SUPER_ADMIN).isActive(true).build());

        User head1 = userRepo.save(User.builder()
                .name("Deepak Mehta").email("deepak@officedesk.com")
                .password(passwordEncoder.encode("pass123"))
                .role(Role.DEPT_HEAD).department(it).isActive(true).build());
        User head2 = userRepo.save(User.builder()
                .name("Kavita Joshi").email("kavita@officedesk.com")
                .password(passwordEncoder.encode("pass123"))
                .role(Role.DEPT_HEAD).department(hr).isActive(true).build());
        User head3 = userRepo.save(User.builder()
                .name("Rajesh Iyer").email("rajesh@officedesk.com")
                .password(passwordEncoder.encode("pass123"))
                .role(Role.DEPT_HEAD).department(finance).isActive(true).build());
        User head4 = userRepo.save(User.builder()
                .name("Sunita Rao").email("sunita@officedesk.com")
                .password(passwordEncoder.encode("pass123"))
                .role(Role.DEPT_HEAD).department(facilities).isActive(true).build());

        // Set dept heads
        it.setHead(head1); deptRepo.save(it);
        hr.setHead(head2); deptRepo.save(hr);
        finance.setHead(head3); deptRepo.save(finance);
        facilities.setHead(head4); deptRepo.save(facilities);

        User agent1 = userRepo.save(User.builder()
                .name("Vikram Singh").email("vikram@officedesk.com")
                .password(passwordEncoder.encode("pass123"))
                .role(Role.AGENT).department(it).isActive(true).build());
        User agent2 = userRepo.save(User.builder()
                .name("Neha Gupta").email("neha@officedesk.com")
                .password(passwordEncoder.encode("pass123"))
                .role(Role.AGENT).department(hr).isActive(true).build());
        User agent3 = userRepo.save(User.builder()
                .name("Amit Kumar").email("amit@officedesk.com")
                .password(passwordEncoder.encode("pass123"))
                .role(Role.AGENT).department(finance).isActive(true).build());
        User agent4 = userRepo.save(User.builder()
                .name("Sanjay Verma").email("sanjay@officedesk.com")
                .password(passwordEncoder.encode("pass123"))
                .role(Role.AGENT).department(facilities).isActive(true).build());

        User emp1 = userRepo.save(User.builder()
                .name("Rahul Sharma").email("rahul@officedesk.com")
                .password(passwordEncoder.encode("pass123"))
                .role(Role.EMPLOYEE).department(it).isActive(true).build());
        User emp2 = userRepo.save(User.builder()
                .name("Priya Patel").email("priya@officedesk.com")
                .password(passwordEncoder.encode("pass123"))
                .role(Role.EMPLOYEE).department(hr).isActive(true).build());

        // SLA configs
        slaConfigRepo.save(SlaConfig.builder().department(it).priority(Priority.LOW).resolutionHours(72).build());
        slaConfigRepo.save(SlaConfig.builder().department(it).priority(Priority.MEDIUM).resolutionHours(48).build());
        slaConfigRepo.save(SlaConfig.builder().department(it).priority(Priority.HIGH).resolutionHours(24).build());
        slaConfigRepo.save(SlaConfig.builder().department(it).priority(Priority.CRITICAL).resolutionHours(4).build());

        slaConfigRepo.save(SlaConfig.builder().department(hr).priority(Priority.LOW).resolutionHours(96).build());
        slaConfigRepo.save(SlaConfig.builder().department(hr).priority(Priority.MEDIUM).resolutionHours(72).build());
        slaConfigRepo.save(SlaConfig.builder().department(hr).priority(Priority.HIGH).resolutionHours(48).build());
        slaConfigRepo.save(SlaConfig.builder().department(hr).priority(Priority.CRITICAL).resolutionHours(8).build());

        slaConfigRepo.save(SlaConfig.builder().department(finance).priority(Priority.LOW).resolutionHours(96).build());
        slaConfigRepo.save(SlaConfig.builder().department(finance).priority(Priority.MEDIUM).resolutionHours(72).build());
        slaConfigRepo.save(SlaConfig.builder().department(finance).priority(Priority.HIGH).resolutionHours(48).build());
        slaConfigRepo.save(SlaConfig.builder().department(finance).priority(Priority.CRITICAL).resolutionHours(8).build());

        slaConfigRepo.save(SlaConfig.builder().department(facilities).priority(Priority.LOW).resolutionHours(72).build());
        slaConfigRepo.save(SlaConfig.builder().department(facilities).priority(Priority.MEDIUM).resolutionHours(48).build());
        slaConfigRepo.save(SlaConfig.builder().department(facilities).priority(Priority.HIGH).resolutionHours(24).build());
        slaConfigRepo.save(SlaConfig.builder().department(facilities).priority(Priority.CRITICAL).resolutionHours(4).build());

        // Sample tickets
        Ticket t1 = ticketRepo.save(Ticket.builder()
                .ticketNo("TKT-2026-0001")
                .title("Laptop not booting")
                .description("My Dell Latitude laptop is not booting since morning. Blue screen error.")
                .status(TicketStatus.IN_PROGRESS)
                .priority(Priority.HIGH)
                .category("Hardware")
                .department(it)
                .raisedBy(emp1)
                .assignedTo(agent1)
                .slaDeadline(LocalDateTime.now().plusHours(20))
                .createdAt(LocalDateTime.now().minusHours(5))
                .build());

        Ticket t2 = ticketRepo.save(Ticket.builder()
                .ticketNo("TKT-2026-0002")
                .title("Salary not credited")
                .description("My salary for June has not been credited. Others in team received theirs.")
                .status(TicketStatus.ASSIGNED)
                .priority(Priority.CRITICAL)
                .category("Salary")
                .department(hr)
                .raisedBy(emp2)
                .assignedTo(head2)
                .slaDeadline(LocalDateTime.now().plusHours(6))
                .slaBreached(true)
                .escalated(true)
                .createdAt(LocalDateTime.now().minusHours(50))
                .build());

        Ticket t3 = ticketRepo.save(Ticket.builder()
                .ticketNo("TKT-2026-0003")
                .title("AC not working in cabin 3B")
                .description("The AC in cabin 3B has been malfunctioning for 2 days.")
                .status(TicketStatus.RAISED)
                .priority(Priority.MEDIUM)
                .category("AC")
                .department(facilities)
                .raisedBy(emp1)
                .slaDeadline(LocalDateTime.now().plusHours(46))
                .createdAt(LocalDateTime.now().minusHours(2))
                .build());

        Ticket t4 = ticketRepo.save(Ticket.builder()
                .ticketNo("TKT-2026-0004")
                .title("Leave balance discrepancy")
                .description("My leave balance shows 5 but should be 12.")
                .status(TicketStatus.CLOSED)
                .priority(Priority.LOW)
                .category("Leave")
                .department(hr)
                .raisedBy(emp2)
                .assignedTo(agent2)
                .resolutionNote("Leave balance corrected from 5 to 12. System sync issue.")
                .slaDeadline(LocalDateTime.now().plusHours(72))
                .closedAt(LocalDateTime.now().minusHours(24))
                .createdAt(LocalDateTime.now().minusDays(5))
                .build());

        commentRepo.save(TicketComment.builder()
                .ticket(t1).postedBy(agent1)
                .comment("Looking into it. Will visit your desk in 30 mins.")
                .isInternal(false).build());

        commentRepo.save(TicketComment.builder()
                .ticket(t2).postedBy(head2)
                .comment("Escalated to finance team. Waiting for their response.")
                .isInternal(true).build());

        ratingRepo.save(TicketRating.builder()
                .ticket(t4).ratedBy(emp2)
                .rating(4).feedback("Quick resolution. Thanks!").build());
    }
}
