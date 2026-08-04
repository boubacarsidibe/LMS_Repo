import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { FooterComponent } from '../../../../shared/components/footer/footer.component';
import { NavbarComponent } from '../../../../shared/components/navbar/navbar.component';

export interface FormationDetailData {
  id: number; libelle: string; description: string; imageUrl: string; niveau: string; domaine: string; duree: string;
  badgeLabel?: string; badgeIcon?: string;
  heroMeta?: Array<{ icon: string; label: string; value: string }>;
  modules?: Array<{ titre: string; icon: string; items: string[]; colSpan?: 1|2; badge?: string; description?: string }>;
  methodePedagogique?: { description: string; items: string[]; sousSections?: Array<{ titre: string; items: string[] }> };
  pointsCles?: Array<{ icon: string; titre: string; description: string }>;
  debouches?: Array<{ icon: string; titre: string; description: string }>;
  ctaText?: string;
  sections?: Array<{ titre: string; icon: string; type: 'grid'|'list'|'cards'|'accordion'; items: Array<{ titre?: string; description?: string; icon?: string; badge?: string; sousItems?: string[] }> }>;
  sidebar?: { duree?: string; format?: string; langue?: string; certificat?: string; lieu?: string; tarif?: string; prochaineRentree?: string; dateLimite?: string; boutonTexte?: string; brochure?: boolean };
}

type DetailCatalog = Record<number, FormationDetailData>;

