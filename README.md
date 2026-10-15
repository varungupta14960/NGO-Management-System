# AEGIS — NGO Management System (Web Edition)

A polished web rehaul of the original MySQL + Java Swing NGO Management System.

## What changed

The original database design is retained, including its 13 normalized tables, business-rule triggers, reporting views and transaction-oriented workflows. The desktop Swing UI is replaced with:

- Next.js responsive frontend
- Spring Boot REST API
- MySQL database
- Modern AEGIS dashboard
- Searchable CRUD pages
- Donation, approval, distribution and assignment workflows
- Seven live SQL report endpoints
- Vercel-ready frontend deployment
- Docker-ready Java backend deployment

## Folders

```text
webapp/
├── frontend/       # Vercel / Next.js
├── backend/        # Spring Boot / REST API
├── database/       # MySQL schema + sample data + views
├── documentation/  # Original project documentation
└── DEPLOYMENT.md
```

See `DEPLOYMENT.md` for the exact deployment sequence.
