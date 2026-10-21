#!/usr/bin/env bash
# Exécute une commande dans l'image « lms-videos » (construite au premier appel).
#   ./lancer.sh node src/outils/parcours-exploratoire.mjs admission
#
# --network host : le conteneur joint directement localhost:4200 (frontend) et :8080 (API).
# --ipc=host     : Chromium plante sans cela (mémoire partagée /dev/shm trop petite).
# LANG / LC_ALL  : les champs date et heure de Chromium suivent la locale du système, pas l'option --lang : sans elles, ils
#                  affichent « mm/dd/yyyy » et « hh:mm AM/PM » dans une vidéo en français.
set -euo pipefail
cd "$(dirname "$0")"
docker image inspect lms-videos >/dev/null 2>&1 || docker build -t lms-videos .
# Les réglages passés dans l'environnement (VOIX=…, CRF=…, DEFILE=…) sont transmis au conteneur.
ENV_TRANSMIS=()
for v in BASE_URL VOIX VITESSE CRF PRESET DEFILE PLEINE; do
  if [ -n "${!v:-}" ]; then ENV_TRANSMIS+=(-e "$v=${!v}"); fi
done
exec docker run --rm --init --ipc=host --network host \
  --user "$(id -u):$(id -g)" -e HOME=/tmp \
  -e LANG=fr_FR.UTF-8 -e LANGUAGE=fr_FR:fr -e LC_ALL=fr_FR.UTF-8 \
  "${ENV_TRANSMIS[@]}" \
  -v "$PWD":/work lms-videos "$@"
