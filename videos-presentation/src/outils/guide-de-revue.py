#!/usr/bin/env python3
"""Génère GUIDE-DE-REVUE.md : pour chaque vidéo produite, sa durée, ses chapitres horodatés et une grille de retours.
    python3 src/outils/guide-de-revue.py
Lit sortie/<id>/<id>.minutage.json (écrit par produire.sh) et le titre dans src/scenarios/<id>.mjs."""
import glob, json, os, re

RACINE = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
PUBLICS = {
    "00": "Tous publics", "01": "Candidats", "02": "Responsable des admissions", "03": "Responsable des admissions",
    "04": "Responsable pédagogique", "05": "Formateurs", "06": "Étudiants", "07": "Alumni",
    "08": "Écoles de formation", "09": "Évaluateurs d'accréditation", "10": "Entreprises", "11": "Candidats formateurs",
}

def hms(s):
    s = int(round(s)); return f"{s // 60}:{s % 60:02d}"

lignes = ["# Guide de revue des vidéos de présentation", "",
          "Ce guide accompagne les vidéos de présentation de la plateforme LMS de l'INPG. Chaque vidéo suit un vrai parcours dans "
          "l'application (aucune maquette), avec voix off et sous-titres incrustés. Les chapitres sont aussi intégrés au fichier MP4 "
          "(navigation par chapitre dans les lecteurs compatibles).", "",
          "## Les vidéos", "", "| N° | Vidéo | Public | Durée |", "|---|---|---|---|"]
details = []
for scen in sorted(glob.glob(os.path.join(RACINE, "src/scenarios/[0-9][0-9]-*.mjs"))):
    vid = os.path.basename(scen)[:-4]
    src = open(scen, encoding="utf-8").read()
    # Le titre de la vidéo est la propriété de premier niveau (deux espaces) ; « ouverture.titre » en compte quatre.
    m = re.search(r"^  titre: (['\"])(.*)\1,\s*$", src, re.M)
    titre = m.group(2).replace("\\'", "'")
    minutage = os.path.join(RACINE, "sortie", vid, f"{vid}.minutage.json")
    if not os.path.exists(minutage):
        lignes.append(f"| {vid[:2]} | {titre} | {PUBLICS.get(vid[:2], '')} | _non produite_ |"); continue
    seg = json.load(open(minutage, encoding="utf-8"))
    # Durée réelle : celle que le montage a imprimée à la fin de la prise (« durée 02:16 · … »), pas une estimation.
    journal = os.path.join(RACINE, "cache", f"prise-{vid}.log")
    duree = re.findall(r"durée (\d+):(\d+)", open(journal, encoding="utf-8").read()) if os.path.exists(journal) else []
    fin = int(duree[-1][0]) * 60 + int(duree[-1][1]) if duree else 0
    lignes.append(f"| {vid[:2]} | {titre} | {PUBLICS.get(vid[:2], '')} | {hms(fin)} |")
    vu, chap = set(), []
    for s in seg:
        c = s.get("chapitre")
        if c and c not in vu:
            vu.add(c); chap.append((s["debutEtape"], c))
    details.append((vid, titre, fin, chap))

lignes += ["", "## Détail par vidéo", ""]
for vid, titre, fin, chap in details:
    lignes += [f"### {vid[:2]} — {titre}", "", f"Fichier : `sortie/{vid}/{vid}.mp4` · sous-titres : `{vid}.srt` · durée {hms(fin)}", "",
               "| Repère | Chapitre |", "|---|---|"] + [f"| {hms(t)} | {c} |" for t, c in chap] + [""]

lignes += ["## Grille de retours", "",
           "À remplir pendant le visionnage (une ligne par remarque).", "",
           "| Vidéo | Repère | Remarque | Type (contenu / forme / fonctionnel) | Priorité |", "|---|---|---|---|---|",
           "| | | | | |", "| | | | | |", "| | | | | |", "",
           "## Pour aller plus loin", "",
           "- `DEFAUTS-CORRIGES.md` : les défauts de l'application trouvés et corrigés en préparant ces vidéos, et les recommandations restantes.",
           "- Les données montrées sont fictives (personnes, écoles et entreprises inventées).",
           "- La voix est une synthèse neuronale : à relire à l'oreille avant diffusion externe.", ""]
open(os.path.join(RACINE, "GUIDE-DE-REVUE.md"), "w", encoding="utf-8").write("\n".join(lignes))
print("GUIDE-DE-REVUE.md écrit —", len(details), "vidéo(s) produite(s)")
