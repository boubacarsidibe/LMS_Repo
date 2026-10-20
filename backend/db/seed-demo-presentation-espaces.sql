-- =====================================================================
-- Données de présentation — espaces pédagogie, alumni, accréditation, entreprise, recrutement de formateurs,
-- rendez-vous (19/10/2026)
-- =====================================================================
--
-- CE N'EST PAS UNE MIGRATION DE SCHÉMA : du CONTENU, rejouable, hors de db/migration (voir seed-demo-presentation.sql,
-- dont c'est la suite). À exécuter APRÈS lui :
--   docker exec -i demo-postgres psql -U admin -d myappdb < backend/db/seed-demo-presentation-espaces.sql
--
-- CE QUE CE SCRIPT NE FAIT PAS, VOLONTAIREMENT
-- Tout ce qui porte une SIGNATURE (certificats, attestations d'accréditation) ou qui vaut décision (délivrance d'un
-- relevé, avis d'un évaluateur) n'est pas écrit ici : une signature HMAC ne se fabrique pas à la main en SQL, et un
-- document « délivré » sans l'être par le service ne prouverait rien. Ces objets sont créés par l'API réelle
-- (videos-presentation/src/outils/amorcer-api.mjs) ou en direct pendant la prise, ce qui est aussi plus parlant.
--
-- ATTENTION AUX COLONNES ÉNUMÉRÉES (@Enumerated STRING) : une seule valeur inconnue (« Langue maternelle » au lieu de
-- LANGUE_MATERNELLE) fait échouer TOUTE la requête qui lit la table, soit un HTTP 500 sur l'écran entier. Les valeurs
-- ci-dessous sont recopiées des énumérations Java, pas écrites de mémoire.
--
-- CONVENTIONS : clés naturelles, jamais d'identifiants numériques ; WHERE NOT EXISTS partout (idempotent) ; dates
-- relatives à aujourd'hui ; mot de passe des comptes créés : Demo@2026 (Test@2026 pour les comptes « test.* »).
-- =====================================================================

BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =====================================================================
-- D. PÉDAGOGIE — départements, activités du personnel
-- =====================================================================
INSERT INTO departements (code, libelle)
SELECT v.code, v.libelle
FROM (VALUES ('FC',  'Formation continue'),
             ('RI',  'Recherche et innovation'),
             ('SCO', 'Scolarité et vie étudiante')) AS v(code, libelle)
WHERE NOT EXISTS (SELECT 1 FROM departements d WHERE d.code = v.code);

-- Les activités du mois en cours et des deux mois précédents (le rapport mensuel filtre sur mois + année).
INSERT INTO activites_personnel (departement_id, auteur_id, type_activite, titre, description, mois, annee)
SELECT d.id, u.id, v.type, v.titre, v.descr,
       EXTRACT(MONTH FROM CURRENT_DATE - make_interval(months => v.decalage))::int,
       EXTRACT(YEAR  FROM CURRENT_DATE - make_interval(months => v.decalage))::int
FROM (VALUES
  ('FC',  'test_pedago',    'FORMATION', 'Session HSE pour sous-traitants',
      'Trois jours de sensibilisation sécurité pour 24 techniciens de sociétés sous-traitantes.', 0),
  ('FC',  'test_admission', 'PROJET',    'Convention avec trois sociétés d''exploitation',
      'Négociation et signature de conventions de stage pour la promotion 2026.', 0),
  ('RI',  'prof_demo',      'RECHERCHE', 'Étude sur la valorisation du gaz torché',
      'Collecte de données de terrain et premier rapport d''étape avec deux partenaires industriels.', 0),
  ('SCO', 'test_pedago',    'AUTRE',     'Rentrée solennelle de la promotion MPG-2026',
      'Organisation de la cérémonie, remise des badges d''accès et visite des installations.', 1),
  ('FC',  'test_pedago',    'FORMATION', 'Atelier de rédaction technique',
      'Atelier de deux jours animé pour les formateurs vacataires.', 1),
  ('RI',  'prof_demo',      'PROJET',    'Mise à jour de la cartographie des compétences',
      'Révision des référentiels de compétences des cinq cours du master.', 1),
  ('SCO', 'test_admission', 'AUTRE',     'Campagne d''admission 2026',
      'Traitement de 18 dossiers, tests académiques et entretiens du jury.', 2),
  ('RI',  'test_pedago',    'RECHERCHE', 'Publication d''un article sur la formation en HSE',
      'Rédaction conjointe avec deux enseignants-chercheurs partenaires.', 2)
) AS v(dep, auteur, type, titre, descr, decalage)
JOIN departements d ON d.code = v.dep
JOIN users u ON u.username = v.auteur
WHERE NOT EXISTS (SELECT 1 FROM activites_personnel a WHERE a.titre = v.titre AND a.departement_id = d.id);

-- Deux étudiants qui ont TERMINÉ leur programme : tous les supports consultés et une note dans chaque cours.
-- CompletionProgrammeService refuse de délivrer un certificat (ou de promouvoir en alumni) à qui n'a pas fini : Khadija
-- Ndiaye reçoit le sien d'avance (amorcer-api.mjs), Elhadji Diouf attend le sien — délivré en direct dans la vidéo.
INSERT INTO progressions_support (date_consultation, etudiant_id, support_id)
SELECT now() - make_interval(days => 3 + (s.id % 20)::int), u.id, s.id
FROM users u
JOIN etudiants e ON e.id_user = u.id
JOIN promotion p ON p.id = e.promotion_id
JOIN formation_cours fc ON fc.formation_id = p.formation_id
JOIN support_cours s ON s.cours_id = fc.cours_id
WHERE u.username IN ('khadija_ndiaye', 'elhadji_diouf')
  AND NOT EXISTS (SELECT 1 FROM progressions_support x WHERE x.etudiant_id = u.id AND x.support_id = s.id);

INSERT INTO note_module (commentaire, date_evaluation, formateur_nom, module, note, cours_id, etudiant_id)
SELECT 'Très bon travail, rigoureux et bien documenté.', CURRENT_DATE - 12, 'Moussa Ndiaye', 'Évaluation finale', 78 + (c.id % 12), c.id, u.id
FROM users u
JOIN etudiants e ON e.id_user = u.id
JOIN promotion p ON p.id = e.promotion_id
JOIN formation_cours fc ON fc.formation_id = p.formation_id
JOIN cours c ON c.id = fc.cours_id
WHERE u.username IN ('khadija_ndiaye', 'elhadji_diouf')
  AND NOT EXISTS (SELECT 1 FROM note_module n WHERE n.etudiant_id = u.id AND n.cours_id = c.id);

-- =====================================================================
-- E. ALUMNI — parcours, enquête, forum, documents partagés
-- =====================================================================
INSERT INTO situations_professionnelles (alumni_profil_id, intitule_poste, entreprise, date_debut, date_fin, statut_emploi)
SELECT u.id, v.poste, v.entreprise, v.debut, v.fin, v.statut
FROM (VALUES
  ('test_alumni', 'Stagiaire ingénieure réservoir',  'Cabinet ENR Conseil', CURRENT_DATE - 900, CURRENT_DATE - 540, 'EN_EMPLOI'),
  ('test_alumni', 'Consultante Énergie',             'Cabinet ENR Conseil', CURRENT_DATE - 540, NULL::date,         'EN_EMPLOI'),
  ('awa_ndoye',   'Technicienne de production',      'Petrosen',            CURRENT_DATE - 1100, CURRENT_DATE - 600, 'EN_EMPLOI'),
  ('awa_ndoye',   'Ingénieure procédés',             'Petrosen',            CURRENT_DATE - 600, NULL::date,         'EN_EMPLOI')
) AS v(username, poste, entreprise, debut, fin, statut)
JOIN users u ON u.username = v.username
WHERE NOT EXISTS (SELECT 1 FROM situations_professionnelles s
                  WHERE s.alumni_profil_id = u.id AND s.intitule_poste = v.poste);

-- Une enquête ouverte à laquelle une alumna a déjà répondu (la seconde répondra en direct).
INSERT INTO enquetes_suivi (titre, description, active)
SELECT 'Enquête d''insertion professionnelle 2026',
       'Mesurer l''utilité des enseignements dans votre poste actuel et l''effet de la formation sur votre carrière.', true
WHERE NOT EXISTS (SELECT 1 FROM enquetes_suivi WHERE titre = 'Enquête d''insertion professionnelle 2026');

INSERT INTO reponses_enquete_suivi (enquete_id, alumni_profil_id, note_application_competences, note_impact_carriere, commentaire)
SELECT q.id, u.id, 4, 5, 'Les cours de production et de HSE servent tous les jours ; le réseau des alumni m''a ouvert deux opportunités.'
FROM enquetes_suivi q, users u
WHERE q.titre = 'Enquête d''insertion professionnelle 2026' AND u.username = 'awa_ndoye'
  AND NOT EXISTS (SELECT 1 FROM reponses_enquete_suivi r WHERE r.enquete_id = q.id AND r.alumni_profil_id = u.id);

-- Le forum des alumni : un seul salon plat (portee = ALUMNI).
INSERT INTO forum_sujets (clos, date_creation, epingle, titre, auteur_id, portee)
SELECT false, now() - make_interval(days => v.jours), v.epingle, v.titre, u.id, 'ALUMNI'
FROM (VALUES
  ('awa_ndoye',   'Offres de stage et d''emploi : partagez ici', true,  21),
  ('test_alumni', 'Retour d''expérience : passer d''un cabinet conseil à un opérateur', false, 8),
  ('awa_ndoye',   'Soirée des alumni le mois prochain : qui vient ?', false, 3)
) AS v(auteur, titre, epingle, jours)
JOIN users u ON u.username = v.auteur
WHERE NOT EXISTS (SELECT 1 FROM forum_sujets s WHERE s.titre = v.titre);

INSERT INTO forum_messages (contenu, date_creation, auteur_id, sujet_id)
SELECT v.contenu, now() - make_interval(days => v.jours, hours => v.heures), u.id, s.id
FROM (VALUES
  ('Offres de stage et d''emploi : partagez ici', 'awa_ndoye',   'Petrosen cherche deux techniciens de production pour son site de Saint-Louis. Envoyez-moi votre CV, je transmets au service RH.', 21, 0),
  ('Offres de stage et d''emploi : partagez ici', 'test_alumni', 'Mon cabinet recrute une ingénieure réservoir junior à Dakar. Profil géosciences apprécié.', 19, 3),
  ('Retour d''expérience : passer d''un cabinet conseil à un opérateur', 'test_alumni', 'Après deux ans en conseil, j''envisage de rejoindre un opérateur. Des retours sur ce changement de culture ?', 8, 0),
  ('Retour d''expérience : passer d''un cabinet conseil à un opérateur', 'awa_ndoye',   'Chez un opérateur, le rythme est plus lent mais la responsabilité opérationnelle est bien plus forte. Je recommande.', 7, 5),
  ('Soirée des alumni le mois prochain : qui vient ?', 'awa_ndoye', 'Rendez-vous à l''institut, 18 h. Une présentation sur la transition énergétique, puis un cocktail.', 3, 0)
) AS v(titre, auteur, contenu, jours, heures)
JOIN users u ON u.username = v.auteur
JOIN forum_sujets s ON s.titre = v.titre
WHERE NOT EXISTS (SELECT 1 FROM forum_messages m WHERE m.sujet_id = s.id AND m.contenu = v.contenu);

-- Documents partagés par des alumni : les deux PDF sont déposés dans alumni-documents/ par preparer-donnees.sh.
INSERT INTO documents_partages (auteur_id, titre, description, nom_fichier_original, nom_fichier_stocke, type_contenu, date_ajout)
SELECT u.id, v.titre, v.descr, v.original, 'demo-' || v.original, 'application/pdf', now() - make_interval(days => v.jours)
FROM (VALUES
  ('awa_ndoye',   'Guide de l''insertion professionnelle', 'CV, entretien, réseau : les conseils des alumni aux nouvelles promotions.', 'guide-insertion-professionnelle.pdf', 14),
  ('test_alumni', 'Veille : rémunérations du secteur énergie', 'Fourchettes indicatives par profil, issues de retours volontaires.', 'veille-salaires-secteur-energie.pdf', 6)
) AS v(auteur, titre, descr, original, jours)
JOIN users u ON u.username = v.auteur
WHERE NOT EXISTS (SELECT 1 FROM documents_partages d WHERE d.titre = v.titre);

-- =====================================================================
-- F. ACCRÉDITATION — écoles candidates et dossiers à instruire
-- =====================================================================
CREATE TEMP TABLE _ecoles (username text, email text, nom text, adresse text, tel text, site text) ON COMMIT DROP;
INSERT INTO _ecoles VALUES
 ('iah_dakar',  'direction@iah-demo.sn',  'Institut Africain des Hydrocarbures', 'Zone industrielle, Mbao, Dakar',        '+221770000201', 'https://iah-demo.example.sn'),
 ('esmp_thies', 'direction@esmp-demo.sn', 'École Supérieure des Métiers du Pétrole', 'Route de Dakar, Thiès',            '+221770000202', 'https://esmp-demo.example.sn'),
 ('crfp_saint_louis','direction@crfp-demo.sn', 'Centre Régional de Formation Pétrolière', 'Quartier Sor, Saint-Louis',        '+221770000203', 'https://crfp-demo.example.sn');

INSERT INTO users (username, email, password, dtype, type_inscription, date_creation)
SELECT e.username, e.email, crypt('Demo@2026', gen_salt('bf', 10)), 'User', 'ECOLE', now() - interval '20 days'
FROM _ecoles e WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.username = e.username OR u.email = e.email);

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM _ecoles e JOIN users u ON u.username = e.username JOIN roles r ON r.code = 'ROLE_ECOLE_FORMATION'
ON CONFLICT DO NOTHING;

