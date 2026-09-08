import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { EnumTypeNotification, NotificationTemplate, RecrutementService } from '../../../../core/recrutement/services/recrutement.service';

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
  NON_ADMISSION: 'Non-admission',
  DOCUMENT_CONFORME: 'Document validé',
  DOCUMENT_NON_CONFORME: 'Document non conforme',
  RAPPEL_DOSSIER_INCOMPLET: 'Rappel : dossier incomplet',
  RAPPEL_PAIEMENT: 'Rappel : paiement en attente',
};

const PLACEHOLDERS: Record<EnumTypeNotification, string[]> = {
  VALIDATION_DOSSIER: ['numeroDossier'],
  REJET_DOSSIER: ['numeroDossier', 'motif'],
  PAIEMENT_SUCCES: ['numeroDossier'],
  CONVOCATION_TEST: ['nomEpreuve', 'date', 'lieuOuLien'],
  CONVOCATION_ENTRETIEN: ['date', 'lieuOuLien'],
  ADMISSION_TEST: ['numeroDossier'],
  ADMISSION_ENTRETIEN: ['numeroDossier'],
  ADMISSION_FINALE: ['numeroDossier'],
  NON_ADMISSION: ['numeroDossier'],
  DOCUMENT_CONFORME: ['numeroDossier', 'typeDocument'],
  DOCUMENT_NON_CONFORME: ['numeroDossier', 'typeDocument'],
  RAPPEL_DOSSIER_INCOMPLET: ['numeroDossier', 'piecesManquantes'],
  RAPPEL_PAIEMENT: ['numeroDossier'],
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

  constructor(private recrutementService: RecrutementService) {}

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
        alert("Échec de l'enregistrement du modèle.");
      },
    });
  }
}
