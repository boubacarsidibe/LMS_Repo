package inpg.sn.lms.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Autowired;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class OtpService {

    private static final int NOMBRE_MAX_ESSAIS = 5;
    private static final int DELAI_RENVOI_SECONDES = 60;
    private static final SecureRandom RANDOM = new SecureRandom();
    
    private final JavaMailSender mailSender;
    private final Clock clock;
    private static final int CAPACITE_CACHE = 10000;

    @Value("${app.mail.from}")
    private String expediteur;

    private static class OtpData {
        String code;
        LocalDateTime expiryTime;
        LocalDateTime prochainEnvoiAutorise;
        int essais;
        boolean consomme;

        OtpData(String code, LocalDateTime expiryTime, LocalDateTime prochainEnvoiAutorise) {
            this.code = code;
            this.expiryTime = expiryTime;
            this.prochainEnvoiAutorise = prochainEnvoiAutorise;
        }
    }

    private final Map<String, OtpData> otpCache = new ConcurrentHashMap<>();

    @Autowired
    public OtpService(JavaMailSender mailSender) {
        this(mailSender, Clock.systemUTC());
    }

    OtpService(JavaMailSender mailSender, Clock clock) {
        this.mailSender = mailSender;
        this.clock = clock;
    }

    // 1. Génération d'un code à 6 chiffres
    public synchronized String generateOtp(String email) {
        LocalDateTime maintenant = LocalDateTime.now(clock);
        otpCache.entrySet().removeIf(entry -> !maintenant.isBefore(entry.getValue().expiryTime));
        if (!otpCache.containsKey(email) && otpCache.size() >= CAPACITE_CACHE) {
            throw new IllegalStateException("Veuillez réessayer dans quelques minutes.");
        }
        AtomicReference<String> codeGenere = new AtomicReference<>();
        otpCache.compute(email, (cle, precedent) -> {
            if (precedent != null && (maintenant.isBefore(precedent.prochainEnvoiAutorise)
                    || (!precedent.consomme && precedent.essais >= NOMBRE_MAX_ESSAIS))) {
                throw new IllegalStateException("Un code a déjà été envoyé. Veuillez patienter avant de réessayer.");
            }
            String code = String.valueOf(100000 + RANDOM.nextInt(900000));
            codeGenere.set(code);
            return new OtpData(code, maintenant.plusMinutes(5), maintenant.plusSeconds(DELAI_RENVOI_SECONDES));
        });
        return codeGenere.get();
    }

    /** Invalide un code sans supprimer le délai de renvoi, notamment après une panne SMTP. */
    public void invalidateOtp(String email) {
        otpCache.computeIfPresent(email, (cle, data) -> {
            data.consomme = true;
            return data;
        });
    }

    // 2. Envoi de l'e-mail
    public void sendOtpEmail(String toEmail, String otpCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        // L'expéditeur doit être un domaine vérifié chez le relais SMTP, sinon Brevo rejette l'envoi.
        message.setFrom(expediteur);
        message.setTo(toEmail);
        message.setSubject("Votre code de vérification OTP");
        message.setText("Bonjour,\n\nVotre code de vérification est : " + otpCode +
                "\n\nCe code expire dans 5 minutes.");

        mailSender.send(message);
    }

    // 3. Validation de l'OTP saisi par l'utilisateur
    public boolean validateOtp(String email, String inputCode) {
        if (email == null || inputCode == null) return false;
        AtomicBoolean valide = new AtomicBoolean(false);
        otpCache.computeIfPresent(email, (cle, data) -> {
            if (!LocalDateTime.now(clock).isBefore(data.expiryTime)) {
                return null;
            }

            if (data.consomme || data.essais >= NOMBRE_MAX_ESSAIS) return data;

            byte[] attendu = data.code.getBytes(StandardCharsets.US_ASCII);
            byte[] recu = inputCode == null ? new byte[0] : inputCode.getBytes(StandardCharsets.US_ASCII);
            if (MessageDigest.isEqual(attendu, recu)) {
                valide.set(true);
                data.consomme = true;
                return data;
            }

            data.essais++;
            return data;
        });
        return valide.get();
    }
}
