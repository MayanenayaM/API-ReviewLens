package com.delivery.reviewlens_api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.delivery.reviewlens_api.dto.EstatisticasResponse;
import com.delivery.reviewlens_api.service.EstatisticasService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reviews/stats")
@RequiredArgsConstructor
public class EstatisticasController {

    private final EstatisticasService service;

    @GetMapping
    public EstatisticasResponse estatisticas() {
        return service.gerar();
    }
}
