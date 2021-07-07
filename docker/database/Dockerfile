FROM mariadb:latest

COPY ../../*.sql /docker-entrypoint-initdb.d/

EXPOSE 3306

ENTRYPOINT ["/bin/bash", "startup.sh"]