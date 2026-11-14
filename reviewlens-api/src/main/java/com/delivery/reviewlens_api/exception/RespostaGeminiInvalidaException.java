package com.delivery.reviewlens_api.exception;

public class RespostaGeminiInvalidaException extends RuntimeException {

    public RespostaGeminiInvalidaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }

    public RespostaGeminiInvalidaException(String mensagem) {
        super(mensagem);
    }
}
