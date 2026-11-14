package com.delivery.reviewlens_api.service;

import org.springframework.stereotype.Service;

import com.delivery.reviewlens_api.dto.Classificacao;
import com.delivery.reviewlens_api.dto.ResultadoClassificacao;
import com.delivery.reviewlens_api.exception.GeminiIndisponivelException;
import com.delivery.reviewlens_api.exception.RespostaGeminiInvalidaException;
import com.delivery.reviewlens_api.model.Sentimento;
import com.delivery.reviewlens_api.model.StatusProcessamento;
import com.delivery.reviewlens_api.model.Topico;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClassificadorService {

    private final GeminiService geminiService;

    public ResultadoClassificacao classificar(String textoSanitizado) {
        try {
            Classificacao classificacao = geminiService.classificar(textoSanitizado);
            return new ResultadoClassificacao(
                    classificacao.sentimento(), classificacao.topico(), StatusProcessamento.PROCESSADO);

        } catch (GeminiIndisponivelException e) {
            log.warn("Gemini indisponível; a avaliação ficará PENDENTE para reprocessamento. Motivo: {}",
                    descrever(e));
            return new ResultadoClassificacao(null, null, StatusProcessamento.PENDENTE);

        } catch (RespostaGeminiInvalidaException e) {
            log.error("Resposta inválida do Gemini; aplicando fallback NEUTRO/OUTROS. Motivo: {}",
                    descrever(e));
            return new ResultadoClassificacao(Sentimento.NEUTRO, Topico.OUTROS, StatusProcessamento.ERRO);
        }
    }

    private String descrever(RuntimeException e) {
        return e.getCause() != null
                ? e.getMessage() + " | causa: " + e.getCause().getMessage()
                : e.getMessage();
    }
}