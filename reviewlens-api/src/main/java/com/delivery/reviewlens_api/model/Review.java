package com.delivery.reviewlens_api.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "reviews",
    uniqueConstraints = @UniqueConstraint(name = "uk_reviews_id_pedido", columnNames = "id_pedido")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_pedido", nullable = false)
    private Long idPedido;

    @Column(nullable = false, length = 1000)
    private String texto;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Sentimento sentimento;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Topico topico;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusProcessamento status;

    @Column(name = "tentativas", nullable = false, columnDefinition = "integer default 0")
    private int tentativas;

    @CreationTimestamp
    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

}