INSERT INTO ecoles_formation (id_user, nom_ecole, adresse, telephone, site_web)
SELECT u.id, e.nom, e.adresse, e.tel, e.site
FROM _ecoles e JOIN users u ON u.username = e.username
WHERE NOT EXISTS (SELECT 1 FROM ecoles_formation x WHERE x.id_user = u.id);

-- Deux dossiers à instruire. Le premier est complet (six pièces) : c'est celui que l'évaluateur traite en direct.
-- Le second est déjà refusé, avec son motif : l'école voit ce que devient un dossier insuffisant.
INSERT INTO demandes_accreditation (ecole_id, statut, date_soumission, intitule_programme, niveau_programme, type_structure,
                                    effectif_cohorte, date_debut_cohorte, plan_actions, motif_rejet, date_decision)
SELECT u.id, v.statut, now() - make_interval(days => v.jours), v.programme, v.niveau, v.structure, v.effectif,
       CURRENT_DATE + v.debut, v.plan,
       CASE WHEN v.statut = 'REJETE' THEN 'Le dossier ne démontre pas la qualification des formateurs ni l''existence d''équipements adaptés aux travaux pratiques annoncés.' END,
       CASE WHEN v.statut = 'REJETE' THEN now() - make_interval(days => v.jours - 6) END
