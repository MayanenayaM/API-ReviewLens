package com.delivery.reviewlens_api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.delivery.reviewlens_api.dto.ReviewRequest;
import com.delivery.reviewlens_api.dto.ReviewResponse;
import com.delivery.reviewlens_api.model.Review;
import com.delivery.reviewlens_api.model.StatusProcessamento;
import com.delivery.reviewlens_api.service.ReviewService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService service;

    @PostMapping
    public ResponseEntity<ReviewResponse> criar(@Valid @RequestBody ReviewRequest request) {
        Review review = service.registrar(request);

        HttpStatus status = review.getStatus() == StatusProcessamento.PROCESSADO
                ? HttpStatus.CREATED
                : HttpStatus.ACCEPTED;

        return ResponseEntity.status(status).body(ReviewResponse.from(review));
    }
}
