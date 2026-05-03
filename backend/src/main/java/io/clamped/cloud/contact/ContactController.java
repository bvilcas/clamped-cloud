package io.clamped.cloud.contact;

import io.clamped.cloud.backendconfig.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/contact")
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse> sendMessage(
            @Valid @RequestBody ContactRequest request,
            Authentication auth
    ) {
        contactService.sendContactMessage(request, auth);
        return ResponseEntity.ok(new ApiResponse(true, "Your message has been sent. We'll be in touch!"));
    }
}
