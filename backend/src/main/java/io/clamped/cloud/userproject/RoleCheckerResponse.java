package io.clamped.cloud.userproject;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoleCheckerResponse {
    private Boolean warn;
    private String message;
    private String code; // Optional - can be null for errors

    // Constructor without data
    public RoleCheckerResponse(Boolean warn, String message) {
        this.warn = warn;
        this.message = message;
        this.code = null;
    }
}
