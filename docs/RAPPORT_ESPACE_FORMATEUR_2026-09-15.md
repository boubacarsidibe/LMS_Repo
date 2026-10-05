# Rapport fonctionnel et technique — espace formateur

Date d'audit : 15 septembre 2026  
Périmètre : parcours `/espace-etudiant/formateur`, interface Angular et API Spring Boot associée.

## 1. Résumé

L'espace formateur est un portail d'animation pédagogique. Il permet à un formateur authentifié de gérer les cours qui lui sont attribués, de publier des ressources, de suivre ses étudiants, de créer des séances, de saisir les présences et les notes, puis d'interagir avec sa communauté.

L'audit a mis en évidence une incohérence dans le dépôt rapide : l'interface annonçait trois types de ressources, alors que seul le dépôt d'un fichier était traité. Cette incohérence est corrigée dans cette livraison :

- le mode **Document pédagogique** accepte les documents autorisés ;
- le mode **Vidéo** accepte un fichier MP4 ;
- le mode **Lien externe** affiche un titre et une URL HTTP(S), puis enregistre la ressource sans fichier local ;
- la limite visible est désormais **10 Mo**, cohérente avec la limite configurée côté serveur.

## 2. Accès, périmètre et sécurité

| Élément | Règle actuelle |
|---|---|
| Route racine | `/espace-etudiant/formateur` |
| Autorisation | `ROLE_FORMATEUR` via `roleGuard` |
| Données de cours | Les opérations vérifient que le formateur est affecté au cours ciblé. |
| Ressources | Un formateur voit, publie et supprime uniquement les ressources de ses cours. |
| Ressources programmées | Elles restent invisibles des étudiants jusqu'à `datePublication`. |
| Téléchargements | Le serveur valide l'extension et le type MIME des fichiers avant stockage. |
| Liens externes | Les nouveaux liens sont limités aux protocoles `http` et `https` et doivent posséder un hôte. |

## 3. Dépendances techniques

### Frontend

| Dépendance | Usage dans l'espace formateur |
|---|---|
| Angular 21 | composants autonomes, routage, rendu et injection de dépendances |
| `@angular/forms` | formulaires réactifs des cours et ressources ; formulaires pilotés par modèle pour filtres et recherche |
| `@angular/router` | shell formateur, garde de rôle, navigation vers le détail étudiant et les rubriques |
| RxJS | appels HTTP, chargements parallèles et traitements asynchrones |
| Tailwind CSS 4 | grille, responsive, états visuels et design system |
| Material Symbols | icônes d'action et de navigation |

### Backend

| Dépendance | Usage dans l'espace formateur |
|---|---|
| Java 17 / Spring Boot 4.1 | API REST et règles métier |
| Spring Web MVC | contrôleurs JSON et multipart pour les fichiers |
| Spring Security + JWT | authentification, rôles et autorisations de périmètre |
| Spring Data JPA / Hibernate | persistance des cours, supports, séances, présences, notes et relations pédagogiques |
| PostgreSQL + Flyway | base relationnelle et migrations de schéma |
| `spring-boot-starter-validation` | validation des requêtes métier lorsque les DTO l'utilisent |
| stockage local configuré par `APP_UPLOAD_DIR` | dépôt des fichiers de cours et des images |

### Contraintes de fichiers

| Sujet | Règle |
|---|---|
| Taille maximum par fichier | 10 Mo (`spring.servlet.multipart.max-file-size`) |
| Taille maximum d'une requête | 20 Mo (`spring.servlet.multipart.max-request-size`) |
| Documents | PDF, Word, PowerPoint, Excel, OpenDocument, texte, CSV et ZIP |
| Médias | JPEG, PNG, WebP, GIF, MP4 et MP3 |
| Refus explicite | HTML, SVG, JavaScript et autres formats exécutables ne sont pas acceptés comme supports |

## 4. Navigation et fonctionnalités

| Rubrique | Fonctionnalités observées | État |
|---|---|---|
| Vue d'ensemble | indicateurs cours/étudiants, prochaines séances, derniers sujets du forum et raccourcis | Opérationnel ; analyse de progression et retours agrégés affichent encore « bientôt disponible » |
| Mes cours et ressources | recherche, filtres semestre/promotion, création, édition, rattachement de promotions, suppression, appel, dépôt et suppression de ressources | Opérationnel |
| Créer un cours | création d'un module, image de couverture optionnelle et rattachement de promotions | Opérationnel |
| Suivi étudiants | liste des étudiants du périmètre, accès au profil individuel | Opérationnel |
| Profil étudiant | historique de présences, notes, moyenne, ajout et suppression de notes | Opérationnel |
| Planning & calendrier | consultation et gestion des séances du formateur | Opérationnel |
| Forum & interaction | sujets, messages et navigation vers les échanges | Opérationnel |
| Messagerie | liste de conversations et fil privé étudiant/formateur | Opérationnel |
| Retours & évaluations | consultation et traitement des retours étudiants | Opérationnel, agrégation tableau de bord à compléter |
| Annonces | publication et consultation dans le shell formateur | Opérationnel |
| Photothèque | albums et dépôt de clichés | Opérationnel |
| Certificats | consultation et gestion des certificats | Opérationnel selon les habilitations métier |
| Rendez-vous | consultation et gestion des créneaux/demandes | Opérationnel |
| Mon profil | lecture et mise à jour du profil formateur | Opérationnel |

