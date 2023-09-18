#!/bin/sh

# Функция для запуска .jar файла
run_jar() {
    local jar_file=$1
    local cfg_function=$2

    if [ -f "/app/$jar_file" ]; then
        $cfg_function
        # Если файл proxy.jar, запускаем v2ray
        if [ "$jar_file" = "proxy.jar" ]; then
            if [ -f "/app/config.json" ]; then
                /usr/local/bin/v2ray run &
            fi
        fi
        java -jar -Xms${XMS} -Xmx${XMX} -XX:MaxDirectMemorySize=${MAX_DIRECT_MEMORY} -XX:MaxMetaspaceSize=${MAX_METASPACE_SIZE} "/app/$jar_file" --spring.config.location="/app/conf/${jar_file%.jar}.yaml"
    fi
}

# Проверка наличия и запуск proxy.jar
run_jar "proxy.jar" "proxy_cfg"

# Проверка наличия и запуск admin.jar
run_jar "admin.jar" "admin_cfg"
