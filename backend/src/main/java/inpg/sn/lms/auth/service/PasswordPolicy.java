package inpg.sn.lms.auth.service;

import java.nio.charset.StandardCharsets;

/** Règle unique appliquée à tous les mots de passe choisis par un utilisateur. */
public final class PasswordPolicy {

    public static final int LONGUEUR_MINIMALE = 8;

    private PasswordPolicy() {
    }

    public static void exigerMotDePasseRobuste(String motDePasse) {
        if (motDePasse == null || motDePasse.codePointCount(0, motDePasse.length()) < LONGUEUR_MINIMALE) {
            throw new IllegalArgumentException(
                    "Le mot de passe doit contenir au moins " + LONGUEUR_MINIMALE + " caractères.");
        }
        if (motDePasse.isBlank() || motDePasse.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Le mot de passe ne doit pas être vide ni dépasser 72 octets UTF-8.");
        }
    }
}
