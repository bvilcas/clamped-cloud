package io.clamped.cloud.cve;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Service
public class CveService {

    private final RestClient restClient;

    public CveService() {
        this.restClient = RestClient.builder()
                .baseUrl("https://services.nvd.nist.gov/rest/json/cves/2.0")
                .build();
    }

    public CveDto lookup(String cveId) {
        NvdResponse response;
        try {
            response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.queryParam("cveId", cveId).build())
                    .retrieve()
                    .body(NvdResponse.class);
        } catch (RestClientException ex) {
            throw new RuntimeException("Failed to contact NVD API: " + ex.getMessage(), ex);
        }

        if (response == null || response.vulnerabilities() == null || response.vulnerabilities().isEmpty()) {
            throw new jakarta.persistence.EntityNotFoundException("CVE not found: " + cveId);
        }

        NvdCveItem item = response.vulnerabilities().get(0).cve();

        String description = item.descriptions().stream()
                .filter(d -> "en".equals(d.lang()))
                .map(NvdDescription::value)
                .findFirst()
                .orElse(null);

        Double cvssScore = null;
        String cvssVector = null;
        String severity = null;

        // Prefer CVSS v3.1, then v3.0, then v2
        if (item.metrics() != null) {
            List<NvdCvssMetric> v31 = item.metrics().cvssMetricV31();
            List<NvdCvssMetric> v30 = item.metrics().cvssMetricV30();
            List<NvdCvssMetric> v2  = item.metrics().cvssMetricV2();

            NvdCvssMetric chosen = null;
            if (v31 != null && !v31.isEmpty()) chosen = v31.get(0);
            else if (v30 != null && !v30.isEmpty()) chosen = v30.get(0);
            else if (v2  != null && !v2.isEmpty())  chosen = v2.get(0);

            if (chosen != null && chosen.cvssData() != null) {
                cvssScore  = chosen.cvssData().baseScore();
                cvssVector = chosen.cvssData().vectorString();
                severity   = chosen.cvssData().baseSeverity();
            }
        }

        String cweId = null;
        if (item.weaknesses() != null && !item.weaknesses().isEmpty()) {
            cweId = item.weaknesses().get(0).description().stream()
                    .filter(d -> "en".equals(d.lang()))
                    .map(NvdDescription::value)
                    .findFirst()
                    .orElse(null);
        }

        return new CveDto(
                item.id(),
                description,
                cvssScore,
                cvssVector,
                severity,
                cweId,
                item.published(),
                item.lastModified()
        );
    }

    // ── NVD JSON model (private, not exposed) ────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NvdResponse(
            int totalResults,
            List<NvdVulnerabilityWrapper> vulnerabilities
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NvdVulnerabilityWrapper(NvdCveItem cve) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NvdCveItem(
            String id,
            List<NvdDescription> descriptions,
            NvdMetrics metrics,
            List<NvdWeakness> weaknesses,
            String published,
            String lastModified
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NvdDescription(String lang, String value) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NvdMetrics(
            List<NvdCvssMetric> cvssMetricV31,
            List<NvdCvssMetric> cvssMetricV30,
            List<NvdCvssMetric> cvssMetricV2
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NvdCvssMetric(NvdCvssData cvssData) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NvdCvssData(
            Double baseScore,
            String baseSeverity,
            String vectorString
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NvdWeakness(List<NvdDescription> description) {}
}
