package com.delivery.reviewlens_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.delivery.reviewlens_api.dto.ResultadoClassificacao;
import com.delivery.reviewlens_api.dto.ReviewRequest;
import com.delivery.reviewlens_api.exception.DuplicidadeException;
import com.delivery.reviewlens_api.exception.TextoInvalidoException;
import com.delivery.reviewlens_api.model.Review;
import com.delivery.reviewlens_api.model.Sentimento;
import com.delivery.reviewlens_api.model.StatusProcessamento;
import com.delivery.reviewlens_api.model.Topico;
import com.delivery.reviewlens_api.repository.ReviewRepository;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository repository;

    @Mock
    private ClassificadorService classificador;

    private ReviewService service;

    @BeforeEach
    void setUp() {
        service = new ReviewService(repository, new TextoSanitizer(), classificador);
    }

    private ReviewRequest pedido(String texto) {
        return new ReviewRequest(10L, texto);
    }

    private Review reviewSalva() {
        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void textoQueFicaCurtoDepoisDaSanitizacaoNaoChegaAoGeminiNemAoBanco() {
        assertThatThrownBy(() -> service.registrar(pedido("<b></b><i></i>  ok")))
                .isInstanceOf(TextoInvalidoException.class);

        verify(classificador, never()).classificar(anyString());
        verify(repository, never()).save(any(Review.class));
    }

    @Test
    void pedidoDuplicadoERecusadoSemGastarChamadaAoGemini() {
        when(repository.existsByIdPedido(10L)).thenReturn(true);

        assertThatThrownBy(() -> service.registrar(pedido("A comida chegou fria e atrasada")))
                .isInstanceOf(DuplicidadeException.class);

        verify(classificador, never()).classificar(anyString());
        verify(repository, never()).save(any(Review.class));
    }

    @Test
    void sucessoSalvaProcessadoComTextoOriginalEEnviaTextoSanitizadoAoGemini() {
        when(classificador.classificar(anyString())).thenReturn(
                new ResultadoClassificacao(Sentimento.NEGATIVO, Topico.ENTREGA, StatusProcessamento.PROCESSADO));
        when(repository.save(any(Review.class))).thenAnswer(i -> i.getArgument(0));

        service.registrar(pedido("<b>A comida</b> chegou fria e atrasada"));

        verify(classificador).classificar("A comida chegou fria e atrasada");

        Review salva = reviewSalva();
        assertThat(salva.getTexto()).isEqualTo("<b>A comida</b> chegou fria e atrasada");
        assertThat(salva.getStatus()).isEqualTo(StatusProcessamento.PROCESSADO);
        assertThat(salva.getSentimento()).isEqualTo(Sentimento.NEGATIVO);
        assertThat(salva.getTopico()).isEqualTo(Topico.ENTREGA);
        assertThat(salva.getTentativas()).isZero();
    }

    @Test
    void geminiIndisponivelSalvaPendenteComUmaTentativa() {
        when(classificador.classificar(anyString())).thenReturn(
                new ResultadoClassificacao(null, null, StatusProcessamento.PENDENTE));
        when(repository.save(any(Review.class))).thenAnswer(i -> i.getArgument(0));

        service.registrar(pedido("A comida chegou fria e atrasada"));

        Review salva = reviewSalva();
        assertThat(salva.getStatus()).isEqualTo(StatusProcessamento.PENDENTE);
        assertThat(salva.getSentimento()).isNull();
        assertThat(salva.getTopico()).isNull();
        assertThat(salva.getTentativas()).isEqualTo(1);
    }

    @Test
    void respostaInvalidaSalvaFallbackNeutroOutrosComoErro() {
        when(classificador.classificar(anyString())).thenReturn(
                new ResultadoClassificacao(Sentimento.NEUTRO, Topico.OUTROS, StatusProcessamento.ERRO));
        when(repository.save(any(Review.class))).thenAnswer(i -> i.getArgument(0));

        service.registrar(pedido("A comida chegou fria e atrasada"));

        Review salva = reviewSalva();
        assertThat(salva.getSentimento()).isEqualTo(Sentimento.NEUTRO);
        assertThat(salva.getTopico()).isEqualTo(Topico.OUTROS);
        assertThat(salva.getStatus()).isEqualTo(StatusProcessamento.ERRO);
        assertThat(salva.getTentativas()).isZero();
    }

    @Test
    void corridaNoBancoViraDuplicidade() {
        when(classificador.classificar(anyString())).thenReturn(
                new ResultadoClassificacao(Sentimento.POSITIVO, Topico.SABOR, StatusProcessamento.PROCESSADO));
        when(repository.save(any(Review.class))).thenThrow(new DataIntegrityViolationException("uk_reviews_id_pedido"));

        assertThatThrownBy(() -> service.registrar(pedido("Pizza deliciosa, sabor incrivel")))
                .isInstanceOf(DuplicidadeException.class);
    }
}
