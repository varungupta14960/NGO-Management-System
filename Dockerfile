# AEGIS single-service deployment image
# Runs Next.js + Spring Boot + MariaDB behind one Nginx public port.

FROM node:20-bookworm AS frontend-build
WORKDIR /src/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
ENV NEXT_PUBLIC_API_URL=/api
RUN npm run build

FROM maven:3.9.9-eclipse-temurin-17 AS backend-build
WORKDIR /src/backend
COPY backend/pom.xml ./
RUN mvn -q -DskipTests dependency:go-offline
COPY backend/src ./src
RUN mvn -q -DskipTests package

FROM node:20-bookworm
ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get update \
    && apt-get install -y --no-install-recommends mariadb-server nginx supervisor openjdk-17-jre-headless ca-certificates bash \
    && rm -rf /var/lib/apt/lists/* \
    && rm -f /etc/nginx/sites-enabled/default

WORKDIR /opt/aegis
COPY --from=frontend-build /src/frontend/.next/standalone ./frontend
COPY --from=frontend-build /src/frontend/.next/static ./frontend/.next/static
COPY --from=backend-build /src/backend/target/ngo-web-backend-1.0.0.jar ./backend.jar
COPY database ./sql
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY supervisord.conf ./supervisord.conf
COPY start.sh ./start.sh
RUN chmod +x ./start.sh \
    && mkdir -p /var/log/nginx /var/log/supervisor /run/mysqld \
    && chown -R mysql:mysql /run/mysqld /var/lib/mysql

ENV PORT=80
EXPOSE 80

CMD ["/opt/aegis/start.sh"]
