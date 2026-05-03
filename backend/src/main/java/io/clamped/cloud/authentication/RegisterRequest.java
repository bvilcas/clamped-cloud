package io.clamped.cloud.authentication;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
// Converts JSON to a Data-Transfer-Object for analyzation
public class RegisterRequest {
    private String firstname;
    private String lastname;
    private String email;
    private String password;
}