FROM (VALUES
  ('iah_dakar',  'SOUMISE', 4,  'Licence professionnelle — Techniques de production pétrolière', 'LICENCE', 'ECOLE_PRIVEE_RECONNUE', 40, 60,
      'Recruter deux formateurs supplémentaires d''ici la rentrée et formaliser le suivi des diplômés dans les six mois.'),
  ('esmp_thies', 'REJETE',  30, 'Certificat — Opérateur de maintenance en raffinerie',        'CERTIFICAT',             'CENTRE_TECHNIQUE_PRO', 25, 90,
      'Compléter les équipements de laboratoire.'),
  ('crfp_saint_louis', 'SOUMISE', 12, 'Certificat — Technicien de forage et de complétion',          'CERTIFICAT',             'CENTRE_TECHNIQUE_PRO', 30, 45,
      'Doubler les heures de simulation et ouvrir un partenariat de stage avec deux opérateurs.')
) AS v(username, statut, jours, programme, niveau, structure, effectif, debut, plan)
JOIN users u ON u.username = v.username
WHERE NOT EXISTS (SELECT 1 FROM demandes_accreditation d WHERE d.ecole_id = u.id);

-- Dix pièces pour le dossier de l'IAH (les huit requises + deux recommandées) : « Requis : complet » s'affiche chez
-- l'évaluateur. Une ligne fichiers_stockes par pièce (la clé étrangère est unique).
CREATE TEMP TABLE _pieces_acc (type text, fichier text, etiquette text) ON COMMIT DROP;
INSERT INTO _pieces_acc VALUES
 ('CURRICULUM',                'curriculum.pdf',                 'Curriculum'),
 ('POLITIQUE_QUALITE',          'politique-qualite.pdf',          'Politique qualité'),
 ('PROGRAMME_FORMATION',        'programme-formation.pdf',        'Programme de formation'),
 ('QUALIFICATIONS_FORMATEURS',  'qualifications-formateurs.pdf',  'Qualifications des formateurs'),
 ('EQUIPEMENTS_DISPONIBLES',    'equipements.pdf',                'Équipements disponibles'),
 ('ENGAGEMENT_REFERENTIEL',    'engagement-referentiel.pdf',     'Engagement envers le référentiel'),
 ('PREUVE_RECONNAISSANCE',      'preuve-reconnaissance.pdf',      'Preuve de reconnaissance'),
 ('RAPPORT_AUTO_EVALUATION',    'rapport-auto-evaluation.pdf',    'Rapport d''auto-évaluation'),
 ('ORGANIGRAMME',               'organigramme.pdf',               'Organigramme'),
 ('REGLEMENT_INTERIEUR',        'reglement-interieur.pdf',        'Règlement intérieur');

