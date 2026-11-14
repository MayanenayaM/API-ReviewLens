package com.delivery.reviewlens_api.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.delivery.reviewlens_api.dto.Classificacao;
import com.delivery.reviewlens_api.dto.ReviewRequest;
import com.delivery.reviewlens_api.dto.ReviewResponse;
import com.delivery.reviewlens_api.exception.DuplicidadeException;
import com.delivery.reviewlens_api.exception.TextoInvalidoException;
import com.delivery.reviewlens_api.model.Review;
import com.delivery.reviewlens_api.model.StatusProcessamento;
import com.delivery.reviewlens_api.repository.ReviewRepository;
import com.delivery.reviewlens_api.model.StatusProcessamento;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private static final int TAMANHO_MINIMO = 10;

    private final ReviewRepository repository;
    private final TextoSanitizer sanitizer;
    private final GeminiService geminiService;

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

        Classificacao classificacao = geminiService.classificar(textoSanitizado);

        Review review = Review.builder()
                .idPedido(request.idPedido())
                .texto(request.texto())                   
                .sentimento(classificacao.sentimento())
                .topico(classificacao.topico())
                .status(StatusProcessamento.PROCESSADO)
                .build();

        try {
            return repository.save(review);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicidadeException(
                    "Já existe uma avaliação cadastrada para o pedido " + request.idPedido() + ".");
        }
    }
}
