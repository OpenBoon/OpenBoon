#!/usr/bin/env bash
# Simple script that handles the startup of the servers. The first argument is the
# hostname of the postgres instance to wait for.

# Wait for postgres database to be ready.
until pg_isready -h $PG_HOST; do
  >&2 echo "Postgres is unavailable - waiting 1 second."
  sleep 1
done

# Do any needed database migrations.
cd applications/wallet
python3 ./app/manage.py migrate --no-input

# Start django server.
gunicorn -c python:gunicornconfig wallet.wsgi &

# Start node server.
cd frontend
npm start &

# Start nginx gateway server
nginx -g "daemon off;"
