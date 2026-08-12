package inpg.sn.lms.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {
    
    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String expediteur;

    private static class OtpData {
        String code;
        LocalDateTime expiryTime;

        OtpData(String code, LocalDateTime expiryTime) {
            this.code = code;
            this.expiryTime = expiryTime;
        }
    }

    private final Map<String, OtpData> otpCache = new ConcurrentHashMap<>();

    public OtpService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // 1. Génération d'un code à 6 chiffres
    public String generateOtp(String email) {
        SecureRandom random = new SecureRandom();
        int code = 100000 + random.nextInt(900000);
        String otpCode = String.valueOf(code);

        // Expiration dans 5 minutes
        otpCache.put(email, new OtpData(otpCode, LocalDateTime.now().plusMinutes(5)));
        return otpCode;
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
        OtpData data = otpCache.get(email);

        if (data == null) {
            return false; // Pas d'OTP généré pour cet e-mail
        }

        if (LocalDateTime.now().isAfter(data.expiryTime)) {
            otpCache.remove(email); // Expiré
            return false;
        }

        if (data.code.equals(inputCode)) {
            otpCache.remove(email); // Code valide, on le consomme
            return true;
        }

        return false;
    }
}