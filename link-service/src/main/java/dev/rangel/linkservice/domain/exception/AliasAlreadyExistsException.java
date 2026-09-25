package dev.rangel.linkservice.domain.exception;

public class AliasAlreadyExistsException extends RuntimeException {
    public AliasAlreadyExistsException(String alias) {
        super("The alias '" + alias + "' is already in use.");
    }
}