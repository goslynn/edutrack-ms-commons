package cl.duocuc.edutrack.ms.infrastructure.exception;

import jakarta.ws.rs.core.Response;

public class UnauthorizedException extends DomainException {

    public UnauthorizedException(String message) {
        this("", message);
    }

    public UnauthorizedException(String code, String message) {
        super(Response.Status.UNAUTHORIZED, code, message);
    }
}
