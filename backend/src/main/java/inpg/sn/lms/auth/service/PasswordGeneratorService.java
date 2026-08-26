package inpg.sn.lms.auth.service;

import java.security.SecureRandom;

import org.springframework.stereotype.Service;

/**
 * Génère un mot de passe initial lorsque la création d'un compte n'en fournit
 * aucun — cas des comptes ouverts par la pédagogie, qui ne dialogue pas avec le
 * titulaire au moment de la création, contrairement à une inscription où la
 * personne choisit elle-même son mot de passe.
 */
@Service
public class PasswordGeneratorService {

    private static final String MINUSCULES = "abcdefghijkmnpqrstuvwxyz";
    private static final String MAJUSCULES = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String CHIFFRES = "23456789";
    private static final String SPECIAUX = "!@#$%&*";
    private static final String TOUS = MINUSCULES + MAJUSCULES + CHIFFRES + SPECIAUX;
    private static final int LONGUEUR = 12;

    private final SecureRandom random = new SecureRandom();

    /** Un mot de passe de 12 caractères mêlant les quatre classes ci-dessus. */
    public String generer() {
        char[] motDePasse = new char[LONGUEUR];
        motDePasse[0] = MINUSCULES.charAt(random.nextInt(MINUSCULES.length()));
        motDePasse[1] = MAJUSCULES.charAt(random.nextInt(MAJUSCULES.length()));
        motDePasse[2] = CHIFFRES.charAt(random.nextInt(CHIFFRES.length()));
        motDePasse[3] = SPECIAUX.charAt(random.nextInt(SPECIAUX.length()));
        for (int i = 4; i < LONGUEUR; i++) {
            motDePasse[i] = TOUS.charAt(random.nextInt(TOUS.length()));
        }
        for (int i = motDePasse.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = motDePasse[i];
            motDePasse[i] = motDePasse[j];
            motDePasse[j] = temp;
        }
        return new String(motDePasse);
    }
}
