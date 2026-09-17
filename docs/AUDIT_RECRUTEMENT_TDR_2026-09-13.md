# Audit ciblé — Recrutement LMS INPG

**Date :** 13 septembre 2026  
**Périmètre :** catalogue, candidature, admission, paiement, tests, entretiens, notifications, exports, statistiques et interface du responsable admission.  
**Référence :** partie recrutement du *TDR — Mise en place du LMS — V02* (paragraphes 22 à 50).  
**Hors périmètre :** scolarité, e-learning, alumni, entreprises, accréditation, personnel et rendez-vous.

## Conclusion

Le parcours recrutement est l’un des domaines les plus avancés du dépôt : il couvre le catalogue public, le brouillon de candidature, les pièces, les critères, les tests, les entretiens, la liste d’attente, les exports et les indicateurs. **Il ne doit toutefois pas être ouvert pour des encaissements réels avant le raccordement d’un prestataire de paiement et la suppression de la saisie de carte simulée.**

Les autres priorités sont la fiabilité des notifications, le gel/versionnement des pièces examinées, la sécurité des comptes, la pagination des listes et une recette de bout en bout avec les agents d’admission.

## 1. Couverture des exigences du TDR

| Exigence recrutement du TDR | État dans le dépôt | Décision ou amélioration nécessaire |
|---|---|---|
| Compte candidat, formulaire, pièces jointes et accès au catalogue | **Couvert dans le code** | Tester le parcours sur mobile, faible débit et fichiers volumineux ; ne pas confondre la présence de l’écran avec la réussite du dépôt réel. |
| Notification de création, dossier incomplet et reprise ultérieure | **Partiel** | Le brouillon serveur est présent. Rendre la sauvegarde et l’état de reprise clairement visibles ; garantir la reprise si l’e-mail ou le réseau échoue. |
| Acceptation/rejet avec motif obligatoire | **Couvert dans le code, à recetter** | Les motifs doivent être structurés, historiques et exploitables dans les exports ; remplacer les invites navigateur par une fenêtre métier validée. |
| Suivi du statut par le candidat | **Couvert dans le code, à recetter** | Afficher une prochaine action claire, le délai attendu et le motif seulement lorsque la politique métier le permet. |
| Paiement via le prestataire désigné, notification candidat/INPG, confirmation du paiement | **Partiel — bloquant avant paiement réel** | Une tentative et une validation administrative existent, mais aucune passerelle de paiement n’est connectée. Choisir un mode manuel traçable ou intégrer un prestataire avec webhook signé et rapprochement. |
| Statistiques par critères et export Excel personnalisable | **Couvert en grande partie** | Valider les critères réellement disponibles dans les données, anonymiser les extractions et paginer les vues/exports volumineux. |
| Extraction des pièces et sélection selon critères | **Couvert dans le code, à recetter** | Versionner les pièces utilisées pour une décision et empêcher un ajout silencieux après examen. |
| Convocations et résultats des tests avec seuil de réussite | **Couvert dans le code, à recetter** | Définir qui paramètre le seuil, qui peut corriger une note et conserver le motif/auteur de chaque correction. |
| Entretiens, sélection, liste d’attente et notifications de non-admission | **Couvert dans le code, à recetter** | Encadrer les actions de masse : aperçu des destinataires, annulation contrôlée, journal d’envoi et traitement des échecs. |
| Fermeture des candidatures après date limite, sauf administration | **Présent sous forme d’état de session ; recette indispensable** | Tester explicitement les états `OUVERT`, `SUSPENDU` et `CLOTURE`, les fuseaux horaires et les exceptions administratives. |

**Lecture :** « couvert dans le code » signifie que des routes et traitements ont été identifiés ; ce n’est pas encore une acceptation par le service admission.

## 2. Ce qui fonctionne déjà et doit être conservé

- Catalogue et sessions publiques, puis formulaire de candidature avec sauvegarde de brouillon.
- Gestion des pièces justificatives, contrôle de conformité et extraction de documents.
- Sessions, critères d’admission, pré-sélection, tests académiques, entretiens, délibération et liste d’attente.
- Journal des étapes QMS-009 : il constitue une bonne base de preuve des décisions.
- Indicateurs, rapports et exports CSV/XLSX/PDF ; génération de reçus et de lettres/convocations PDF.
- Séparation candidat / responsable admission côté serveur et contrôles de propriété sur le dossier candidat.

L’interface Angular couvre également les écrans candidat, suivi, paiement, sessions, candidatures, tests, entretiens, délibération, statistiques, notifications et paramètres admission. Il ne faut pas réécrire ce domaine : il faut le fiabiliser et le recetter.

