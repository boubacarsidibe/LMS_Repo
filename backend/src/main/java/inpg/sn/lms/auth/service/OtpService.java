package inpg.sn.lms.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
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

    @Value("${app.mail.from}")
    private String expediteur;

    private static class OtpData {
        String code;
        LocalDateTime expiryTime;
        LocalDateTime prochainEnvoiAutorise;
        int essais;

        OtpData(String code, LocalDateTime expiryTime, LocalDateTime prochainEnvoiAutorise) {
            this.code = code;
            this.expiryTime = expiryTime;
            this.prochainEnvoiAutorise = prochainEnvoiAutorise;
        }
    }

    private final Map<String, OtpData> otpCache = new ConcurrentHashMap<>();

    public OtpService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // 1. Génération d'un code à 6 chiffres
    public String generateOtp(String email) {
        LocalDateTime maintenant = LocalDateTime.now();
        AtomicReference<String> codeGenere = new AtomicReference<>();
        otpCache.compute(email, (cle, precedent) -> {
            if (precedent != null && maintenant.isBefore(precedent.prochainEnvoiAutorise)) {
                throw new IllegalStateException("Un code a déjà été envoyé. Veuillez patienter avant de réessayer.");
            }
            String code = String.valueOf(100000 + RANDOM.nextInt(900000));
            codeGenere.set(code);
            return new OtpData(code, maintenant.plusMinutes(5), maintenant.plusSeconds(DELAI_RENVOI_SECONDES));
        });
        return codeGenere.get();
    }

    /** Retire un code du cache, par exemple lorsque son envoi par e-mail a échoué. */
    public void invalidateOtp(String email) {
        otpCache.remove(email);
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
        AtomicBoolean valide = new AtomicBoolean(false);
        otpCache.computeIfPresent(email, (cle, data) -> {
            if (LocalDateTime.now().isAfter(data.expiryTime) || data.essais >= NOMBRE_MAX_ESSAIS) {
                return null;
            }

            byte[] attendu = data.code.getBytes(StandardCharsets.US_ASCII);
            byte[] recu = inputCode == null ? new byte[0] : inputCode.getBytes(StandardCharsets.US_ASCII);
            if (MessageDigest.isEqual(attendu, recu)) {
                valide.set(true);
                return null;
            }

            data.essais++;
            return data.essais >= NOMBRE_MAX_ESSAIS ? null : data;
        });
        return valide.get();
    }
}
