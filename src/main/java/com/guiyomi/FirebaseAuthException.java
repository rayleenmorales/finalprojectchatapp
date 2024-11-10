package com.guiyomi;

public class FirebaseAuthException extends Exception {
    private final String errorCode;

    public FirebaseAuthException(String errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}