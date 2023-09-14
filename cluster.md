В этом руководстве вы узнаете, как добавить новую машину в существующий кластер.

## Предварительные требования

 [Шаг за шагом следуйте моим инструкциям по установке с нуля](https://github.com/romaxa55/v2ray-web-manager/blob/master/step-by-step-install.md)
 
 [Шаг за шагом следуйте моим инструкциям по настройке с нуля](https://github.com/romaxa55/v2ray-web-manager/blob/master/step-by-step-conf.md)
 
 Уже может работать в режиме одной машины.
 
 ## Установка на новой машине

 
 #### 1. Установка необходимого программного обеспечения
  
 - ubuntu    
 ```bash
 # Получить права root
sudo su
# Обновить источники программ
apt-get update
# Установить необходимое программное обеспечение
apt install vim nginx openjdk-8-jre wget unzip  -y
# Установить v2ray - с официального сайта
bash <(curl -L -s https://raw.githubusercontent.com/v2fly/fhs-install-v2ray/master/install-release.sh)

 ```
 - CentOS
 ```bash
sudo su
yum update
yum makecache
yum install epel-release
# Установить необходимое программное обеспечение
yum install vim nginx java-1.8.0-openjdk wget unzip -y
# Установить v2ray - с официального сайта
bash <(curl -L -s https://raw.githubusercontent.com/v2fly/fhs-install-v2ray/master/install-release.sh)
 ```
    
####  2. Настройка nginx
```bash
# Перейти в папку с конфигурационными файлами nginx
cd /etc/nginx/conf.d
vi v2ray-manager.conf
```
> Скопируйте следующую конфигурацию, i для редактирования, правая кнопка мыши для вставки (может отличаться в разных SSH-клиентах)
> ESC :wq для выхода и сохранения

```
server {

  listen 80 ;
  server_name 127.0.0.1; # Измените на свой IP/домен
 

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
```

```bash
# Применить настройки nginx
# Если нет ошибок, то настройка прошла успешно
nginx -s reload
```        
####  3. Загрузка файлов releases

 [Страница релизов java-сервиса](https://github.com/romaxa55/v2ray-web-manager/releases)
    
 ```bash
# Создать директорию
mkdir /opt/jar -p
cd /opt/jar 

# Загрузить пакет releases
wget -c https://glare.now.sh/romaxa55/v2ray-web-manager/v2ray-proxy -O v2ray-proxy.jar

 ```
 
 
 ####  4. Настройка
      
 ```bash

# Загрузить файл конфигурации прокси-сервиса
wget -c --no-check-certificate https://raw.githubusercontent.com/romaxa55/v2ray-web-manager/master/conf/proxy.yaml

# Загрузить специальный файл конфигурации v2ray
wget -c --no-check-certificate https://raw.githubusercontent.com/romaxa55/v2ray-web-manager/master/conf/config.json
```  
 
Настройте файл proxy в соответствии со своими требованиями, вы можете загрузить его на свой компьютер, внести изменения и затем загрузить обратно в /opt/jar/, сохраняя кодировку UTF-8.    
 - В proxy.yaml вам нужно будет настроить следующее:
          
      proxy:
      # Измените на ту же строку, что и в admin.yaml
        authPassword: ''
        
      manager:
          # 【Внимание】Измените на адрес машины, на которой находится admin, и убедитесь, что telnet работает
          address:  http://127.0.0.1:9091
  
  
 ####  5. Настройка v2ray
 
 ```bash
# Создать резервную копию стандартной конфигурации v2ray
mv /usr/local/etc/v2ray/config.json /usr/local/etc/v2ray/config.json.bak

# Скопировать конфигурацию в директорию v2ray
cp /opt/jar/config.json /usr/local/etc/v2ray/

# Перезапустить v2ray
service v2ray restart
 ```
      
 ####  6. Запуск java
      
 ```bash

# Запустить v2ray-proxy
nohup java -jar -Xms40m -Xmx40m -XX:MaxDirectMemorySize=10M -XX:MaxMetaspaceSize=80m /opt/jar/v2ray-proxy.jar --spring.config.location=/opt/jar/proxy.yaml > /dev/null 2>&1 &
 ```
 
 ####  7. Просмотр логов
 ```bash

# Проверить, запущен ли процесс java
ps -ef |grep java 

# Просмотреть лог v2ray-proxy
tail -100f /opt/jar/logs/v2ray-proxy.log

# Просмотреть ошибки в логе v2ray-proxy (версия > v3.1.5)
tail -100f /opt/jar/logs/v2ray-proxy.log.ERROR
    
# ctrl+c для выхода из просмотра лога

 ```
 
 ### 8.Административный интерфейс
 
Смотрите： [Шаг за шагом следуйте моим инструкциям по настройке с нуля](https://github.com/romaxa55/v2ray-web-manager/blob/master/step-by-step-conf.md)

Где домен для доступа указывает на IP/домен текущей машины.

### 9.Поддержка HTTPS

Смотрите：[Средний уровень - предоставление поддержки tls (https/wss)](https://github.com/romaxa55/v2ray-web-manager/blob/master/support-https.md)

Если поддержка TLS распространяется на поддомены, просто скопируйте файлы TLS на текущую машину и настройте их.

Обратите внимание: домены не должны совпадать. Например, если домен машины 1 - test.test.com, домен машины 2 не может быть таким же, он должен быть, например, test2.test.com. Административный интерфейс различает серверы по разным IP/доменам.

Теория： [Высший уровень - режимы](https://github.com/romaxa55/v2ray-web-manager/blob/master/step-by-step-model.md)

      
 
  
