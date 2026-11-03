package com.boss.dataentry.validation;

/** Raised for a single malformed row during bulk upload; the caller skips it and keeps the message as a warning. */
public class FieldValidationException extends Exception {

    public FieldValidationException(String message) {
        super(message);
    }
}
