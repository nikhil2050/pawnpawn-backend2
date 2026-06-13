package com.pnpn.shared.exception;

import org.springframework.http.HttpStatus;

public class PnPnException extends RuntimeException {
    private final HttpStatus status;

    public PnPnException(String message) {
        super(message);
        this.status = HttpStatus.INTERNAL_SERVER_ERROR;
    }

    public PnPnException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public PnPnException(String message, Throwable cause) {
        super(message, cause);
        this.status = HttpStatus.INTERNAL_SERVER_ERROR;
    }

    public PnPnException(String message, Throwable cause, HttpStatus status) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
