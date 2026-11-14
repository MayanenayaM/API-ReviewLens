package com.delivery.reviewlens_api.dto;

import java.util.Map;

public record EstatisticasResponse(
        long totalProcessadas,
        Map<String, Long> sentimentos,
        Map<String, Long> topicos,
        EntregaMes entregaMes
) {

    public record EntregaMes(
            String mes,
            long total,
            long negativas,
            double percentualNegativo,
            boolean alertaCritico
    ) {
    }
}