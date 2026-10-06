package dev.rangel.redirectservice.infrastructure.web.controller;

import dev.rangel.redirectservice.application.dto.RedirectResult;
import dev.rangel.redirectservice.application.service.RedirectResolutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RedirectController {

    private final RedirectResolutionService resolutionService;

    @GetMapping("/{code}")
    public ResponseEntity<Void> redirect(@PathVariable("code") String code) {
        RedirectResult result = resolutionService.resolve(code);

        return switch (result) {
            case RedirectResult.Success success ->
                    ResponseEntity.status(HttpStatus.FOUND)
                            .cacheControl(CacheControl.noStore())
                            .location(success.targetUri())
                            .build();

            case RedirectResult.Failure failure ->
                    ResponseEntity.notFound().build();
        };
    }
}