FROM mariadb:latest
ARG DEBIAN_FRONTEND=noninteractive

WORKDIR /app

RUN apt-get update

RUN apt-get -y install python3
RUN apt-get -y install python3-pip
RUN apt-get -y install inspircd

COPY . .

RUN pip install -r requirements.txt

#EXPOSE 6667

ENTRYPOINT ["/bin/bash", "startup.sh"]