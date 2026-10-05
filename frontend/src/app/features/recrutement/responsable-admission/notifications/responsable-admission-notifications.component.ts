import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { EnumTypeNotification, NotificationTemplate, RecrutementService } from '../../../../core/recrutement/services/recrutement.service';
import { NotificationService } from '../../../../shared/components/ui/notification.service';

interface TemplateAffiche extends NotificationTemplate {
  label: string;
  placeholders: string[];
  enregistrement: boolean;
}

const LIBELLES: Record<EnumTypeNotification, string> = {
  VALIDATION_DOSSIER: 'Dossier validé (présélection)',
  REJET_DOSSIER: 'Dossier rejeté',
  PAIEMENT_SUCCES: 'Paiement confirmé',
  CONVOCATION_TEST: 'Convocation au test académique',
  CONVOCATION_ENTRETIEN: 'Convocation à l\'entretien',
  ADMISSION_TEST: 'Admis au test académique',
  ADMISSION_ENTRETIEN: 'Admis à l\'entretien',
  ADMISSION_FINALE: 'Admission finale',
  LISTE_ATTENTE: 'Liste complémentaire',
  NON_ADMISSION: 'Non-admission',
  DOCUMENT_CONFORME: 'Document validé',
  DOCUMENT_NON_CONFORME: 'Document non conforme',
  RAPPEL_DOSSIER_INCOMPLET: 'Rappel : dossier incomplet',
  RAPPEL_PAIEMENT: 'Rappel : paiement en attente',
};

// « nomCandidat » est servi pour tous les types : le serveur le résout à partir de
// l'adresse du destinataire, l'appelant n'a pas à le fournir.
const PLACEHOLDERS: Record<EnumTypeNotification, string[]> = {
  VALIDATION_DOSSIER: ['nomCandidat', 'numeroDossier'],
  REJET_DOSSIER: ['nomCandidat', 'numeroDossier', 'motif'],
  PAIEMENT_SUCCES: ['nomCandidat', 'numeroDossier'],
  CONVOCATION_TEST: ['nomCandidat', 'nomEpreuve', 'date', 'lieuOuLien'],
  CONVOCATION_ENTRETIEN: ['nomCandidat', 'date', 'lieuOuLien'],
  ADMISSION_TEST: ['nomCandidat', 'numeroDossier'],
  ADMISSION_ENTRETIEN: ['nomCandidat', 'numeroDossier'],
  ADMISSION_FINALE: ['nomCandidat', 'numeroDossier'],
  LISTE_ATTENTE: ['nomCandidat', 'numeroDossier'],
  NON_ADMISSION: ['nomCandidat', 'numeroDossier'],
  DOCUMENT_CONFORME: ['nomCandidat', 'numeroDossier', 'typeDocument'],
  DOCUMENT_NON_CONFORME: ['nomCandidat', 'numeroDossier', 'typeDocument'],
  RAPPEL_DOSSIER_INCOMPLET: ['nomCandidat', 'numeroDossier', 'piecesManquantes'],
  RAPPEL_PAIEMENT: ['nomCandidat', 'numeroDossier'],
};

@Component({
  selector: 'app-responsable-admission-notifications',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './responsable-admission-notifications.component.html',
})
export class ResponsableAdmissionNotificationsComponent implements OnInit {
  templates: TemplateAffiche[] = [];
  loading = false;
  errorMessage = '';

  constructor(private recrutementService: RecrutementService, private notifications: NotificationService) {}

  ngOnInit(): void {
    this.charger();
  }

  private charger(): void {
    this.loading = true;
    this.errorMessage = '';
    this.recrutementService.getAllTemplates().subscribe({
      next: (data) => {
        this.templates = (data || []).map((t) => ({
          ...t,
          label: LIBELLES[t.type] || t.type,
          placeholders: PLACEHOLDERS[t.type] || [],
          enregistrement: false,
        }));
        this.loading = false;
      },
      error: () => {
        this.templates = [];
        this.loading = false;
        this.errorMessage = 'Impossible de charger les modèles de notifications.';
      },
    });
  }

  enregistrer(template: TemplateAffiche): void {
    template.enregistrement = true;
    this.recrutementService.modifierTemplate(template.type, {
      type: template.type,
      sujet: template.sujet,
      corps: template.corps,
      actif: template.actif,
    }).subscribe({
      next: () => (template.enregistrement = false),
      error: () => {
        template.enregistrement = false;
        this.notifications.erreur("Échec de l'enregistrement du modèle.");
      },
    });
  }
}
