# 1. Étape de build
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

# 2. Étape d'exécution (Serveur Node.js pour le SSR)
FROM node:20-alpine AS runner
WORKDIR /app
COPY --from=build /app/dist /app/dist
COPY package*.json ./
# Installation uniquement des dépendances de prod si nécessaire
RUN npm ci --only=production

EXPOSE 4000
# Adaptez le chemin selon le nom de votre projet dans /dist
CMD ["node", "dist/votre-projet/server/main.mjs"]