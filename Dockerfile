FROM openjdk:8-jre-alpine3.9 as admin
ARG JAR
ARG JAR_PATH

ENV XMS=40m
ENV XMX=300m
ENV MAX_DIRECT_MEMORY=300M
ENV MAX_METASPACE_SIZE=300m

WORKDIR /app

ADD --chown=1000:nogroup $JAR $JAR_PATH
COPY ./docker/admin_config.sh /usr/local/bin/admin_cfg

COPY ./docker/entrypoint.sh /entrypoint.sh

# Устанавливаем временную зону, создаем директории, копируем файлы и устанавливаем разрешения
RUN set -x && \
    echo "Europe/Moscow" > /etc/timezone && \
    mkdir db conf && \
    chown -R 1000:nogroup . && \
    chmod +x /usr/local/bin/admin_cfg /entrypoint.sh

EXPOSE 9091/tcp
ENTRYPOINT ["/entrypoint.sh"]

