package io.clamped.cloud.calendar;

import io.clamped.cloud.backendconfig.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/calendar")
public class CalendarController {

    private final CalendarService calendarService;

    public CalendarController(CalendarService calendarService) {
        this.calendarService = calendarService;
    }

    @GetMapping("/vulns")
    public ResponseEntity<ApiResponse> getCalendarVulns(Authentication authentication) {
        List<CalendarVulnDto> vulns = calendarService.getCalendarVulns(authentication);
        return ResponseEntity.ok(new ApiResponse(true, "Calendar vulnerabilities fetched", vulns));
    }
}
