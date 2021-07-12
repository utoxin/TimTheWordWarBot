#!/bin/bash

pip install -r /app/source/requirements.txt
pip install -e /app/source

cd /app

python /app/source/timmy/timmy_bot.py