# Collecte de données INPG — reçu, appliqué, à demander

Source : « Questionnaire de collecte des données INPG — Plateforme LMS », rempli par un membre du personnel de
l'INPG le **2026-09-30** (fichier `infos1.pdf`, racine du dépôt). Exploité le 2026-10-12, puis recoupé le même jour
avec le site officiel de l'INPG (`inpg.sn` redirige vers <https://inpgsenegal.com/>) et la presse.

Deux pièces jointes citées dans le formulaire **n'étaient pas dans le PDF** : « Logo INPG officiel.png » et
« En tête et bas de page docu… » (nom tronqué). À récupérer auprès du service qui a rempli le formulaire.

## 1. Ce que l'INPG a communiqué, et ce qu'on en a fait

| Rubrique | Réponse de l'INPG | Traitement |
|---|---|---|
| Formations (8) | Mastère en Ingénierie Pétrolier et Gazier ; MBA en Economie et Management du Pétrole et du Gaz ; Licence en Maintenance électrique ; Licence en Maintenance mécanique ; CPS en Maintenance électrique ; CPS en Maintenance mécanique ; CPS en Qualité Hygiène Sécurité Environnement ; CPS en Soudage industriel | **Appliqué** : migrations `V202610121500` (7 formations) et `V202610121700` (voir §2, décision 3). Le MBA et le Mastère reprennent des fiches déjà au catalogue |
| Niveaux d'accès | Mastère et MBA : Bac+5 · Licence : DUT, BTS, Licence 3 · CPS : 4ᵉ secondaire | **Appliqué** dans la description des formations et dans la liste des niveaux d'études |
| Durées | Mastère 18 mois · MBA 18 mois · Licence 12 mois · CPS 6 mois | **Appliqué**. Le site confirme le Mastère : « 12 mois de cours » + 6 mois de stage |
| Présélection automatique | **Oui.** Mastère : Bac+5 · MBA : Bac+5, pas de limite d'âge · Licence : Bac+2/Bac+3, 28 ans au plus · CPS : 4ᵉ au minimum, 30 ans au plus | **Rendu possible** : la liste des niveaux couvre « 4ᵉ secondaire » et « Bac+2 », avec un test par profil. Les critères se saisissent **par session** ; aucune session réelle n'existe encore (dates inconnues) |
| Pièces du dossier | Pièce d'identité, relevés de notes, attestation de diplôme, CV | **Appliqué** (décision 1) : diplôme obligatoire, sauf niveau 4ᵉ secondaire |
| Barème de notation | **« Non, certaines formations utilisent un autre système »** — système non précisé | **À demander, prioritaire** (§3) |
| Contact public | Adresse, téléphone, e-mails, horaires : **exacts**. Réseaux sociaux : **corrects** | Aucun changement (la page Contact était conforme ; le site officiel donne les mêmes coordonnées) |
| Destinataire du formulaire de contact | `training@inpg.sn` | **Appliqué** : réglage dédié `app.contact.email` (variable `APP_CONTACT_EMAIL`) |
| Relevé de notes | En-tête : logo INPG, « Ministère de tutelle : MEP » ; signature et cachet ; pied de page : adresse et contacts ; signataire : Directeur Pédagogique Industriel (DPI) | **Appliqué** : « Ministère de tutelle : Ministère de l'Énergie et du Pétrole (MEP) » (décision 6), bloc « Fait à Dakar, le … / Le Directeur Pédagogique Industriel » avec place pour signature et cachet, sur la page de la synthèse. Pied de page corrigé (décision 2) |
| Certificat | Signataire : Directeur général ; logo officiel | Aucun changement : le modèle officiel (`certificat-formation-fond.pdf`) porte déjà le signataire |
| Responsable admission | Babacar KEBE · babacar.kebe@inpg.sn (téléphone : voir `infos1.pdf`) | **Pas de compte créé** (décision 4) |
| Responsable pédagogique | Mame Mor Diarra NDIAYE · mamemordiarra.ndiaye@inpg.sn (téléphone : voir `infos1.pdf`) | **Pas de compte créé** (décision 4) |
| Formateurs | Babacar KEBE (gaz et instrumentation) ; Moussa OULARE (pétrole et gaz) ; Assane DIENG (pétrole et gaz) ; Ablaye THIAM (mines) | Aucune coordonnée fournie : rien à créer |

## 2. Décisions prises le 2026-10-12, après recherches

