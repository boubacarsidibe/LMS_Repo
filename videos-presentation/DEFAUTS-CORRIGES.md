# Défauts de l'application trouvés et corrigés en préparant les vidéos

Chaque parcours a été joué dans un vrai navigateur avant d'être filmé. Ce qui ne marchait pas — ou se contredisait — a été corrigé
dans l'application, avec un test de non-régression vérifié **rouge sur l'ancien code, vert sur le nouveau** (sauf mention contraire).

## Avant la reprise (premier lot)
- Backend en boucle de redémarrage (« Empty key ») : clé de signature absente → repli (`CleSignature`).
- Statuts d'admission : 20 statuts côté serveur contre 5 côté interface → module partagé `statuts-candidature.ts`, actions par statut.
- Dépôt de pièces refusé (HTTP 409) hors statut brouillon → garde par statut.
- Cloche de notifications en erreur pour un compte sans profil candidat ; photos de démonstration non déposées.
- Échelle des notes : /100 avec équivalent /20 harmonisée.

## Cette session
1. **10 tests périmés** alignés sur le comportement correct (auth, moyenne, paiements). Suite backend : 605 tests verts.
2. **Suivi des étudiants (formateur + pédagogie)** : « progression 0 % » et « dernière connexion : Non renseignée » étaient codés en dur →
   `ActiviteEtudiantService` (même règle que l'espace étudiant ; « dernière activité » = dernier support ouvert, la connexion n'étant pas tracée).
3. **Tableau de bord formateur** : deux encadrés « Bientôt disponible » pour des fonctions qui existent → données réelles.
4. `<h1 class="sr-only">` dupliqué dans les layouts pédagogie et admission (deux h1 par page).
5. « Admissions Portal » (anglais) dans l'en-tête mobile.
6. **Mes cours (formateur)** : « 67 étudiants » = somme des effectifs par cours pour 19 personnes → étudiants distincts.
7. **Dates en anglais dans toute l'application** (« Saturday 24 October », « 19 Oct 2026 ») : locale française jamais enregistrée (98 usages) ;
   moyennes « 47.5 » → « 47,5 ».
8. Score de test « 13 (seuil : 10) » sans échelle → « 13 / 20 ».
9. **Paiement** : montant « communiqué à la validation » → frais annoncés avant le règlement (nouvel endpoint, contrôle de propriété).
10. **Fiches de formation publiques vides pour les 17 formations** : un `JsonNode` Jackson 2 était sérialisé comme un objet Java par Jackson 3
    (Spring Boot 4). Vérifié en rendu réel.
11. **Annonces du formateur** : les destinataires cochés étaient ignorés par le serveur (clé `cohorteIds` contre `promotionId`) → toute annonce
    partait à « toutes les promotions ». Corrigé (une annonce par promotion) + contrôle de périmètre (403 hors de ses promotions).
12. **Aide du portail Admissions** : décrivait l'ancien parcours en 5 étapes et affirmait « pas de retour en arrière » → réécrite (9 étapes réelles).
13. **Paiements** : « Relancer les impayés (3) » alors que le serveur n'en relance que 2 → compteur aligné.
14. Espace alumni : tuile « Moyenne au diplôme » coupée sur 4 lignes.
15. **Étudiants (pédagogie)** : couleur de la moyenne (seuil 50) en contradiction avec le statut « À risque » (seuil 60) ; colonne « Alumni »
    (action « Marquer alumni ») hors écran à 1280 px ; matricule coupé en 3 lignes.
16. Jeu de données : valeur d'énumération inventée (« Langue maternelle ») → HTTP 500 sur « Ma candidature » du candidat formateur.
17. **Forum des alumni inutilisable** : impossible d'ouvrir un sujet (HTTP 400 « Failed to read request ») — Jackson 3 refuse un champ primitif
    absent du corps. Corrigé globalement (5 DTO concernés).
18. **ÉVALUATEUR — la page se figeait** (« la page ne répond plus ») dès l'ouverture d'un dossier en évaluation : accesseur recalculé à chaque
    détection de changements autour de 31 champs `ngModel`. Mesuré en navigateur réel : gel avant, 41 ms après.
19. Barre de navigation : `replace('_', ' ')` ne remplaçait que la première occurrence.
20. Paiements : statut affiché en code brut (« PRESELECTIONNE ») → libellés.
21. Paiements : après « Valider », la liste « en attente de paiement » restait périmée (même dossier payé dans un tableau, impayé dans l'autre).
22. **Six boîtes natives `confirm()`/`prompt()`** malgré la convention du projet → `ConfirmationService` (invisibles d'un partage d'écran).
23. Admission : la recherche de candidatures ne trouvait pas « Rama Kane » (prénom + nom, tel qu'affiché).

## Gravité
- **Bloquants** (fonction inutilisable) : 17, 18, 11, 10, 2.
- **Incohérences visibles** : 7, 9, 13, 20, 21, 12, 15.
- **Ergonomie / hygiène** : 4, 5, 6, 14, 19, 22, 23.

## Recommandations restantes (non traitées)
- Formulaire de demande entreprise : adresse, téléphone et secteur à ressaisir à chaque demande (seule la raison sociale est reprise).
- `GET …/fiche` répond 404 quand aucune fiche technique n'existe : état normal traité comme une erreur HTTP.
- Le nom saisi à l'inscription d'une école devient un identifiant technique ; il est redemandé à la première demande d'accréditation.
- Aucune limitation de débit sur la page publique de vérification des certificats (déjà signalé dans CLAUDE.md).
- Courriel sortant non configuré dans cet environnement (pas de mot de passe SMTP) : notifications par courriel non éprouvées de bout en bout.
- Brancher une vraie passerelle de paiement (le règlement est déclaré puis confirmé à la main).
