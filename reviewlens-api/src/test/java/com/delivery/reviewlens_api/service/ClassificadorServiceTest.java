package com.delivery.reviewlens_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.delivery.reviewlens_api.dto.Classificacao;
import com.delivery.reviewlens_api.dto.ResultadoClassificacao;
import com.delivery.reviewlens_api.exception.GeminiIndisponivelException;
import com.delivery.reviewlens_api.exception.RespostaGeminiInvalidaException;
import com.delivery.reviewlens_api.model.Sentimento;
import com.delivery.reviewlens_api.model.StatusProcessamento;
import com.delivery.reviewlens_api.model.Topico;

@ExtendWith(MockitoExtension.class)
class ClassificadorServiceTest {

    @Mock
    private GeminiService geminiService;

    @InjectMocks
    private ClassificadorService classificador;

    @Test
    void sucessoViraProcessado() {
        when(geminiService.classificar(anyString()))
                .thenReturn(new Classificacao(Sentimento.NEGATIVO, Topico.ENTREGA));

        ResultadoClassificacao r = classificador.classificar("A comida chegou fria");

        assertThat(r.status()).isEqualTo(StatusProcessamento.PROCESSADO);
        assertThat(r.sentimento()).isEqualTo(Sentimento.NEGATIVO);
        assertThat(r.topico()).isEqualTo(Topico.ENTREGA);
    }

    @Test
    void geminiIndisponivelViraPendenteSemClassificacao() {
        when(geminiService.classificar(anyString()))
                .thenThrow(new GeminiIndisponivelException("falha", new RuntimeException("timeout")));

        ResultadoClassificacao r = classificador.classificar("A comida chegou fria");

        assertThat(r.status()).isEqualTo(StatusProcessamento.PENDENTE);
        assertThat(r.sentimento()).isNull();
        assertThat(r.topico()).isNull();
    }

    @Test
    void respostaInvalidaAplicaFallbackNeutroOutros() {
        when(geminiService.classificar(anyString()))
                .thenThrow(new RespostaGeminiInvalidaException("valor fora do dominio"));

        ResultadoClassificacao r = classificador.classificar("A comida chegou fria");

        assertThat(r.sentimento()).isEqualTo(Sentimento.NEUTRO);
        assertThat(r.topico()).isEqualTo(Topico.OUTROS);
        assertThat(r.status()).isEqualTo(StatusProcessamento.ERRO);
    }
}