1. **Attestation de diplôme : obligatoire, sauf pour un candidat de niveau « 4ᵉ secondaire ».**
   L'INPG l'exige dans la liste des pièces ; la plateforme la laissait facultative. Les CPS s'ouvrent dès la 4ᵉ secondaire :
   y exiger un diplôme exclurait leur public (c'est le niveau d'accès que l'INPG lui-même indique). Un niveau absent ou
   « Autre » n'exonère personne. Appliqué au formulaire (étape Documents : intitulé « Attestation de diplôme », badge
   « Requis », erreur explicite) et à la relance des dossiers incomplets (`ApplicationController.piecesRequises`). Les
   dossiers déjà déposés sans diplôme seront donc signalés « incomplets » à la prochaine relance : c'est voulu.
   *Limite* : la FAQ du site ne cite pas l'attestation de diplôme parmi les pièces (lettre de motivation et CV seulement) ;
   la réponse du personnel, plus récente et plus précise, a prévalu.
2. **Adresse du papier à en-tête : « Boulevard de l'Est ».** Les PDF imprimaient « Boulevard de Saint Louis » (repris du
   document Word de l'INPG). Le questionnaire confirme « Boulevard de l'Est — Point E » comme exact, le site officiel écrit
   « Immeuble Adja Koone Boulevard de l'est – Point E Dakar », et un annuaire indépendant dit de même : trois sources contre
   une. Corrigé dans `PapierEnTete` et `PapierEnTeteOpenPdf`. Le téléphone « +221 33 865 91 48 / 49 » reste : seul le
   Word cite le « 49 », rien ne le contredit.
