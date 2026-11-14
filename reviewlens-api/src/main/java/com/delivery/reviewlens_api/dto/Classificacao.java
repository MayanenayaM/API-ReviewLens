package com.delivery.reviewlens_api.dto;

import com.delivery.reviewlens_api.model.Sentimento;
import com.delivery.reviewlens_api.model.Topico;

public record Classificacao(Sentimento sentimento, Topico topico) {
}