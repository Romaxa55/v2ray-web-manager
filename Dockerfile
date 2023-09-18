ARG V2RAY_VERSION=5.7.0

FROM openjdk:8-jre-slim as admin
ARG V2RAY_VERSION
ARG JAR
ARG JAR_PATH
ARG SCRIPT
ARG SCRIPT_BIN
ENV XMS=40m
ENV XMX=300m
ENV MAX_DIRECT_MEMORY=300M
ENV MAX_METASPACE_SIZE=300m

WORKDIR /app

ADD --chown=1000:nogroup $JAR $JAR_PATH
COPY $SCRIPT /usr/local/bin/$SCRIPT_BIN

COPY ./docker/entrypoint.sh /entrypoint.sh

# Устанавливаем временную зону, создаем директории, копируем файлы и устанавливаем разрешения
RUN set -x && \
    echo "Europe/Moscow" > /etc/timezone && \
    mkdir db conf && \
    chown -R 1000:nogroup . && \
    chmod +x /usr/local/bin/$SCRIPT_BIN /entrypoint.sh

# Проверка значения SCRIPT_BIN и загрузка бинарного файла, если условие выполняется
RUN if [ "$SCRIPT_BIN" = "proxy_cfg" ]; then \
    apt-get update && \
    apt-get install -y curl unzip bash && \
    ARCH=$(uname -m); \
    case $ARCH in \
      x86_64) \
        V2RAY_FILE="v2ray-linux-64.zip" ;; \
      aarch64) \
        V2RAY_FILE="v2ray-linux-arm64-v8a.zip" ;; \
      *) \
        echo "Unsupported architecture"; exit 1 ;; \
    esac; \
    curl -L -o v2ray.zip https://github.com/v2fly/v2ray-core/releases/download/v$V2RAY_VERSION/$V2RAY_FILE && \
    unzip v2ray.zip && \
    mv v2ray /usr/local/bin/ && \
    mv geoip.dat /usr/local/bin/ && \
    chmod +x /usr/local/bin/v2ray && \
    rm v2ray.zip && \
    apt-get autoremove -y && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*; \
    fi
    
COPY conf/config.json /app/config.json


EXPOSE 9091/tcp
ENTRYPOINT ["/entrypoint.sh"]