## 3. Améliorations prioritaires

| ID | Priorité | Constat vérifié | Amélioration attendue |
|---|---|---|---|
| REC-01 | P0 | Le backend indique explicitement qu’aucune passerelle réelle n’est connectée. L’interface présente pourtant des moyens de paiement et des champs de carte/CVC. | Retirer immédiatement toute saisie de carte/CVC du mode manuel. Choisir un prestataire, utiliser sa page de paiement hébergée, vérifier ses webhooks côté serveur et conserver référence, preuve, auteur et historique des tentatives. |
| REC-02 | P1 | Aucune implémentation SMS n’a été identifiée dans le domaine recrutement, alors que le TDR prévoit des notifications e-mail/SMS. | Choisir un fournisseur SMS, définir consentement, modèle, coût, statut de livraison et relance. À défaut, annoncer seulement l’e-mail et la notification interne. |
| REC-03 | P1 | Les pièces peuvent être déposées sans règle explicite de gel selon l’état du dossier ; une décision peut donc reposer sur une version différente de celle visible après coup. | Définir les états où un complément est autorisé ; figer les pièces examinées ; réouvrir le dossier via une action tracée si un changement est nécessaire. |
| REC-04 | P1 | Les envois e-mail sont effectués pendant le parcours métier et certaines erreurs SMTP sont seulement journalisées. | Mettre en place une file persistante après validation de la transaction, avec statut, reprises, anti-doublon et tableau des échecs. Une action ne doit pas afficher « envoyé » si elle ne l’est pas. |
| REC-05 | P1 | Comptes de démonstration, récupération de mot de passe et durée de vie des JWT concernent directement candidats et agents d’admission. | Isoler les données fictives du profil réel ; borner OTP et tentatives ; révoquer les sessions après changement de mot de passe ; appliquer une politique de mot de passe côté API. |
| REC-06 | P1 | Les décisions critiques du front utilisent souvent `alert`, `confirm` et `prompt`. | Remplacer par des modales accessibles : motif obligatoire, validation, aperçu des impacts, état de traitement et retour d’erreur lisible. Cela est essentiel pour les rejets, annulations, paiements et actions de masse. |
| REC-07 | P2 | Les listes de candidatures/paiements sont chargées puis filtrées en mémoire sur certains parcours. | Filtrer et paginer côté base ; fournir des exports asynchrones pour gros volumes ; utiliser les mêmes filtres dans la liste, les KPI et l’export. |
| REC-08 | P2 | Les contrôles de fichier reposent sur extension/type déclaré ; aucun examen antivirus ou de signature binaire n’a été identifié. | Ajouter analyse antivirus/quarantaine, contrôle de contenu et limites par type ; journaliser les fichiers refusés ; prévoir la suppression ou conservation légale. |
| REC-09 | P2 | Les notifications et paiements en masse nécessitent une preuve opérationnelle, pas seulement un appel HTTP réussi. | Ajouter l’historique d’envoi par destinataire, l’aperçu avant envoi, l’idempotence, le taux d’échec et la possibilité de relancer uniquement les échecs. |
| REC-10 | P2 | La confidentialité est particulièrement sensible : CV, diplômes, notes, coordonnées, décisions et paiements. | Écrire les règles de conservation, accès, export, suppression/anonymisation et traçabilité ; restreindre les exports aux rôles nécessaires et documenter le responsable de traitement. |
| REC-11 | P2 | Le domaine possède 17 fichiers de tests backend mais une seule spécification frontend identifiée ; les tests Angular ne sont pas actuellement exécutables selon l’audit technique. | Réparer la configuration de tests frontend puis automatiser les scénarios recrutement prioritaires : propriété d’un dossier, changement de statut, paiement, test, entretien, export et refus d’accès. |
| REC-12 | P3 | L’application Angular signale un dépassement du budget de bundle initial lors du build. | Après les P0/P1, mesurer le chargement des pages publiques recrutement et lazy-loader les écrans back-office lourds ; ne pas sacrifier l’accessibilité ou la fiabilité pour ce seul indicateur. |

## 4. Parcours de référence à imposer

```text
Session ouverte
  → Compte candidat / brouillon
  → Dépôt des pièces et soumission
  → Contrôle de complétude et conformité
  → Pré-sélection selon critères
  → Convocation + résultat de test
  → Convocation + évaluation d’entretien
  → Admission / liste d’attente / refus motivé
  → Paiement vérifié ou rapprochement manuel
  → Finalisation de l’inscription et ouverture des droits étudiant
```

