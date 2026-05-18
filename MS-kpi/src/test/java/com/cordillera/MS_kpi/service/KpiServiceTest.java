package com.cordillera.MS_kpi.service;

import com.cordillera.MS_kpi.client.DatoClient;
import com.cordillera.MS_kpi.dto.DatoDto;
import com.cordillera.MS_kpi.dto.KpiDto;
import com.cordillera.MS_kpi.dto.KpiResponse;
import com.cordillera.MS_kpi.entity.Kpi;
import com.cordillera.MS_kpi.factory.KpiCalculatorCreator;
import com.cordillera.MS_kpi.factory.KpiCalculatorRegistry;
import com.cordillera.MS_kpi.kafka.KpiEventPublisher;
import com.cordillera.MS_kpi.repository.KpiRepository;
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
class KpiServiceTest {

    @Mock KpiRepository         kpiRepository;
    @Mock KpiCalculatorRegistry calculatorRegistry;
    @Mock KpiEventPublisher     eventPublisher;
    @Mock DatoClient            datoClient;

    @InjectMocks KpiService kpiService;

    private Kpi kpiGuardado;

    @BeforeEach
    void setUp() {
        kpiGuardado = Kpi.builder()
                .id(1L)
                .nombre("Ventas Mayo")
                .tipoCalculo("SUMA")
                .valor(new BigDecimal("150000.0000"))
                .periodo("2026-05")
                .unidad("CLP")
                .descripcion("Suma de ventas del mes")
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ── calcular ───────────────────────────────────────────────────────────

    @Test
    void calcular_retornaKpiResponse() {
        KpiDto dto = new KpiDto();
        dto.setNombre("Ventas Mayo");
        dto.setTipoCalculo("SUMA");
        dto.setValores(List.of(new BigDecimal("100000"), new BigDecimal("50000")));
        dto.setPeriodo("2026-05");

        KpiCalculatorCreator creatorMock = mock(KpiCalculatorCreator.class);
        when(calculatorRegistry.obtenerCreador("SUMA")).thenReturn(creatorMock);
        when(creatorMock.calcular(dto.getValores())).thenReturn(new BigDecimal("150000.0000"));
        when(kpiRepository.save(any(Kpi.class))).thenReturn(kpiGuardado);

        KpiResponse response = kpiService.calcular(dto);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getNombre()).isEqualTo("Ventas Mayo");
        assertThat(response.getValor()).isEqualByComparingTo("150000.0000");
        assertThat(response.getTipoCalculo()).isEqualTo("SUMA");
        verify(eventPublisher).publicarKpi(kpiGuardado);
    }

    @Test
    void calcular_unidadNula_usaClpPorDefecto() {
        KpiDto dto = new KpiDto();
        dto.setNombre("Test");
        dto.setTipoCalculo("PROMEDIO");
        dto.setValores(List.of(new BigDecimal("200")));
        dto.setPeriodo("2026-05");
        dto.setUnidad(null);

        Kpi kpiClp = Kpi.builder().id(2L).nombre("Test").tipoCalculo("PROMEDIO")
                .valor(new BigDecimal("200")).periodo("2026-05").unidad("CLP")
                .createdAt(LocalDateTime.now()).build();

        KpiCalculatorCreator creatorMock = mock(KpiCalculatorCreator.class);
        when(calculatorRegistry.obtenerCreador("PROMEDIO")).thenReturn(creatorMock);
        when(creatorMock.calcular(anyList())).thenReturn(new BigDecimal("200"));
        when(kpiRepository.save(any(Kpi.class))).thenReturn(kpiClp);

        KpiResponse response = kpiService.calcular(dto);

        assertThat(response.getUnidad()).isEqualTo("CLP");
    }

    // ── calcularDesdeDatos ─────────────────────────────────────────────────

