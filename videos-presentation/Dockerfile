# ==========================================================================
# Image de production des vidéos de présentation de la plateforme.
#
# Tout tourne dans ce conteneur, pour que le résultat ne dépende pas de la
# machine : navigateur (Playwright), encodage (ffmpeg) et voix off (edge-tts).
#
# Pourquoi pas l'enregistreur vidéo de Playwright ? Il encode en VP8 à 1 Mbit/s,
# codé en dur et non réglable : le texte d'une interface y devient flou. On
# capture donc les images brutes (protocole CDP, avec leur horodatage exact) et
# on les encode à froid en H.264 haute qualité (voir src/lib/enregistreur.mjs).
# ==========================================================================
FROM mcr.microsoft.com/playwright:v1.64.1-noble

ENV DEBIAN_FRONTEND=noninteractive \
    PIP_BREAK_SYSTEM_PACKAGES=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1

# ffmpeg (libx264 + libass pour les sous-titres incrustés), Inter (la police de
# l'application, aussi utilisée pour les sous-titres) et Python pour edge-tts.
RUN apt-get update \
 && apt-get install -y --no-install-recommends ffmpeg fonts-inter python3-pip \
 && rm -rf /var/lib/apt/lists/*

# Voix neuronale Microsoft (≥ 7.3.0 : jeton Sec-MS-GEC v2, sans lequel le service répond 403).
RUN pip install "edge-tts>=7.3.1"

# Le paquet npm « playwright » doit avoir EXACTEMENT la version de l'image
# (les navigateurs préinstallés dans /ms-playwright en dépendent).
RUN mkdir /opt/pw && cd /opt/pw && npm init -y >/dev/null && npm install --no-audit --no-fund playwright@1.64.1

WORKDIR /work