## 5. Inventaire des formulaires et champs

### 5.1 Dépôt rapide de ressources

| Champ | Type | Requis | Règle / comportement |
|---|---|---:|---|
| Type de ressource | choix unique | Oui | Document pédagogique, lien externe ou vidéo MP4 |
| Fichier | dépôt / sélection | Oui pour document et vidéo | glisser-déposer ou explorateur ; taille maximale 10 Mo ; vidéo limitée au MP4 côté interface |
| Titre affiché | texte | Oui pour lien | 150 caractères maximum |
| Adresse du lien | URL | Oui pour lien | doit commencer par `http://` ou `https://` ; validation serveur de l'URI et de l'hôte |
| Cours cible | liste | Oui | limité aux cours du formateur connecté |
| Date de publication | date | Non | date du jour par défaut ; une date future programme la visibilité étudiant |

Actions : publier, afficher l'état de publication, vider le fichier après succès, rafraîchir les cartes de cours et la liste des ressources. Les ressources peuvent ensuite être ouvertes ou supprimées depuis la liste.

### 5.2 Création d'un cours

| Champ | Type | Requis | Règle |
|---|---|---:|---|
| Code | texte | Oui | identifiant métier du cours |
| Titre | texte | Oui | intitulé affiché |
| Image de couverture | fichier image | Non | aperçu local avant envoi ; formats d'image validés côté serveur |
| Semestre | liste | Oui | semestre de rattachement |
| Description | texte multiligne | Non | contenu de présentation |
| Volume horaire | nombre | Oui | supérieur ou égal à zéro |
| Promotions concernées | cases à cocher | Non | détermine les étudiants visibles dans le suivi |

### 5.3 Gestion des cours existants

| Formulaire / action | Champs ou données | Résultat |
|---|---|---|
| Modifier un cours | code, titre, semestre, description, volume horaire | met à jour le cours affecté au formateur |
| Gérer les promotions | une ou plusieurs promotions | rattache les promotions et met à jour le périmètre étudiant |
| Supprimer un cours | confirmation explicite | supprime le cours et ses ressources associées selon les règles serveur |
| Faire l'appel | titre de séance, date ; puis présence par étudiant | crée une séance rapide et enregistre les présences |
| Lien de séance | lieu ou URL de visioconférence | met à jour une séance existante |

### 5.4 Notes et suivi individuel

| Champ | Type | Requis | Règle |
|---|---|---:|---|
| Cours | liste | Oui | cours du périmètre formateur |
| Module / évaluation | texte | Oui | libellé de la note |
| Note | texte numérique | Oui | valeur de 0 à 20 ; la virgule est acceptée puis normalisée |
| Commentaire | texte | Non | observation pédagogique |

Le profil étudiant présente aussi les présences et une chronologie combinant notes et séances.

## 6. Contrats API principaux

| Méthode | Route | Usage |
|---|---|---|
| `GET` | `/api/espace-etudiant/formateur/dashboard` | indicateurs du tableau de bord |
| `GET/PUT` | `/api/espace-etudiant/formateur/profile` | profil formateur |
| `GET` | `/api/espace-etudiant/formateur/supports` | liste des ressources du formateur |
| `POST` | `/api/espace-etudiant/formateur/supports` | dépôt multipart d'un document ou d'une vidéo |
| `POST` | `/api/espace-etudiant/formateur/supports/lien` | publication d'un lien HTTP(S) |
| `DELETE` | `/api/espace-etudiant/formateur/supports/{id}` | suppression d'une ressource du périmètre |
| `GET/POST/PUT/DELETE` | `/api/espace-etudiant/formateur/seances…` | séances, planning et liens de visioconférence |
| `GET/POST` | `/api/espace-etudiant/formateur/seances/{id}/presences` | lecture et enregistrement de l'appel |
| `GET/POST/DELETE` | `/api/espace-etudiant/formateur/etudiants/{id}/notes…` | notes du profil étudiant |

## 7. Points de vigilance et améliorations recommandées

