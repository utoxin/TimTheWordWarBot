FROM python:slim

WORKDIR /app

ENV VIRTUAL_ENV=/opt/venv
RUN python -m venv $VIRTUAL_ENV
ENV PATH="$VIRTUAL_ENV/bin:$PATH"

COPY requirements.txt .

RUN pip install -r /app/requirements.txt

ENTRYPOINT /bin/bash /app/source/docker/bot/startup.sh
