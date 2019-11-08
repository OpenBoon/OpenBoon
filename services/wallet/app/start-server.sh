#!/usr/bin/env bash
# Simple scripts that handles the startup of the server. The first argument is the
# hostname of the postgres instance to wait for.

# Wait for postgres database to be ready.
host="$1"
until pg_isready -h $host; do
  >&2 echo "Postgres is unavailable - sleeping"
  sleep 1
done

# Do any needed database migrations.
./manage.py migrate --no-input

# Start the server.
gunicorn -b :8080 wallet.wsgi
