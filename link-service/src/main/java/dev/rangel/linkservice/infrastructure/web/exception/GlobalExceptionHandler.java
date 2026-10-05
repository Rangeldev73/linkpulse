package dev.rangel.linkservice.infrastructure.web.exception;

import dev.rangel.linkservice.domain.exception.AliasAlreadyExistsException;
import dev.rangel.linkservice.domain.exception.CodeGenerationException;
import dev.rangel.linkservice.domain.exception.LinkNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.net.URI;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AliasAlreadyExistsException.class)
    public ProblemDetail handleAliasAlreadyExists(AliasAlreadyExistsException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );
        problem.setTitle("Alias Collision");
        problem.setType(URI.create("https://linkpulse.dev/errors/alias-already-exists"));
        return problem;
    }

    @ExceptionHandler(CodeGenerationException.class)
    public ProblemDetail handleCodeGenerationException(CodeGenerationException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to generate a unique link code. Please try again later."
        );
        problem.setTitle("Code Generation Failure");
        problem.setType(URI.create("https://linkpulse.dev/errors/code-generation-failed"));
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationErrors(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "One or more validation errors occurred."
        );
        problem.setTitle("Invalid Request Payload");
        problem.setType(URI.create("https://linkpulse.dev/errors/invalid-payload"));

        var errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        FieldError::getField,
                        error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value",
                        (existing, replacement) -> existing
                ));

        problem.setProperty("invalidFields", errors);
        return problem;
    }

    @ExceptionHandler(LinkNotFoundException.class)
    public ProblemDetail handleLinkNotFound(LinkNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problem.setTitle("Resource Not Found");
        problem.setType(URI.create("https://linkpulse.dev/errors/link-not-found"));
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUncaughtException(Exception ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ex.getMessage()
        );
        problem.setTitle("Internal Server Error");
        problem.setType(URI.create("https://linkpulse.dev/errors/internal-server-error"));
        return problem;
    }

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ProblemDetail handleAuthenticationException(org.springframework.security.core.AuthenticationException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                ex.getMessage() != null ? ex.getMessage() : "Full authentication is required to access this resource."
        );
        problem.setTitle("Unauthorized");
        problem.setType(URI.create("https://linkpulse.dev/errors/unauthorized"));
        return problem;
    }
}