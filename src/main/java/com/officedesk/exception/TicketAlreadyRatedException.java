package com.officedesk.exception;

public class TicketAlreadyRatedException extends RuntimeException {
    public TicketAlreadyRatedException(String message) {
        super(message);
    }
}
