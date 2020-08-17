#!/usr/bin/env bash

until pg_isready -h $PG_HOST; do
  >&2 echo "Postgres is unavailable - waiting 1 second."
  sleep 1
done

echo "Starting GCP Marketplace Handler."
python3 -u /applications/wallet/gcp_marketplace_tools/bin/marketplace_pubsub_handler.py
