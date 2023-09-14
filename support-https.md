# Руководство: Предоставление поддержки TLS для сервиса

Настройка сервера для доступа по HTTPS, чтобы V2ray поддерживал режим TLS.

## 1. Установка фронтенда и бэкенда по руководству для новичков

[Шаг за шагом следуйте моим инструкциям по установке с нуля](https://github.com/romaxa55/v2ray-web-manager/blob/master/step-by-step-install.md)

Установка прошла успешно, и вы можете войти, используя учетные данные администратора.

## 2. Настройка сервера для поддержки HTTPS

Требования:
- Открыт порт 443 на сервере.
- Доменное имя может быть разрешено к IP текущей машины Linux.

### 2.1 Получение SSL-сертификата

Есть два способа получения SSL-сертификата:

#### 2.1.1 (Не рекомендуется) Получение через веб-страницу

Пропустить...

#### 2.1.2 Выдача собственного сертификата, использование cloudflare CDN
Ссылка на руководство - PR

#### 2.1.3 Получение и настройка бесплатного сертификата с помощью acme.sh в командной строке Linux

**1. Установка acme.sh**

```bash
curl https://get.acme.sh | sh
```

**2. Настройка после установки**

Установите acme.sh в домашний каталог: ~/.acme.sh/ и создайте псевдоним для bash, чтобы упростить дальнейшее использование

```bash
alias acme.sh=~/.acme.sh/acme.sh
echo 'alias acme.sh=~/.acme.sh/acme.sh' >>/etc/profile
```

**3. Создание cronjob, который каждый день в 0:00 проверяет сертификат. Если он скоро истечет и требуется обновление, он будет автоматически обновлен (можно проверить с помощью **crontab -l**)**

```bash
00 00 * * * root /root/.acme.sh/acme.sh --cron --home /root/.acme.sh &>/var/log/acme.sh.logs
```

**4. Установка сертификата**

> Используя этот метод установки, сертификат будет обновляться каждые 60 дней.

Используйте команду --installcert и укажите целевое местоположение, затем файлы сертификата будут скопированы в это место!

Пример (для домена: **XXXX.com**):

- Сначала создайте папку для хранения сертификата

```bash
mkdir -p /etc/nginx/ssl_cert/XXXX.com
```

- Установите сертификат

```bash
acme.sh --install-cert -d XXXX.com \
--key-file /etc/nginx/ssl_cert/XXXX.com/XXXX.com.key \
--fullchain-file /etc/nginx/ssl_cert/XXXX.com/XXXX.com.cer \
--reloadcmd  "service nginx force-reload"
```

После успешного выполнения в терминале будет отображено: **Reload success**

### 2.2 Настройка SSL-сертификата в Nginx

> По умолчанию порт 80 перенаправляется на 443

> Конфигурационный файл находится по адресу: **/etc/nginx/conf.d/v2ray-manager.conf**

**Пример конфигурационного файла:**

```
server {
    listen 443 ssl http2;
    server_name XXXX.com;
    root /opt/jar/web;
    ssl_certificate       /etc/nginx/ssl_cert/XXXX.com/XXXX.com.cer;
    ssl_certificate_key   /etc/nginx/ssl_cert/XXXX.com/XXXX.com.key;
    ssl_protocols TLSv1.1 TLSv1.2 TLSv1.3;
    ssl_ciphers TLS13-AES-128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE:ECDH:AES:HIGH:!NULL:!aNULL:!MD5:!ADH:!RC4;

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
}
       
server {
    listen 80;
    server_name XXXX.com;
    return 301 https:///$http_host$request_uri;
}
```

**Примените конфигурационный файл:**

```bash
nginx -s reload
```

## 3. Не забудьте

Посетите свой домен, войдите как администратор и измените информацию о сервисе:

Левая панель меню: 【Сервер】--【Список серверов】,

Найдите сервер, соответствующий IP

   * Домен для доступа - измените на: XXXX.com
   * Порт доступа - измените на: 443
   * Поддержка TLS - измените на: Да

## 4. Использование

Добавьте пользователя, затем войдите в систему управления,

Получите свою ссылку v2ray или адрес подписки!

Теперь вы можете видеть, что ваш сервис и конфигурация v2ray переключены в режим tls!
