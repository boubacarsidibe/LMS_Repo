#!/usr/bin/env bash
# Prépare la base de démonstration utilisée par les vidéos : jeux de données SQL, fichiers déposés, instantané.
#
#   ./preparer-donnees.sh            charge les données (idempotent) et prend l'instantané de référence
#   ./restaurer.sh                   remet la base dans l'état de l'instantané avant une prise
#
# Pré-requis : la pile Docker tourne (docker compose up -d).
# ATTENTION : l'instantané est pris sur l'état COURANT de la base. Après une répétition (--essai) ou une prise, lancer d'abord
# ./restaurer.sh : sinon les actions jouées par la répétition (évaluations, réponses, sujets de forum…) sont figées dans la
# référence et les prises suivantes affichent « Mettre à jour » à la place de « Envoyer ».
set -euo pipefail
cd "$(dirname "$0")/.."

PSQL="docker exec -i demo-postgres psql -U admin -d myappdb -v ON_ERROR_STOP=1"

echo "→ Jeux de données SQL"
for f in seed-comptes-test seed-demo-formateur seed-demo-client seed-demo-presentation seed-demo-presentation-espaces; do
  echo "   $f.sql"
  $PSQL -q < "backend/db/$f.sql" > /dev/null
done

# Pièces des dossiers d'accréditation et documents des alumni : générées une fois (Chromium imprime du HTML en PDF).
if [ ! -d videos-presentation/donnees/accreditation ]; then
  echo "→ Génération des PDF de démonstration"
  (cd videos-presentation && ./lancer.sh node src/outils/generer-documents.mjs)
fi

echo "→ Fichiers déposés (pièces des candidats, photos de la photothèque, accréditation, alumni)"
docker exec demo-backend mkdir -p /app/uploads/demo /app/uploads/photos
for f in videos-presentation/donnees/documents/*.pdf; do docker cp "$f" "demo-backend:/app/uploads/demo/$(basename "$f")"; done
for f in videos-presentation/donnees/photos/*.png; do docker cp "$f" "demo-backend:/app/uploads/photos/$(basename "$f")"; done
for f in videos-presentation/donnees/supports/*.pdf; do docker cp "$f" "demo-backend:/app/uploads/$(basename "$f")"; done
docker exec demo-backend mkdir -p /app/uploads/demo/accreditation /app/uploads/alumni-documents
for f in videos-presentation/donnees/accreditation/*.pdf; do docker cp "$f" "demo-backend:/app/uploads/demo/accreditation/$(basename "$f")"; done
# Les documents partagés des alumni sont lus sous le nom stocké « demo-<nom d'origine> » (voir le script SQL).
for f in videos-presentation/donnees/alumni/*.pdf; do docker cp "$f" "demo-backend:/app/uploads/alumni-documents/demo-$(basename "$f")"; done

echo "→ Objets créés par l'API réelle (relevés délivrés)"
(cd videos-presentation && ./lancer.sh node src/outils/amorcer-api.mjs)

echo "→ Références des documents signés (lues dans la base, pour les ouvrir par leur page publique de vérification)"
mkdir -p videos-presentation/cache
docker exec demo-postgres psql -U admin -d myappdb -tA -c "SELECT json_build_object(
  'attestation', (SELECT reference_attestation FROM demandes_accreditation WHERE reference_attestation IS NOT NULL ORDER BY id LIMIT 1),
  'certificat', (SELECT c.reference FROM certificat c JOIN etudiants e ON e.id_user = c.etudiant_id WHERE e.matricule = 'INPG-2026-0012' LIMIT 1))" \
  > videos-presentation/cache/references.json
cat videos-presentation/cache/references.json

echo "→ Instantané de référence"
mkdir -p videos-presentation/cache
docker exec demo-postgres pg_dump -U admin -d myappdb -Fc > videos-presentation/cache/reference.dump
echo "   $(du -h videos-presentation/cache/reference.dump | cut -f1) — videos-presentation/cache/reference.dump"