Chaque flèche doit avoir : un acteur autorisé, une condition d’entrée, une décision enregistrée, un auteur, une date, des pièces figées si elles sont examinées, une notification traçable et une possibilité de correction gouvernée. Le journal QMS-009 existant doit devenir la source de vérité de ce parcours.

## 5. Plan d’action recrutement

### Lot 1 — Sécuriser et rendre le paiement honnête

1. Retirer la collecte de carte/CVC et renommer les écrans selon le processus réel.
2. Mettre le paiement au statut « en attente de vérification » avec référence et pièce justificative si le contrôle reste manuel.
3. Ou intégrer le prestataire choisi : création de transaction, retour utilisateur, webhook signé, idempotence, rapprochement et reçu.
4. Isoler les comptes de démonstration, protéger la récupération de compte et révoquer les sessions sensibles.

**Critère de sortie :** aucun candidat ne peut croire qu’un paiement a été débité sans confirmation vérifiée ; aucun candidat ne peut faire passer lui-même son paiement à succès.

### Lot 2 — Fiabiliser les décisions admission

1. Formaliser la machine d’états, les motifs obligatoires et les autorisations par rôle.
2. Figer/versionner les pièces au moment de la décision et tracer les réouvertures.
3. Remplacer les dialogues navigateur par des modales métier accessibles.
4. Mettre les e-mails/SMS dans une file persistante et suivre les échecs.

**Critère de sortie :** chaque dossier montre un historique cohérent, une décision explicable et des notifications dont l’état est connu.

### Lot 3 — Industrialiser les volumes et la recette

1. Ajouter pagination, filtres serveur et exports asynchrones.
2. Réparer les tests Angular et automatiser les scénarios de recrutement critiques.
3. Tester les actions de masse, les statuts de session, les dates limites, la concurrence et les permissions croisées.
4. Valider avec l’équipe admission les critères statistiques, les durées de conservation et les modèles de notification.

**Critère de sortie :** une campagne avec un volume réaliste de dossiers reste exploitable, traçable et récupérable.

## 6. Recette métier minimale

| Scénario | Résultat attendu |
|---|---|
| Candidat enregistre un brouillon, se déconnecte puis revient | Les données et pièces autorisées sont reprises ; rien n’est soumis par erreur. |
| Session suspendue ou clôturée | Le candidat ne peut plus soumettre ; l’agent autorisé garde l’exception prévue. |
| Rejet d’un dossier | Motif obligatoire, auteur/date dans le journal, notification avec état d’envoi. |
| Ajout de pièce après décision | Refus ou réouverture formelle ; jamais une modification silencieuse du dossier examiné. |
| Paiement manuel | Aucune donnée carte saisie ; référence, preuve, validateur et reçu sont traçables. |
| Paiement prestataire, si activé | Un webhook authentifié est la seule voie vers le succès ; les doublons ne créent pas de second encaissement. |
| Test et entretien | Seuls les agents habilités modifient résultats/décisions ; toute correction est justifiée et historisée. |
| Action de masse | Aperçu des destinataires, filtre conservé, résultat par destinataire, échecs relançables. |
| Export de candidatures | Champs et périmètre choisis, accès restreint, aucun dossier hors filtre ni colonne non autorisée. |

## 7. Fonctionnalités recrutement à ajouter

Ces fonctionnalités sont classées par valeur métier. Elles doivent être paramétrables **par session de candidature** : chaque formation ou campagne peut avoir ses critères, dates, frais, pièces, jury et modèles de message.

