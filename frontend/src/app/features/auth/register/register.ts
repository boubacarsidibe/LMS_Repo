import { Component, inject } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, ValidationErrors, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { Auth } from '../../../core/auth/services/authService';

/**
 * confirmPassword n'avait jusqu'ici que Validators.required : un mot de passe
 * de confirmation différent du premier passait la validation sans le
 * moindre avertissement (relevé lors d'un audit externe). Validateur de
 * groupe plutôt que de champ, car il doit lire les deux contrôles à la fois.
 */
function motsDePasseIdentiques(groupe: AbstractControl): ValidationErrors | null {
  const motDePasse = groupe.get('password')?.value;
  const confirmation = groupe.get('confirmPassword')?.value;
  return !confirmation || motDePasse === confirmation ? null : { motsDePasseDifferents: true };
}

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    RouterLinkActive
  ],
  templateUrl: './register.html',
  styleUrl: './register.css'
})
export class Register {
  private fb = inject(FormBuilder);
  private authService = inject(Auth);
  private router = inject(Router);

  /**
   * Parcours interrompu à reprendre après l'inscription (transmis par la page de
   * connexion, elle-même appelée depuis « Postuler » sur la vitrine des offres).
   * Prioritaire sur la redirection par {@link typeInscription}, qui ne connaît
   * que les trois parcours historiques.
   */
  private readonly returnUrl = inject(ActivatedRoute).snapshot.queryParamMap.get('returnUrl');

  errorMessage: string = '';
  isLoading: boolean = false;

  /**
   * Un compte tout juste créé n'a encore aucun rôle métier (attribué
   * uniquement à la soumission réelle d'une candidature ou d'une demande
   * d'accréditation) : sans ce choix, redirectByRole renvoyait aussi bien un
   * futur candidat qu'un représentant d'école vers l'accueil générique, sans
   * aucune indication sur la suite à donner.
   */
  typeInscription: 'CANDIDAT' | 'ECOLE' | 'ENTREPRISE' | 'FORMATEUR' = 'CANDIDAT';

  /**
   * École et entreprise s'inscrivent au nom d'une organisation, pas d'une
   * personne : "raisonSociale"/"nomEcole" ne sont recueillis qu'à la
   * soumission réelle de la demande (EntrepriseController, EcoleAccreditationController),
   * jamais à l'inscription. Un seul champ "nom de l'organisation" ici évite
   * de faire deviner un prénom/nom inexistant.
   */
  get estOrganisation(): boolean {
    return this.typeInscription === 'ECOLE' || this.typeInscription === 'ENTREPRISE';
  }

  get libelleNomOrganisation(): string {
    return this.typeInscription === 'ECOLE' ? "Nom de l'école" : "Nom de l'entreprise";
  }

  // Déclaration de tous les contrôles liés au HTML
  registerForm: FormGroup = this.fb.group({
    prenom: ['', Validators.required],
    nom: ['', Validators.required],
    nomOrganisation: [''],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', Validators.required]
  }, { validators: motsDePasseIdentiques });

  /**
   * Champs dans leur ordre d'apparition à l'écran, avec l'identifiant de
   * l'élément correspondant : sert à désigner le premier champ qui bloque
   * l'envoi et à y placer le curseur.
   */
  private static readonly CHAMPS: ReadonlyArray<{ nom: string; id: string; libelle: string }> = [
    { nom: 'prenom', id: 'reg-prenom', libelle: 'Prénom' },
    { nom: 'nom', id: 'reg-nom', libelle: 'Nom' },
    { nom: 'nomOrganisation', id: 'reg-nom-organisation', libelle: "Nom de l'organisation" },
    { nom: 'email', id: 'reg-email', libelle: 'Email institutionnel' },
    { nom: 'password', id: 'reg-password', libelle: 'Mot de passe' },
    { nom: 'confirmPassword', id: 'reg-confirm', libelle: 'Confirmation du mot de passe' }
  ];

  /**
   * Un champ n'affiche son erreur qu'une fois que l'utilisateur l'a quitté —
   * jamais avant qu'il ait eu l'occasion d'y saisir quoi que ce soit.
   *
   * <p>Un indicateur « une tentative d'envoi a eu lieu » couvrait auparavant
   * cette condition : au premier clic sur « Créer un compte », les cinq champs
   * du formulaire vide viraient au rouge d'un coup, y compris ceux que
   * l'utilisateur n'avait jamais touchés. Un mur de messages ne désigne aucun
   * responsable : c'est {@link onSubmit} qui signale désormais le premier
   * champ bloquant, un à la fois.
   */
  champEnErreur(nom: string): boolean {
    const controle = this.registerForm.get(nom);
    return !!controle && controle.invalid && controle.touched;
  }

