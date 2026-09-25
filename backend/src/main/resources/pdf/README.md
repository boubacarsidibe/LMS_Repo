# Papier à en-tête des documents PDF de l'INPG

Repris de « En tete et bas de page document.docx » et de « Logo INPG officiel.png » (fournis par l'INPG).

| Fichier | Origine |
|---|---|
| `logo-inpg.png` | Logo officiel rogné, ramené à 924 x 251 px (~440 ppi à 150 pt de large) et détouré : le blanc du fond est devenu transparent (« couleur vers alpha »), composé sur blanc il redonne l'original (réduit). Le fichier fourni est `backend/Logo INPG officiel.png`. |
| `icone-adresse.png`, `icone-telephone.png` | Les deux pictogrammes bleus (#3EA9E0) du pied de page du document Word, agrandis puis ramenés à ~96 px de haut (les originaux font 32 x 27 px ; embarqués plus grands, ils alourdissaient chaque PDF de ~100 Ko pour rien : une icône de 10 pt n'a pas besoin de 1 500 ppi). |
| `polices/` | Liberation Sans (Regular, Bold, Italic), métriquement compatible avec Arial ; Montserrat (certificat). Licence SIL OFL 1.1, voir `OFL.txt` et `Liberation-LICENSE.txt`. |

Mesures du papier à en-tête Word (A4) : marges 63,8 pt à gauche et à droite, 70,9 pt en haut, 56,7 pt en bas ;
logo en haut à gauche à 14,2 pt du bord ; pied de page centré à 19,8 pt du bord, en gras 8 pt, avec le site en
bleu #0563C1 souligné. Le logo est affiché ici à sa proportion naturelle (le document Word l'étire de 22 %).

`PapierEnTete` applique ce papier à n'importe quel `PDDocument` ; `RapportPdf` est le moteur de mise en page des
rapports (retour à la ligne, sauts de page, tuiles de chiffres clés) qui l'utilise.
