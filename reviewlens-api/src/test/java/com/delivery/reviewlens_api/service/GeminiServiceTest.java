package com.delivery.reviewlens_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import com.delivery.reviewlens_api.dto.Classificacao;
import com.delivery.reviewlens_api.exception.RespostaGeminiInvalidaException;
import com.delivery.reviewlens_api.model.Sentimento;
import com.delivery.reviewlens_api.model.Topico;

import tools.jackson.databind.json.JsonMapper;

class GeminiServiceTest {

    private final JsonMapper mapper = JsonMapper.builder().build();
    private final GeminiService service = new GeminiService(RestClient.create(), mapper, "modelo-teste");

    private String respostaCom(String textoInterno) {
        return mapper.writeValueAsString(Map.of("candidates", List.of(
                Map.of("content", Map.of("parts", List.of(Map.of("text", textoInterno)))))));
    }

    @Test
    void interpretaRespostaValida() {
        Classificacao c = service.interpretar(
                respostaCom("{\"sentimento\":\"NEGATIVO\",\"topico\":\"ENTREGA\"}"));
        assertThat(c.sentimento()).isEqualTo(Sentimento.NEGATIVO);
        assertThat(c.topico()).isEqualTo(Topico.ENTREGA);
    }

    @Test
    void aceitaJsonDentroDeCercasDeCodigo() {
        String texto = "```json\n{\"sentimento\":\"POSITIVO\",\"topico\":\"SABOR\"}\n```";
        Classificacao c = service.interpretar(respostaCom(texto));
        assertThat(c.sentimento()).isEqualTo(Sentimento.POSITIVO);
        assertThat(c.topico()).isEqualTo(Topico.SABOR);
    }

    @Test
    void valorForaDoDominioLancaExcecao() {
        assertThatThrownBy(() -> service.interpretar(
                respostaCom("{\"sentimento\":\"FELIZ\",\"topico\":\"SABOR\"}")))
                .isInstanceOf(RespostaGeminiInvalidaException.class);
    }

    @Test
    void jsonMalformadoLancaExcecao() {
        assertThatThrownBy(() -> service.interpretar(respostaCom("isso nao e json")))
                .isInstanceOf(RespostaGeminiInvalidaException.class);
    }

    @Test
    void respostaSemCandidatosLancaExcecao() {
        assertThatThrownBy(() -> service.interpretar("{\"candidates\":[]}"))
                .isInstanceOf(RespostaGeminiInvalidaException.class);
    }
}