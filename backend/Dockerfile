# -------------------------------------------------------------
# Étape 1 : Build de l'application avec Maven
# -------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS builder

# Définir le répertoire de travail dans le container
WORKDIR /app

# Copier le fichier pom.xml et télécharger les dépendances (optimisation du cache Docker)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copier le code source et compiler le projet en ignorant les tests pour aller plus vite
COPY src ./src
RUN mvn package -DskipTests

# -------------------------------------------------------------
# Étape 2 : Image d'exécution légère (JRE uniquement)
# -------------------------------------------------------------
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Récupérer uniquement le fichier .jar généré à l'étape précédente
COPY --from=builder /app/target/*.jar app.jar

# Créer le dossier d'upload des fichiers attachés
RUN mkdir -p /app/uploads && chmod 777 /app/uploads

# Exposer le port sur lequel Spring Boot écoute (8080 par défaut)
EXPOSE 8080

# Définir des variables d'environnement si besoin
ENV JAVA_OPTS=""

# Commande de lancement
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]