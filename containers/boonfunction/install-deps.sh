#!/bin/bash

FILE=/app/requirements.txt
if [ -f "$FILE" ]; then
    pip3 install -r $FILE
fi
