#!/bin/bash
set -euo pipefail

DATADIR=/var/lib/mysql
SOCKET=/run/mysqld/mysqld.sock

mkdir -p /run/mysqld "$DATADIR"
chown -R mysql:mysql /run/mysqld "$DATADIR"

if [ ! -d "$DATADIR/mysql" ]; then
  echo "[AEGIS] Initializing MariaDB data directory..."
  mariadb-install-db --user=mysql --datadir="$DATADIR" >/dev/null
fi

echo "[AEGIS] Starting MariaDB..."
su -s /bin/bash mysql -c "mariadbd --datadir=$DATADIR --socket=$SOCKET --bind-address=127.0.0.1 --port=3306" >/var/log/mariadb.log 2>&1 &

for i in $(seq 1 60); do
  if mariadb-admin --socket="$SOCKET" ping --silent >/dev/null 2>&1; then break; fi
  sleep 1
done
if ! mariadb-admin --socket="$SOCKET" ping --silent >/dev/null 2>&1; then
  echo "[AEGIS] MariaDB failed to start"
  cat /var/log/mariadb.log || true
  exit 1
fi

if [ ! -f "$DATADIR/.aegis_initialized" ]; then
  echo "[AEGIS] Creating database and loading AEGIS schema..."
  mariadb --socket="$SOCKET" -uroot <<'SQL'
CREATE DATABASE IF NOT EXISTS ngo_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'aegis'@'localhost' IDENTIFIED BY 'aegis';
CREATE USER IF NOT EXISTS 'aegis'@'127.0.0.1' IDENTIFIED BY 'aegis';
GRANT ALL PRIVILEGES ON ngo_db.* TO 'aegis'@'localhost';
GRANT ALL PRIVILEGES ON ngo_db.* TO 'aegis'@'127.0.0.1';
FLUSH PRIVILEGES;
SQL
  mariadb --socket="$SOCKET" -uroot < /opt/aegis/sql/database.sql
  mariadb --socket="$SOCKET" -uroot < /opt/aegis/sql/sample_data.sql
  mariadb --socket="$SOCKET" -uroot < /opt/aegis/sql/views.sql
  touch "$DATADIR/.aegis_initialized"
  echo "[AEGIS] Database initialized."
fi

exec /usr/bin/supervisord -c /opt/aegis/supervisord.conf