| Priorité | Fonctionnalité | Ce qu’elle apporte |
|---|---|---|
| P1 | **Liste de contrôle candidate** | Le candidat voit immédiatement les pièces manquantes, les pièces refusées, la raison du refus et l’action à faire. Le dossier ne peut être soumis que lorsqu’il est cohérent. |
| P1 | **Centre de notifications candidat** | Une frise unique rassemble chaque décision, convocation, relance, paiement et message. Le candidat peut distinguer « lu », « à faire » et « expiré ». |
| P1 | **Rendez-vous d’entretien avec réservation de créneau** | Le candidat choisit parmi les créneaux ouverts, reçoit une confirmation et peut demander un report selon une règle définie. Cela remplace les échanges manuels confus. |
| P1 | **Paiement réellement intégré ou dépôt de preuve** | Selon le choix INPG : redirection vers un prestataire sécurisé, ou dépôt d’un justificatif avec validation par l’agent. Dans les deux cas : reçu, statut, historique et relance. |
| P1 | **Répartition des dossiers entre agents/jurys** | Affectation d’un ou plusieurs évaluateurs, charge de travail visible, commentaire interne, conflit d’intérêt et validation à deux niveaux pour une décision sensible. |
| P1 | **Relances automatiques configurables** | Relancer les dossiers incomplets, convocations non confirmées et paiements en attente, avec délai, canal, modèle et limite de relances par session. |
| P2 | **Barème d’évaluation structuré** | Pour tests et entretiens : critères pondérés, notes, commentaires, score total et seuil par session. Cela rend la délibération plus juste et explicable. |
| P2 | **Détection de doublons** | Avertir lorsqu’un e-mail, téléphone, pièce d’identité ou combinaison de données correspond déjà à une candidature, sans bloquer abusivement les homonymes. |
| P2 | **Gestion des demandes de complément** | L’agent demande précisément une pièce ou une correction, fixe une échéance et le candidat répond dans un espace dédié. Le dossier garde toutes les versions. |
| P2 | **Import contrôlé des résultats et convocations** | Import Excel avec aperçu, validation des colonnes, rapport d’erreurs et annulation possible pour les listes de candidats, résultats de test ou créneaux. |
| P2 | **Tableau de pilotage opérationnel** | Indicateurs de délai moyen par étape, dossiers bloqués, taux de complétude, taux de conversion, paiements non vérifiés, no-show aux entretiens et charge par agent. |
| P2 | **Historique interne par dossier** | Notes privées, mention d’un collègue, pièces consultées, décisions, changements de statut et communications — jamais visibles du candidat sauf information explicitement publiée. |
| P2 | **Gestion de liste d’attente** | Rang ou règles de priorité, date d’expiration, proposition automatique d’une place libérée et expiration tracée de l’offre. |
| P3 | **Prévisualisation et annotation de documents** | Prévisualiser un PDF/image, annoter une anomalie et demander une correction sans télécharger ni échanger par e-mail. |
| P3 | **Questionnaire post-recrutement** | Mesurer la clarté du formulaire, les délais et les difficultés rencontrées afin d’améliorer la campagne suivante. |

### Ordre recommandé

1. Livrer la checklist, le centre de notifications, la gestion de complément, les relances et le rendez-vous d’entretien : ils réduisent immédiatement les dossiers incomplets et les échanges manuels.
2. Ajouter paiement fiable, affectation des dossiers et barème d’évaluation : ils sécurisent les décisions et les encaissements.
3. Ajouter import contrôlé, détection de doublons, liste d’attente avancée et tableau de pilotage : ils rendent les campagnes à volume élevé réellement pilotables.

Les fonctions d’IA de tri automatique ne sont pas recommandées en priorité. Si elles sont envisagées plus tard, elles doivent rester une aide explicable à l’agent, jamais une décision d’admission autonome, et être évaluées pour les biais.

## Preuves principales

- [CandidatController.java:219](F:/Real_projects/Inpg/LMS_Repo/backend/src/main/java/inpg/sn/lms/recrutement/controller/CandidatController.java:219) — soumission, brouillon, pièces, suivi et paiement candidat.
- [AdminRecrutementController.java:305](F:/Real_projects/Inpg/LMS_Repo/backend/src/main/java/inpg/sn/lms/recrutement/controller/AdminRecrutementController.java:305) — dossiers, exports, décisions, paiements, tests et entretiens admission.
- [PaiementService.java:79](F:/Real_projects/Inpg/LMS_Repo/backend/src/main/java/inpg/sn/lms/recrutement/service/PaiementService.java:79) et [CandidatController.java:557](F:/Real_projects/Inpg/LMS_Repo/backend/src/main/java/inpg/sn/lms/recrutement/controller/CandidatController.java:557) — paiement réel non raccordé.
- [AnalyticsController.java:87](F:/Real_projects/Inpg/LMS_Repo/backend/src/main/java/inpg/sn/lms/recrutement/controller/AnalyticsController.java:87) — KPI, rapports et exports analytiques.
- [formulaire.component.ts:401](F:/Real_projects/Inpg/LMS_Repo/frontend/src/app/features/recrutement/candidature/formulaire/formulaire.component.ts:401) — dialogues navigateur pour erreurs de formulaire/fichier.
- [paiement.component.html:277](F:/Real_projects/Inpg/LMS_Repo/frontend/src/app/features/recrutement/candidature/paiement/paiement.component.html:277) — interface de saisie bancaire à retirer tant qu’aucun prestataire n’est actif.
- [recrutement.routes.ts](F:/Real_projects/Inpg/LMS_Repo/frontend/src/app/features/recrutement/recrutement.routes.ts) — parcours Angular recrutement.
