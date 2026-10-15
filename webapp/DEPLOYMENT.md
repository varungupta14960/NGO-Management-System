# AEGIS Web Deployment

This folder is the web rehaul of the original Java Swing NGO Management System.

## Architecture

- `frontend/` — Next.js frontend. Deploy this to Vercel.
- `backend/` — Spring Boot REST API. Deploy this to Railway/Render/Fly.io or another Java host.
- `database/` — original MySQL schema, sample data and reporting views.

## 1. Create the MySQL database

Create a MySQL 8 database and run, in this order:

1. `database/database.sql`
2. `database/sample_data.sql`
3. `database/views.sql`

If your provider already gives you a database and does not allow `CREATE DATABASE`, remove the first three database-selection lines from `database/database.sql` (`DROP DATABASE`, `CREATE DATABASE`, `USE ngo_db`) and select your provider's database before executing it. For `sample_data.sql` and `views.sql`, remove `USE ngo_db` if necessary.

## 2. Deploy the backend

Use the `backend/` directory as the service root, or deploy the repository with the backend Dockerfile.

Environment variables:

```text
DB_URL=jdbc:mysql://HOST:3306/DATABASE?useSSL=true&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USER=YOUR_DB_USER
DB_PASSWORD=YOUR_DB_PASSWORD
FRONTEND_URL=https://YOUR-VERCEL-DOMAIN.vercel.app
PORT=8080
```

The API health endpoint is:

```text
https://YOUR-BACKEND-DOMAIN/api/health
```

It should return JSON containing `"ok":true`.

## 3. Deploy the frontend to Vercel

Set the Vercel project root directory to:

```text
frontend
```

Build command:

```text
npm run build
```

Environment variable:

```text
NEXT_PUBLIC_API_URL=https://YOUR-BACKEND-DOMAIN/api
```

After deployment the submission link is the Vercel URL.

## 4. Local development

Backend:

```bash
cd backend
mvn spring-boot:run
```

Frontend:

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:3000`.

## Important

The public web application is not the old Swing application. The old Java desktop source remains in the parent project for academic/reference purposes. The new web version keeps the original MySQL schema and exposes the database through a REST API, then presents it through a modern responsive frontend.