3. **Doublons de formations : fusionnés ou masqués, sans rien supprimer d'utile.**
   - Le Mastère existait deux fois (« Ingénierie du Pétrole et du Gaz » du site, et mon « MIPG »). Le site le décrit comme
     un « Mastère Spécialisant », « 12 mois de cours » + 6 mois de stage, après un Bac+5 : c'est la même formation. Gardée
     la fiche du site, la plus riche, mise au nom officiel, niveau « Mastère », 18 mois (code `ING-PET-GAZ-02` conservé : le
     chargeur du catalogue s'en sert comme clé). Ligne `MIPG` retirée.
   - Les quatre formations de démonstration (`DEMO-DS`, `DEMO-FIN`, `DEMO-PROC`, `MPG`) quittent le catalogue public et la
     liste proposée aux entreprises ; leurs promotions, cours et étudiants restent intacts pour les démonstrations. Les
     initialiseurs de démo les créent désormais hors catalogue. Elles ne sont pas dans le chargeur du catalogue : rien ne
     les republie. Pour les remontrer : `UPDATE formation SET publiee = TRUE WHERE code IN (…)`.
   - Les 14 autres formations du catalogue (HSE, Renforcement de capacités, Certifications…) **restent** : le site officiel
     présente bien ces offres. Le questionnaire ne liste que les 8 formations « diplômantes ».
4. **Comptes réels : non créés.** Un compte de responsable admission donne accès aux dossiers personnels de tous les
   candidats. L'environnement de recette est joint par un tunnel public (Cloudflare) : y créer ces comptes avec un mot de
   passe provisoire serait une faille. Et leur transmettre ce mot de passe par e-mail n'aboutirait pas : le SPF d'`inpg.sn`
   n'autorise pas Brevo (voir CLAUDE.md). À faire à la mise en production, avec changement de mot de passe obligatoire à la
   première connexion. Babacar KEBE étant à la fois responsable admission et formateur, prévoir **un compte portant deux
   rôles**, pas deux comptes.
5. **Alertes internes d'admission : adresse inchangée en recette, à régler en production.** Paiement à valider et nouvelle
   candidature partent vers `app.admission.email` (`admin@inpg.sn`). En production, lancer le backend avec
   `APP_ADMISSION_EMAIL=babacar.kebe@inpg.sn` ; en recette, l'adresse par défaut évite d'envoyer à une vraie personne les
   alertes des candidatures de test. Même logique pour `APP_CONTACT_EMAIL` (défaut : `training@inpg.sn`).
6. **« MEP » : écrit en toutes lettres, « Ministère de l'Énergie et du Pétrole (MEP) ».** Le Journal Officiel n° 7917 du
   27 juin 2026 publie le décret n° 2026-1151 « relatif aux attributions du Ministre de l'Énergie et du Pétrole » ; ce nom
   remplace « Énergie, Pétrole et Mines » (décret 2024-1593). *Limite* : aucune source ne dit explicitement que ce ministère
   est la tutelle de l'INPG ; c'est ce qu'écrit le personnel. Si le ministère change de nom, une seule constante à modifier
   (`RelevePdfService.MINISTERE_TUTELLE`).
7. **Noms des signataires : jamais imprimés sans confirmation.** La presse (2025-2026) désigne le directeur général de
   l'INPG comme **Pr Fall Mbaye**, en fonction depuis octobre 2024. Information à faire confirmer par l'INPG avant tout usage
   sur un document ; rien n'est imprimé de cette source.

## 3. Constats nouveaux, à faire trancher par l'INPG

- **Prix affichés sans confirmation.** Trois fiches du catalogue annoncent un tarif : « 500 000 XOF » (Hygiène et sécurité au
  travail), « 450 000 FCFA » (Espaces confinés et travail en hauteur), « 24 500 € / an » (MBA en Finance et Stratégie). Le
  personnel déclare ne pas disposer des tarifs. Laissés en place, faute de preuve qu'ils soient faux ; **à faire valider**.
  Les trois « prochaine rentrée » de 2024, passées, ont été retirées (`V202610121700`).
- **« MBA en Finance et Stratégie »** (24 500 €, rentrée 2024) ne figure ni dans la liste du personnel ni sur le site : ressemble
  à un contenu de gabarit. Non masqué, car le chargeur du catalogue republie cette fiche à chaque démarrage ; à retirer du
  fichier `data/catalogue-formations.json` et de la base si l'INPG confirme qu'elle n'existe pas.

## 4. Réponses manquantes — à aller chercher

Classées par effet sur la plateforme. Les réponses « Pas disponible » / « Pas d'information » / vides.

| # | Question restée sans réponse | Ce qui est bloqué |
|---|---|---|
| 1 | **Barème de notation** : quelles formations n'utilisent pas le /20, et quel système ? | **Prioritaire.** Les notes sont saisies sur 100 et affichées /20 partout (migration V44, seuil de validation à 60, mentions du relevé). Un autre barème fausserait moyennes, statuts de suivi et relevés |
| 2 | **Dates de la prochaine rentrée et date limite de dépôt**, par formation | Impossible de créer de vraies sessions de recrutement |
| 3 | **Tarif de chaque formation** et sous quelle forme (comptant, tranches…) ; valider ou retirer les trois prix du §3 | Aucun prix fiable au catalogue ; paiement en ligne de toute façon suspendu |
| 4 | **Contenu pédagogique détaillé** de chaque formation officielle | Fiches des 7 nouvelles formations réduites à une description factuelle (niveau, accès, durée) |
| 5 | **Maquette de cours complète** par formation | Pas de cours réels : seuls ceux de démonstration existent |
| 6 | **Coefficient de chaque cours** | Moyenne générale pondérée par le coefficient par défaut (1) |
| 7 | **Nomenclature des cohortes** en cours ou à venir | Promotions réelles non créées |
| 8 | **Montant des frais de dossier** | Champ déjà retiré des sessions (paiement suspendu) |
| 9 | **Format de référence souhaité du certificat** | Le modèle officiel actuel est conservé, à confirmer |
| 10 | **Noms** du Directeur général et du Directeur Pédagogique Industriel (seules leurs fonctions sont données) | Imprimés sans nom |
| 11 | **Coordonnées et cours** de chaque formateur réel (e-mail, téléphone) | Comptes formateurs réels non créables ; affectations aux cours impossibles |
| 12 | **Fichiers joints** : logo officiel haute résolution, modèle de cachet/sceau, document d'en-tête et pied de page | Cachet absent des documents ; logo à comparer à `frontend/public/images/logo-inpg.png` |
| 13 | Signification de **« CPS »** (introuvable sur le site et dans la presse) | Écrit tel quel au catalogue, jamais développé |
| 14 | Le **second numéro « / 49 »** du téléphone est-il toujours valide ? | Imprimé sur les PDF, absent du site |
| 15 | L'attestation de diplôme est-elle bien exigée pour **toutes** les formations autres que les CPS ? | Règle de la décision 1 |

## Sources des recherches (2026-10-12)

- Site officiel : <https://inpgsenegal.com/> (adresse, téléphone, e-mails, horaires), <https://inpgsenegal.com/inpgsenegal-com-faq/>
  (conditions d'accès au Mastère : Bac+5, dossier de candidature, frais), <https://inpgsenegal.com/ingenierie-du-petrole-et-du-gaz/>
  (Mastère : 12 mois de cours + 6 mois de stage).
- Annuaire : <https://legrandfrere.africa/etablissement/institut-national-du-petrole-et-du-gaz-inpg/> (adresse « Point E, Boulevard de l'Est, Immeuble Adja Koone »).
- Journal Officiel n° 7917 du 27 juin 2026 : <https://www.vie-publique.sn/documents/14077/JO-7917-du-27-juin-2026> (décret 2026-1151, Ministre de l'Énergie et du Pétrole).
- Presse : <https://fr.allafrica.com/stories/202511020096.html> (INPG et ESP, maintenance pétrolière et gazière ; directeur général).
