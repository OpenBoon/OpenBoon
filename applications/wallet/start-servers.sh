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
gunicorn -b :8080 wallet.wsgi &

# Start GCP Marketplace Handler
if [ $MARKETPLACE_ENABLED -eq "true" ];
then
    echo "Starting GCP Marketplace Handler."
    python3 gcp_marketplace_handler/bin/marketplacehandler.py &
fi

# Start node server.
cd frontend
npm start &



# Start nginx gateway server
nginx -g "daemon off;"
