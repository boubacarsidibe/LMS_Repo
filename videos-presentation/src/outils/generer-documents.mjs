// Génère les PDF de démonstration déposés dans les dossiers d'accréditation des écoles et dans l'espace de partage
// des alumni. Les contenus sont fictifs et le disent (bandeau « Document de démonstration ») : aucune donnée réelle.
//
//   ./lancer.sh node src/outils/generer-documents.mjs        → donnees/accreditation/*.pdf, donnees/alumni/*.pdf
//
// Chromium sait imprimer du HTML en PDF (page.pdf) : pas de bibliothèque PDF à installer, et la mise en page suit les
// mêmes polices que l'application.
import { chromium } from '/opt/pw/node_modules/playwright/index.mjs';
import fs from 'node:fs';
import path from 'node:path';

const SORTIE = process.env.SORTIE ?? '/work/donnees';

const page = (titre, sousTitre, sections, auteur) => `<!doctype html><html lang="fr"><head><meta charset="utf-8">
<style>
  @page { size: A4; margin: 22mm 20mm 20mm; }
  body { font-family: Inter, 'DejaVu Sans', sans-serif; color: #191c1e; font-size: 11pt; line-height: 1.5; }
  .bandeau { border-left: 4px solid #30609a; background: #eef3fa; padding: 6px 12px; font-size: 9pt; color: #46464e; margin-bottom: 18px; }
  h1 { font-size: 22pt; margin: 0 0 4px; color: #0e2f5a; letter-spacing: -0.02em; }
  .sous { color: #46464e; margin-bottom: 20px; }
  h2 { font-size: 13pt; color: #30609a; margin: 20px 0 6px; border-bottom: 1px solid #c7c5d0; padding-bottom: 3px; }
  table { border-collapse: collapse; width: 100%; margin: 8px 0; font-size: 10pt; }
  th, td { border: 1px solid #c7c5d0; padding: 5px 8px; text-align: left; }
  th { background: #f2f4f6; }
  ul { margin: 6px 0 6px 18px; padding: 0; }
  .pied { margin-top: 28px; font-size: 9pt; color: #46464e; }
</style></head><body>
<div class="bandeau">Document de démonstration — contenu fictif, produit pour la présentation de la plateforme.</div>
<h1>${titre}</h1><div class="sous">${sousTitre}</div>
${sections.map(([h, corps]) => `<h2>${h}</h2>${corps}`).join('\n')}
<div class="pied">${auteur}</div>
</body></html>`;

const p = (t) => `<p>${t}</p>`;
const ul = (...items) => `<ul>${items.map((i) => `<li>${i}</li>`).join('')}</ul>`;
const table = (entetes, lignes) =>
  `<table><tr>${entetes.map((e) => `<th>${e}</th>`).join('')}</tr>${lignes
    .map((l) => `<tr>${l.map((c) => `<td>${c}</td>`).join('')}</tr>`)
    .join('')}</table>`;

const ECOLE = 'Institut Africain des Hydrocarbures';

