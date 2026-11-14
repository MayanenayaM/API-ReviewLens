package com.delivery.reviewlens_api.exception;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.delivery.reviewlens_api.dto.ErroResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> tratarValidacao(MethodArgumentNotValidException ex) {
        List<String> mensagens = ex.getBindingResult().getFieldErrors().stream()
                .map(erro -> erro.getDefaultMessage())
                .toList();
        return montar(HttpStatus.BAD_REQUEST, mensagens);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> tratarCorpoInvalido(HttpMessageNotReadableException ex) {
        return montar(HttpStatus.BAD_REQUEST,
                List.of("Corpo da requisição ausente ou JSON inválido."));
    }

    @ExceptionHandler(TextoInvalidoException.class)
    public ResponseEntity<ErroResponse> tratarTextoInvalido(TextoInvalidoException ex) {
        return montar(HttpStatus.BAD_REQUEST, List.of(ex.getMessage()));
    }

    @ExceptionHandler(DuplicidadeException.class)
    public ResponseEntity<ErroResponse> tratarDuplicidade(DuplicidadeException ex) {
        return montar(HttpStatus.CONFLICT, List.of(ex.getMessage()));
    }

    // Banco fora do ar ou sem conexão: o serviço está temporariamente indisponível
    @ExceptionHandler({
            CannotCreateTransactionException.class,
            DataAccessResourceFailureException.class,
            TransientDataAccessException.class})
    public ResponseEntity<ErroResponse> tratarBancoIndisponivel(Exception ex) {
        log.error("Banco de dados indisponível: {}", ex.getMessage());
        return montar(HttpStatus.SERVICE_UNAVAILABLE,
                List.of("Serviço temporariamente indisponível. Tente novamente em instantes."));
    }

    // Rede de segurança: qualquer erro não previsto vira uma resposta limpa, sem stack trace
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> tratarInesperado(Exception ex) {

        if (ex instanceof ErrorResponse erroDoSpring) {
            HttpStatus status = HttpStatus.resolve(erroDoSpring.getStatusCode().value());
            if (status == null) {
                status = HttpStatus.INTERNAL_SERVER_ERROR;
            }
            return montar(status, List.of(status.getReasonPhrase()));
        }
        log.error("Erro inesperado ao processar a requisição", ex);
        return montar(HttpStatus.INTERNAL_SERVER_ERROR, List.of("Erro interno inesperado."));
    }

    private ResponseEntity<ErroResponse> montar(HttpStatus status, List<String> mensagens) {
        ErroResponse corpo = new ErroResponse(
                LocalDateTime.now(), status.value(), status.getReasonPhrase(), mensagens);
        return ResponseEntity.status(status).body(corpo);
    }
}