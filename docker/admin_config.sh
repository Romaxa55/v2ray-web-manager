#!/bin/sh


cat <<EOL > /app/conf/admin.yaml
admin:
  email: ${ADMIN_EMAIL:-admin@admin.com}
  password: ${ADMIN_PASSWORD:-123456}

proxy:
  authPassword: ${PROXY_AUTH_PASSWORD:-""}
  subscriptionTemplate: /subscribe/%s?type=%s&timestamp=%s&token=%s

email:
  host: ${SMTP_HOST:-""}
  userName: ${SMTP_USERNAME:-""}
  password: ${SMTP_PASSWORD:-""}
  port: ${SMTP_PORT:-""}
  startTlsEnabled: ${SMTP_STARTTLS_ENABLED:-false}

  exceedConnections: Ваши текущие соединения превышают максимальное ограничение для этой учетной записи. В данный момент система риска автоматически снизит количество ваших соединений наполовину и будет поддерживать это состояние в течение одного часа. Если вы не нарушите мониторинговые критерии в течение этого часа, количество соединений для вашей учетной записи будет восстановлено.
  vCodeTemplate: 'Ваш код подтверждения: %s. Пожалуйста, используйте его в течение 10 минут.'
  overdueDate: Срок действия вашей учетной записи скоро истекает. Пожалуйста, обратите внимание на дату окончания, которая составляет %s, и подумайте о продлении.

logging:
  file: ${LOGGING_FILE_PATH:-/var/log/admin.log}
  file.max-history: 7
  level:
    root: ${LOGGING_LEVEL_ROOT:-info}

server:
  port: ${SERVER_PORT:-9091}
  tomcat:
    max-threads: ${TOMCAT_MAX_THREADS:-5}
    min-threads: ${TOMCAT_MIN_THREADS:-2}

spring:
  datasource:
    driver-class-name: org.sqlite.JDBC
    hikari:
      maximum-pool-size: ${HIKARI_MAX_POOL_SIZE:-5}
      minimum-idle: ${HIKARI_MIN_IDLE:-2}
    password: ${DB_PASSWORD:-""}
    type: com.zaxxer.hikari.HikariDataSource
    url: jdbc:sqlite:${DB_PATH:-./db/admin.db}
    username: ${DB_USERNAME:-""}
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: GMT+3
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: com.jhl.admin.util.SQLiteDialect
        enable_lazy_load_no_trans: true
        format_sql: ''
        show_sql: true

EOL
