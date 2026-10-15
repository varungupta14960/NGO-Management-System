# AEGIS — Single-Service Deployment

This version packages the Next.js frontend, Spring Boot API and MariaDB database into one Docker service. Nginx exposes one public port and proxies `/api/*` to Spring Boot.

## Railway
1. Push the repository to GitHub.
2. Create a Railway project and connect the repository.
3. Railway detects the root `Dockerfile` automatically.
4. Generate a public domain in the service Networking settings.
5. Add a Railway Volume mounted at `/var/lib/mysql` so database data survives redeploys.

No separate MySQL/Aiven service is required. On first startup, the container creates `ngo_db`, loads `database.sql`, `sample_data.sql`, and `views.sql`, then starts the API and frontend.

## Important
This is designed for a college demonstration / single-service deployment. For a production system, run the database as a managed service instead.
