#!/bin/sh

cat <<EOL > /app/proxy.yaml
proxy:
  authPassword: ${PROXY_AUTH_PASSWORD:-''}
  localPort: ${PROXY_LOCAL_PORT:-8081}
  maxConnections: ${PROXY_MAX_CONNECTIONS:-300}

logging:
  file: ${LOGGING_FILE_PATH:-/var/logs/v2ray-proxy.log}
  file.max-history: ${LOGGING_FILE_MAX_HISTORY:-7}
  level:
    root: ${LOGGING_LEVEL_ROOT:-info}

manager:
  address: ${MANAGER_ADDRESS:-http://127.0.0.1:9091}
  getProxyAccountUrl: \${manager.address}/proxy/proxyAccount/ac?accountNo={accountNo}&domain={domain}
  reportFlowUrl: \${manager.address}/report/flowStat
  reportOverConnectionLimitUrl: \${manager.address}/report/connectionLimit?accountNo={accountNo}

server:
  port: ${SERVER_PORT:-8091}
  tomcat:
    max-threads: ${TOMCAT_MAX_THREADS:-1}
    min-threads: ${TOMCAT_MIN_THREADS:-1}
EOL
