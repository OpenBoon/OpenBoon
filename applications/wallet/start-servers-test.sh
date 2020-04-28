#!/usr/bin/env bash


# Start GCP Marketplace Handler
if [ $MARKETPLACE_ENABLED -eq "true" ];
then
    echo "Starting GCP Marketplace Handler."
    python3 gcp_marketplace_handler/bin/marketplacehandler.py &
fi

echo "Done"
