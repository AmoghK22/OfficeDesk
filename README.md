# OfficeDesk — Corporate Grievance & Support Platform

A full-stack internal ticketing system built with **Spring Boot 3** (Java 17) + **React 19** + **PostgreSQL**.

## ✨ Features
- Role-based access: Employee, Agent, Dept Head, Super Admin
- Ticket lifecycle: RAISED → ASSIGNED → IN_PROGRESS → RESOLVED → CLOSED / REOPENED
- SLA tracking & automated escalation (every 15 minutes via scheduler)
- Department management with auto-assignment to least-loaded agent
- Internal comments (agents only) vs public comments
- Star ratings & feedback on closed tickets
- Admin panel: user management, SLA config
- JWT authentication with forgot/reset password flow
- Paginated, filterable ticket lists with live search


## 📁 Project Structure

```
demo/
├── src/main/java/com/officedesk/
│   ├── config/          # Security, CORS, DataSeeder
│   ├── controller/      # REST endpoints (Auth, Ticket, Admin)
│   ├── dto/             # Request/Response DTOs (organized by domain)
│   ├── entity/          # JPA entities
│   ├── enums/           # Role, TicketStatus, Priority, DepartmentName
│   ├── exception/       # Custom exceptions + GlobalExceptionHandler
│   ├── repository/      # Spring Data JPA repositories
│   ├── security/        # JWT filter, utility, UserDetailsService
│   ├── service/         # Business logic (Ticket, Auth, Admin, Escalation)
│   └── util/            # CategoryDeptMapping
├── frontend/
│   ├── src/
│   │   ├── api/         # Axios instance with interceptors
│   │   ├── components/  # Navbar, SlaCountdown
│   │   ├── context/     # AuthContext, ToastContext
│   │   └── pages/       # Login, Register, Dashboard, TicketList,
│   │                    # TicketDetail, CreateTicket, AdminPanel, ForgotPassword
│   ├── Dockerfile       # Multi-stage: node build → nginx
│   └── nginx.conf       # SPA routing + /api proxy to backend
├── Dockerfile           # Multi-stage Spring Boot build
├── docker-compose.yml   # Orchestrates db + backend + frontend
└── .env.example         # Template for environment variables
