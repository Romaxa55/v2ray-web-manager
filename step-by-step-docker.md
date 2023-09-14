# Предварительные условия для использования этого метода

- На вашем сервере установлен Docker
- Вы уже установили v2ray

# Давайте начнем

## Настройка v2ray

```bash
# Резервное копирование стандартной конфигурации v2ray
mv /etc/v2ray/config.json /etc/v2ray/config.json.bak

# Перезапись конфигурации v2ray
echo '{
  "api": {
    "services": [
      "HandlerService",
      "LoggerService",
      "StatsService"
    ],
    "tag": "api"
  },
  "inboundDetour": [
    {
      "listen": "127.0.0.1",
      "port": 62789,
      "protocol": "dokodemo-door",
      "settings": {
        "address": "127.0.0.1"
      },
      "tag": "api"
    }
  ],
  "log": {
    "loglevel": "info"
  },
  "inbounds": [
    {
      "listen": "127.0.0.1",
      "port": 6001,
      "protocol": "vmess",
      "settings": {
        "clients": [],
        "disableInsecureEncryption": false
      },
      "sniffing": {
        "destOverride": [
          "http",
          "tls"
        ],
        "enabled": true
      },
      "streamSettings": {
        "network": "ws",
        "security": "none",
        "wsSettings": {
          "headers": {},
          "path": "/ws/"
        }
      },
      "tag": "6001"
    }
  ],
  "outbounds": [
    {
      "protocol": "freedom",
      "settings": {}
    },
    {
      "protocol": "blackhole",
      "settings": {},
      "tag": "blocked"
    }
  ],
  "policy": {
    "system": {
      "statsInboundDownlink": true,
      "statsInboundUplink": true
    }
  },
  "routing": {
    "rules": [
      {
        "inboundTag": [
          "api"
        ],
        "outboundTag": "api",
        "type": "field"
      },
      {
        "ip": [
          "geoip:private"
        ],
        "outboundTag": "blocked",
        "type": "field"
      },
      {
        "outboundTag": "blocked",
        "protocol": [
          "bittorrent"
        ],
        "type": "field"
      }
    ]
  },
  "stats": {}
}' > /etc/v2ray/config

# Перезапуск v2ray
# service v2ray restart
```

## Настройка параметров контейнера


> Все места, помеченные как пожалуйста, измените, требуют вашего внимания и должны быть изменены в соответствии с вашими потребностями.

```sh
mkdir -p ~/conf
echo "admin:
  #[пожалуйста, измените] логин и пароль при первом запуске
  email: admin@admin.com
  password: 123456

proxy:
  #[пожалуйста, измените] пароль для взаимодействия с proxy, также является приватным ключом для различных токенов
  authPassword: ''
  subscriptionTemplate: /subscribe/%s?type=%s&timestamp=%s&token=%s

email:
  #SMTP адрес
  host: 
  #Имя пользователя
  userName: 
  #Пароль
  password: 
  #Порт
  port: 
  #По умолчанию false, если почта не поддерживает startTls, не включайте
  startTlsEnabled: false

  exceedConnections: Ваши текущие соединения превысили максимальное количество соединений для вашего аккаунта. Система автоматически уменьшила количество ваших соединений на половину на один час. Если вы не достигнете этого предела в течение часа, количество ваших соединений будет восстановлено.
  vCodeTemplate: 'Ваш код подтверждения: %s, используйте его в течение 10 минут'
  overdueDate: Срок действия вашего аккаунта истекает: %s, пожалуйста, обновите его.

logging:
  file: /opt/jar/logs/admin.log
  file.max-history: 7
  level:
    root: info

server:
  port: 9091
  tomcat:
    max-threads: 5
    min-threads: 2
spring:
  datasource:
    driver-class-name: org.sqlite.JDBC
    hikari:
      maximum-pool-size: 5
      minimum-idle: 2
    password: ''
    type: com.zaxxer.hikari.HikariDataSource
    url: jdbc:sqlite:/opt/jar/db/admin.db
    username: ''
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: GMT+8
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: com.jhl.admin.util.SQLiteDialect
        enable_lazy_load_no_trans: true
        format_sql: ''
        show_sql: true
" > ~/conf/admin.yaml
echo "proxy:
  #[пожалуйста, измените] должен совпадать с proxy.authPassword в admin.yaml
  authPassword: ''
  localPort: 8081
  maxConnections: 300

logging:
  file: /opt/jar/logs/v2ray-proxy.log
  file.max-history: 7
  level:
    root: info
manager:
  #Если admin не находится на этом же сервере, измените адрес
  address: http://127.0.0.1:9091
  getProxyAccountUrl: ${manager.address}/proxy/proxyAccount/ac?accountNo={accountNo}&domain={domain}
  reportFlowUrl: ${manager.address}/report/flowStat
  reportOverConnectionLimitUrl: ${manager.address}/report/connectionLimit?accountNo={accountNo}

server:
  port: 8091
  tomcat:
    max-threads: 1
    min-threads: 1
" > ~/conf/proxy.yaml
echo 'server {

  listen 80;
  server_name 127.0.0.1; #[пожалуйста, измените] замените на ваш IP/домен. Если вы используете доменное имя, введите его в браузере.
  root /opt/jar/web;
                
  location /api {
    proxy_pass http://127.0.0.1:9091/;
  }

  location /ws/ {
    proxy_redirect off;
    proxy_pass http://127.0.0.1:8081;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_set_header Host $http_host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
  } 

}' > ~/conf/v2ray-mng.conf
```

## Запуск контейнера

```sh
docker run -d --name v2 --net=host -v ~/conf:/opt/conf greatbody/v2ray-web-manager:0.0.19
```

Теперь вы можете перейти в браузер и ввести `http://IP` или `http://ваш_домен` для доступа.
Убедитесь, что ваша межсетевая экран разрешает входящие запросы на порт 80.

## Примечания

### Некоторые ограничения версии в Docker
* nginx временно не поддерживает tls
* Может работать только на **одном** сервере
* Отладка проблем будет сложнее
* Больше ресурсов в использовании

### Предстоящие улучшения
* Службы admin и proxy в разных контейнерах, можно использовать `docker-compose, Docker Swarm`
* Журналы контейнера поддерживают просмотр соответствующих служебных журналов через `docker logs`
* nginx tls

### Будем рады, если кто-то сможет продолжить работу и предложить RP

Этот документ предоставлен @greatbody.
Перевел @romaxa55


