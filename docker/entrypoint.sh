#!/bin/sh

# Функция для запуска .jar файла
run_jar() {
    local jar_file=$1
    local cfg_function=$2

    if [ -f "/app/$jar_file" ]; then
        $cfg_function
        java -jar -Xms${XMS} -Xmx${XMX} -XX:MaxDirectMemorySize=${MAX_DIRECT_MEMORY} -XX:MaxMetaspaceSize=${MAX_METASPACE_SIZE} "/app/$jar_file" --spring.config.location="/app/${jar_file%.jar}.yaml"
    fi
}

# Проверка наличия и запуск v2ray-proxy.jar
run_jar "v2ray-proxy.jar" "proxy_cfg"

# Проверка наличия и запуск admin.jar
run_jar "admin.jar" "admin_cfg"
