#!/usr/bin/env bash
# Produit toutes les vidéos, l'une après l'autre (la mémoire du poste ne permet pas deux prises en parallèle).
# Une prise qui échoue n'arrête pas les suivantes ; le bilan est écrit dans cache/produire-tout.log.
#   ./produire-tout.sh                 toutes les vidéos
#   ./produire-tout.sh 05-formateur …  seulement celles-ci
cd "$(dirname "$0")"
mkdir -p cache
JOURNAL=cache/produire-tout.log
LISTE=("$@")
if [ ${#LISTE[@]} -eq 0 ]; then
  for f in src/scenarios/[0-9][0-9]-*.mjs; do LISTE+=("$(basename "$f" .mjs)"); done
fi
echo "--- $(date +%F\ %H:%M)" >> "$JOURNAL"
for id in "${LISTE[@]}"; do
  debut=$(date +%s)
  echo "[$(date +%H:%M:%S)] début $id" | tee -a "$JOURNAL"
  if ./produire.sh "$id" > "cache/prise-$id.log" 2>&1; then
    echo "[$(date +%H:%M:%S)] OK    $id ($(( $(date +%s) - debut )) s) — $(tail -n 2 "cache/prise-$id.log" | tr '\n' ' ')" | tee -a "$JOURNAL"
  else
    echo "[$(date +%H:%M:%S)] ÉCHEC $id — voir cache/prise-$id.log" | tee -a "$JOURNAL"
  fi
done
echo "[$(date +%H:%M:%S)] terminé" | tee -a "$JOURNAL"
