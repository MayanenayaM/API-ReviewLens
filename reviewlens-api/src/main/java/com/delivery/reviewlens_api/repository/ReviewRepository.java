package com.delivery.reviewlens_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.delivery.reviewlens_api.model.Review;

import java.util.List;

import org.springframework.data.domain.Pageable;

import com.delivery.reviewlens_api.model.StatusProcessamento;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByIdPedido(Long idPedido);
     List<Review> findByStatusOrderByDataCriacaoAsc(StatusProcessamento status, Pageable pageable);
}
