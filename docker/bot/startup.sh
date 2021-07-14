#!/bin/bash

pip install -e /app/source

cd /app

while :
do
  python /app/source/timmy/timmy_bot.py
  sleep 10
done