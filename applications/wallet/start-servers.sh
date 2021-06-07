#!/usr/bin/env bash
# Simple script that handles the startup of the servers. The first argument is the
# hostname of the postgres instance to wait for.
set -e

# Wait for postgres database to be ready.
until pg_isready -h $PG_HOST; do
  >&2 echo "Postgres is unavailable - waiting 1 second."
  sleep 1
done

# Wait for archivist to be ready.
until $(curl --output /dev/null --silent --head --fail ${BOONAI_API_URL}/monitor/health); do
  >&2 echo 'Archivist is unavailable - waiting 5 seconds.'
  sleep 5
done

# Do any needed database migrations.
cd applications/wallet
python3 ./app/manage.py migrate --no-input

# Start django server.
gunicorn -c python:gunicornconfig wallet.wsgi &

# Start celery queue
celery -A wallet worker --loglevel=info &
celery -A wallet beat -l info --scheduler django_celery_beat.schedulers:DatabaseScheduler &

# Start node server.
cd frontend
npm start &

# Start nginx gateway server
nginx -g "daemon off;"
