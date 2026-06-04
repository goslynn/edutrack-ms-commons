package cl.duocuc.edutrack.ms.infrastructure.exception;

import jakarta.ws.rs.core.Response;

public class BadRequestException extends DomainException {

    public BadRequestException(String message) {
        this("", message);
    }

    public BadRequestException(String code, String message) {
        super(Response.Status.BAD_REQUEST, code, message);
    }

}
