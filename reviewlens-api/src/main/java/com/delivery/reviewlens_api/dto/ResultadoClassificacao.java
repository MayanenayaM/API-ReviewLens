package com.delivery.reviewlens_api.dto;

import com.delivery.reviewlens_api.model.Sentimento;
import com.delivery.reviewlens_api.model.StatusProcessamento;
import com.delivery.reviewlens_api.model.Topico;

public record ResultadoClassificacao(
        Sentimento sentimento,
        Topico topico,
        StatusProcessamento status
) {
}