    @Test
    void calcularDesdeDatos_conDatosDisponibles_retornaKpi() {
        DatoDto dato1 = new DatoDto(); dato1.setTipo("VENTA"); dato1.setValor(new BigDecimal("80000"));
        DatoDto dato2 = new DatoDto(); dato2.setTipo("VENTA"); dato2.setValor(new BigDecimal("70000"));
        DatoDto dato3 = new DatoDto(); dato3.setTipo("GASTO"); dato3.setValor(new BigDecimal("30000"));

        when(datoClient.listarPorPeriodo("2026-05")).thenReturn(List.of(dato1, dato2, dato3));

        KpiCalculatorCreator creatorMock = mock(KpiCalculatorCreator.class);
        when(calculatorRegistry.obtenerCreador("SUMA")).thenReturn(creatorMock);
        when(creatorMock.calcular(List.of(new BigDecimal("80000"), new BigDecimal("70000"))))
                .thenReturn(new BigDecimal("150000"));
        when(kpiRepository.save(any(Kpi.class))).thenReturn(kpiGuardado);

        KpiResponse response = kpiService.calcularDesdeDatos("SUMA", "VENTA", "2026-05", "Ventas Mayo");

        assertThat(response).isNotNull();
        verify(datoClient).listarPorPeriodo("2026-05");
    }

    @Test
    void calcularDesdeDatos_sinDatosDelTipo_lanzaExcepcion() {
        DatoDto dato = new DatoDto(); dato.setTipo("GASTO"); dato.setValor(new BigDecimal("1000"));

        when(datoClient.listarPorPeriodo("2026-05")).thenReturn(List.of(dato));

        assertThatThrownBy(() ->
                kpiService.calcularDesdeDatos("SUMA", "VENTA", "2026-05", "Sin ventas"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No se encontraron datos de tipo 'VENTA'");
    }

    @Test
    void calcularDesdeDatos_listaVaciaDelCliente_lanzaExcepcion() {
        when(datoClient.listarPorPeriodo("2026-05")).thenReturn(List.of());

        assertThatThrownBy(() ->
                kpiService.calcularDesdeDatos("PROMEDIO", "VENTA", "2026-05", "Test"))
                .isInstanceOf(RuntimeException.class);
    }

    // ── listar ─────────────────────────────────────────────────────────────

    @Test
    void listarTodos_retornaListaCompleta() {
        when(kpiRepository.findAll()).thenReturn(List.of(kpiGuardado));

        List<KpiResponse> lista = kpiService.listarTodos();

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).getNombre()).isEqualTo("Ventas Mayo");
    }

    @Test
    void listarTodos_sinRegistros_retornaListaVacia() {
        when(kpiRepository.findAll()).thenReturn(List.of());

        assertThat(kpiService.listarTodos()).isEmpty();
    }

    @Test
    void listarPorPeriodo_retornaSoloDelPeriodo() {
        when(kpiRepository.findByPeriodo("2026-05")).thenReturn(List.of(kpiGuardado));

        List<KpiResponse> lista = kpiService.listarPorPeriodo("2026-05");

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).getPeriodo()).isEqualTo("2026-05");
    }

    @Test
    void listarPorTipo_normalizaMayusculas() {
        when(kpiRepository.findByTipoCalculo("SUMA")).thenReturn(List.of(kpiGuardado));

        List<KpiResponse> lista = kpiService.listarPorTipo("suma");

        assertThat(lista).hasSize(1);
        verify(kpiRepository).findByTipoCalculo("SUMA");
    }

    // ── buscarPorId ────────────────────────────────────────────────────────

    @Test
    void buscarPorId_existente_retornaResponse() {
        when(kpiRepository.findById(1L)).thenReturn(Optional.of(kpiGuardado));

        KpiResponse response = kpiService.buscarPorId(1L);

        assertThat(response.getId()).isEqualTo(1L);
    }

    @Test
    void buscarPorId_noExiste_lanzaExcepcion() {
        when(kpiRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> kpiService.buscarPorId(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("KPI no encontrado con id: 99");
    }
}
