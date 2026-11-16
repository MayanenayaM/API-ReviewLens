package com.delivery.reviewlens_api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.CannotCreateTransactionException;

import com.delivery.reviewlens_api.dto.ReviewRequest;
import com.delivery.reviewlens_api.exception.DuplicidadeException;
import com.delivery.reviewlens_api.exception.GlobalExceptionHandler;
import com.delivery.reviewlens_api.model.Review;
import com.delivery.reviewlens_api.model.Sentimento;
import com.delivery.reviewlens_api.model.StatusProcessamento;
import com.delivery.reviewlens_api.model.Topico;
import com.delivery.reviewlens_api.service.ReviewService;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

    @Mock
    private ReviewService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ReviewController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private String json(Long idPedido, String texto) {
        return "{\"idPedido\": " + idPedido + ", \"texto\": \"" + texto + "\"}";
    }

    private MvcResult postar(String corpo, org.springframework.test.web.servlet.ResultMatcher esperado) throws Exception {
        return mockMvc.perform(post("/api/reviews").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(esperado)
                .andReturn();
    }

    private String texto(MvcResult resultado) throws Exception {
        return resultado.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private Review review(StatusProcessamento status, Sentimento sentimento, Topico topico) {
        return Review.builder()
                .id(1L).idPedido(10L).texto("A comida chegou fria e atrasada")
                .sentimento(sentimento).topico(topico).status(status)
                .dataCriacao(LocalDateTime.now())
                .build();
    }

    @Test
    void avaliacaoProcessadaRetorna201() throws Exception {
        when(service.registrar(any(ReviewRequest.class)))
                .thenReturn(review(StatusProcessamento.PROCESSADO, Sentimento.NEGATIVO, Topico.ENTREGA));

        MvcResult r = postar(json(10L, "A comida chegou fria e atrasada"), status().isCreated());

        assertThat(texto(r)).contains("PROCESSADO").contains("NEGATIVO").contains("ENTREGA");
    }

    @Test
    void avaliacaoPendenteRetorna202() throws Exception {
        when(service.registrar(any(ReviewRequest.class)))
                .thenReturn(review(StatusProcessamento.PENDENTE, null, null));

        MvcResult r = postar(json(10L, "A comida chegou fria e atrasada"), status().isAccepted());

        assertThat(texto(r)).contains("PENDENTE");
    }

    @Test
    void textoVazioRetorna400SemChamarOService() throws Exception {
        postar(json(10L, ""), status().isBadRequest());
        verify(service, never()).registrar(any());
    }

    @Test
    void textoSoComEspacosRetorna400() throws Exception {
        postar(json(10L, "            "), status().isBadRequest());
        verify(service, never()).registrar(any());
    }

    @Test
    void textoComMenosDeDezCaracteresRetorna400() throws Exception {
        MvcResult r = postar(json(10L, "ruim"), status().isBadRequest());

        assertThat(texto(r)).contains("entre 10 e 1000");
        verify(service, never()).registrar(any());
    }

    @Test
    void textoComMaisDeMilCaracteresRetorna400() throws Exception {
        postar(json(10L, "a".repeat(1001)), status().isBadRequest());
        verify(service, never()).registrar(any());
    }

    @Test
    void idPedidoAusenteRetorna400() throws Exception {
        MvcResult r = postar("{\"texto\": \"Comida muito boa mesmo\"}", status().isBadRequest());

        assertThat(texto(r)).contains("idPedido");
        verify(service, never()).registrar(any());
    }

    @Test
    void corpoVazioOuJsonInvalidoRetorna400() throws Exception {
        postar("", status().isBadRequest());
        postar("{", status().isBadRequest());
        verify(service, never()).registrar(any());
    }

    @Test
    void duplicidadeRetorna409() throws Exception {
        when(service.registrar(any(ReviewRequest.class)))
                .thenThrow(new DuplicidadeException("Já existe uma avaliação cadastrada para o pedido 10."));

        MvcResult r = postar(json(10L, "A comida chegou fria e atrasada"), status().isConflict());

        assertThat(texto(r)).contains("Já existe uma avaliação");
    }

    @Test
    void bancoForaDoArRetorna503SemVazarDetalhesInternos() throws Exception {
        when(service.registrar(any(ReviewRequest.class)))
                .thenThrow(new CannotCreateTransactionException("conexao recusada em localhost:5432"));

        MvcResult r = postar(json(10L, "A comida chegou fria e atrasada"), status().isServiceUnavailable());

        assertThat(texto(r)).contains("temporariamente indisponível").doesNotContain("localhost:5432");
    }

    @Test
    void erroInesperadoRetorna500LimpoSemStackTrace() throws Exception {
        when(service.registrar(any(ReviewRequest.class)))
                .thenThrow(new IllegalStateException("detalhe interno que nao pode vazar"));

        MvcResult r = postar(json(10L, "A comida chegou fria e atrasada"), status().isInternalServerError());

        assertThat(texto(r)).contains("Erro interno inesperado").doesNotContain("detalhe interno");
    }
}