-- Les écoles dont le dossier est complet : l'IAH (instruit en direct dans la vidéo de l'évaluateur) et le CRFP (accrédité
-- d'avance par amorcer-api.mjs, pour que la liste publique des écoles accréditées et l'attestation existent).
CREATE TEMP TABLE _ecoles_pieces (username text, prefixe text) ON COMMIT DROP;
INSERT INTO _ecoles_pieces VALUES ('iah_dakar', 'IAH-'), ('crfp_saint_louis', 'CRFP-');

INSERT INTO fichiers_stockes (chemin_stockage, date_upload, mime_type, nom_original, taille_octets)
SELECT '/app/uploads/demo/accreditation/' || p.fichier, now() - interval '4 days', 'application/pdf',
       e.prefixe || p.fichier, 30000
FROM _pieces_acc p CROSS JOIN _ecoles_pieces e
WHERE NOT EXISTS (SELECT 1 FROM fichiers_stockes f WHERE f.nom_original = e.prefixe || p.fichier);

INSERT INTO pieces_accreditation (type_document, demande_id, fichier_stocke_id)
SELECT p.type, d.id, f.id
FROM _pieces_acc p
CROSS JOIN _ecoles_pieces e
JOIN users u ON u.username = e.username
JOIN demandes_accreditation d ON d.ecole_id = u.id
JOIN fichiers_stockes f ON f.nom_original = e.prefixe || p.fichier
WHERE NOT EXISTS (SELECT 1 FROM pieces_accreditation x WHERE x.demande_id = d.id AND x.type_document = p.type);

