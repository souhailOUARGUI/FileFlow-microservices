package org.example.userservice.service;

import org.example.userservice.exception.CodeInvalidException;
import org.example.userservice.exception.EmailAlreadyExistException;
import org.example.userservice.repository.UserRepository;
import org.example.userservice.utils.CodeData;
import org.example.userservice.utils.EmailTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EmailVerificationService {

    private final Map<String, CodeData> verificationCodes = new ConcurrentHashMap<>();
    private final Set<String> verifiedEmails = ConcurrentHashMap.newKeySet();
    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    public void sendVerificationCode(String email) {
        // Check if email already exists
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistException("Cet email est déjà utilisé");
        }

        // Generate 6-digit code
        String code = generateVerificationCode();
        
        // Store code with 10 minutes expiration
        verificationCodes.put(email, new CodeData(code, LocalDateTime.now().plusMinutes(10)));

        // Send email
        String htmlContent = EmailTemplate.getVerificationCodeTemplate(code);
        emailService.sendEmail(email, "FileFlow - Code de vérification", htmlContent);
    }

    public boolean verifyCode(String email, String code) {
        CodeData codeData = verificationCodes.get(email);
        
        if (codeData == null) {
            throw new CodeInvalidException("Aucun code de vérification trouvé pour cet email");
        }

        if (codeData.isExpired()) {
            verificationCodes.remove(email);
            throw new CodeInvalidException("Le code de vérification a expiré");
        }

        if (!codeData.getCode().equals(code)) {
            throw new CodeInvalidException("Code de vérification incorrect");
        }

        // Mark email as verified
        verifiedEmails.add(email);
        verificationCodes.remove(email);
        
        return true;
    }

    public boolean isEmailVerified(String email) {
        return verifiedEmails.contains(email);
    }

    public void removeVerifiedEmail(String email) {
        verifiedEmails.remove(email);
    }

    private String generateVerificationCode() {
        int code = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(code);
    }
}
