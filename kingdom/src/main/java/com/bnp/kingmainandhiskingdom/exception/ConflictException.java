package com.bnp.kingmainandhiskingdom.exception;

/** The request itself is fine, but the current state of the data does not allow it (answered with 409 Conflict). */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
