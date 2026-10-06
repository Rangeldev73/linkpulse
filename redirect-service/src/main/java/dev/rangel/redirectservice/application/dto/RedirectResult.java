package dev.rangel.redirectservice.application.dto;

import java.net.URI;

public sealed interface RedirectResult {
    record Success(URI targetUri) implements RedirectResult {}
    record Failure(RedirectFailureReason reason) implements RedirectResult {}
}