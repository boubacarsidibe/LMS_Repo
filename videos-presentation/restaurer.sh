#!/usr/bin/env bash
# Remet la base dans l'état de l'instantané de référence (voir preparer-donnees.sh).
# À lancer avant CHAQUE prise : les scénarios créent des comptes, des dossiers, des notes… ; sans cela, une
# seconde prise partirait d'un état déjà modifié par la première.
set -euo pipefail
cd "$(dirname "$0")/.."
DUMP="${1:-videos-presentation/cache/reference.dump}"
[ -f "$DUMP" ] || { echo "Instantané introuvable : $DUMP — lancez d'abord ./preparer-donnees.sh" >&2; exit 1; }

# Le backend garde des connexions ouvertes : on le met en pause le temps de la restauration, puis on le relance.
docker stop demo-backend > /dev/null
docker exec demo-postgres psql -U admin -d postgres -q -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = 'myappdb' AND pid <> pg_backend_pid();" > /dev/null
docker exec demo-postgres dropdb -U admin --if-exists myappdb
docker exec demo-postgres createdb -U admin myappdb
docker exec -i demo-postgres pg_restore -U admin -d myappdb --no-owner < "$DUMP"
docker start demo-backend > /dev/null

printf "Attente du backend"
for _ in $(seq 1 90); do
  if curl -s -o /dev/null -w '%{http_code}' -X POST http://localhost:8080/api/auth/login -H 'Content-Type: application/json' -d '{}' | grep -q '^4'; then echo " — prêt"; exit 0; fi
  printf "."; sleep 2
done
echo; echo "Le backend ne répond pas après 3 minutes." >&2; exit 1
