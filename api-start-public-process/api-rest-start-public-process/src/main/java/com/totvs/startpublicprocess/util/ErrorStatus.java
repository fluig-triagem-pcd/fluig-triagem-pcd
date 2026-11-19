package com.totvs.startpublicprocess.util;

public class ErrorStatus {

    private final String message;

    public ErrorStatus(Exception e) {
        this.message = e == null ? "Erro" : String.valueOf(e.getMessage());
    }

    public String getMessage() {
        return message;
    }
}
