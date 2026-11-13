package com.delivery.reviewlens_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ReviewRequest(

        @NotNull(message = "O campo idPedido é obrigatório.")
        @Positive(message = "O idPedido deve ser um número positivo.")
        Long idPedido,

        @NotBlank(message = "O texto da avaliação não pode ser vazio, nulo ou conter apenas espaços.")
        @Size(min = 10, max = 1000, message = "O texto da avaliação deve ter entre 10 e 1000 caracteres.")
        String texto
) {
}
