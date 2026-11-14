package com.delivery.reviewlens_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import com.delivery.reviewlens_api.dto.ResultadoClassificacao;
import com.delivery.reviewlens_api.model.Review;
import com.delivery.reviewlens_api.model.Sentimento;
import com.delivery.reviewlens_api.model.StatusProcessamento;
import com.delivery.reviewlens_api.model.Topico;
import com.delivery.reviewlens_api.repository.ReviewRepository;

@ExtendWith(MockitoExtension.class)
class ReprocessamentoServiceTest {

    @Mock
    private ReviewRepository repository;

    @Mock
    private ClassificadorService classificador;

    private ReprocessamentoService service;

    @BeforeEach
    void setUp() {
        service = new ReprocessamentoService(repository, new TextoSanitizer(), classificador, 20, 3);
    }

    private Review pendente(long id, int tentativas) {
        return Review.builder()
                .id(id)
                .idPedido(id)
                .texto("Texto de teste da avaliacao numero " + id)
                .status(StatusProcessamento.PENDENTE)
                .tentativas(tentativas)
                .build();
    }

    private void filaCom(Review... reviews) {
        when(repository.findByStatusOrderByDataCriacaoAsc(eq(StatusProcessamento.PENDENTE), any(Pageable.class)))
                .thenReturn(List.of(reviews));
    }

    @Test
    void geminiVoltouEntaoClassificaEMarcaProcessado() {
        Review r = pendente(1, 1);
        filaCom(r);
        when(classificador.classificar(anyString())).thenReturn(
                new ResultadoClassificacao(Sentimento.NEGATIVO, Topico.ENTREGA, StatusProcessamento.PROCESSADO));

        int resolvidas = service.reprocessarPendentes();

        assertThat(resolvidas).isEqualTo(1);
        assertThat(r.getStatus()).isEqualTo(StatusProcessamento.PROCESSADO);
        assertThat(r.getSentimento()).isEqualTo(Sentimento.NEGATIVO);
        assertThat(r.getTopico()).isEqualTo(Topico.ENTREGA);
        verify(repository).save(r);
    }

    @Test
    void geminiAindaForaContaTentativaEInterrompeOCiclo() {
        Review primeira = pendente(1, 1);
        Review segunda = pendente(2, 1);
        filaCom(primeira, segunda);
        when(classificador.classificar(anyString())).thenReturn(
                new ResultadoClassificacao(null, null, StatusProcessamento.PENDENTE));

        int resolvidas = service.reprocessarPendentes();

        assertThat(resolvidas).isZero();
        assertThat(primeira.getTentativas()).isEqualTo(2);
        assertThat(primeira.getStatus()).isEqualTo(StatusProcessamento.PENDENTE);
        assertThat(segunda.getTentativas()).isEqualTo(1);
        verify(classificador, times(1)).classificar(anyString());
    }

    @Test
    void aoAtingirOLimiteDeTentativasViraErro() {
        Review r = pendente(1, 2);
        filaCom(r);
        when(classificador.classificar(anyString())).thenReturn(
                new ResultadoClassificacao(null, null, StatusProcessamento.PENDENTE));

        service.reprocessarPendentes();

        assertThat(r.getTentativas()).isEqualTo(3);
        assertThat(r.getStatus()).isEqualTo(StatusProcessamento.ERRO);
    }

    @Test
    void respostaInvalidaSalvaFallbackComoErro() {
        Review r = pendente(1, 1);
        filaCom(r);
        when(classificador.classificar(anyString())).thenReturn(
                new ResultadoClassificacao(Sentimento.NEUTRO, Topico.OUTROS, StatusProcessamento.ERRO));

        int resolvidas = service.reprocessarPendentes();

        assertThat(resolvidas).isZero();
        assertThat(r.getSentimento()).isEqualTo(Sentimento.NEUTRO);
        assertThat(r.getTopico()).isEqualTo(Topico.OUTROS);
        assertThat(r.getStatus()).isEqualTo(StatusProcessamento.ERRO);
    }
}
