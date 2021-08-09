#!/bin/bash

FILE=/app/function/requirements.txt
if [ -f "$FILE" ]; then
    pip3 install -r $FILE
fi
