# Certificat de formation INPG

`certificat-formation-fond.pdf` est le modèle officiel de l'INPG (« Modèle Certificat INPG.pdf »)
dont les six zones variables ont été vidées : nom du bénéficiaire, date de naissance, intitulé de la
formation, période, « Fait à Dakar, le … » et code de bas de page. Tout le reste (cadre, logo, filigrane,
textes fixes, nom du signataire) vient du modèle d'origine, sans redessin.

`CertificatPdfService` superpose les valeurs variables à ce fond. Mesures relevées sur le modèle
(origine en bas à gauche, page 870,236 x 644,281 pt, centre horizontal 435,118) :

| Zone                     | Police                                   | Corps  | Ligne de base | Alignement          |
|--------------------------|------------------------------------------|--------|---------------|---------------------|
| Civilité + nom           | Liberation Sans Bold (≈ Arial gras)    | 22     | y = 359,668   | ligne centrée       |
| « né le JJ/MM/AAAA »     | Liberation Sans Regular (≈ Arial)     | 15     | y = 359,668   | suite de la ligne   |
| Intitulé de la formation | Liberation Sans Bold, étirée à 81,4 %  | 24,576 | y = 280,632   | centré              |
| Période                  | Montserrat Regular                       | 15     | y = 239,85    | centré              |
| Fait à Dakar, le …       | Montserrat Regular                       | 9      | y = 82,368    | à gauche, x = 75,4  |
| Code de bas de page      | Montserrat SemiBold Italic, 115,6 % large| 8,507  | y = 41,528    | à gauche, x = 54,18 |

Le modèle d'origine porte les données d'une personne réelle : il ne doit pas être versionné, seul ce
fond vierge l'est. Le texte variable est en bleu nuit (41, 40, 92), comme le texte du modèle. Les polices sont sous licence SIL OFL 1.1 :
Montserrat (`../pdf/polices/OFL.txt`) et Liberation Sans (`../pdf/polices/Liberation-LICENSE.txt`), métriquement compatible avec Arial,
celle du modèle, dont les polices embarquées ne contiennent que les glyphes déjà utilisés.