1. **Tableau de bord** — remplacer les blocs « bientôt disponible » par des indicateurs réels de présence, progression et retours, avec une période sélectionnable.
2. **Dépôt de ressource** — ajouter la possibilité de remplacer ou modifier une ressource existante, en conservant son historique de publication.
3. **Vidéo** — le dépôt local MP4 est intentionnellement limité à 10 Mo. Pour des vidéos longues, prévoir une intégration de stockage vidéo dédié ou un lien vers une plateforme approuvée.
4. **Séances** — proposer une création complète depuis le planning (type, durée, lieu/lien) en plus de la création rapide depuis l'appel.
5. **Recherche globale** — elle cherche déjà étudiants, cours et supports chargés ; une recherche serveur avec pagination sera nécessaire lorsque le volume augmentera.
6. **Audit métier** — journaliser les publications, suppressions, notes et présences si la procédure qualité exige une traçabilité nominative.

## 8. Vérifications effectuées pour cette mise à jour

- cohérence entre les options du dépôt rapide et les données réellement envoyées ;
- limite client alignée sur les limites multipart du serveur ;
- validation de l'URL sur client et serveur ;
- contrôle du périmètre formateur avant publication ;
- compilation Angular de production et compilation Maven (`-DskipTests compile`) réussies.

## 9. Audit des problèmes relevés

Cet audit est fondé sur la lecture des écrans et des parcours formateur ainsi que sur les contrats front-end / API. Il ne remplace pas un test de charge ni un audit de sécurité d'infrastructure.

### Priorités

| Niveau | Signification |
|---|---|
| P1 | corriger avant ouverture large : confidentialité, périmètre métier ou action sensible |
| P2 | corriger dans le prochain cycle : parcours incomplet, fiabilité ou tenue en charge |
| P3 | planifier : cohérence d'interface, confort ou maintenabilité |

### 9.1 Problèmes de logique et de périmètre métier

| Priorité | Constat | Effet pour le formateur ou l'établissement | Correction attendue |
|---:|---|---|---|
| P1 | Les écrans **Mes cours**, **Créer un cours**, **Suivi des étudiants** et **Photothèque** demandent la route générique `GET /cohortes`, alors qu'une route `GET /formateur/cohortes` existe. | Un formateur peut recevoir les promotions de tout l'établissement, y compris celles qui ne sont liées à aucun de ses cours ; les listes deviennent aussi trompeuses. | Remplacer les appels génériques par la route formateur et garder le contrôle de périmètre côté serveur. |
| P1 | La consultation des certificats retourne la liste globale dès lors que l'utilisateur est formateur. | Risque de consultation de certificats sans rapport avec les cours ou promotions du formateur. | Filtrer les certificats par cours, promotion ou apprenant réellement rattaché au formateur ; tester les refus d'accès. |
| P2 | Le tableau de bord et le suivi annoncent progression, engagement, niveau, export PDF et pagination, mais plusieurs contrôles sont désactivés ou marqués « Bientôt disponible ». | L'interface promet des décisions pédagogiques qu'elle ne permet pas encore de prendre. | Masquer les fonctions non livrées ou les livrer avec données, filtres et export réels. |
| P2 | En créant un cours, un échec d'envoi de l'image est seulement journalisé dans la console puis l'utilisateur est redirigé. | Le cours est créé sans image, sans que le formateur ne sache qu'une partie de son action a échoué. | Afficher un état partiel explicite avec reprise de l'envoi ou annulation contrôlée. |
| P2 | La suppression d'une séance s'appuie sur `confirm()` et ne traite pas l'échec API. | Après une erreur réseau, l'écran peut continuer à afficher une séance supprimée alors qu'elle existe encore. | Utiliser la boîte de dialogue de l'application, afficher une erreur et recharger ou restaurer l'état après échec. |
| P2 | Plusieurs chargements transforment une erreur réseau en liste vide sans message visible, notamment les supports. | Une indisponibilité ressemble à une absence de données ; le formateur peut prendre une mauvaise décision. | Distinguer systématiquement chargement, vide réel et erreur, avec une action « Réessayer ». |
| P3 | Les valeurs de semestre de la création de cours sont écrites en dur avec des années passées. | Les choix vieillissent et obligent une livraison technique pour chaque changement de période. | Alimenter la liste depuis le référentiel académique ou une configuration administrable. |

### 9.2 Problèmes techniques, d'exploitation et de performance

