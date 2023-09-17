#!/bin/sh
admin_cfg
java -jar -Xms${XMS} -Xmx${XMX} -XX:MaxDirectMemorySize=${MAX_DIRECT_MEMORY} -XX:MaxMetaspaceSize=${MAX_METASPACE_SIZE} /app/admin.jar --spring.config.location=/app/admin.yaml