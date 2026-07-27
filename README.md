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

---

## 🏃 Running Locally (Docker Compose)

### Prerequisites
- Docker + Docker Compose installed

### Steps
```bash
# 1. Copy and fill in your env variables
cp .env.example .env
# Edit .env with your real values

# 2. Start everything
docker-compose up -d

# 3. Open in browser
# Frontend: http://localhost:5173
# Backend API: http://localhost:8084
```

### Demo Accounts (password: `pass123`)
| Email | Role |
|---|---|
| rahul@officedesk.com | Employee |
| vikram@officedesk.com | Agent (IT) |
| deepak@officedesk.com | Dept Head (IT) |
| admin@officedesk.com | Super Admin |

---

## 🔧 Running Locally (Manual Dev Mode)

### Backend
```bash
# Requires Java 17 and PostgreSQL running
cd demo
# Set up local properties (already in .gitignore)
./mvnw spring-boot:run
# Backend starts at http://localhost:8084
```

### Frontend
```bash
cd demo/frontend
npm install
npm run dev
# Frontend starts at http://localhost:5173 (proxies /api to :8084)
```

---

## 🚀 Deployment on Render (Free Tier)

Render is the recommended free platform. You'll deploy three services:

### Step 1: Database — Render PostgreSQL
1. Go to [render.com](https://render.com) → New → PostgreSQL
2. Name: `officedesk-db`, choose Free plan
3. Copy the **Internal Database URL** (used in backend env)

### Step 2: Backend — Render Web Service
1. New → Web Service → Connect your GitHub repo
2. **Root Directory**: `demo`
3. **Build Command**: `./mvnw package -DskipTests -B`
4. **Start Command**: `java -jar target/officedesk-*.jar`
5. **Environment Variables** to set:
   ```
   SPRING_DATASOURCE_URL=<Internal Database URL from step 1>
   SPRING_DATASOURCE_USERNAME=<db username>
   SPRING_DATASOURCE_PASSWORD=<db password>
   JWT_SECRET=<random 64-char string>
   SPRING_PROFILES_ACTIVE=prod
   CORS_ALLOWED_ORIGINS=https://your-frontend.onrender.com
   PORT=8084
   ```
6. Choose **Free** instance type

### Step 3: Frontend — Render Static Site
1. New → Static Site → Connect your GitHub repo
2. **Root Directory**: `demo/frontend`
3. **Build Command**: `npm install && npm run build`
4. **Publish Directory**: `dist`
5. **Environment Variables**:
   ```
   VITE_API_URL=https://your-backend.onrender.com/api
   ```
6. In Render dashboard → Redirects/Rewrites: Add rule `/* → /index.html` (200 rewrite, for SPA routing)

> **Note**: Free tier services on Render spin down after 15 min of inactivity. First request may take ~30s.

---

## 🌐 Alternative: Deploy with Docker on Railway / Fly.io

### Using Docker Compose (Railway)
1. Push your repo to GitHub
2. On Railway: New Project → Deploy from GitHub
3. Add a PostgreSQL plugin
4. Set environment variables as shown above
5. Railway will auto-detect `docker-compose.yml`

---

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
```

---

## 🔐 Security Notes
- **Never commit `.env`** — it is gitignored. Use `.env.example` as template.
- Set `JWT_SECRET` to a random string of at least 32 characters in production.
- Change all default passwords from the DataSeeder before production use.
- Consider setting `spring.jpa.hibernate.ddl-auto=validate` in production instead of `update`.