-- L'école de test (test.ecole) voit un dossier déjà déposé : on lui donne un intitulé et deux pièces.
UPDATE demandes_accreditation d
SET intitule_programme = 'Master professionnel — Gestion des opérations pétrolières',
    niveau_programme = 'MASTER', type_structure = 'ECOLE_PRIVEE_RECONNUE', effectif_cohorte = 30,
    date_debut_cohorte = CURRENT_DATE + 75,
    plan_actions = 'Mettre en place un comité pédagogique semestriel avec deux professionnels du secteur.'
FROM users u
WHERE u.username = 'test_ecole' AND d.ecole_id = u.id AND d.intitule_programme IS NULL;

INSERT INTO fichiers_stockes (chemin_stockage, date_upload, mime_type, nom_original, taille_octets)
SELECT '/app/uploads/demo/accreditation/' || p.fichier, now() - interval '2 days', 'application/pdf', 'ISFT-' || p.fichier, 30000
FROM _pieces_acc p WHERE p.type IN ('POLITIQUE_QUALITE', 'PROGRAMME_FORMATION')
  AND NOT EXISTS (SELECT 1 FROM fichiers_stockes f WHERE f.nom_original = 'ISFT-' || p.fichier);

INSERT INTO pieces_accreditation (type_document, demande_id, fichier_stocke_id)
SELECT p.type, d.id, f.id
FROM _pieces_acc p
JOIN users u ON u.username = 'test_ecole'
JOIN demandes_accreditation d ON d.ecole_id = u.id
JOIN fichiers_stockes f ON f.nom_original = 'ISFT-' || p.fichier
WHERE p.type IN ('POLITIQUE_QUALITE', 'PROGRAMME_FORMATION')
  AND NOT EXISTS (SELECT 1 FROM pieces_accreditation x WHERE x.demande_id = d.id AND x.type_document = p.type);

-- =====================================================================
-- G. ENTREPRISES — comptes, demandes de formation et échanges avec l'équipe
-- =====================================================================
CREATE TEMP TABLE _ent (username text, email text, mdp text, raison text, secteur text, adresse text, tel text) ON COMMIT DROP;
INSERT INTO _ent VALUES
 ('test_entreprise', 'test.entreprise@inpg.sn',  'Test@2026', 'Sahel Énergie SA',               'Production d''énergie',       'Avenue Cheikh Anta Diop, Dakar', '+221770000301'),
 ('teranga_services','rh@teranga-services-demo.sn','Demo@2026','Teranga Pétrole & Services',      'Services pétroliers',         'Zone portuaire, Dakar',          '+221770000302');

INSERT INTO users (username, email, password, dtype, type_inscription, date_creation)
SELECT e.username, e.email, crypt(e.mdp, gen_salt('bf', 10)), 'User', 'ENTREPRISE', now() - interval '45 days'
FROM _ent e WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.username = e.username OR u.email = e.email);

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM _ent e JOIN users u ON u.username = e.username JOIN roles r ON r.code = 'ROLE_ENTREPRISE'
ON CONFLICT DO NOTHING;

INSERT INTO entreprises (id_user, raison_sociale, secteur, adresse, telephone)
SELECT u.id, e.raison, e.secteur, e.adresse, e.tel
FROM _ent e JOIN users u ON u.username = e.username
WHERE NOT EXISTS (SELECT 1 FROM entreprises x WHERE x.id_user = u.id);

INSERT INTO demandes_entreprise (entreprise_id, type, formation_id, description, effectif_concerne, dates_souhaitees,
                                 statut, date_soumission, date_reponse, reponse_equipe)
SELECT u.id, v.type, f.id, v.descr, v.effectif, v.dates, v.statut,
       now() - make_interval(days => v.jours),
       CASE WHEN v.statut IN ('REPONDUE', 'CLOTUREE') THEN now() - make_interval(days => v.jours - 3) END,
       v.reponse
FROM (VALUES
  ('test_entreprise',  'BESOIN_FORMATION', 'CER-HSE-04', 'Mise à niveau HSE de nos techniciens de maintenance avant l''arrêt programmé du premier trimestre.', 18, 'Janvier 2027', 'REPONDUE', 14,
      'Nous vous proposons une session intra-entreprise de trois jours, dispensée sur votre site. Un devis détaillé vous est transmis par courriel.'),
  ('test_entreprise',  'DEVIS_SESSION',    'CER-TEC-OPE-05', 'Devis pour une session de formation des opérateurs de salle de contrôle.', 12, 'Mars 2027', 'EN_COURS', 6, NULL),
  ('test_entreprise',  'BESOIN_FORMATION', NULL,         'Formation sur mesure : intégrité des pipelines et gestion de la corrosion.', 8, 'À définir avec vous', 'SOUMISE', 1, NULL),
  ('teranga_services', 'DEVIS_SESSION',    'CER-HSE-04', 'Devis pour deux sessions de certification HSE destinées à nos équipes de terrain.', 30, 'Février 2027', 'SOUMISE', 2, NULL)
) AS v(username, type, formation_code, descr, effectif, dates, statut, jours, reponse)
JOIN users u ON u.username = v.username
LEFT JOIN formation f ON f.code = v.formation_code
WHERE NOT EXISTS (SELECT 1 FROM demandes_entreprise d WHERE d.entreprise_id = u.id AND d.description = v.descr);

