package com.delivery.reviewlens_api.dto;

import java.time.LocalDateTime;

import com.delivery.reviewlens_api.model.Review;
import com.delivery.reviewlens_api.model.Sentimento;
import com.delivery.reviewlens_api.model.StatusProcessamento;
import com.delivery.reviewlens_api.model.Topico;

public record ReviewResponse(
        Long id,
        Long idPedido,
        String texto,
        Sentimento sentimento,
        Topico topico,
        StatusProcessamento status,
        LocalDateTime dataCriacao
) {
    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getIdPedido(),
                review.getTexto(),
                review.getSentimento(),
                review.getTopico(),
                review.getStatus(),
                review.getDataCriacao());
    }
}
