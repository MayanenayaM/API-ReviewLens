package com.delivery.reviewlens_api.service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.delivery.reviewlens_api.dto.EstatisticasResponse;
import com.delivery.reviewlens_api.dto.EstatisticasResponse.EntregaMes;
import com.delivery.reviewlens_api.model.Sentimento;
import com.delivery.reviewlens_api.model.StatusProcessamento;
import com.delivery.reviewlens_api.model.Topico;
import com.delivery.reviewlens_api.repository.ReviewRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EstatisticasService {

    private static final StatusProcessamento STATUS_VALIDO = StatusProcessamento.PROCESSADO;

    private static final Topico TOPICO_CRITICO = Topico.ENTREGA;

    private final ReviewRepository repository;

    @Transactional(readOnly = true)
    public EstatisticasResponse gerar() {
        return gerar(YearMonth.now());
    }

    // Visível no pacote para poder ser testado com um mês fixo
    EstatisticasResponse gerar(YearMonth mes) {
        // Começa com todos os valores em zero, para o gráfico sempre mostrar todas as categorias
        Map<String, Long> sentimentos = zerados(Sentimento.values());
        repository.contarPorSentimento(STATUS_VALIDO).forEach(c -> {
            if (c.getSentimento() != null) {
                sentimentos.put(c.getSentimento().name(), c.getTotal());
            }
        });

        Map<String, Long> topicos = zerados(Topico.values());
        repository.contarPorTopico(STATUS_VALIDO).forEach(c -> {
            if (c.getTopico() != null) {
                topicos.put(c.getTopico().name(), c.getTotal());
            }
        });

        long totalProcessadas = sentimentos.values().stream().mapToLong(Long::longValue).sum();

        return new EstatisticasResponse(totalProcessadas, sentimentos, topicos, calcularEntregaDoMes(mes));
    }

    private EntregaMes calcularEntregaDoMes(YearMonth mes) {
        LocalDateTime inicio = mes.atDay(1).atStartOfDay();
        LocalDateTime fim = mes.plusMonths(1).atDay(1).atStartOfDay();

        long total = repository.contarPorTopicoNoPeriodo(STATUS_VALIDO, TOPICO_CRITICO, inicio, fim);

        long negativas = total == 0
                ? 0
                : repository.contarPorTopicoESentimentoNoPeriodo(
                        STATUS_VALIDO, TOPICO_CRITICO, Sentimento.NEGATIVO, inicio, fim);

        double percentual = total == 0 ? 0.0 : Math.round(negativas * 1000.0 / total) / 10.0;

        // "Mais de 50%": comparação exata com inteiros, sem risco de arredondamento
        boolean alertaCritico = total > 0 && negativas * 2 > total;

        return new EntregaMes(mes.toString(), total, negativas, percentual, alertaCritico);
    }

    private Map<String, Long> zerados(Enum<?>[] valores) {
        Map<String, Long> mapa = new LinkedHashMap<>();
        for (Enum<?> valor : valores) {
            mapa.put(valor.name(), 0L);
        }
        return mapa;
    }
}