-- Quelques messages entre l'entreprise de démonstration et l'équipe pédagogique.
INSERT INTO messages_entreprise (entreprise_id, auteur_id, contenu, date_creation)
SELECT e.id, a.id, v.contenu, now() - make_interval(days => v.jours, hours => v.heures)
FROM (VALUES
  ('test_entreprise', 'test_entreprise', 'Bonjour, pourriez-vous nous indiquer les disponibilités de vos formateurs HSE en janvier ?', 13, 0),
  ('test_entreprise', 'test_pedago',     'Bonjour, deux formateurs certifiés sont disponibles la semaine du 11 janvier. Nous vous envoyons le devis.', 12, 20),
  ('test_entreprise', 'test_entreprise', 'Parfait, merci. Nous reviendrons vers vous avec la liste définitive des participants.', 12, 2)
) AS v(entreprise, auteur, contenu, jours, heures)
JOIN users e ON e.username = v.entreprise
JOIN users a ON a.username = v.auteur
WHERE NOT EXISTS (SELECT 1 FROM messages_entreprise m WHERE m.entreprise_id = e.id AND m.contenu = v.contenu);

-- =====================================================================
-- H. RECRUTEMENT DE FORMATEURS — offres publiées et candidatures
-- =====================================================================
INSERT INTO offres_recrutement_formateur (intitule, domaine, description_missions, profil_recherche, type_contrat, lieu, nb_postes,
                                          formation_id, date_publication, date_limite_candidature, statut, cree_par_user_id)
SELECT v.intitule, v.domaine, v.missions, v.profil, v.contrat, 'Dakar', v.postes, f.id,
       CURRENT_DATE - v.publie, CURRENT_DATE + v.limite, v.statut, a.id
FROM (VALUES
  ('Formateur en géosciences du réservoir', 'Géosciences', 'Dispenser les cours de géologie des réservoirs et encadrer les travaux pratiques de modélisation.',
      'Doctorat ou ingénieur confirmé, cinq ans d''expérience en exploration-production, goût de la pédagogie.', 'VACATAIRE', 2, 'MPG', 12, 30, 'PUBLIEE'),
  ('Formateur en hygiène, sécurité et environnement', 'HSE', 'Animer les sessions de certification HSE et accompagner les exercices pratiques.',
      'Certification NEBOSH ou équivalent, expérience terrain sur un site industriel classé.', 'CDD', 1, 'CER-HSE-04', 5, 21, 'PUBLIEE'),
  ('Formateur en économie de l''énergie', 'Économie', 'Intervenir dans le MBA : marchés de l''énergie, évaluation de projets.',
      'Master ou doctorat en économie, expérience en cabinet ou en opérateur.', 'PRESTATION', 1, 'MBA-ECO-MAN-01', 40, -5, 'CLOTUREE'),
  ('Formateur en instrumentation et régulation', 'Procédés', 'Concevoir et animer les travaux pratiques d''instrumentation.',
      'Ingénieur automaticien, expérience en raffinerie ou en production.', 'CDI', 1, 'ING-PET-GAZ-02', 0, 45, 'BROUILLON')
) AS v(intitule, domaine, missions, profil, contrat, postes, formation_code, publie, limite, statut)
JOIN formation f ON f.code = v.formation_code
JOIN users a ON a.username = 'test_admission'
WHERE NOT EXISTS (SELECT 1 FROM offres_recrutement_formateur o WHERE o.intitule = v.intitule);

CREATE TEMP TABLE _cf (username text, email text, nom text, prenom text, tel text, diplome text, annees int, offre text,
                       statut text, decision text, motif text, jours int) ON COMMIT DROP;
INSERT INTO _cf VALUES
 ('test_candidat_formateur', 'test.candidat.formateur@inpg.sn', 'Sarr',   'Ibrahima', '+221771000001', 'Doctorat en géologie pétrolière',      9, 'Formateur en géosciences du réservoir',          'DOSSIER_RECU',           NULL,         NULL, 3),
 ('cf_mame_diagne',          'mame.diagne@inpg-demo.sn',          'Diagne', 'Mame',     '+221771000002', 'Ingénieure géoscientifique',             7, 'Formateur en géosciences du réservoir',          'EN_COURS_DE_TRAITEMENT', NULL,         NULL, 7),
 ('cf_cheikh_kane',          'cheikh.kane@inpg-demo.sn',          'Kane',   'Cheikh',   '+221771000003', 'Master HSE, certifié NEBOSH',            8, 'Formateur en hygiène, sécurité et environnement','TERMINE',                'RETENU',     'Profil très solide, expérience terrain avérée.', 11),
 ('cf_aida_thiam',           'aida.thiam@inpg-demo.sn',           'Thiam',  'Aïda',     '+221771000004', 'Licence en chimie',                      2, 'Formateur en hygiène, sécurité et environnement','TERMINE',                'NON_RETENU', 'Expérience terrain insuffisante pour le niveau attendu.', 12);