const DOCUMENTS = {
  accreditation: {
    'politique-qualite.pdf': page('Politique qualité', `${ECOLE} — édition 2026`, [
      ['Engagement de la direction', p("La direction s'engage à fournir des formations conformes aux exigences du secteur pétrolier et gazier, à mesurer la satisfaction des apprenants et à améliorer en continu ses dispositifs.")],
      ['Objectifs mesurables', table(['Indicateur', 'Cible 2026'], [['Satisfaction des apprenants', '≥ 4,2 / 5'], ['Taux de réussite aux évaluations', '≥ 85 %'], ['Insertion à six mois', '≥ 70 %']])],
      ['Revue annuelle', p('Un comité qualité se réunit chaque trimestre ; les écarts relevés donnent lieu à un plan d’actions suivi par le directeur des études.')],
    ], `${ECOLE} — Direction qualité`),
    'programme-formation.pdf': page('Programme de formation', 'Licence professionnelle — Techniques de production pétrolière', [
      ['Objectifs', p('Former des techniciens supérieurs capables de conduire et de surveiller une installation de production d’hydrocarbures.')],
      ['Maquette', table(['Unité', 'Volume horaire', 'Crédits'], [['Géologie des réservoirs', '40 h', '4'], ['Procédés de production', '60 h', '6'], ['Hygiène, sécurité, environnement', '30 h', '3'], ['Instrumentation et régulation', '45 h', '5'], ['Stage en entreprise', '8 semaines', '12']])],
      ['Modalités d’évaluation', ul('Contrôle continu (40 %)', 'Examen terminal (40 %)', 'Rapport de stage soutenu (20 %)')],
    ], `${ECOLE} — Direction des études`),
    'qualifications-formateurs.pdf': page('Qualifications des formateurs', 'Équipe pédagogique permanente et intervenants', [
      ['Équipe permanente', table(['Formateur', 'Diplôme', 'Expérience'], [['M. A. Diagne', 'Ingénieur pétrolier, ENSPM', '14 ans'], ['Mme K. Sow', 'Docteure en géosciences', '11 ans'], ['M. B. Faye', 'Master HSE, certifié NEBOSH', '9 ans']])],
      ['Intervenants professionnels', p('Quatre ingénieurs en poste dans des sociétés d’exploitation interviennent chaque semestre, à raison de 20 heures chacun.')],
    ], `${ECOLE} — Ressources humaines`),
    'equipements.pdf': page('Équipements disponibles', 'Salles, laboratoires et simulateurs', [
      ['Inventaire', table(['Équipement', 'Quantité', 'Usage'], [['Simulateur de forage', '2', 'Travaux pratiques'], ['Laboratoire d’analyse des fluides', '1', 'Caractérisation'], ['Salles informatiques', '3', 'Modélisation de réservoirs'], ['Bibliothèque technique', '1', '4 500 ouvrages']])],
    ], `${ECOLE} — Services généraux`),
    'curriculum.pdf': page('Curriculum', 'Licence professionnelle — Techniques de production pétrolière', [
      ['Structure sur trois ans', table(['Année', 'Contenu', 'Crédits'], [['1', 'Sciences de base, géologie, informatique', '60'], ['2', 'Procédés, instrumentation, HSE', '60'], ['3', 'Spécialisation et stage de huit semaines', '60']])],
      ['Compétences visées', ul('Conduire une unité de production', 'Diagnostiquer une défaillance d’instrumentation', 'Appliquer les règles HSE en situation')],
    ], `${ECOLE} — Direction des études`),
    'engagement-referentiel.pdf': page('Engagement envers le référentiel', 'Programmes de formation Oil & Gas — référentiel DRCQ', [
      ['Déclaration', p(`${ECOLE} s’engage à respecter l’ensemble des conditions du référentiel d’accréditation, à faciliter les visites du comité d’experts et à signaler toute modification substantielle du programme.`)],
      ['Représentant légal', p('Le directeur général, M. Cheikh Diagne, signataire de la présente déclaration.')],
    ], `${ECOLE} — Direction générale`),
    'preuve-reconnaissance.pdf': page('Preuve de reconnaissance', 'Autorisation d’ouverture et agrément', [
      ['Références', table(['Document', 'Référence', 'Date'], [['Autorisation d’ouverture', 'MESRI-2019-0412', '14 mars 2019'], ['Agrément du programme', 'ANAQ-2023-0087', '2 octobre 2023']])],
    ], `${ECOLE} — Secrétariat général`),
    'organigramme.pdf': page('Organigramme', 'Organisation de l’établissement', [
      ['Direction', ul('Directeur général', 'Directeur des études', 'Responsable qualité', 'Responsable des relations entreprises')],
      ['Départements', p('Production et procédés ; géosciences ; hygiène, sécurité et environnement ; instrumentation.')],
    ], `${ECOLE} — Direction générale`),
    'reglement-interieur.pdf': page('Règlement intérieur', 'Droits et devoirs des apprenants', [
      ['Assiduité', p('Toute absence doit être justifiée sous 48 heures. Au-delà de trois absences non justifiées, l’accès aux évaluations de fin de semestre est suspendu.')],
      ['Discipline et sécurité', ul('Port obligatoire des équipements de protection en laboratoire', 'Respect des consignes d’évacuation', 'Interdiction de toute fraude lors des évaluations')],
    ], `${ECOLE} — Secrétariat général`),
    'rapport-auto-evaluation.pdf': page('Rapport d’auto-évaluation', 'Référentiel d’accréditation — analyse par section', [
      ['Points forts', ul('Équipe pédagogique stable et qualifiée', 'Équipements récents', 'Partenariats avec trois sociétés d’exploitation')],
      ['Points à améliorer', ul('Formalisation du suivi des diplômés', 'Mise à jour annuelle de la cartographie des compétences')],
    ], `${ECOLE} — Comité qualité`),
  },
  alumni: {
    'guide-insertion-professionnelle.pdf': page('Guide de l’insertion professionnelle', 'Conseils des alumni INPG aux nouvelles promotions', [
      ['Préparer son CV', ul('Une page, des résultats chiffrés', 'Mettre en avant les certifications HSE', 'Adapter le CV à chaque offre')],
      ['Réussir l’entretien', p('Connaître l’entreprise, ses sites et ses projets ; préparer deux exemples concrets de situations vécues en stage ou en projet.')],
      ['Réseau', p('Le réseau des alumni est la première source d’offres non publiées : n’hésitez pas à solliciter vos aînés.')],
    ], 'Partagé par les alumni de l’INPG'),
    'veille-salaires-secteur-energie.pdf': page('Veille : rémunérations du secteur énergie', 'Fourchettes indicatives par profil, Afrique de l’Ouest', [
      ['Fourchettes mensuelles (FCFA)', table(['Profil', 'Débutant', 'Confirmé'], [['Technicien production', '450 000', '900 000'], ['Ingénieur procédés', '700 000', '1 600 000'], ['Responsable HSE', '650 000', '1 400 000']])],
      ['Précaution', p('Valeurs indicatives, issues de retours volontaires d’alumni ; elles varient selon l’employeur et la zone d’affectation.')],
    ], 'Partagé par les alumni de l’INPG'),
  },
};

const navigateur = await chromium.launch();
const ctx = await navigateur.newContext();
const pageWeb = await ctx.newPage();
for (const [dossier, docs] of Object.entries(DOCUMENTS)) {
  fs.mkdirSync(path.join(SORTIE, dossier), { recursive: true });
  for (const [nom, html] of Object.entries(docs)) {
    await pageWeb.setContent(html, { waitUntil: 'load' });
    await pageWeb.pdf({ path: path.join(SORTIE, dossier, nom), format: 'A4', printBackground: true });
    console.log(`  ${dossier}/${nom}`);
  }
}
await navigateur.close();
