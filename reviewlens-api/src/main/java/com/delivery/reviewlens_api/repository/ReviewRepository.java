package com.delivery.reviewlens_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.delivery.reviewlens_api.model.Review;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByIdPedido(Long idPedido);
}
