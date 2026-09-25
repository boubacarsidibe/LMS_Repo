package inpg.sn.lms.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordConfig {

    /** BCrypt ne lit que 72 octets et, dans cette version de Spring Security, refuse au-delà. */
    private static final int LIMITE_BCRYPT_OCTETS = 72;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BcryptSansPlafond();
    }

    /**
     * Aucune longueur maximale n'est imposée à l'utilisateur : un mot de passe de plus de
     * 72 octets est d'abord condensé (SHA-256, Base64 : 44 caractères) avant BCrypt.
     * Jusqu'à 72 octets rien ne change, donc les hachages déjà en base restent valides.
     */
    static final class BcryptSansPlafond implements PasswordEncoder {

        private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

        @Override
        public String encode(CharSequence motDePasse) {
            return bcrypt.encode(preparer(motDePasse));
        }

        @Override
        public boolean matches(CharSequence motDePasse, String hache) {
            return bcrypt.matches(preparer(motDePasse), hache);
        }

        @Override
        public boolean upgradeEncoding(String hache) {
            return bcrypt.upgradeEncoding(hache);
        }

        private static String preparer(CharSequence motDePasse) {
            String valeur = motDePasse.toString();
            if (valeur.getBytes(StandardCharsets.UTF_8).length <= LIMITE_BCRYPT_OCTETS) {
                return valeur;
            }
            try {
                byte[] condense = MessageDigest.getInstance("SHA-256").digest(valeur.getBytes(StandardCharsets.UTF_8));
                return Base64.getEncoder().encodeToString(condense);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("SHA-256 indisponible", e);
            }
        }
    }
}
