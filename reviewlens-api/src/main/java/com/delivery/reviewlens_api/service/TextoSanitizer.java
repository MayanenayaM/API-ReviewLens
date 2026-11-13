package com.delivery.reviewlens_api.service;

import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class TextoSanitizer {

    private static final Pattern TAGS_HTML = Pattern.compile("<[^>]*>");
    private static final Pattern ESPACOS_E_QUEBRAS = Pattern.compile("\\s+");
    private static final Pattern CARACTERES_DE_CONTROLE = Pattern.compile("\\p{Cntrl}");
    private static final Pattern CARACTERES_PERIGOSOS = Pattern.compile("[<>{}\\[\\]\\\\`|^~#]");

    public String sanitizar(String texto) {
        if (texto == null) {
            return "";
        }
        String resultado = ESPACOS_E_QUEBRAS.matcher(texto).replaceAll(" ");     // quebras de linha viram espaço
        resultado = CARACTERES_DE_CONTROLE.matcher(resultado).replaceAll("");    // caracteres invisíveis
        resultado = TAGS_HTML.matcher(resultado).replaceAll("");                 // <tag>...</tag>
        resultado = CARACTERES_PERIGOSOS.matcher(resultado).replaceAll("");      // símbolos usados em injeção
        resultado = ESPACOS_E_QUEBRAS.matcher(resultado).replaceAll(" ");        // reorganiza espaços
        return resultado.trim();
    }
}