INSERT INTO users (username, email, password, dtype, type_inscription, date_creation)
SELECT c.username, c.email, crypt(CASE WHEN c.username LIKE 'test%' THEN 'Test@2026' ELSE 'Demo@2026' END, gen_salt('bf', 10)),
       'User', 'FORMATEUR', now() - interval '30 days'
FROM _cf c WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.username = c.username OR u.email = c.email);

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM _cf c JOIN users u ON u.username = c.username JOIN roles r ON r.code = 'ROLE_CANDIDAT_FORMATEUR'
ON CONFLICT DO NOTHING;

INSERT INTO candidatures_formateur (offre_id, candidat_user_id, nom, prenom, email, telephone, diplome_principal, annees_experience,
                                    statut, decision, motif_decision, date_soumission, date_decision, traite_par_user_id, civilite, genre,
                                    nationalite, niveau_etude, ville, pays_residence)
SELECT o.id, u.id, c.nom, c.prenom, c.email, c.tel, c.diplome, c.annees, c.statut, c.decision, c.motif,
       now() - make_interval(days => c.jours),
       CASE WHEN c.statut = 'TERMINE' THEN now() - make_interval(days => c.jours - 4) END,
       CASE WHEN c.statut = 'TERMINE' THEN t.id END,
       CASE WHEN c.prenom IN ('Mame', 'Aïda') THEN 'Mme' ELSE 'M.' END,
       CASE WHEN c.prenom IN ('Mame', 'Aïda') THEN 'Féminin' ELSE 'Masculin' END,
       'Sénégalaise', 'Bac+8', 'Dakar', 'Sénégal'
FROM _cf c
JOIN users u ON u.username = c.username
JOIN offres_recrutement_formateur o ON o.intitule = c.offre
LEFT JOIN users t ON t.username = 'test_admission'
WHERE NOT EXISTS (SELECT 1 FROM candidatures_formateur x WHERE x.candidat_user_id = u.id AND x.offre_id = o.id);

INSERT INTO candidature_formateur_domaines_expertise (candidature_id, domaine)
SELECT k.id, v.domaine
FROM candidatures_formateur k JOIN users u ON u.id = k.candidat_user_id
JOIN (VALUES ('test_candidat_formateur', 'Géologie des réservoirs'), ('test_candidat_formateur', 'Sismique'),
             ('cf_mame_diagne', 'Modélisation de réservoirs'), ('cf_cheikh_kane', 'HSE industriel'), ('cf_cheikh_kane', 'Audit de sécurité'),
             ('cf_aida_thiam', 'Chimie analytique')) AS v(username, domaine) ON v.username = u.username
WHERE NOT EXISTS (SELECT 1 FROM candidature_formateur_domaines_expertise x WHERE x.candidature_id = k.id AND x.domaine = v.domaine);

INSERT INTO langues_candidature_formateur (candidature_id, langue, niveau)
SELECT k.id, v.langue, v.niveau
FROM candidatures_formateur k JOIN users u ON u.id = k.candidat_user_id
CROSS JOIN (VALUES ('Français', 'LANGUE_MATERNELLE'), ('Anglais', 'COURANT')) AS v(langue, niveau)
WHERE u.username IN ('test_candidat_formateur', 'cf_mame_diagne', 'cf_cheikh_kane', 'cf_aida_thiam')
  AND NOT EXISTS (SELECT 1 FROM langues_candidature_formateur x WHERE x.candidature_id = k.id AND x.langue = v.langue);

INSERT INTO experiences_candidature_formateur (candidature_id, employeur, pays, ville, poste_actuel, intitule_poste, date_debut, date_fin)
SELECT k.id, v.employeur, 'Sénégal', 'Dakar', v.actuel, v.poste, CURRENT_DATE - v.debut, CASE WHEN v.actuel THEN NULL ELSE CURRENT_DATE - v.fin END
FROM candidatures_formateur k JOIN users u ON u.id = k.candidat_user_id
JOIN (VALUES
  ('test_candidat_formateur', 'Société d''exploitation pétrolière', 'Géologue de réservoir senior', true,  3300, 0),
  ('test_candidat_formateur', 'Bureau d''études géosciences',        'Géologue',                     false, 4300, 3300),
  ('cf_mame_diagne',          'Opérateur gazier',                    'Ingénieure géoscientifique',   true,  2500, 0),
  ('cf_cheikh_kane',          'Raffinerie de Mbao',                  'Responsable HSE',              true,  2900, 0),
  ('cf_aida_thiam',           'Laboratoire d''analyses',             'Technicienne de laboratoire',  true,  700,  0)
) AS v(username, employeur, poste, actuel, debut, fin) ON v.username = u.username
WHERE NOT EXISTS (SELECT 1 FROM experiences_candidature_formateur x WHERE x.candidature_id = k.id AND x.intitule_poste = v.poste);

