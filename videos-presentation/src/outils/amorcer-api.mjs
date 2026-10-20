// Amorçage par l'API RÉELLE : crée les objets qu'on ne peut pas fabriquer honnêtement en SQL (un relevé « délivré », un
// certificat signé…) en passant par les mêmes endpoints que l'application. Rejouable : chaque création est précédée d'un
// constat de l'existant (un relevé délivré deux fois produirait deux documents).
//
//   ./lancer.sh node src/outils/amorcer-api.mjs
//
// Lancé par preparer-donnees.sh, avant la prise de l'instantané de référence.
const API = process.env.API_URL ?? 'http://localhost:8080/api';

async function appeler(methode, chemin, { jeton, corps } = {}) {
  const reponse = await fetch(`${API}${chemin}`, {
    method: methode,
    headers: { 'Content-Type': 'application/json', ...(jeton ? { Authorization: `Bearer ${jeton}` } : {}) },
    body: corps ? JSON.stringify(corps) : undefined,
  });
  const texte = await reponse.text();
  let json = null;
  try { json = texte ? JSON.parse(texte) : null; } catch { /* corps non JSON */ }
  if (!reponse.ok) throw new Error(`${methode} ${chemin} → HTTP ${reponse.status} ${texte.slice(0, 200)}`);
  return json;
}

async function seConnecter(email, mdp) {
  const { token } = await appeler('POST', '/auth/login', { corps: { email, password: mdp } });
  return token;
}

// ---------------------------------------------------------------- relevés délivrés
// Les alumni ont besoin d'un relevé à consulter ; deux étudiants de la promotion en ont déjà un, de sorte que la liste
// « Relevés délivrés » de la direction pédagogique n'est pas vide quand la vidéo l'ouvre.
const PERIODE = 'Semestre 1 — 2026';
const jeton = await seConnecter('test.pedago@inpg.sn', 'Test@2026');
const etudiants = await appeler('GET', '/pedagogie/etudiants', { jeton });
const parNom = (nom) => etudiants.find((e) => e.nom === nom);
const cibles = ['Rokhaya Gueye', 'Awa Ndoye', 'Mariama Fall', 'Khadija Ndiaye'].map(parNom).filter(Boolean);
const deja = await appeler('GET', '/pedagogie/releves', { jeton });
const aDelivrer = cibles.filter((c) => !deja.some((r) => r.etudiantId === c.id && r.periode === PERIODE));
if (aDelivrer.length) {
  const bilan = await appeler('POST', '/pedagogie/releves', { jeton, corps: { periode: PERIODE, etudiantIds: aDelivrer.map((e) => e.id) } });
  console.log(`  relevés délivrés : ${bilan.delivres.length}` + (bilan.sansNote.length ? ` (sans note : ${bilan.sansNote.join(', ')})` : ''));
} else {
  console.log('  relevés : déjà délivrés');
}

// ---------------------------------------------------------------- certificat délivré
// Khadija Ndiaye a terminé son programme : son certificat est signé par le service réel (une signature HMAC ne se
// fabrique pas en SQL). Il apparaît dans « Mes résultats » côté étudiant et ouvre la page publique de vérification.
const khadija = parNom('Khadija Ndiaye');
if (khadija) {
  const certificats = await appeler('GET', '/pedagogie/certificats', { jeton });
  if (certificats.some((c) => c.etudiantId === khadija.id)) {
    console.log('  certificat : déjà délivré');
  } else {
    const promotions = await appeler('GET', '/pedagogie/promotions', { jeton });
    const mpg = promotions.find((p) => p.code === 'MPG-2026');
    const c = await appeler('POST', '/pedagogie/certificats', {
      jeton,
      corps: {
        etudiantId: khadija.id,
        intituleFormation: 'Master Pétrole & Gaz',
        cohorteId: mpg?.id ?? null,
        civilite: 'Madame',
        dateNaissance: '1998-03-12',
        dateDebut: '2026-01-12',
        dateFin: '2026-10-09',
      },
    });
    console.log(`  certificat délivré : ${c.reference ?? '(référence non renvoyée)'}`);
  }
}

// ---------------------------------------------------------------- une école accréditée d'avance
// Le Centre Régional de Formation Pétrolière est instruit par l'évaluateur de démonstration, de bout en bout, par l'API
// réelle : prise en charge, jugement des critères du référentiel, avis, décision. L'attestation est ainsi SIGNÉE par le
// service, et l'école apparaît dans la liste publique des écoles accréditées. (Le dossier de l'IAH, lui, reste à instruire :
// c'est celui que la vidéo de l'évaluateur traite en direct.)
const jetonEval = await seConnecter('test.evaluateur@inpg.sn', 'Test@2026');
const demandes = await appeler('GET', '/admin/accreditation/demandes', { jeton: jetonEval });
const crfp = demandes.find((d) => d.nomEcole === 'Centre Régional de Formation Pétrolière');
if (!crfp) {
  console.log('  accréditation : dossier du CRFP introuvable');
} else if (crfp.statut !== 'SOUMISE') {
  console.log(`  accréditation : déjà instruit (${crfp.statut})`);
} else {
  await appeler('PUT', `/admin/accreditation/demandes/${crfp.id}/prendre-en-charge`, { jeton: jetonEval });
  const criteres = (await appeler('GET', '/admin/accreditation/criteres', { jeton: jetonEval })).filter((c) => c.actif);
  for (const c of criteres) {
    await appeler('PUT', `/admin/accreditation/demandes/${crfp.id}/criteres/${c.id}`, { jeton: jetonEval, corps: { appreciation: 'CONFORME' } });
  }
  await appeler('POST', `/admin/accreditation/demandes/${crfp.id}/avis`, {
    jeton: jetonEval,
    corps: { avis: 'FAVORABLE', commentaire: "Dossier complet, équipe qualifiée, équipements adaptés : l'ensemble des conditions du référentiel est rempli." },
  });
  const d = await appeler('PUT', `/admin/accreditation/demandes/${crfp.id}/accrediter`, {
    jeton: jetonEval,
    corps: { motif: 'Accréditation pleine : toutes les conditions du référentiel sont remplies.' },
  });
  console.log(`  accréditation : CRFP accrédité (${criteres.length} critères jugés), attestation ${d.referenceAttestation ?? '—'}`);
}
