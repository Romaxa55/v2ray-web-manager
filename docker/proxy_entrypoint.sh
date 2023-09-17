#!/bin/sh
proxy_cfg
java -jar -Xms${XMS} -Xmx${XMX} -XX:MaxDirectMemorySize=${MAX_DIRECT_MEMORY} -XX:MaxMetaspaceSize=${MAX_METASPACE_SIZE} /app/proxy.jar --spring.config.location=/app/proxy.yaml