package io.clamped.cloud.contact;

import io.clamped.cloud.notification.EmailService;
import io.clamped.cloud.user.User;
import io.clamped.cloud.user.UserPrincipal;
import io.clamped.cloud.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class ContactService {

    private final EmailService emailService;
    private final UserRepository userRepository;

    @Value("${app.admin-email}")
    private String adminEmail;

    public ContactService(EmailService emailService, UserRepository userRepository) {
        this.emailService = emailService;
        this.userRepository = userRepository;
    }

    public void sendContactMessage(ContactRequest request, Authentication auth) {
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String subject = "Clamped Contact — " + user.getFirstname() + " " + user.getLastname();
        String body = """
                From: %s %s <%s>

                %s
                """.formatted(
                user.getFirstname(),
                user.getLastname(),
                user.getEmail(),
                request.message()
        );

        emailService.send(adminEmail, subject, body);
    }
}
