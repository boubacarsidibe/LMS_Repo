package inpg.sn.lms.recrutement.controller;

import inpg.sn.lms.exception.ResourceNotFoundException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import inpg.sn.lms.recrutement.entity.Candidature;
import inpg.sn.lms.recrutement.enums.EnumStatutPaiement;
import inpg.sn.lms.recrutement.entity.Paiement;
import inpg.sn.lms.recrutement.entity.PieceJustificative;
import inpg.sn.lms.recrutement.enums.EnumStatutCandidature;
import inpg.sn.lms.recrutement.repository.CandidatureRepository;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final CandidatureRepository candidatureRepo;

    public ApplicationController(CandidatureRepository candidatureRepo) {
        this.candidatureRepo = candidatureRepo;
    }

    // ==============================================
    // GET /api/applications/kpis
    // ==============================================
    @Transactional(readOnly = true)
    @GetMapping("/kpis")
    @PreAuthorize("hasRole('RESPONSABLE_ADMISSION')")
    public ResponseEntity<Map<String, Object>> getKpis(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long programId,
            @RequestParam(required = false) Long sessionId) {
        List<Candidature> all = candidatureRepo.findAll();
        // Apply filters
        if (status != null && !status.isEmpty()) {
            all = all.stream().filter(c -> c.getStatut().name().equalsIgnoreCase(status)).collect(Collectors.toList());
        }
        if (sessionId != null) {
            all = all.stream().filter(c -> c.getSessionCandidature() != null
                    && c.getSessionCandidature().getId().equals(sessionId)).collect(Collectors.toList());
        }
        long total = all.size();
        long aVerifier = all.stream().filter(c -> hasDocumentsEnAttente(c)).count();
        long conformes = all.stream().filter(c -> isDossierComplet(c)).count();
        long incomplets = all.stream().filter(c -> isDossierIncomplet(c)).count();

        Map<String, Object> kpis = new HashMap<>();
        kpis.put("totalCandidatures", total);
        kpis.put("aVerifier", aVerifier);
        kpis.put("conformes", conformes);
        kpis.put("incomplets", incomplets);
        // Nombre de relances à faire = incomplets
        kpis.put("relancesPotentielles", incomplets);
        return ResponseEntity.ok(kpis);
    }

    // ==============================================
    // POST /api/applications/reminder-incomplets
    // ==============================================
    @PostMapping("/reminder-incomplets")
    @PreAuthorize("hasRole('RESPONSABLE_ADMISSION')")
    public ResponseEntity<Map<String, Object>> sendReminder() {
        List<Candidature> all = candidatureRepo.findAll();
        long count = all.stream().filter(this::isDossierIncomplet).count();
        // Simulation d'envoi
        Map<String, Object> result = new HashMap<>();
        result.put("message", count + " relances envoyées avec succès");
        result.put("count", count);
        return ResponseEntity.ok(result);
    }

    // ==============================================
    // GET /api/applications (list + filtres)
    // ==============================================
    @Transactional(readOnly = true)
    @GetMapping
    @PreAuthorize("hasRole('RESPONSABLE_ADMISSION')")
    public ResponseEntity<List<Map<String, Object>>> listApplications(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long programId,
            @RequestParam(required = false) Long sessionId) {
        List<Candidature> all = candidatureRepo.findAll();
        if (search != null && !search.isEmpty()) {
            String q = search.toLowerCase();
            all = all.stream().filter(c -> c.getNumeroDossier().toLowerCase().contains(q)
                    || (c.getCandidat() != null && c.getCandidat().getNom() != null
                            && c.getCandidat().getNom().toLowerCase().contains(q))
                    || (c.getCandidat() != null && c.getCandidat().getPrenom() != null
                            && c.getCandidat().getPrenom().toLowerCase().contains(q))
                    || (c.getCandidat() != null && c.getCandidat().getUser() != null
                            && c.getCandidat().getUser().getEmail() != null
                            && c.getCandidat().getUser().getEmail().toLowerCase().contains(q)))
                    .collect(Collectors.toList());
        }
        if (status != null && !status.isEmpty()) {
            all = all.stream().filter(c -> c.getStatut().name().equalsIgnoreCase(status))
                    .collect(Collectors.toList());
        }
        if (sessionId != null) {
            all = all.stream().filter(c -> c.getSessionCandidature() != null
                    && c.getSessionCandidature().getId().equals(sessionId)).collect(Collectors.toList());
        }
        return ResponseEntity.ok(all.stream().map(this::toApplicationMap).collect(Collectors.toList()));
    }

    // ==============================================
    // GET /api/applications/:id
    // ==============================================
    @Transactional(readOnly = true)
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('RESPONSABLE_ADMISSION')")
    public ResponseEntity<Map<String, Object>> getApplication(@PathVariable Long id) {
        Candidature c = candidatureRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Candidature introuvable"));
        return ResponseEntity.ok(toApplicationDetailMap(c));
    }

    // ==============================================
    // PATCH /api/applications/:id/documents/status
    // ==============================================
    @PatchMapping("/{id}/documents/status")
    @PreAuthorize("hasRole('RESPONSABLE_ADMISSION')")
    public ResponseEntity<Map<String, Object>> updateDocumentStatus(@PathVariable Long id,
                                                                     @RequestBody Map<String, Object> body) {
        Candidature c = candidatureRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Candidature introuvable"));
        Long pieceId = body.get("pieceId") != null ? ((Number) body.get("pieceId")).longValue() : null;
        Boolean valide = body.get("valide") != null ? (Boolean) body.get("valide") : null;
        if (pieceId != null && valide != null) {
            for (PieceJustificative p : c.getPiecesJustificatives()) {
                if (p.getId().equals(pieceId)) {
                    p.setEstConforme(valide);
                }
            }
        }
        candidatureRepo.save(c);
        return ResponseEntity.ok(toApplicationDetailMap(c));
    }

    // ==============================================
    // GET /api/applications/export
    // ==============================================
    @Transactional(readOnly = true)
    @GetMapping("/export")
    @PreAuthorize("hasRole('RESPONSABLE_ADMISSION')")
    public ResponseEntity<byte[]> exportApplications() {
        List<Candidature> all = candidatureRepo.findAll();
        StringBuilder csv = new StringBuilder();
        csv.append("Numéro,Nom,Prénom,Email,Statut,Session,Date Soumission,Pièces,Paiement\n");
        for (Candidature c : all) {
            String nom = c.getCandidat() != null && c.getCandidat().getNom() != null ? c.getCandidat().getNom() : "";
            String prenom = c.getCandidat() != null && c.getCandidat().getPrenom() != null ? c.getCandidat().getPrenom() : "";
            String email = c.getCandidat() != null && c.getCandidat().getUser() != null ? c.getCandidat().getUser().getEmail() : "";
            String pieces = allDocsValid(c) ? "VALIDÉ" : isDossierIncomplet(c) ? "INCOMPLET" : "EN ATTENTE";
            String paiement = estPaye(c) ? "PAYÉ" : "NON PAYÉ";
            String session = c.getSessionCandidature() != null ? c.getSessionCandidature().getLibelle() : "";
            String dateSoumission = c.getDateSoumission() != null ? c.getDateSoumission().toString() : "";
            // L'en-tête annonce neuf colonnes ; le gabarit n'en produisait que huit, pour sept
            // valeurs — d'où un IllegalFormatException à chaque appel. « Session » et « Date
            // Soumission » n'étaient jamais écrites, et nom et prénom se retrouvaient fusionnés
            // dans une seule cellule. Cet export n'a donc jamais abouti.
            csv.append(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                    cellule(c.getNumeroDossier()), cellule(nom), cellule(prenom), cellule(email),
                    cellule(String.valueOf(c.getStatut())), cellule(session), cellule(dateSoumission),
                    cellule(pieces), cellule(paiement)));
        }
        byte[] bytes = csv.toString().getBytes();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=candidatures_export.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(bytes);
    }

    // ==============================================
    // Helpers
    // ==============================================
    private boolean hasDocumentsEnAttente(Candidature c) {
        return c.getPiecesJustificatives().stream().anyMatch(p -> !p.isEstConforme());
    }

    private boolean isDossierComplet(Candidature c) {
        return !c.getPiecesJustificatives().isEmpty() && c.getPiecesJustificatives().stream().allMatch(PieceJustificative::isEstConforme);
    }

    private boolean isDossierIncomplet(Candidature c) {
        return c.getPiecesJustificatives().isEmpty() || c.getPiecesJustificatives().stream().anyMatch(p -> !p.isEstConforme());
    }

    private boolean allDocsValid(Candidature c) {
        return !c.getPiecesJustificatives().isEmpty() && c.getPiecesJustificatives().stream().allMatch(PieceJustificative::isEstConforme);
    }

    private Map<String, Object> toApplicationMap(Candidature c) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", c.getId());
        m.put("numeroDossier", c.getNumeroDossier());
        m.put("statut", c.getStatut().name());
        m.put("dateSoumission", c.getDateSoumission() != null ? c.getDateSoumission().toString() : null);
        m.put("documentStatus", allDocsValid(c) ? "VALIDÉ" : (isDossierIncomplet(c) ? "INCOMPLET" : "EN ATTENTE"));
        m.put("paymentStatus", estPaye(c) ? "PAYÉ" : "NON PAYÉ");
        if (c.getCandidat() != null) {
            m.put("candidatNom", c.getCandidat().getNom() != null ? c.getCandidat().getNom() : "");
            m.put("candidatPrenom", c.getCandidat().getPrenom() != null ? c.getCandidat().getPrenom() : "");
            m.put("candidatEmail", c.getCandidat().getUser() != null ? c.getCandidat().getUser().getEmail() : "");
            m.put("candidatNationalite", c.getCandidat().getNationalite() != null ? c.getCandidat().getNationalite() : "");
            m.put("candidatTelephone", c.getCandidat().getTelephone() != null ? c.getCandidat().getTelephone() : "");
        }
        if (c.getSessionCandidature() != null) {
            m.put("sessionLibelle", c.getSessionCandidature().getLibelle());
            Map<String, Object> prog = new HashMap<>();
            if (c.getSessionCandidature().getProgram() != null) {
                prog.put("id", c.getSessionCandidature().getProgram().getId());
                prog.put("title", c.getSessionCandidature().getProgram().getTitle());
                prog.put("level", c.getSessionCandidature().getProgram().getLevel());
            }
            m.put("program", prog);
        }
        return m;
    }

    private Map<String, Object> toApplicationDetailMap(Candidature c) {
        Map<String, Object> m = toApplicationMap(c);
        m.put("pieces", c.getPiecesJustificatives().stream().map(p -> {
            Map<String, Object> pm = new HashMap<>();
            pm.put("id", p.getId());
            pm.put("typeDocument", p.getTypeDocument());
            pm.put("estConforme", p.isEstConforme());
            pm.put("fichierNom", p.getFichierStocke() != null ? p.getFichierStocke().getNomOriginal() : "");
            return pm;
        }).collect(Collectors.toList()));
        if (c.getPaiement() != null) {
            Map<String, Object> pm = new HashMap<>();
            pm.put("id", c.getPaiement().getId());
            pm.put("montant", c.getPaiement().getMontant());
            pm.put("statut", c.getPaiement().getStatut());
            pm.put("reference", c.getPaiement().getReferenceTransaction());
            m.put("paiement", pm);
        }
        return m;
    }

    /**
     * Un dossier est paye lorsque son paiement est au statut SUCCES.
     *
     * <p>Le test etait ecrit {@code "SUCCESS".equalsIgnoreCase(statut.name())}, avec deux S :
     * l'enumeration vaut SUCCES. La comparaison etait donc TOUJOURS fausse, et tout dossier
     * regle s'affichait « NON PAYE » dans la fiche et dans l'export CSV. Comparer l'enumeration
     * a elle-meme rend la faute impossible : le compilateur refuserait une constante inexistante.
     */
    private boolean estPaye(Candidature c) {
        return c.getPaiement() != null && c.getPaiement().getStatut() == EnumStatutPaiement.SUCCES;
    }

    /**
     * Encadre une valeur pour le CSV. Un nom de famille contenant une virgule, ou un motif
     * de rejet contenant un guillemet, decalerait sinon toutes les colonnes suivantes du
     * fichier — la convention veut qu'un guillemet interne soit double.
     */
    private String cellule(String valeur) {
        String contenu = valeur != null ? valeur : "";
        return "\"" + contenu.replace("\"", "\"\"") + "\"";
    }

}