-- =====================================================================
-- I. RENDEZ-VOUS — créneaux publiés par l'équipe et quelques demandes à traiter
-- =====================================================================
-- Des créneaux de 30 minutes sur les prochains jours ouvrés (au plus trois jours) pour trois membres du personnel : une liste
-- plus longue allonge inutilement l'écran de réservation.
INSERT INTO creneaux_disponibilite (personnel_id, date_heure_debut, date_heure_fin, reserve)
SELECT u.id, (jour::date + h.debut), (jour::date + h.debut + interval '30 minutes'), false
FROM users u
CROSS JOIN generate_series(CURRENT_DATE + 1, CURRENT_DATE + 4, interval '1 day') AS jour
CROSS JOIN (VALUES (time '09:00'), (time '10:30'), (time '15:00')) AS h(debut)
WHERE u.username IN ('test_pedago', 'test_admission', 'prof_demo')
  AND EXTRACT(ISODOW FROM jour) < 6
  AND NOT EXISTS (SELECT 1 FROM creneaux_disponibilite c WHERE c.personnel_id = u.id AND c.date_heure_debut = jour::date + h.debut);

-- Réparation : une première version publiait sept jours de créneaux. On retire ceux, libres, qui dépassent la fenêtre.
DELETE FROM creneaux_disponibilite c
USING users u
WHERE u.id = c.personnel_id AND u.username IN ('test_pedago', 'test_admission', 'prof_demo')
  AND NOT c.reserve AND c.date_heure_debut::date > CURRENT_DATE + 4
  AND NOT EXISTS (SELECT 1 FROM rendez_vous r WHERE r.creneau_id = c.id);

-- Trois demandes : une acceptée (le créneau est alors réservé), une en attente, une avec une autre date proposée.
WITH demandes(demandeur, personnel, rang, modalite, motif, statut, commentaire) AS (VALUES
  ('test_ecole',       'test_pedago',    1, 'EN_LIGNE',   'Précisions sur les pièces attendues pour la demande d''accréditation', 'ACCEPTE',                'Avec plaisir, nous vous attendons en visioconférence.'),
  ('test_entreprise',  'test_pedago',    2, 'PRESENTIEL', 'Organisation d''une session HSE intra-entreprise en janvier',          'EN_ATTENTE',             NULL),
  ('test_candidat2',   'test_admission', 1, 'EN_LIGNE',   'Question sur le calendrier des tests académiques',                     'PROPOSITION_AUTRE_DATE', 'Ce créneau est pris : je vous propose le lendemain à la même heure.')
), cibles AS (
  SELECT d.*, du.id AS demandeur_id,
         (SELECT c.id FROM creneaux_disponibilite c JOIN users pu ON pu.id = c.personnel_id
           WHERE pu.username = d.personnel AND NOT c.reserve ORDER BY c.date_heure_debut OFFSET d.rang - 1 LIMIT 1) AS creneau_id
  FROM demandes d JOIN users du ON du.username = d.demandeur
)
INSERT INTO rendez_vous (creneau_id, demandeur_id, modalite, lieu_ou_lien, motif, statut, commentaire_reponse, date_heure_proposee_debut,
                         date_heure_proposee_fin, date_reponse)
SELECT t.creneau_id, t.demandeur_id, t.modalite,
       CASE WHEN t.modalite = 'EN_LIGNE' THEN 'https://visio.inpg-demo.sn/salle/rendez-vous' ELSE 'Institut, bâtiment A, bureau 12' END,
       t.motif, t.statut, t.commentaire,
       CASE WHEN t.statut = 'PROPOSITION_AUTRE_DATE' THEN (SELECT c.date_heure_debut + interval '1 day' FROM creneaux_disponibilite c WHERE c.id = t.creneau_id) END,
       CASE WHEN t.statut = 'PROPOSITION_AUTRE_DATE' THEN (SELECT c.date_heure_fin + interval '1 day' FROM creneaux_disponibilite c WHERE c.id = t.creneau_id) END,
       CASE WHEN t.statut <> 'EN_ATTENTE' THEN now() - interval '1 day' END
FROM cibles t
WHERE t.creneau_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM rendez_vous r WHERE r.demandeur_id = t.demandeur_id AND r.motif = t.motif);

UPDATE creneaux_disponibilite c SET reserve = true
WHERE EXISTS (SELECT 1 FROM rendez_vous r WHERE r.creneau_id = c.id AND r.statut IN ('ACCEPTE', 'EN_ATTENTE', 'PROPOSITION_AUTRE_DATE'));

COMMIT;
