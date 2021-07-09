FROM python:slim

WORKDIR /app

COPY startup.sh /app
COPY . /app
