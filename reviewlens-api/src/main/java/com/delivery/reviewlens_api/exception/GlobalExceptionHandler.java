package com.delivery.reviewlens_api.exception;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.delivery.reviewlens_api.dto.ErroResponse;

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

    private ResponseEntity<ErroResponse> montar(HttpStatus status, List<String> mensagens) {
        ErroResponse corpo = new ErroResponse(
                LocalDateTime.now(), status.value(), status.getReasonPhrase(), mensagens);
        return ResponseEntity.status(status).body(corpo);
    }
}
