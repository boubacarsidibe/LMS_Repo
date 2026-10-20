#!/usr/bin/env bash
# Remet la base de démonstration dans son état de référence, puis produit une vidéo (ou la répète).
#
#   ./produire.sh 01-candidat            prise complète → sortie/01-candidat/01-candidat.mp4
#   ./produire.sh 01-candidat --essai    répétition rapide (sans voix ni image) : liste les erreurs de l'application
#
# La remise à zéro est indispensable : les scénarios créent de vrais comptes, dossiers, notes… ; relancés sans elle, ils
# se heurteraient à ce que la prise précédente a laissé (« cette adresse e-mail est déjà utilisée »).
set -euo pipefail
cd "$(dirname "$0")"
./restaurer.sh
./lancer.sh node src/produire.mjs "$@"
