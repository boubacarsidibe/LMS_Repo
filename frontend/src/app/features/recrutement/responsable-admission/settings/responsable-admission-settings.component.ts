import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RecrutementService } from '../../../../core/recrutement/services/recrutement.service';

interface EmailTemplate {
  cle: 'confirmation' | 'convocation';
  label: string;
  objet: string;
  corps: string;
}

@Component({
  selector: 'app-responsable-admission-settings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './responsable-admission-settings.component.html',
})
export class ResponsableAdmissionSettingsComponent implements OnInit {
  private recrutementService = inject(RecrutementService);

  activeTab: 'general' | 'rules' | 'templates' = 'general';
  toastMessage = '';
  toastVisible = false;

  institutName = 'INPG Dakar - Institut National du Pétrole et du Gaz';
  contactEmail = 'admissions@inpg.sn';
  candidaturesSpontanees = true;
  paiementEnLigne = true;

  seuilTests: number | null = null;
  sessionOuverteId: number | null = null;
  chargementRegles = true;
  erreurRegles = '';

  templates: EmailTemplate[] = [
    {
      cle: 'confirmation',
      label: 'Confirmation de réception',
      objet: 'Confirmation de réception de votre dossier - INPG',
      corps: `Bonjour {NOM_CANDIDAT},\n\nNous vous confirmons la bonne réception de votre dossier pour la session {SESSION}.\nVotre numéro de dossier est : {NUMERO_DOSSIER}.\n\nCordialement,\nL'équipe Admissions INPG`,
    },
    {
      cle: 'convocation',
      label: 'Convocation au test',
      objet: 'Convocation au test académique - INPG',
      corps: `Bonjour {NOM_CANDIDAT},\n\nVous êtes convoqué(e) au test académique de la session {SESSION}.\nMerci de vous présenter muni(e) d'une pièce d'identité.\n\nCordialement,\nL'équipe Admissions INPG`,
    },
  ];
  selectedTemplateCle: EmailTemplate['cle'] = 'confirmation';

  get selectedTemplate(): EmailTemplate {
    return this.templates.find((template) => template.cle === this.selectedTemplateCle) ?? this.templates[0];
  }

  ngOnInit(): void {
    this.chargerRegles();
  }

  switchTab(tab: 'general' | 'rules' | 'templates'): void {
    this.activeTab = tab;
  }

  selectTemplate(cle: EmailTemplate['cle']): void {
    this.selectedTemplateCle = cle;
  }

  private chargerRegles(): void {
    this.chargementRegles = true;
    this.recrutementService.getRules().subscribe({
      next: (regles) => {
        this.seuilTests = regles.seuilTests;
        this.sessionOuverteId = regles.sessionId;
        this.chargementRegles = false;
      },
      error: () => {
        this.erreurRegles = 'Impossible de charger le seuil de réussite.';
        this.chargementRegles = false;
      },
    });
  }

  enregistrerSeuilTests(): void {
    if (this.seuilTests == null) return;
    this.erreurRegles = '';
    this.recrutementService.updateRules(this.seuilTests).subscribe({
      next: (regles) => {
        this.seuilTests = regles.seuilTests;
        this.saveSettings('Seuil de réussite enregistré.');
      },
      error: (error) => {
        this.erreurRegles = error?.error?.message || "L'enregistrement du seuil a échoué.";
      },
    });
  }

  sauvegarderOngletActif(): void {
    if (this.activeTab === 'rules') {
      this.enregistrerSeuilTests();
      return;
    }
    this.saveSettings(this.activeTab === 'templates' ? 'Modèle enregistré avec succès.' : 'Paramètres enregistrés avec succès.');
  }

  private saveSettings(message: string): void {
    this.toastMessage = message;
    this.toastVisible = true;
    setTimeout(() => (this.toastVisible = false), 3000);
  }
}
