# Новичок-руководство по установке

Только на CentOS7/ ubuntu 16 + протестирован следующий процесс установки.

#### 1. Установка необходимого ПО

- ubuntu/debian （рекомендуется）
```bash
# Получить права root
sudo su
# Обновить репозитории
apt-get update
# Установить необходимые программы
apt install vim nginx openjdk-8-jre wget unzip  -y
# Установить v2ray - с официального сайта новая версия
bash <(curl -L -s https://raw.githubusercontent.com/v2fly/fhs-install-v2ray/master/install-release.sh) --version 5.3.0

```
- CentOS (есть проблемы связанные с selinux)
```bash
sudo su
yum update
yum makecache
yum install epel-release
# Установить необходимые программы
yum install vim nginx java-1.8.0-openjdk wget unzip -y
# Установить v2ray - с официального сайта
bash <(curl -L -s https://raw.githubusercontent.com/v2fly/fhs-install-v2ray/master/install-release.sh) --version 5.3.0
```

       
####  2. Настройка nginx
```bash
# Перейти в директорию с конфигурационными файлами nginx
cd /etc/nginx/conf.d
vi v2ray-manager.conf
```
> Скопируйте приведенную ниже конфигурацию, используйте i для редактирования, правый клик мыши для вставки (может отличаться в разных SSH-клиентах).
> Нажмите ESC, затем :wq для сохранения и выхода.

```
server {

  listen 80 ;
  server_name 127.0.0.1; #Замените на свой IP-адрес или доменное имя
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

}
```

```bash
# Применить настройки nginx
# Если выполнение прошло без ошибок, значит, конфигурация успешна
nginx -s reload
```        
            
####  3. Загрузка файлов из раздела "releases"


 [Страница релизов Java-сервиса](https://github.com/romaxa55/v2ray-web-manager/releases)
 
 [Страница релизов веб-интерфейса](https://github.com/romaxa55/v2ray-manager-console/releases)

```bash
# Создать директорию
mkdir /opt/jar -p
cd /opt/jar 

# Загрузить файлы релизов
wget -c https://glare.now.sh/master-coder-ll/v2ray-web-manager/admin -O admin.jar
wget -c https://glare.now.sh/master-coder-ll/v2ray-manager-console/dist -O dist.zip
wget -c https://glare.now.sh/master-coder-ll/v2ray-web-manager/v2ray-proxy -O v2ray-proxy.jar

# Распаковать веб-интерфейс в директорию "web"
unzip dist.zip  -d web

# Завершение развертывания веб-проекта
```


####  4. Настройка
     
```bash
# Загрузить конфигурационный файл управляющего сервиса
wget -c --no-check-certificate https://raw.githubusercontent.com/master-coder-ll/v2ray-web-manager/master/conf/admin.yaml

# Загрузить конфигурационный файл прокси-сервиса
wget -c --no-check-certificate https://raw.githubusercontent.com/master-coder-ll/v2ray-web-manager/master/conf/proxy.yaml

# Загрузить специализированный конфигурационный файл для v2ray
wget -c --no-check-certificate https://raw.githubusercontent.com/master-coder-ll/v2ray-web-manager/master/conf/config.json

```  

Настройте файлы конфигурации admin и proxy в соответствии со своими потребностями. Вы можете загрузить их на свой компьютер, внести изменения и затем загрузить обратно в /opt/jar/, убедившись, что используется кодировка UTF-8.  
- В файле admin.yaml вам потребуется настроить следующие параметры：

    # Все параметры после двоеточия должны быть отделены пробелом 
      email: 
         #Адрес SMTP
         host: 
         #Имя пользователя
         userName: 
         #Пароль
         password:
         #Порт
         port: 
         #По умолчанию false, если почта не поддерживает startTls, не активируйте
         startTlsEnabled: false
      
      proxy:
       #Пароль для взаимодействия с прокси, также используется как приватный ключ для различных токенов
       authPassword: ''
       
      admin:
        #Логин и пароль при первом запуске
        email: admin@admin.com
        password: 123456


- В файле proxy.yaml вам потребуется настроить следующие параметры:
         
      proxy:
        authPassword: ''
        
      manager:
          # Если сервис admin находится не на этом же сервере, измените адрес
          address:  http://127.0.0.1:9091
 
В моем случае регистрация пользователей не требуется, и сервисы admin и proxy находятся на одном и том же сервере. Мне нужно было изменить только параметр proxy.authPassword в обоих конфигурационных файлах на случайную строку, например, 1234abc. Для входа в систему я использовал стандартные учетные данные администратора.
 
####  5. Настройка v2ray

```bash
# Создать резервную копию стандартной конфигурации v2ray
mv /usr/local/etc/v2ray/config.json /usr/local/etc/v2ray/config.json.bak

# Копировать конфигурацию в директорию v2ray
cp /opt/jar/config.json /usr/local/etc/v2ray/

# Перезапустить v2ray
service v2ray stop
service v2ray start
```
     
####  6. Запуск Java
     
```bash
# Создать стандартный каталог базы данных
mkdir /opt/jar/db -p

# Запустить admin
nohup java -jar -Xms40m -Xmx40m -XX:MaxDirectMemorySize=10M -XX:MaxMetaspaceSize=80m  /opt/jar/admin.jar --spring.config.location=/opt/jar/admin.yaml > /dev/null 2>&1 &

# Запустить v2ray-proxy
nohup java -jar -Xms40m -Xmx40m -XX:MaxDirectMemorySize=10M -XX:MaxMetaspaceSize=80m /opt/jar/v2ray-proxy.jar --spring.config.location=/opt/jar/proxy.yaml > /dev/null 2>&1 &
```

####  7. Просмотр логов
```bash
# Просмотреть логи admin
tail -100f /opt/jar/logs/admin.log

# Просмотреть ошибки в логах admin (версия > v3.1.5)
tail -100f /opt/jar/logs/admin.log.ERROR
    
# Просмотреть логи v2ray-proxy
tail -100f /opt/jar/logs/v2ray-proxy.log

# Просмотреть ошибки в логах v2ray-proxy (версия > v3.1.5)
tail -100f /opt/jar/logs/v2ray-proxy.log.ERROR
    
# ctrl+c для выхода из просмотра логов
```


### Все завершено, развертывание успешно!
Если при посещении http://ip отображается страница входа и вы успешно вошли в систему с учетной записью администратора, это означает, что сервис успешно запущен.
