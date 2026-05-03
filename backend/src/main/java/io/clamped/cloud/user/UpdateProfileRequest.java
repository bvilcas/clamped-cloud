package io.clamped.cloud.user;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
// Data container that is not responsible for logic
///  EXTRA LOGIC IN FRONTEND (no white space), IF BOTH ARE BLANK THEN DONT DO ANYTHING
public class UpdateProfileRequest {
    private String firstName;
    private String lastName;
}
