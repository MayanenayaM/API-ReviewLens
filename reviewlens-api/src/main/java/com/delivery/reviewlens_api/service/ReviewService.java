package com.delivery.reviewlens_api.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.delivery.reviewlens_api.dto.ResultadoClassificacao;
import com.delivery.reviewlens_api.dto.ReviewRequest;
import com.delivery.reviewlens_api.exception.DuplicidadeException;
import com.delivery.reviewlens_api.exception.TextoInvalidoException;
import com.delivery.reviewlens_api.model.Review;
import com.delivery.reviewlens_api.model.StatusProcessamento;
import com.delivery.reviewlens_api.repository.ReviewRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private static final int TAMANHO_MINIMO = 10;

    private final ReviewRepository repository;
    private final TextoSanitizer sanitizer;
    private final ClassificadorService classificador;

    public Review registrar(ReviewRequest request) {
        String textoSanitizado = sanitizer.sanitizar(request.texto());

        if (textoSanitizado.length() < TAMANHO_MINIMO) {
            throw new TextoInvalidoException(
                    "O texto da avaliação ficou com menos de 10 caracteres válidos após a sanitização.");
        }

        if (repository.existsByIdPedido(request.idPedido())) {
            throw new DuplicidadeException(
                    "Já existe uma avaliação cadastrada para o pedido " + request.idPedido() + ".");
        }

        ResultadoClassificacao resultado = classificador.classificar(textoSanitizado);

        Review review = Review.builder()
                .idPedido(request.idPedido())
                .texto(request.texto())
                .sentimento(resultado.sentimento())
                .topico(resultado.topico())
                .status(resultado.status())
                .tentativas(resultado.status() == StatusProcessamento.PENDENTE ? 1 : 0)
                .build();

        try {
            return repository.save(review);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicidadeException(
                    "Já existe uma avaliação cadastrada para o pedido " + request.idPedido() + ".");
        }
    }
}