| Priorité | Constat | Risque | Correction attendue |
|---:|---|---|---|
| P1 | Les fichiers de support sont déposés dans le stockage local de l'application. Aucun antivirus, quarantaine ou contrôle de contenu n'apparaît dans le parcours de dépôt. | Un fichier malveillant ou une pièce lourde peut atteindre le serveur ; le stockage local limite aussi la montée en charge et la reprise après incident. | Mettre les dépôts en quarantaine avec analyse antivirus, journalisation et stockage objet ou volume sauvegardé. |
| P2 | L'écriture du fichier et l'enregistrement de la ressource ne forment pas une opération atomique. | Un échec après l'une des deux étapes peut laisser un fichier orphelin ou une ressource indisponible. | Prévoir une compensation en cas d'erreur, une tâche de nettoyage et des métriques sur les échecs de stockage. |
| P2 | Le layout formateur précharge cours, étudiants et supports pour la recherche globale ; la recherche n'est pas déportée au serveur ni temporisée. | Les temps de chargement et la mémoire navigateur augmentent avec les effectifs, avant même que l'utilisateur fasse une recherche. | Ajouter recherche serveur, pagination, limite de résultats et délai de saisie (debounce). |
| P2 | Le suivi des étudiants charge et affiche toute la liste sans pagination active. | La page devient lente et difficile à exploiter sur une promotion importante. | Ajouter pagination serveur, tri stable, filtres fonctionnels et total de résultats. |
| P2 | Le lancement d'une séance ouvre directement `lieuOuLien` dans un nouvel onglet. | Un lien non maîtrisé peut conduire vers un site non fiable ; l'expérience varie selon la valeur enregistrée. | Valider et normaliser les URL côté client et serveur, limiter aux protocoles HTTP(S), informer l'utilisateur qu'il quitte l'application. |
| P3 | Les images de couverture sont lues côté navigateur avec `FileReader` sans limite de taille client, alors que l'API accepte un ensemble précis de formats. | Une grande image peut dégrader le navigateur ; `image/*` autorise aussi des formats qui seront ensuite refusés par le serveur. | Aligner l'attribut `accept` sur les formats serveur et bloquer les fichiers trop volumineux avant lecture. |
| P3 | Les requêtes du tableau de bord sont indépendantes et les échecs ne donnent pas un état global. | Les indicateurs peuvent sembler cohérents alors qu'ils proviennent de chargements partiels. | Centraliser l'état de chargement, signaler les données partielles et prévoir un rafraîchissement explicite. |

### 9.3 Problèmes de design, d'expérience et d'accessibilité

| Priorité | Constat | Conséquence | Correction attendue |
|---:|---|---|---|
| P2 | Des actions de ligne du suivi étudiant ne deviennent visibles qu'au survol (`hover`). | Elles sont difficiles à découvrir sur mobile, au clavier ou sur écran tactile. | Garder les actions visibles, ou utiliser un bouton « Plus d'actions » accessible au clavier avec libellé. |
| P2 | Les cartes KPI et les filtres désactivés occupent une place importante avec le libellé « Bientôt disponible ». | L'écran paraît inachevé et ralentit le parcours vers les fonctions réellement utilisables. | Retirer ces éléments jusqu'à disponibilité, ou afficher une feuille de route hors du parcours opérationnel. |
| P2 | Les retours d'erreur et de succès ne sont pas uniformes selon les écrans. | L'utilisateur ne sait pas toujours si une sauvegarde, une suppression ou un chargement a réellement abouti. | Employer une même convention : message près du champ pour validation, bannière pour erreur de page, notification pour réussite. |
| P3 | La confirmation native du navigateur pour supprimer une séance rompt le style et le vocabulaire de l'application. | Le parcours paraît moins maîtrisé et le contexte de l'action est réduit. | Remplacer par une modale de confirmation qui nomme la séance et rappelle l'impact. |
| P3 | Le dépôt rapide a maintenant des modes distincts document, lien et vidéo, mais il ne montre pas de progression d'envoi. | Sur une connexion lente, le formateur peut cliquer plusieurs fois ou croire que la publication est bloquée. | Afficher progression, état de transfert et empêcher le double envoi jusqu'à la réponse. |

### 9.4 Plan de correction conseillé

1. **Sécuriser le périmètre (P1)** : corriger les promotions et certificats visibles par rôle, puis ajouter des tests d'autorisation avec deux formateurs de promotions différentes.
2. **Fiabiliser les actions (P2)** : états d'erreur distincts, suppression de séance robuste, pagination et recherche serveur.
3. **Durcir les dépôts (P1/P2)** : antivirus/quarantaine, stockage durable, nettoyage des fichiers orphelins et suivi des erreurs.
4. **Rendre l'interface crédible (P2/P3)** : retirer les faux contrôles « bientôt disponible », rendre les actions accessibles et unifier confirmations et notifications.

### 9.5 Critères de recette après correction

- un formateur A ne voit ni promotion, ni certificat, ni étudiant du périmètre du formateur B ;
- une erreur de chargement est identifiable visuellement et peut être relancée ;
- une suppression de séance en échec ne modifie pas l'état affiché ;
- la recherche globale et le suivi restent réactifs avec plusieurs milliers d'étudiants ;
- un dépôt de fichier affiche son avancement, refuse les formats et tailles non admis, puis conserve une trace exploitable de l'opération.
