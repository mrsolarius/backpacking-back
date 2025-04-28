# Guide de déploiement de l'application Backpacking Backend sur Linux

Ce guide vous aidera à déployer l'application Backpacking Backend sur un environnement Linux en suivant une approche étape par étape.

## Table des matières
1. [Installation des dépendances nécessaires](#1-installation-des-dépendances-nécessaires)
2. [Configuration de l'application pour la production](#2-configuration-de-lapplication-pour-la-production)
3. [Construction de l'application](#3-construction-de-lapplication)
4. [Intégration avec systemd](#4-intégration-avec-systemd)
5. [Configuration du service pour démarrage automatique](#5-configuration-du-service-pour-démarrage-automatique)
6. [Gestion du service](#6-gestion-du-service)
7. [Vérification du fonctionnement](#7-vérification-du-fonctionnement)

## 1. Installation des dépendances nécessaires

### Java 21
L'application nécessite Java 21 pour fonctionner, comme indiqué dans le fichier `build.gradle`.

```bash
# Mise à jour des paquets
sudo apt update

# Installation d'OpenJDK 21
sudo apt install -y openjdk-21-jdk

# Vérification de l'installation
java -version
```

### PostgreSQL
L'application utilise PostgreSQL comme base de données.

```bash
# Installation de PostgreSQL
sudo apt install -y postgresql postgresql-contrib

# Démarrage du service PostgreSQL
sudo systemctl start postgresql
sudo systemctl enable postgresql

# Configuration de la base de données
sudo -u postgres psql -c "CREATE USER backpaking WITH PASSWORD 'password';"
sudo -u postgres psql -c "CREATE DATABASE backpaking WITH OWNER backpaking;"
sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE backpaking TO backpaking;"
```

### FFmpeg
L'application utilise FFmpeg pour la conversion d'images.

```bash
# Installation de FFmpeg
sudo apt install -y ffmpeg

# Vérification de l'installation
ffmpeg -version
```

### Git (pour le clone du dépôt)

```bash
# Installation de Git
sudo apt install -y git
```

## 2. Configuration de l'application pour la production

### Structure de répertoires

```bash
# Création des répertoires d'application
sudo mkdir -p /opt/backpaking
sudo mkdir -p /opt/backpaking/logs
sudo mkdir -p /opt/backpaking/uploads
sudo mkdir -p /var/log/backpaking
```

### Clone du dépôt

```bash
# Clonage du dépôt dans un répertoire temporaire
git clone <url-du-repo> /tmp/backpaking-src
cd /tmp/backpaking-src
```

### Création du fichier de configuration application.properties

Créez un fichier `application.properties` pour la production dans `/opt/backpaking/application.properties`:

```bash
sudo nano /opt/backpaking/application.properties
```

Ajoutez le contenu suivant :

```properties
# Configuration de base de données
spring.datasource.url=jdbc:postgresql://localhost:5432/backpaking
spring.datasource.username=backpaking
spring.datasource.password=password
spring.datasource.driver-class-name=org.postgresql.Driver

# Configuration JPA
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# Configuration Liquibase
spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.yaml

# Configuration de l'application
app.upload.dir=/opt/backpaking/uploads
app.root.url=https://votre-domaine.com
app.webp.quality=80
app.image.format=jpg

# Configuration JWT
jwt.secret=UneClefSecreteTresLongueEtComplexePourLaProductionA1B2C3D4E5F6G7H8I9J0
jwt.expiration=2592000000

# Configuration serveur
server.port=8080
server.servlet.context-path=/api

# Configuration logging
logging.file.name=/var/log/backpaking/app.log
logging.level.org.springframework=INFO
logging.level.fr.louisvolat=INFO

# Configuration des uploads
spring.servlet.multipart.max-file-size=20MB
spring.servlet.multipart.max-request-size=20MB

# Configuration actuator pour monitoring
management.endpoints.web.exposure.include=health,info,metrics
management.endpoint.health.show-details=when_authorized
```

## 3. Construction de l'application

Construisez l'application avec Gradle :

```bash
cd /tmp/backpaking-src
./gradlew build -x test
```

Copiez le fichier JAR généré vers le répertoire d'installation :

```bash
sudo cp build/libs/backpaking-2.1.0.jar /opt/backpaking/
```

## 4. Intégration avec systemd

Créez un fichier de service systemd pour l'application :

```bash
sudo nano /etc/systemd/system/backpaking.service
```

Ajoutez le contenu suivant :

```ini
[Unit]
Description=Backpaking Backend Service
After=network.target postgresql.service
Wants=postgresql.service

[Service]
Type=simple
User=root
Group=root
WorkingDirectory=/opt/backpaking
ExecStart=/usr/bin/java -jar /opt/backpaking/backpaking-2.1.0.jar --spring.config.location=file:/opt/backpaking/application.properties
SuccessExitStatus=143
TimeoutStopSec=10
Restart=on-failure
RestartSec=5

# Configuration des limites système
LimitNOFILE=65536
LimitNPROC=4096

# Configuration de la sécurité
PrivateTmp=true
ProtectHome=true
ProtectSystem=full
NoNewPrivileges=true
ReadWritePaths=/opt/backpaking/uploads /var/log/backpaking

[Install]
WantedBy=multi-user.target
```

## 5. Configuration du service pour démarrage automatique

Recharger la configuration de systemd et activer le service :

```bash
# Recharger la configuration systemd
sudo systemctl daemon-reload

# Activer le service pour qu'il démarre automatiquement
sudo systemctl enable backpaking.service
```

## 6. Gestion du service

Vous pouvez maintenant gérer le service avec systemctl :

```bash
# Démarrer le service
sudo systemctl start backpaking.service

# Vérifier l'état du service
sudo systemctl status backpaking.service

# Arrêter le service
sudo systemctl stop backpaking.service

# Redémarrer le service
sudo systemctl restart backpaking.service
```

## 7. Vérification du fonctionnement

Vérifiez les logs pour vous assurer que l'application démarre correctement :

```bash
# Consulter les logs de l'application
sudo tail -f /var/log/backpaking/app.log

# Consulter les logs systemd
sudo journalctl -u backpaking.service -f
```

Vérifiez que l'application est accessible :

```bash
# Test de base avec curl
curl http://localhost:8080/api/travels

# Si vous avez configuré un pare-feu, assurez-vous que le port 8080 est ouvert
sudo ufw allow 8080/tcp
```

## Remarques supplémentaires

### Sécurité
1. Utilisez un mot de passe fort pour la base de données en production
2. Changez la clé secrète JWT (`jwt.secret`) pour une clé aléatoire et sécurisée
3. Envisagez de configurer un proxy inverse comme Nginx pour gérer HTTPS et limiter l'exposition directe de l'application

### Monitoring
Pour surveiller l'application en production, vous pouvez utiliser les endpoints Actuator qui ont été configurés :
```
http://localhost:8080/api/actuator/health
http://localhost:8080/api/actuator/info
http://localhost:8080/api/actuator/metrics
```

### Mises à jour
Pour mettre à jour l'application :
1. Arrêtez le service : `sudo systemctl stop backpaking.service`
2. Remplacez le fichier JAR par la nouvelle version
3. Redémarrez le service : `sudo systemctl start backpaking.service`

Ce guide de déploiement vous permet de configurer et d'exécuter l'application Backpacking Backend sur un environnement Linux, avec une gestion complète via systemd et un démarrage automatique au boot du système.