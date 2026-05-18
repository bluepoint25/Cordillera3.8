package com.cordillera.MS_reportes.service;

import com.cordillera.MS_reportes.client.DatoClient;
import com.cordillera.MS_reportes.client.KpiClient;
import com.cordillera.MS_reportes.dto.KpiDto;
import com.cordillera.MS_reportes.dto.ReporteDto;
import com.cordillera.MS_reportes.dto.ReporteResponse;
import com.cordillera.MS_reportes.entity.Reporte;
import com.cordillera.MS_reportes.factory.ReporteCreador;
import com.cordillera.MS_reportes.factory.ReporteCreadorRegistry;
import com.cordillera.MS_reportes.kafka.ReporteEventPublisher;
import com.cordillera.MS_reportes.repository.ReporteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("null")
@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

    @Mock ReporteRepository      reporteRepository;
    @Mock ReporteCreadorRegistry creadorRegistry;
    @Mock ReporteEventPublisher  eventPublisher;
    @Mock KpiClient              kpiClient;
    @Mock DatoClient             datoClient;

    @InjectMocks ReporteService reporteService;

    private Reporte reporteGuardado;

    @BeforeEach
    void setUp() {
        reporteGuardado = Reporte.builder()
                .id(1L)
                .tipo("KPI")
                .titulo("Reporte KPI Mayo 2026")
                .contenido("Contenido generado por el sistema")
                .periodo("2026-05")
                .estado("GENERADO")
                .generadoPor("Administrador del Sistema")
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ── generar ────────────────────────────────────────────────────────────

    @Test
    void generar_conKpisYDatos_retornaReporteResponse() {
        ReporteDto dto = new ReporteDto();
        dto.setTipo("KPI");
        dto.setTitulo("Reporte KPI Mayo 2026");
        dto.setPeriodo("2026-05");
        dto.setGeneradoPor("Administrador del Sistema");

        KpiDto kpi = new KpiDto();
        kpi.setNombre("Ventas"); kpi.setValor(new BigDecimal("100000")); kpi.setPeriodo("2026-05");

        ReporteCreador creadorMock = mock(ReporteCreador.class);
        when(kpiClient.listarPorPeriodo("2026-05")).thenReturn(List.of(kpi));
        when(datoClient.listarPorPeriodo("2026-05")).thenReturn(List.of());
        when(creadorRegistry.obtenerCreador("KPI")).thenReturn(creadorMock);
        when(creadorMock.construirReporte(eq(dto), anyList(), anyList())).thenReturn(reporteGuardado);
        when(reporteRepository.save(reporteGuardado)).thenReturn(reporteGuardado);

        ReporteResponse response = reporteService.generar(dto);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTipo()).isEqualTo("KPI");
        assertThat(response.getPeriodo()).isEqualTo("2026-05");
        assertThat(response.getEstado()).isEqualTo("GENERADO");
        verify(eventPublisher).publicarReporte(reporteGuardado);
    }

    @Test
    void generar_sinKpisNiDatos_invocaFactoryIgual() {
        ReporteDto dto = new ReporteDto();
        dto.setTipo("RESUMEN");
        dto.setTitulo("Resumen vacío");
        dto.setPeriodo("2026-01");

        Reporte reporteVacio = Reporte.builder()
                .id(2L).tipo("RESUMEN").titulo("Resumen vacío")
                .contenido("Sin datos disponibles").periodo("2026-01")
                .estado("GENERADO").createdAt(LocalDateTime.now()).build();

        ReporteCreador creadorMock = mock(ReporteCreador.class);
        when(kpiClient.listarPorPeriodo("2026-01")).thenReturn(List.of());
        when(datoClient.listarPorPeriodo("2026-01")).thenReturn(List.of());
        when(creadorRegistry.obtenerCreador("RESUMEN")).thenReturn(creadorMock);
        when(creadorMock.construirReporte(eq(dto), anyList(), anyList())).thenReturn(reporteVacio);
        when(reporteRepository.save(reporteVacio)).thenReturn(reporteVacio);

        ReporteResponse response = reporteService.generar(dto);

        assertThat(response.getTipo()).isEqualTo("RESUMEN");
        verify(creadorRegistry).obtenerCreador("RESUMEN");
    }

    @Test
    void generar_tipoInvalido_lanzaExcepcion() {
        ReporteDto dto = new ReporteDto();
        dto.setTipo("INVALIDO");
        dto.setTitulo("Test");
        dto.setPeriodo("2026-05");

        when(kpiClient.listarPorPeriodo("2026-05")).thenReturn(List.of());
        when(datoClient.listarPorPeriodo("2026-05")).thenReturn(List.of());
        when(creadorRegistry.obtenerCreador("INVALIDO"))
                .thenThrow(new IllegalArgumentException("Tipo de reporte inválido: 'INVALIDO'"));

        assertThatThrownBy(() -> reporteService.generar(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tipo de reporte inválido");
    }

    // ── listar ─────────────────────────────────────────────────────────────

    @Test
    void listarTodos_retornaListaCompleta() {
        when(reporteRepository.findAll()).thenReturn(List.of(reporteGuardado));

        List<ReporteResponse> lista = reporteService.listarTodos();

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).getTitulo()).isEqualTo("Reporte KPI Mayo 2026");
    }

    @Test
    void listarTodos_sinRegistros_retornaListaVacia() {
        when(reporteRepository.findAll()).thenReturn(List.of());

        assertThat(reporteService.listarTodos()).isEmpty();
    }

    @Test
    void listarPorPeriodo_retornaSoloDelPeriodo() {
        when(reporteRepository.findByPeriodo("2026-05")).thenReturn(List.of(reporteGuardado));

        List<ReporteResponse> lista = reporteService.listarPorPeriodo("2026-05");

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).getPeriodo()).isEqualTo("2026-05");
    }

    @Test
    void listarPorPeriodo_sinResultados_retornaListaVacia() {
        when(reporteRepository.findByPeriodo("2025-01")).thenReturn(List.of());

        assertThat(reporteService.listarPorPeriodo("2025-01")).isEmpty();
    }

    @Test
    void listarPorTipo_normalizaMayusculas() {
        when(reporteRepository.findByTipo("MENSUAL")).thenReturn(List.of());

        reporteService.listarPorTipo("mensual");

        verify(reporteRepository).findByTipo("MENSUAL");
    }

    // ── buscarPorId ────────────────────────────────────────────────────────

    @Test
    void buscarPorId_existente_retornaResponse() {
        when(reporteRepository.findById(1L)).thenReturn(Optional.of(reporteGuardado));

        ReporteResponse response = reporteService.buscarPorId(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getGeneradoPor()).isEqualTo("Administrador del Sistema");
    }

    @Test
    void buscarPorId_noExiste_lanzaExcepcion() {
        when(reporteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reporteService.buscarPorId(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Reporte no encontrado con id: 99");
    }
}
