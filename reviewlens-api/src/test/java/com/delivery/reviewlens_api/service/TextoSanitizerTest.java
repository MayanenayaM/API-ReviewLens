package com.delivery.reviewlens_api.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TextoSanitizerTest {

    private final TextoSanitizer sanitizer = new TextoSanitizer();

    @Test
    void removeTagsHtml() {
        assertThat(sanitizer.sanitizar("<b>Comida</b> muito boa")).isEqualTo("Comida muito boa");
    }

    @Test
    void transformaQuebrasDeLinhaEmUmEspaco() {
        assertThat(sanitizer.sanitizar("Linha um\n\n\nlinha dois")).isEqualTo("Linha um linha dois");
    }

    @Test
    void removeSimbolosPerigosos() {
        assertThat(sanitizer.sanitizar("Ignore {tudo} [acima] `agora`")).isEqualTo("Ignore tudo acima agora");
    }

    @Test
    void mantemAcentosEPontuacaoNormal() {
        String texto = "Entrega rápida, comida ótima! Paguei R$ 30,00.";
        assertThat(sanitizer.sanitizar(texto)).isEqualTo(texto);
    }

    @Test
    void textoNuloViraVazio() {
        assertThat(sanitizer.sanitizar(null)).isEmpty();
    }
}
