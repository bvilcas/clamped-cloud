package io.clamped.cloud.authentication;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
// Data-Transfer-Object (DTO).
public class AuthenticationRequest {
    private String email;
    private String password; // accessible in any class within the same package
}