const DETAILS_MOCK: DetailCatalog = {
  1: {
    id: 1, libelle: 'MBA en Économie et Management du Pétrole et du Gaz',
    description: 'Notre programme « Petroleum Economics and Management » vous apportera une expertise aux multiples facettes… Dynamisez votre potentiel !',
    imageUrl: 'https://lh3.googleusercontent.com/aida-public/AB6AXuA_W0k9ChSjdBuRhTWbQpnPFDGEIxoaoorUuhPfhYjGsK32UbIuHG6d0Y2JHK0LqqRYiF7ennrtPf2Ki5yh7eWIISVSzEO2pqWIk4t_cNzAzYwIYNg06jnrC6bWlMxxgUbSn1-EjAphDIpBa-Lsx_Jwfwp5zNmC7Kl9iSYo4aqSVQZHFl_vtsVh1z62xcv2pVuuLyiKpjlfDwZQYyl8g_YvUKQcTclnIfNz3VoOxhyOb-hKMmH_cgVNfA',
    niveau: 'Executive MBA', domaine: 'Management', duree: '18-24 Mois', badgeLabel: 'MBA Level Program', badgeIcon: 'school',
    modules: [
      { titre: 'Techniques Pétrolières & Gazières', icon: 'oil_barrel', items: ['Exploration & Production', 'Transport & Raffinage', 'Energies Alternatives', 'QHSE'] },
      { titre: 'Analyse Economique et Environnement', icon: 'eco', items: ['Fondements', 'Macroéconomie', 'Microéconomie'] },
      { titre: 'Gestion Financière et Management', icon: 'account_balance', items: ['Comptabilité & Analyse financière', 'Gestion budgétaire & Projets', 'Droit, Marketing, RH'] },
      { titre: 'Techniques Quantitatives & Outils de Décision', icon: 'query_stats', items: ['Excel Avancé', 'Statistiques', 'Prévision & Rentabilité Oil & Gas', 'Optimisation'], colSpan: 2 },
      { titre: 'Économie des Hydrocarbures', icon: 'public', items: ['Géopolitique', 'Cadre Juridique Sénégalais', 'Amont, Aval, GNL', 'Stage SONATRACH'] },
    ],
    methodePedagogique: { description: 'Une approche d\'apprentissage hybride et immersive, combinant théorie académique rigoureuse et pratique professionnelle.', items: ['Cours théoriques', 'Exercices', 'Études de cas', 'Travaux de recherche', 'E-learning', 'Présentations', 'Visites d’entreprises', 'Conférences', 'Mémoire', 'Voyage d’études'] },
  },
  2: {
    id: 2, libelle: 'Ingénierie du Pétrole et du Gaz',
    description: 'Un programme technique de pointe couvrant l\'exploration, la production et le raffinage. Conçu pour former les futurs leaders techniques de l\'industrie énergétique mondiale.',
    imageUrl: 'https://lh3.googleusercontent.com/aida-public/AB6AXuCy0DWJm1eVgsiUN3JKWjdx9zkfwFswnsaPBEAD5J1knPtwR9skYTgP8XMQSbd8tfNU99XYwqV7XWwTmXZrRcxI73At8fr_IdbAe1y0WNGZ9-9dVGEj1_0l568w7S4GGOLYWrzSA_umK2SoxOif4kBrh6srnT9opHnYdMnygub309QqSxqLnSNXu7aMYMmibRDFLBbD9Tz7B6y1-AZXGGwTKSoqQyIzmBD3iv6m2uN_-T8AsvQ6jo2Wqg',
    niveau: 'Master 2', domaine: 'Ingénierie', duree: '12 mois', badgeLabel: 'Spécialisation de haut niveau pour ingénieurs', badgeIcon: 'school',
    heroMeta: [{ icon: 'calendar_month', label: 'Durée', value: '12 mois (cours) + 6 mois (stage)' }, { icon: 'translate', label: 'Langue', value: 'Français (supports en anglais)' }],
    pointsCles: [
      { icon: 'landscape', titre: 'Géologie pétrolière', description: 'Analyse structurale, évaluation des réservoirs et modélisation des bassins sédimentaires.' },
      { icon: 'precision_manufacturing', titre: 'Forage', description: 'Ingénierie des fluides, mécanique des roches, complétion des puits et sécurité des opérations.' },
      { icon: 'factory', titre: 'Installations de surface', description: 'Conception des plateformes, séparation des fluides, traitement du gaz et transport par pipeline.' },
      { icon: 'science', titre: 'Raffinage et pétrochimie', description: 'Procédés de distillation, craquage catalytique et valorisation des hydrocarbures.' },
    ],
    methodePedagogique: { description: 'Une pédagogie active et concrète délivrée à temps plein, axée sur l\'application pratique.', items: [],
      sousSections: [{ titre: 'Partenariats d\'excellence', items: ['Formateurs IFP Training & IFP School', 'Interventions d\'experts de l\'industrie'] }, { titre: 'Approche pratique (50/50)', items: ['Études de cas sur données réelles', 'Pratique sur logiciels professionnels', 'Documentation de haute qualité'] }] },
    debouches: [
      { icon: 'engineering', titre: 'Ingénieur de production', description: 'Optimisation des flux d\'extraction et gestion opérationnelle des champs.' },
      { icon: 'construction', titre: 'Ingénieur forage', description: 'Conception des trajectoires de puits et supervision technique sur site.' },
      { icon: 'manage_accounts', titre: 'Responsable technique', description: 'Direction de projets d\'ingénierie et coordination des équipes.' },
    ],
    ctaText: 'Prêt à développer votre expertise ?',
  },
  3: { id: 3, libelle: 'Renforcement de Capacités', description: 'Programmes courts et intensifs pour actualiser les compétences des professionnels du secteur pétrolier et gazier.', imageUrl: 'https://lh3.googleusercontent.com/aida-public/AB6AXuAkQkRSzD_KKRPwNeHlGDrY9wBwrDkpt2yLZNORckLWnX9sgSMJUmLwgnUmF_WpuTSwg5gs0r1Cc9Tfn973iexdNQVxDNXqm7h_yne4l4tJMi2Dl9hjWl-E9OPWKu4sEvpiGyQyrP3zG6k36GgsEzXJu-PjcFVC54hSZSKfCl5eK-31TMrBFLSDaShjCM-zYhHt2PiEuhTP3F8PYYV7picL-zU7W9n0W4qnnkIJfCmKqVZaKWIkbQ6ScA', niveau: 'Certificat', domaine: 'Management', duree: 'Courte durée', badgeLabel: 'Formation Professionnelle Continue', badgeIcon: 'auto_stories' },
  4: {
    id: 4, libelle: 'Certifications HSE',
    description: 'Certifications internationales : Survie, Lutte incendie, H₂S, et sécurité opérationnelle.',
    imageUrl: 'https://lh3.googleusercontent.com/aida-public/AB6AXuBObmr2ldoCiTTmAfZeOilPQ9uPxCSycTKtEgWiWSLdrf64dk2NAks8F1iZdj3wamLvH_18zZUz_SYP6SNlTQJOrvNiOH-VopUFAPhGLE0cqqJMtutquZLKsRENtCSxk40BxG3U_RKdio0RJiwOsxsOK-BMFrqgiOQ1kBVLYMlaWCicvMQUwXP4yjlmzG_HQ2fOl7KIgzGYQDVENkgho-T6OxKDYdcea3rDFmnhsv3zifuwM9pe8pn_JQ',
    niveau: 'Certificat', domaine: 'Sécurité', duree: 'Certificat', badgeLabel: 'Certifications HSE', badgeIcon: 'shield',
    modules: [
      { titre: 'Survie (Survival)', icon: 'water', items: ['BOSIET', 'FOET', 'HUET', 'TBOSIT', 'TFOET', 'THUET'], badge: 'Offshore' },
      { titre: 'Sécurité Chimique', icon: 'science', items: ['Manipulation', 'Stockage', 'Transport', 'Prévention'], badge: 'Chimique' },
      { titre: 'Hygiène et Sécurité au Travail', icon: 'health_and_safety', items: ['Superviseur HSE', 'Permis de travail', 'Identification dangers', 'Enquête accidents'], badge: 'Général' },
      { titre: 'Espaces Confinés & Travail en Hauteur', icon: 'height', items: ['Planification', 'Équipements', 'Sauvetage', 'Réglementation'], badge: 'Hauteur' },
      { titre: 'Conduite Défensive', icon: 'directions_car', items: ['Conduite urbaine', 'Off-road', 'Gestion de trajet', 'Gestion de flotte'], badge: 'Véhicule' },
      { titre: 'Lutte Incendie', icon: 'local_fire_department', items: ['Basic Fire Fighting', 'Fire Warden', 'Évacuation', 'Extincteurs'], badge: 'Feu' },
      { titre: 'Détection H₂S et Gaz', icon: 'co2', items: ['Équipement', 'Interprétation alarmes', 'Urgence', 'Milieu hostile'], badge: 'Gaz' },
      { titre: 'Levage et Gréage', icon: 'forklift', items: ['Inspection matériel', 'Calcul charges', 'Élinguage', 'Communication'], badge: 'Manutention' },
      { titre: 'Rôles Spécifiques HSE', icon: 'supervisor_account', items: ['HLO - HDA', 'Fire Watcher', 'Surveillant espace clos', 'Superviseur'], badge: 'Rôle', colSpan: 2 },
    ],
  },
  5: {
    id: 5, libelle: 'Certifications Techniciens et Opérations',
    description: 'Programmes de certification technique intensifs conçus pour former l\'élite opérationnelle de l\'industrie pétrolière et gazière. Une approche alliant rigueur académique et excellence pratique.',
    imageUrl: 'https://lh3.googleusercontent.com/aida-public/AB6AXuCa9EHSySRVwY6h6rIrpEWgs1fykX5Z1e25jEwK43JmHif1ZW0tx7eqbBvhdQf6ERfqXpmXcwUoUN4V9h7LI6pElsx2i8gmrPoY4KCy1kf3kAHqdrI_L4kSLI6PrgJYaGCXG-ADJxV-U-n8SsN0SWSvaJ9jG912UhloYZzRnTrqafT7rgUc1HWazDNyQMhy9B1cbU5vPlGt2RjP_8TjDzHhrEUqq1YHzkQpIGKTFNX56YL54dhdiDsoBg',
    niveau: 'Certificat', domaine: 'Technique', duree: 'Certificat', badgeLabel: 'Formation Continue', badgeIcon: 'build',
    modules: [
      { titre: 'Maintenance Industrielle', icon: 'build', items: ['Maintenance mécanique et électrique', 'Diagnostic approfondi', 'Stratégies de maintenance préventive'], description: 'Maintenance mécanique et électrique des installations pétrolières.' },
      { titre: 'Opérations de Production', icon: 'factory', items: ['Surveillance et contrôle', 'Optimisation des flux', 'Gestion de la pression', 'Contrôle qualité en continu'], description: 'Supervision et contrôle des processus de production.' },
      { titre: 'Instrumentation & Régulation', icon: 'tune', items: ['Systèmes de contrôle industriels', 'Boucles de régulation', 'Capteurs de pointe', 'Automatisation avancée'], description: 'Gestion des systèmes de contrôle industriels.' },
      { titre: 'Logistique Pétrolière', icon: 'local_shipping', items: ['Chaîne d\'approvisionnement', 'Stockage sécurisé', 'Transfert de fluides', 'Gestion des terminaux'], description: 'Chaîne logistique et gestion des produits pétroliers.' },
      { titre: 'Tuyauterie & Chaudronnerie', icon: 'plumbing', items: ['Conception', 'Assemblage', 'Intégrité structurelle', 'Réseaux sous pression'], description: 'Compétences techniques en tuyauterie industrielle.' },
      { titre: 'Électricité Industrielle', icon: 'bolt', items: ['Systèmes électriques', 'Sécurité en atmosphères explosives', 'Architecture haute tension', 'Normes ATEX'], description: 'Systèmes électriques et sécurité en zones explosives.' },
    ],
    methodePedagogique: { description: 'Notre approche privilégie l\'immersion totale et l\'acquisition de compétences tangibles pour répondre immédiatement aux exigences du terrain.', items: [],
      sousSections: [
        { titre: '80% Pratique et Immersion', items: ['L\'essentiel du programme se déroule en conditions réelles ou simulées', 'Manipulation d\'équipements industriels standards', 'Développement de réflexes opérationnels immédiats'] },
        { titre: 'Ateliers Techniques Spécialisés', items: ['Sessions intensives en petits groupes', 'Laboratoires équipés des dernières technologies', 'Résolution de pannes complexes et étalonnage de précision'] },
        { titre: 'Stages Industriels Intégrés', items: ['Immersion directe au sein d\'entreprises partenaires', 'Secteur pétrolier et gazier', 'Transition fluide vers le marché du travail'] },
      ] },
  },
  6: {
    id: 6, libelle: 'Géosciences et Ingénierie de Réservoir',
    description: 'Une formation d\'excellence pour maîtriser l\'exploration et la caractérisation des réservoirs d\'hydrocarbures, alliant géologie de pointe et ingénierie de précision.',
    imageUrl: 'https://lh3.googleusercontent.com/aida-public/AB6AXuDT7mmySIUiOGMvNFn0l_0z4AdaQsuWVFaIuhqbLSqI2c8kQ3VE9fzd4A9Gyp5ZVEN0jayKLu3MTUp_TVazRIZGZE6JPeq_f1FccVYe4iUFbif9z4z9uduLfah5tpYde0U64DSH-fVQpfp6OBEAC0Fa5IFCl_m2AC00B-UxDJ0jHORAiGF1cDOujxerrkh967WEToteXn5cut9K2pc1GLvAe8cQ6aYRHLdLzMe5-MVx1UuJCFdZyHTIsg',
    niveau: 'Master 2', domaine: 'Géosciences', duree: '2 Ans', badgeLabel: 'Spécialisation Géosciences', badgeIcon: 'terrain',
    modules: [
      { titre: 'Géologie Pétrolière', icon: 'landscape', items: ['Analyse des bassins sédimentaires', 'Systèmes pétroliers'], description: 'Analyse des bassins sédimentaires et systèmes pétroliers.' },
      { titre: 'Géophysique & Sismique', icon: 'graphic_eq', items: ['Interprétation des données sismiques', 'Imagerie du sous-sol'], description: 'Interprétation des données sismiques et imagerie.' },
      { titre: 'Pétrophysique', icon: 'science', items: ['Mesure des propriétés des roches', 'Saturation des fluides'], description: 'Mesure des propriétés des roches et saturation en fluides.' },
      { titre: 'Ingénierie de Réservoir', icon: 'account_tree', items: ['Simulation des flux de fluides', 'Optimisation de la récupération'], description: 'Simulation des flux de fluides et optimisation de la récupération.' },
      { titre: 'Forage d\'Exploration', icon: 'precision_manufacturing', items: ['Techniques de forage exploratoire', 'Diagraphies'], description: 'Techniques de forage exploratoire et diagraphies.' },
      { titre: 'Évaluation Économique', icon: 'insights', items: ['Analyse de rentabilité des projets', 'Gestion des risques'], description: 'Analyse de la rentabilité des projets et gestion des risques.' },
    ],
    methodePedagogique: { description: 'Formation intensive alliant théorie et pratique sur les plateformes standards de l\'industrie.', items: [],
      sousSections: [
        { titre: 'Logiciels de Simulation Industriels', items: ['Apprentissage intensif sur Petrel et Eclipse', 'Modélisation 3D et simulation des fluides'] },
        { titre: 'Travaux Pratiques en Laboratoire', items: ['Analyses pétrophysiques sur carottes réelles', 'Mesures de porosité, perméabilité et saturation'] },
        { titre: 'Sorties Géologiques sur le Terrain', items: ['Observations in situ des analogues de réservoirs', 'Consolidation de la compréhension spatiale'] },
      ] },
    sidebar: { duree: '2 Ans (4 Semestres)', langue: 'Français / Anglais Technique', certificat: 'Master en Ingénierie', prochaineRentree: 'Octobre 2024' },
  },
  7: {
    id: 7, libelle: 'Survie (Survival)',
    description: 'Programmes de formation essentiels pour les environnements offshore, garantissant la sécurité et la préparation aux urgences selon les standards internationaux de l\'industrie pétrolière et gazière.',
    imageUrl: 'https://lh3.googleusercontent.com/aida-public/AB6AXuDNu3KGpwucQ1UAI7_i_GLi0TEirnoiOM5j_Rr4QKdplemZkDt2X7H0FD1l03t5qWFYSwVMqJZWCCAYf3-P3C8AKYvulItcdPzO1xAYrLtuxFbqAK1PR1Prp9LWq0-diZnPX60Gmca2Nc7RsD5lSDknyND6RtK1mxKne5xJKisJbtewJT36ftm8NxUJzatzX2-GQY9GMAup5rXKxjFtPBlDTsdukIxYZ93irIZRSQAww-uABPJrw2W3kHUnkhtr6pm2jcPpbao668s9',
    niveau: 'Certificat', domaine: 'Sécurité', duree: 'Courte durée', badgeLabel: 'Certifications HSE', badgeIcon: 'school',
    modules: [
      { titre: 'BOSIET', icon: 'water', items: ['Formation initiale de sécurité offshore de base'], badge: 'Offshore', description: 'Formation initiale de sécurité offshore de base (Basic Offshore Safety Induction and Emergency Training).' },
      { titre: 'FOET', icon: 'safety_check', items: ['Formation complémentaire d\'urgence offshore'], badge: 'Offshore', description: 'Formation complémentaire d\'urgence offshore (Further Offshore Emergency Training).' },
      { titre: 'HUET', icon: 'flight', items: ['Évacuation sous-marine d\'hélicoptère'], badge: 'Hélicoptère', description: 'Formation à l\'évacuation sous-marine d\'hélicoptère (Helicopter Underwater Escape Training).' },
      { titre: 'TBOSIT', icon: 'sunny', items: ['Formation de base adaptée aux environnements tropicaux'], badge: 'Tropical', description: 'Formation de base de sécurité offshore adaptée aux environnements tropicaux.' },
      { titre: 'TFOET', icon: 'tsunami', items: ['Formation avancée pour les zones tropicales'], badge: 'Tropical', description: 'Formation complémentaire d\'urgence offshore pour les zones tropicales.' },
      { titre: 'THUET', icon: 'air', items: ['Évacuation sous-marine en milieu tropical'], badge: 'Tropical / Helo', description: 'Formation à l\'évacuation sous-marine d\'hélicoptère en milieu tropical.' },
    ],
  },
  8: {
    id: 8, libelle: 'Sécurité chimique (Chemical Safety)',
    description: 'Maîtrisez les protocoles critiques de manipulation, stockage et transport des matières dangereuses en environnement industriel. Cette certification complète est alignée sur les standards HSE internationaux du secteur énergétique.',
    imageUrl: 'https://lh3.googleusercontent.com/aida-public/AB6AXuD6Xf4NlEycoUaB8VQSzX7W4IVCCpDsR9q9bJvboI6nz2acjkSM4a2Qs0fVU6tFaq3CQzI_nazZmoYjDC9hstcEihK-q7hsG3fPS7x6m3EwndNML3n48pkQMubTTgZUOXx8XujZ1uc9Gi_XoYJjhXG6p3yX8gIOaFXQfr-JDLDwLYfBQ0ilnYhDEx0i4NK89juEwBr0aGuoTl_LbXAg0-gSHfgcjnJLZ98UWoUJv45Tlj-o1wvAT7RAXiQNBADpZBt-Ndww91IWkjhx',
    niveau: 'Certificat', domaine: 'Sécurité', duree: 'Courte durée', badgeLabel: 'Certifications HSE', badgeIcon: 'verified',
    modules: [
      { titre: 'Manipulation des Matières Dangereuses', icon: 'science', items: ['Protocoles de stockage', 'Transfert sécurisé', 'Agents réactifs', 'Utilisation industrielle'] },
      { titre: 'Prévention des Déversements', icon: 'water_drop', items: ['Confinement', 'Réponse rapide', 'Protection environnementale', 'Rejets accidentels'] },
      { titre: 'Programme de Communication des Dangers', icon: 'campaign', items: ['Fiches SDS', 'Étiquetage GHS', 'Briefing équipe', 'Communication des risques'] },
      { titre: 'Transport des Marchandises Dangereuses', icon: 'local_shipping', items: ['ADR (Route)', 'IMDG (Mer)', 'IATA (Air)', 'Conformité réglementaire'], colSpan: 2 },
    ],
  },
  9: {
    id: 9, libelle: 'Hygiène et sécurité au travail',
    description: 'Programme complet de certification HSE (Hygiène, Sécurité, Environnement) conçu pour doter les professionnels des compétences essentielles en identification des dangers, évaluation des risques et gestion de la sécurité.',
    imageUrl: 'https://lh3.googleusercontent.com/aida-public/AB6AXuB9OY4ja6HIGACnfr1H2OzynubRrgdRxVxIjU9xes7jjmbAWdLM0qjEBCg3TiNxO3RZyobMeKbVomset1PWT6ZSVwf5bh8CMwOU3-IhOs4JVXMJLuxZtXCUAKuVt2E_SUKILZIgz1QoxXEK0ec36Gz9W9vogWLq6mGa-TCF1tqKGIvTwDTl4jeudI4bFEetPgqOetST6txTPDd1pCp9gV8UIziEP-4vKZkv_qXxwZBVbLrJt_4A--FEnuPMigmCROKWRmmIyZT0qL_4',
    niveau: 'Certificat', domaine: 'Sécurité', duree: 'Courte durée', badgeLabel: 'Certification', badgeIcon: 'school',
    heroMeta: [{ icon: 'schedule', label: 'Durée', value: '12 Semaines' }, { icon: 'translate', label: 'Langue', value: 'Français / Anglais Technique' }],
    modules: [
      { titre: 'Superviseur HSE', icon: 'supervisor_account', items: ['Leadership et supervision', 'Responsabilités HSE', 'Culture sécurité'], badge: 'Module 01' },
      { titre: 'Permis de Travail', icon: 'assignment_turned_in', items: ['Autorisations formelles', 'Activités à haut risque', 'Tâches isolées'], badge: 'Module 02' },
      { titre: 'Identification des Dangers', icon: 'warning', items: ['Méthodologies HIRA', 'Évaluation des risques', 'Atténuation proactive'], badge: 'Module 03' },
      { titre: 'Enquête Accidents', icon: 'analytics', items: ['Analyse des causes racines', 'Protocoles de reporting', 'Plans d\'actions préventives'], badge: 'Module 04' },
    ],
    sections: [
      { titre: 'Thèmes Spécialisés', icon: 'science', type: 'list', items: [
        { titre: 'COSHH (Contrôle des Substances Dangereuses pour la Santé)', icon: 'coronavirus' },
        { titre: 'NORM (Matières Radioactives Naturelles)', icon: 'swipe_vertical' },
        { titre: 'Sécurité Basée sur le Comportement', icon: 'psychology' },
        { titre: 'Sécurité de la Manipulation des Produits Chimiques', icon: 'sanitizer' },
      ]},
      { titre: 'Directives Fondamentales', icon: 'health_and_safety', type: 'cards', items: [
        { titre: 'Règles Vitales (Life Saving Rules)', icon: 'policy', description: 'Protocoles non-négociables conçus pour prévenir les blessures mortelles lors d\'opérations à haut risque.' },
        { titre: 'Règles d\'Or (Golden Rules)', icon: 'stars', description: 'Principes fondamentaux pour maintenir une culture omniprésente de sécurité et de conformité.' },
      ]},
    ],
    sidebar: { duree: '12 Semaines', langue: 'Français / Anglais Technique', lieu: 'Hybride (Campus Dakar & En ligne)', certificat: 'Professionnel HSE INPG', tarif: '500 000 XOF', prochaineRentree: '15 Octobre 2024', boutonTexte: 'S\'inscrire à la cohorte' },
  },
  10: {
    id: 10, libelle: 'Espaces confinés et travail en hauteur',
    description: 'Certification HSE avancée conçue pour les protocoles de sécurité rigoureux dans les environnements industriels à haut risque. Maîtrisez la planification, l\'exécution et les opérations de sauvetage pour les espaces confinés et les travaux en hauteur.',
    imageUrl: 'https://lh3.googleusercontent.com/aida-public/AB6AXuCLi9Bu1u8h5ch-DbBZ-p8M7Sirx5BTDlLjfoMlxob7RKjcpbdWBCQrBtvGhROO6jm5e1hIQZ1v6Q_I7Thb-Zb70JbAY6K05eaG67m5suUM9DrTlsx7eTm-PqwgwptwAfJwaWNLGesm8COvYGX83TNRx_cJ8Ye8EIjnn6LZOb2QG6dMbSouhaip8X39XHOuiqE6TKhVQk3vwIMKLRFpgEUFM69WLcOogvilkfZl6NYoiIdn-DiywQKkj5_2n5I_A8dMauoUM5iGErnK',
    niveau: 'Certificat', domaine: 'Sécurité', duree: 'Courte durée', badgeLabel: 'Certification HSE Fondamentale', badgeIcon: 'verified',
    heroMeta: [{ icon: 'schedule', label: 'Durée', value: '4 Jours Intensifs (32 Heures)' }, { icon: 'translate', label: 'Langue', value: 'Bilingue (Fr / En)' }, { icon: 'group', label: 'Capacité', value: 'Max 12 Participants / Session' }, { icon: 'location_on', label: 'Lieu', value: 'Centre de Formation INPG, Dakar' }],
    pointsCles: [
      { icon: 'architecture', titre: 'Planification Espaces Confinés', description: 'Évaluation stratégique des risques, protocoles de tests atmosphériques et permis d\'entrée.' },
      { icon: 'height', titre: 'Sécurité en Hauteur', description: 'Systèmes anti-chute, techniques d\'ancrage et inspection des équipements.' },
      { icon: 'medical_services', titre: 'Opérations de Sauvetage d\'Urgence', description: 'Méthodes d\'extraction, premiers secours en environnements contraints.' },
      { icon: 'gavel', titre: 'Conformité Réglementaire', description: 'Navigation dans les cadres légaux et les directives institutionnelles strictes de l\'INPG.' },
    ],
    sidebar: { tarif: '450 000 FCFA', duree: '4 Jours Intensifs (32 Heures)', lieu: 'Centre de Formation INPG, Dakar', langue: 'Bilingue (Fr / En)', boutonTexte: 'S\'inscrire', brochure: true },
  },
  11: {
    id: 11, libelle: 'Conduite défensive (Defensive Driving)',
    description: 'Maîtrisez les techniques de conduite avancées pour prévenir les accidents et assurer la sécurité dans divers environnements, des zones urbaines aux conditions tout-terrain difficiles.',
    imageUrl: 'https://lh3.googleusercontent.com/aida-public/AB6AXuBARu70Jr0YLTBJ0SiMdOZwm1RyEvSgkO888TU_8jjDZrjpb8W955CPG9RMshPgcLr_4OAOxCj_LweSLy3TaRR0gfQobQMKQkFbauUTUw0fK2luhYkvBeKPxdgNgVVIMnC6hMxTG3ZQ4Q7YBbrBtz2TQ0d8T02QwCciyv8xfzzraKETtdzlZFFFbny0GItIi0CiT35xKeAr7mf6vALVph_ZrMe3jJ7t-QuI4_2V61rc1PzqkLfAU8Tlby2LGsqvoubNXgiTWD__rAFJ',
    niveau: 'Certificat', domaine: 'Sécurité', duree: 'Courte durée', badgeLabel: 'Certifications HSE', badgeIcon: 'verified',
    modules: [
      { titre: 'Conduite Défensive Urbaine', icon: 'directions_car', items: ['Trafic urbain', 'Anticipation des dangers', 'Intersections complexes'], description: 'Naviguer en toute sécurité dans le trafic urbain, en anticipant les dangers et en gérant les intersections complexes.' },
      { titre: 'Conduite Défensive Tout-Terrain', icon: 'terrain', items: ['Contrôle du véhicule', 'Surfaces irrégulières', 'Faible adhérence'], description: 'Maîtriser le contrôle du véhicule sur des surfaces irrégulières à faible adhérence.' },
      { titre: 'Gestion des Trajets', icon: 'route', items: ['Planification d\'itinéraires', 'Évaluation des risques', 'Protocoles de voyage'], description: 'Planifier des itinéraires sûrs, évaluer les risques et mettre en œuvre des protocoles robustes.' },
      { titre: 'Gestion de Flotte', icon: 'local_shipping', items: ['Maintenance des véhicules', 'Suivi des conducteurs', 'Conformité'], description: 'Optimiser la maintenance des véhicules, surveiller le comportement des conducteurs et assurer la conformité.' },
    ],
  },
  12: {
    id: 12, libelle: 'Lutte contre l\'incendie (Firefighting)',
    description: 'Programme de formation complet conçu pour doter le personnel des compétences essentielles en prévention, évaluation et réponse efficace aux incendies dans les environnements industriels.',
    imageUrl: 'https://lh3.googleusercontent.com/aida-public/AB6AXuAgYh1sdErHsHwbxGjg0hcM188uvqetL8MCQNSiD7xYD25QiCDiDnabkCvcEeelsd7or7Q-u424kqTUuXR8zufuYPgMm60OCYA4IOt292seA1IqVvTymf93jjUYzm5joM7B4dylXxhsCM0uXSJNR-sJZu-TIm0It_XPljRRQ163ImpUg6HUEoXmHphn11ta9HQiqK9y2GgH5fcsBo7_mWHLj0FVge5P7ptf8R7U4BnYeLoybLkdxb_DOL1lusaPknC4U7I_I8g6Seol',
    niveau: 'Certificat', domaine: 'Sécurité', duree: 'Courte durée', badgeLabel: 'Certification HSE', badgeIcon: 'local_fire_department',
    heroMeta: [{ icon: 'schedule', label: 'Durée', value: '3 Jours' }, { icon: 'school', label: 'Format', value: 'Présentiel / Hybride' }, { icon: 'language', label: 'Langue', value: 'FR / EN' }, { icon: 'workspace_premium', label: 'Certificat', value: 'Certifié INPG' }],
    modules: [
      { titre: 'Lutte Incendie de Base', icon: 'local_fire_department', items: ['Triangle du feu', 'Types d\'extincteurs', 'Protocoles d\'évacuation'], badge: 'Module 1', description: 'Fondamentaux de la chimie du feu, identification des classes de feu et application pratique des extincteurs portatifs.' },
      { titre: 'Chef d\'Équipe Incendie', icon: 'shield', items: ['Gestion de crise', 'Procédures de balayage', 'Évaluation des risques'], badge: 'Module 2', description: 'Responsabilités avancées du personnel désigné : leadership en situation d\'urgence, coordination des évacuations et liaison avec les services d\'urgence.' },
    ],
    sections: [
      { titre: 'Public Cible', icon: 'group', type: 'grid', items: [
        { titre: 'Travailleurs Industriels', icon: 'engineering' },
        { titre: 'Agents HSE', icon: 'admin_panel_settings' },
        { titre: 'Gestionnaires d\'Installations', icon: 'apartment' },
        { titre: 'Personnel d\'Entreprise', icon: 'group' },
      ]},
    ],
    sidebar: { duree: '3 Jours', format: 'Présentiel / Hybride', langue: 'FR / EN', certificat: 'Certifié INPG', lieu: 'Campus INPG, Dakar / Sur site sur demande' },
  },
  13: {
    id: 13, libelle: 'Détection H₂S et gaz',
    description: 'Maîtrisez les protocoles de sécurité critiques et les compétences techniques requises pour les environnements exposés au sulfure d\'hydrogène. Une formation rigoureuse et reconnue internationalement par l\'INPG.',
    imageUrl: 'https://lh3.googleusercontent.com/aida-public/AB6AXuAmCJKDWLkEXeGQpParvgMnL1MkGcNGlnFllOzbofzpeS9wqCuV3HrrAjlXgCImBASR5tfHx2Sda23RJltGaRP3YCySsBsyShn0JI1VwXyPWm5PMxB9hj3JMyVztjDXM50hDJ0mPiIMcagpSe4vvsIM1D5biWrRutEbnggn1BOtPMMlV84P-dj_HBPJC4jhZa0VE7pUxYilIrbNcWFkxo1rtOHFxFHkke5AMF-LK0odUblBGnle-yvwTQIwH6tv53NbxVeeQBORBZQy',
    niveau: 'Certificat', domaine: 'Sécurité', duree: 'Courte durée', badgeLabel: 'Certifications HSE', badgeIcon: 'verified',
    modules: [
      { titre: 'Sensibilisation H₂S', icon: 'visibility', items: ['Propriétés du sulfure d\'hydrogène', 'Risques d\'exposition', 'Protocoles d\'intervention d\'urgence de base'], badge: 'Niveau 1', description: 'Compréhension fondamentale des propriétés du H₂S, des risques d\'exposition et des protocoles d\'intervention d\'urgence de base.' },
      { titre: 'Formation H₂S de Base', icon: 'build', items: ['Équipement de détection', 'Protection respiratoire (SCBA)', 'Pratiques sécuritaires en environnements dangereux'], badge: 'Niveau 2', description: 'Application pratique des équipements de détection, protection respiratoire et pratiques de travail sécuritaires.' },
    ],
    sidebar: { duree: '2 Jours (16 Heures)', langue: 'Français / Anglais Technique', certificat: 'Certifié INPG HSE' },
  },
  14: {
    id: 14, libelle: 'Levage et gréage',
    description: 'Programmes de formation complets conçus pour assurer la sécurité et la maîtrise des opérations critiques de levage et de gréage. Maîtrisez les normes industrielles avec nos modules dirigés par des experts.',
    imageUrl: 'https://lh3.googleusercontent.com/aida-public/AB6AXuBTt5XV1wj0V8yBwk80jSwuKpb25VetkKYd1DSVSm0HpULv1raQRDZcdnQwSyNQbsNFza6wbgJ2Hq2Y8JsmrL63JABdBu1KE3iYUe65b1w9E9WILhgyxokGHZ4ISYYy2m7MKlkaPIxLZQKRuU1SlTIfa6C_w3KUCTQzr6GiQic0I49u8D1lbpOFBdNoPBBVauvhQ3c1rTiixodzxRd0ymzjbp3NRkogGzN-Ytw_4EReEqriszMoFkCOIw',
    niveau: 'Certificat', domaine: 'Sécurité', duree: 'Courte durée', badgeLabel: 'Certifications HSE', badgeIcon: 'verified',
    modules: [
      { titre: 'Banksman Slinger (Élingueur Signaleur)', icon: 'front_loader', items: ['Communication essentielles', 'Techniques d\'élingage', 'Sécurité des opérations de grue'], description: 'Apprenez les techniques essentielles de communication et d\'élingage pour des opérations de grue sécurisées.' },
      { titre: 'Personne en Charge des Opérations de Levage', icon: 'engineering', items: ['Planification', 'Organisation', 'Contrôle des tâches de levage'], description: 'Développez des compétences en leadership pour planifier, organiser et contrôler les tâches de levage en toute sécurité.' },
      { titre: 'Opérateur de Pont Roulant', icon: 'forklift', items: ['Opération', 'Inspection', 'Protocoles de sécurité'], description: 'Formation pratique sur l\'utilisation, l\'inspection et les protocoles de sécurité des ponts roulants.' },
      { titre: 'Opérateur de Chariot Élévateur', icon: 'forklift', items: ['Vérifications pré-utilisation', 'Manutention des charges', 'Conduite sécuritaire'], description: 'Certification complète couvrant les vérifications pré-utilisation, la manutention des charges et les pratiques de conduite sécuritaire.' },
      { titre: 'Gestion du Magasin de Gréage', icon: 'inventory', items: ['Contrôle des stocks', 'Procédures d\'inspection', 'Stockage des équipements'], description: 'Apprenez le contrôle efficace des stocks, les procédures d\'inspection et le stockage du matériel de gréage.' },
      { titre: 'Opérateur de Transpalette / Gerbeur', icon: 'local_shipping', items: ['Utilisation sécuritaire', 'Chargement latéral', 'Transpalettes manuels/électriques'], description: 'Formation spécialisée pour l\'utilisation sécuritaire des transpalettes et gerbeurs.' },
    ],
  },
  15: {
    id: 15, libelle: 'Rôles spécifiques',
    description: 'Un programme de formation spécialisé conçu par l\'INPG pour les rôles critiques offshore et industriels. Équipez votre personnel des protocoles de sécurité essentiels et des compétences opérationnelles requises pour les environnements à haut risque.',
    imageUrl: 'https://lh3.googleusercontent.com/aida-public/AB6AXuDFe7ERPhyHKkdx_ctwm1C87CW0iv7C-Yi4QBuaA2Z-ws4KKcOmT6uwm_iamzzm78HITRQ6AIF9Zij3ZDNHIeZjyjMe3TLvR1bQ_W0TpxbO1CBfqfryObT5t07HQhxnYmpw5D6kGG8j5biT3HSmY1Cl40dPvf8EX3CgJ-wbpgqo4fThASv2XFztqmikAwaWxlss5Rm9R9_ip5mORgMdSmdOZrRit5vMrozLv3F-ZIDk6Vt2l43Ul-blf99AdOqPBYqzsok_KpWDT22t',
    niveau: 'Certificat', domaine: 'Sécurité', duree: 'Courte durée', badgeLabel: 'Certification HSE', badgeIcon: 'verified',
    heroMeta: [{ icon: 'schedule', label: 'Durée', value: '3 Semaines' }],
    modules: [
      { titre: 'HLO – HDA (Agent d\'Appontage Hélicoptère)', icon: 'flight_takeoff', items: ['Préparation zone', 'Communication', 'Interventions d\'urgence'], badge: 'Sécurité Aérienne', description: 'Formation complète sur les responsabilités quotidiennes et les procédures d\'urgence. Durée : 4 Jours.' },
      { titre: 'Fire Watcher / Homme de Garde', icon: 'local_fire_department', items: ['Surveillance zones de travail à chaud', 'Identification risques incendie', 'Extinction primaire', 'Procédures d\'alarme'], badge: 'Travail à Chaud / Sécurité', description: 'Prépare le personnel à surveiller activement les zones de travail à chaud.' },
      { titre: 'Surveillant Espace Clos', icon: 'door_front', items: ['Gestion permis de travail', 'Surveillance atmosphère', 'Contrôle accès', 'Extraction sans entrée'], badge: 'CSE / Surveillance', description: 'Formation critique pour les surveillants d\'espaces clos.' },
    ],
    sidebar: { certificat: 'Certification INPG (Validité 2 ans)', duree: '3 Semaines' },
  },
  16: {
    id: 16, libelle: 'MBA en Finance et Stratégie',
    description: 'Un programme d\'excellence conçu pour former les futurs leaders de la finance de marché et d\'entreprise. Maîtrisez les outils quantitatifs, l\'analyse stratégique et les nouvelles technologies financières.',
    imageUrl: 'https://lh3.googleusercontent.com/aida-public/AB6AXuDDyL0rl1qjeBZM6czu_uDWtlcMKdjpelsl-Fce7WHPYXXhGHWSBhS1ld-KdWG0DFwypYX7lB6xFHtLW0WBH5UXxcoAj4G2nszTp_4qM8XyckUDF1j-wMWuhZv9gEtoBYVER3LNxHMLntSJknM8MLrc03FMf4UzOM19EiZzKjOPWDScq4nqoi2zgrQQAk7Gdn9TmBremKzyHOHSy5dOErnPQwRqOdqFlOL--xt5UpKlmQUDmvDKw4U7YQ',
    niveau: 'Executive MBA', domaine: 'Finance', duree: '18-24 Mois', badgeLabel: 'Diplôme d\'Établissement - Niveau Master', badgeIcon: 'workspace_premium',
    pointsCles: [
      { icon: 'trending_up', titre: 'Analyse Quantitative', description: 'Maîtriser les modèles de pricing complexes et l\'évaluation des produits dérivés.' },
      { icon: 'account_tree', titre: 'Stratégie d\'Investissement', description: 'Concevoir et back-tester des stratégies algorithmiques et de gestion de portefeuille.' },
      { icon: 'shield', titre: 'Gestion des Risques', description: 'Implémenter des protocoles de couverture (hedging) et comprendre la régulation (Bâle III/IV).' },
      { icon: 'terminal', titre: 'Programmation Financière', description: 'Développer des applications en Python, C++ et utiliser les bases de données financières.' },
    ],
    sections: [
      { titre: 'Programme Détaillé', icon: 'menu_book', type: 'accordion', items: [
        { titre: 'Fondamentaux et Outils Quantitatifs', badge: 'Semestre 1', sousItems: ['Calcul Stochastique pour la Finance — 30h', 'Analyse des Données et Machine Learning — 45h', 'Programmation Orientée Objet (C++) — 30h', 'Microéconomie et Théorie des Jeux — 20h'] },
        { titre: 'Spécialisation et Marchés', badge: 'Semestre 2', sousItems: ['Produits Dérivés Avancés — 45h', 'Gestion de Portefeuille et Algorithmique — 30h'] },
        { titre: 'Stage de Fin d\'Études', badge: 'Expérience', description: '6 mois d\'immersion professionnelle obligatoire au sein d\'une institution financière, validés par la soutenance d\'un mémoire de recherche appliquée.' },
      ]},
    ],
    sidebar: { duree: '12 mois (Temps plein)', tarif: '24 500 € / an', prochaineRentree: '15 Septembre 2024', lieu: 'Paris - La Défense', dateLimite: '30 Juin', boutonTexte: 'Postuler / Créer un compte', brochure: true },
  },
};

@Component({
  selector: 'app-formation-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, FooterComponent, NavbarComponent],
  templateUrl: './formation-detail.component.html',
})
export class FormationDetailComponent implements OnInit {
  formation: FormationDetailData | null = null;
  notFound = false;

  constructor(private route: ActivatedRoute) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    const id = idParam ? Number(idParam) : NaN;
    if (!isNaN(id) && DETAILS_MOCK[id]) {
      this.formation = DETAILS_MOCK[id];
    } else {
      this.notFound = true;
    }
  }

  methodeIcon(item: string): string {
    const map: Record<string, string> = {
      'Cours théoriques': 'menu_book', 'Exercices': 'edit_note', 'Études de cas': 'assignment',
      'Travaux de recherche': 'search', 'E-learning': 'laptop_chromebook', 'Présentations': 'co_present',
      'Visites d’entreprises': 'factory', 'Conférences': 'mic', 'Mémoire': 'workspace_premium',
      'Voyage d’études': 'flight_takeoff',
    };
    return map[item] || 'check_circle';
  }
}