package com.delivery.reviewlens_api.model;

public enum StatusProcessamento {
    PROCESSADO,  // classificada com sucesso pelo Gemini
    PENDENTE,    // Gemini indisponível/timeout: aguarda reprocessamento 
    ERRO         // falha de classificação que não deve entrar nas estatísticas 
}
