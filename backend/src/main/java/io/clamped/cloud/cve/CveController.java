package io.clamped.cloud.cve;

import io.clamped.cloud.backendconfig.ApiResponse;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cve")
public class CveController {

    private final CveService cveService;

    public CveController(CveService cveService) {
        this.cveService = cveService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse> lookup(@RequestParam String id) {
        try {
            CveDto dto = cveService.lookup(id.trim());
            return ResponseEntity.ok(new ApiResponse(true, "CVE found", dto));
        } catch (EntityNotFoundException ex) {
            return ResponseEntity.status(404)
                    .body(new ApiResponse(false, ex.getMessage(), null));
        } catch (Exception ex) {
            return ResponseEntity.status(502)
                    .body(new ApiResponse(false, "Could not reach NVD API: " + ex.getMessage(), null));
        }
    }
}
