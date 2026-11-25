package dev.rangel.analyticsservice.infrastructure.web.controller;

import dev.rangel.analyticsservice.application.dto.ClickStatsResponse;
import dev.rangel.analyticsservice.application.service.ClickStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class ClickStatsController {

    private final ClickStatsService clickStatsService;

    @GetMapping("/{code}/clicks")
    public ClickStatsResponse getClicks(
            @PathVariable("code") String code,
            @RequestParam(name = "granularity", defaultValue = "hour") String granularity,
            @RequestParam(name = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(name = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        return clickStatsService.getClicks(code, granularity, from, to);
    }
}
