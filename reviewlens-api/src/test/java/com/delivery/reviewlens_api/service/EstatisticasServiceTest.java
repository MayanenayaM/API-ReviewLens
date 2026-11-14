package com.delivery.reviewlens_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.delivery.reviewlens_api.dto.EstatisticasResponse;
import com.delivery.reviewlens_api.model.Sentimento;
import com.delivery.reviewlens_api.model.StatusProcessamento;
import com.delivery.reviewlens_api.model.Topico;
import com.delivery.reviewlens_api.repository.ReviewRepository;
import com.delivery.reviewlens_api.repository.ReviewRepository.ContagemSentimento;
import com.delivery.reviewlens_api.repository.ReviewRepository.ContagemTopico;

@ExtendWith(MockitoExtension.class)
class EstatisticasServiceTest {

    private static final StatusProcessamento OK = StatusProcessamento.PROCESSADO;
    private static final YearMonth NOVEMBRO = YearMonth.of(2026, 11);
    private static final LocalDateTime INICIO_NOV = LocalDateTime.of(2026, 11, 1, 0, 0);
    private static final LocalDateTime FIM_NOV = LocalDateTime.of(2026, 12, 1, 0, 0);

    @Mock
    private ReviewRepository repository;

    @InjectMocks
    private EstatisticasService service;

    private ContagemSentimento sent(Sentimento sentimento, long total) {
        return new ContagemSentimento() {
            public Sentimento getSentimento() { return sentimento; }
            public Long getTotal() { return total; }
        };
    }

    private ContagemTopico top(Topico topico, long total) {
        return new ContagemTopico() {
            public Topico getTopico() { return topico; }
            public Long getTotal() { return total; }
        };
    }

    private void entregaDoMes(YearMonth mes, LocalDateTime inicio, LocalDateTime fim, long total, long negativas) {
        when(repository.contarPorTopicoNoPeriodo(OK, Topico.ENTREGA, inicio, fim)).thenReturn(total);
        when(repository.contarPorTopicoESentimentoNoPeriodo(OK, Topico.ENTREGA, Sentimento.NEGATIVO, inicio, fim))
                .thenReturn(negativas);
    }

    @Test
    void somaApenasProcessadasEPreencheCategoriasSemDadosComZero() {
        // As consultas só respondem quando o status é PROCESSADO (RN-09)
        when(repository.contarPorSentimento(OK))
                .thenReturn(List.of(sent(Sentimento.NEGATIVO, 4), sent(Sentimento.POSITIVO, 4), sent(Sentimento.NEUTRO, 1)));
        when(repository.contarPorTopico(OK))
                .thenReturn(List.of(top(Topico.ENTREGA, 5), top(Topico.SABOR, 2)));

        EstatisticasResponse r = service.gerar(NOVEMBRO);

        assertThat(r.totalProcessadas()).isEqualTo(9);
        assertThat(r.sentimentos()).containsEntry("NEGATIVO", 4L).containsEntry("POSITIVO", 4L).containsEntry("NEUTRO", 1L);
        assertThat(r.topicos()).hasSize(6)
                .containsEntry("ENTREGA", 5L)
                .containsEntry("SABOR", 2L)
                .containsEntry("TEMPERATURA", 0L)
                .containsEntry("OUTROS", 0L);
    }

    @Test
    void maisDeCinquentaPorCentoNegativoDisparaAlerta() {
        entregaDoMes(NOVEMBRO, INICIO_NOV, FIM_NOV, 5, 3);

        EstatisticasResponse.EntregaMes e = service.gerar(NOVEMBRO).entregaMes();

        assertThat(e.mes()).isEqualTo("2026-11");
        assertThat(e.total()).isEqualTo(5);
        assertThat(e.negativas()).isEqualTo(3);
        assertThat(e.percentualNegativo()).isEqualTo(60.0);
        assertThat(e.alertaCritico()).isTrue();
    }

    @Test
    void exatamenteCinquentaPorCentoNaoDisparaAlerta() {
        entregaDoMes(NOVEMBRO, INICIO_NOV, FIM_NOV, 4, 2);

        EstatisticasResponse.EntregaMes e = service.gerar(NOVEMBRO).entregaMes();

        assertThat(e.percentualNegativo()).isEqualTo(50.0);
        assertThat(e.alertaCritico()).isFalse();
    }

    @Test
    void semEntregasNoMesNaoHaAlertaENaoDivideTemPorZero() {
        EstatisticasResponse.EntregaMes e = service.gerar(NOVEMBRO).entregaMes();

        assertThat(e.total()).isZero();
        assertThat(e.percentualNegativo()).isEqualTo(0.0);
        assertThat(e.alertaCritico()).isFalse();
        verify(repository, never()).contarPorTopicoESentimentoNoPeriodo(any(), any(), any(), any(), any());
    }

    @Test
    void dezembroTerminaNoDiaPrimeiroDeJaneiroDoAnoSeguinte() {
        entregaDoMes(YearMonth.of(2026, 12),
                LocalDateTime.of(2026, 12, 1, 0, 0), LocalDateTime.of(2027, 1, 1, 0, 0), 2, 2);

        EstatisticasResponse.EntregaMes e = service.gerar(YearMonth.of(2026, 12)).entregaMes();

        assertThat(e.mes()).isEqualTo("2026-12");
        assertThat(e.percentualNegativo()).isEqualTo(100.0);
        assertThat(e.alertaCritico()).isTrue();
    }
}
