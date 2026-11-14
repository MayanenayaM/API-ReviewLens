package com.delivery.reviewlens_api.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.delivery.reviewlens_api.dto.ResultadoClassificacao;
import com.delivery.reviewlens_api.model.Review;
import com.delivery.reviewlens_api.model.StatusProcessamento;
import com.delivery.reviewlens_api.repository.ReviewRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ReprocessamentoService {

    private final ReviewRepository repository;
    private final TextoSanitizer sanitizer;
    private final ClassificadorService classificador;
    private final int tamanhoLote;
    private final int maxTentativas;

    public ReprocessamentoService(
            ReviewRepository repository,
            TextoSanitizer sanitizer,
            ClassificadorService classificador,
            @Value("${reviewlens.reprocessamento.tamanho-lote:20}") int tamanhoLote,
            @Value("${reviewlens.reprocessamento.max-tentativas:5}") int maxTentativas) {
        this.repository = repository;
        this.sanitizer = sanitizer;
        this.classificador = classificador;
        this.tamanhoLote = tamanhoLote;
        this.maxTentativas = maxTentativas;
    }

    // fixedDelay: espera o ciclo anterior terminar antes de contar o intervalo (nunca roda dois ao mesmo tempo)
    @Scheduled(
            fixedDelayString = "${reviewlens.reprocessamento.intervalo-ms:60000}",
            initialDelayString = "${reviewlens.reprocessamento.atraso-inicial-ms:30000}")
    public void executar() {
        try {
            reprocessarPendentes();
        } catch (RuntimeException e) {
            // Uma falha inesperada não pode matar o agendamento
            log.error("Erro inesperado no ciclo de reprocessamento: {}", e.getMessage(), e);
        }
    }

    /** Devolve quantas avaliações foram classificadas com sucesso neste ciclo. */
    public int reprocessarPendentes() {
        List<Review> pendentes = repository.findByStatusOrderByDataCriacaoAsc(
                StatusProcessamento.PENDENTE, PageRequest.of(0, tamanhoLote));

        if (pendentes.isEmpty()) {
            return 0;
        }

        log.info("Reprocessando {} avaliação(ões) PENDENTE(s).", pendentes.size());
        int resolvidas = 0;

        for (Review review : pendentes) {
            ResultadoClassificacao resultado =
                    classificador.classificar(sanitizer.sanitizar(review.getTexto()));

            if (resultado.status() == StatusProcessamento.PENDENTE) {
                registrarFalha(review);
                // Se o Gemini ainda está fora, não adianta tentar o resto do lote agora
                log.warn("Gemini ainda indisponível; encerrando este ciclo de reprocessamento.");
                break;
            }

            review.setSentimento(resultado.sentimento());
            review.setTopico(resultado.topico());
            review.setStatus(resultado.status());
            repository.save(review);

            if (resultado.status() == StatusProcessamento.PROCESSADO) {
                resolvidas++;
            }
        }

        log.info("Reprocessamento concluído: {} avaliação(ões) classificada(s) com sucesso.", resolvidas);
        return resolvidas;
    }

    private void registrarFalha(Review review) {
        review.setTentativas(review.getTentativas() + 1);
        if (review.getTentativas() >= maxTentativas) {
            review.setStatus(StatusProcessamento.ERRO);
            log.error("Avaliação {} marcada como ERRO após {} tentativas de reprocessamento.",
                    review.getId(), review.getTentativas());
        }
        repository.save(review);
    }
}
