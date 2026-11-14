package com.delivery.reviewlens_api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ClassificacaoGemini(String sentimento, String topico) {
}
