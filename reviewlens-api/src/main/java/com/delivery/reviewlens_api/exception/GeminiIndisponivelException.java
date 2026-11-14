package com.delivery.reviewlens_api.exception;

public class GeminiIndisponivelException extends RuntimeException {

    public GeminiIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
