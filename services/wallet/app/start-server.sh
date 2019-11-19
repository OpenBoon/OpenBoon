#!/usr/bin/env bash
# Simple scripts that handles the startup of the server. The first argument is the
# hostname of the postgres instance to wait for.

# Wait for postgres database to be ready.
until pg_isready -h $PG_HOST; do
  >&2 echo "Postgres is unavailable - waiting 1 second."
  sleep 1
done

# Do any needed database migrations.
python3 ./manage.py migrate --no-input

# Start the server.
gunicorn -b :8080 wallet.wsgi &
nginx -g "daemon off;"