  choisirTypeInscription(type: 'CANDIDAT' | 'ECOLE' | 'ENTREPRISE' | 'FORMATEUR'): void {
    this.typeInscription = type;
    const prenom = this.registerForm.get('prenom')!;
    const nom = this.registerForm.get('nom')!;
    const nomOrganisation = this.registerForm.get('nomOrganisation')!;

    if (this.estOrganisation) {
      prenom.clearValidators();
      nom.clearValidators();
      nomOrganisation.setValidators(Validators.required);
    } else {
      prenom.setValidators(Validators.required);
      nom.setValidators(Validators.required);
      nomOrganisation.clearValidators();
    }
    prenom.updateValueAndValidity();
    nom.updateValueAndValidity();
    nomOrganisation.updateValueAndValidity();
  }

  /**
   * Réponse au clic lorsque le formulaire n'est pas envoyable : désigne le
   * premier champ qui bloque, affiche son message et y place le curseur.
   *
   * <p>Sans cela, le clic ne produisait rien d'utile — l'envoi était refusé en
   * silence, et le seul retour visible était l'ensemble des champs virant au
   * rouge simultanément. Un champ à la fois, celui sur lequel agir, avec le
   * curseur déjà dedans.
   */
  private signalerPremierChampBloquant(): void {
    const bloquant = Register.CHAMPS.find(champ => this.registerForm.get(champ.nom)?.invalid)
      // Erreur portée par le groupe et non par un champ : les deux mots de
      // passe diffèrent. C'est la confirmation qu'il faut corriger.
      ?? (this.registerForm.hasError('motsDePasseDifferents')
        ? Register.CHAMPS.find(champ => champ.nom === 'confirmPassword')
        : undefined);

    if (!bloquant) {
      return;
    }

    const libelle = bloquant.nom === 'nomOrganisation' ? this.libelleNomOrganisation : bloquant.libelle;
    this.registerForm.get(bloquant.nom)!.markAsTouched();
    this.errorMessage = `Le formulaire n'est pas complet : commencez par « ${libelle} ».`;

    // Le message et le champ marqué sont l'essentiel ; amener le champ à
    // l'écran et y poser le curseur n'est que du confort. Appels optionnels
    // donc : ces deux API ne sont pas garanties partout (jsdom, par exemple,
    // ne fournit pas scrollIntoView), et une exception ici priverait
    // l'utilisateur du seul retour utile au clic.
    const element = document.getElementById(bloquant.id);
    element?.scrollIntoView?.({ behavior: 'smooth', block: 'center' });
    element?.focus?.({ preventScroll: true });
  }

  onSubmit(): void {
    if (this.registerForm.invalid) {
      this.signalerPremierChampBloquant();
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    const { prenom, nom, nomOrganisation, email, password } = this.registerForm.value;

    // Génération automatique d'un username : à partir du prénom/nom pour un
    // candidat, du nom de l'organisation pour une école ou une entreprise.
    const username = this.estOrganisation
      ? nomOrganisation.trim().toLowerCase().replace(/\s+/g, '_')
      : `${prenom.trim()}_${nom.trim()}`.toLowerCase();

    this.authService.register(username, email, password, this.typeInscription).subscribe({
      next: () => {
        this.isLoading = false;
        // Une URL interne seulement : « // » désignerait un autre site.
        if (this.returnUrl && this.returnUrl.startsWith('/') && !this.returnUrl.startsWith('//')) {
          this.router.navigateByUrl(this.returnUrl);
        } else if (this.typeInscription === 'ECOLE') {
          this.router.navigate(['/accreditation']);
        } else if (this.typeInscription === 'ENTREPRISE') {
          this.router.navigate(['/espace-entreprise']);
        } else if (this.typeInscription === 'FORMATEUR') {
          // Pas d'offre déjà choisie (sinon returnUrl l'aurait emportée ci-dessus) :
          // direction la vitrine pour en sélectionner une.
          this.router.navigate(['/offres-formateurs']);
        } else {
          this.router.navigate(['/recrutement/sessions']);
        }
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || 'Une erreur est survenue lors de l\'inscription.';
      }
    });
  }
}
