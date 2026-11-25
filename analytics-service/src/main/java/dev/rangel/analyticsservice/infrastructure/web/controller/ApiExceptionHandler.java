package dev.rangel.analyticsservice.infrastructure.web.controller;

import dev.rangel.analyticsservice.application.dto.InvalidStatsQueryException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(InvalidStatsQueryException.class)
    public ProblemDetail handleInvalidStatsQuery(InvalidStatsQueryException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid Statistics Query");
        problem.setType(URI.create("https://linkpulse.dev/errors/invalid-stats-query"));
        return problem;
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "Full authentication is required to access this resource."
        );
        problem.setTitle("Unauthorized");
        problem.setType(URI.create("https://linkpulse.dev/errors/unauthorized"));
        return problem;
    }

    /** Malformed 'from' / 'to' (not an ISO-8601 instant) fails before reaching the service. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Invalid value for parameter '" + ex.getName() + "'. Expected an ISO-8601 instant, e.g. 2026-11-10T10:00:00Z"
        );
        problem.setTitle("Invalid Statistics Query");
        problem.setType(URI.create("https://linkpulse.dev/errors/invalid-stats-query"));
        return problem;
    }
}
