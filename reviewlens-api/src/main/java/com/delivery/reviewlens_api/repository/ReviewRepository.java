package com.delivery.reviewlens_api.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.delivery.reviewlens_api.model.Review;
import com.delivery.reviewlens_api.model.Sentimento;
import com.delivery.reviewlens_api.model.StatusProcessamento;
import com.delivery.reviewlens_api.model.Topico;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByIdPedido(Long idPedido);

    List<Review> findByStatusOrderByDataCriacaoAsc(StatusProcessamento status, Pageable pageable);

    // ---- Projeções ----

    interface ContagemSentimento {
        Sentimento getSentimento();
        Long getTotal();
    }

    interface ContagemTopico {
        Topico getTopico();
        Long getTotal();
    }

    // ---- Estatísticas ----

    @Query("""
            select r.sentimento as sentimento, count(r) as total
            from Review r
            where r.status = :status
            group by r.sentimento
            """)
    List<ContagemSentimento> contarPorSentimento(@Param("status") StatusProcessamento status);

    @Query("""
            select r.topico as topico, count(r) as total
            from Review r
            where r.status = :status
            group by r.topico
            """)
    List<ContagemTopico> contarPorTopico(@Param("status") StatusProcessamento status);

    // ---- Volume de um tópico dentro de um período ----

    @Query("""
            select count(r)
            from Review r
            where r.status = :status
              and r.topico = :topico
              and r.dataCriacao >= :inicio
              and r.dataCriacao < :fim
            """)
    long contarPorTopicoNoPeriodo(
            @Param("status") StatusProcessamento status,
            @Param("topico") Topico topico,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);

    @Query("""
            select count(r)
            from Review r
            where r.status = :status
              and r.topico = :topico
              and r.sentimento = :sentimento
              and r.dataCriacao >= :inicio
              and r.dataCriacao < :fim
            """)
    long contarPorTopicoESentimentoNoPeriodo(
            @Param("status") StatusProcessamento status,
            @Param("topico") Topico topico,
            @Param("sentimento") Sentimento sentimento,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);
}
