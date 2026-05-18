package com.cordillera.MS_data.service;

import com.cordillera.MS_data.dto.DatoDto;
import com.cordillera.MS_data.dto.DatoResponse;
import com.cordillera.MS_data.entity.DatoIngresado;
import com.cordillera.MS_data.kafka.DatoEventPublisher;
import com.cordillera.MS_data.repository.DatoRepository;
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
class DatoServiceTest {

    @Mock DatoRepository     datoRepository;
    @Mock DatoEventPublisher eventPublisher;

    @InjectMocks DatoService datoService;

    private DatoIngresado datoGuardado;

    @BeforeEach
    void setUp() {
        datoGuardado = DatoIngresado.builder()
                .id(1L)
                .fuente("Sistema ERP")
                .tipo("VENTA")
                .valor(new BigDecimal("75000.0000"))
                .periodo("2026-05")
                .descripcion("Venta registrada en tienda")
                .procesado(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ── registrar ──────────────────────────────────────────────────────────

    @Test
    void registrar_exitoso_retornaDatoResponse() {
        DatoDto dto = new DatoDto();
        dto.setFuente("Sistema ERP");
        dto.setTipo("venta");
        dto.setValor(new BigDecimal("75000"));
        dto.setPeriodo("2026-05");
        dto.setDescripcion("Venta registrada en tienda");

        when(datoRepository.save(any(DatoIngresado.class))).thenReturn(datoGuardado);

        DatoResponse response = datoService.registrar(dto);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTipo()).isEqualTo("VENTA");
        assertThat(response.getValor()).isEqualByComparingTo("75000.0000");
        assertThat(response.getPeriodo()).isEqualTo("2026-05");
        assertThat(response.getProcesado()).isFalse();
        verify(eventPublisher).publicarDato(datoGuardado);
    }

    @Test
    void registrar_normalizaTipoAMayusculas() {
        DatoDto dto = new DatoDto();
        dto.setFuente("ERP");
        dto.setTipo("gasto");
        dto.setValor(new BigDecimal("5000"));
        dto.setPeriodo("2026-05");

        DatoIngresado gastoGuardado = DatoIngresado.builder()
                .id(2L).fuente("ERP").tipo("GASTO")
                .valor(new BigDecimal("5000")).periodo("2026-05")
                .procesado(false).createdAt(LocalDateTime.now()).build();

        when(datoRepository.save(any(DatoIngresado.class))).thenReturn(gastoGuardado);

        DatoResponse response = datoService.registrar(dto);

        assertThat(response.getTipo()).isEqualTo("GASTO");
    }

    // ── listar ─────────────────────────────────────────────────────────────

    @Test
    void listarTodos_retornaListaCompleta() {
        when(datoRepository.findAll()).thenReturn(List.of(datoGuardado));

        List<DatoResponse> lista = datoService.listarTodos();

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).getFuente()).isEqualTo("Sistema ERP");
    }

    @Test
    void listarTodos_sinRegistros_retornaListaVacia() {
        when(datoRepository.findAll()).thenReturn(List.of());

        assertThat(datoService.listarTodos()).isEmpty();
    }

    @Test
    void listarPorPeriodo_retornaSoloDelPeriodo() {
        when(datoRepository.findByPeriodo("2026-05")).thenReturn(List.of(datoGuardado));

        List<DatoResponse> lista = datoService.listarPorPeriodo("2026-05");

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).getPeriodo()).isEqualTo("2026-05");
    }

    @Test
    void listarPorPeriodo_periodoSinDatos_retornaListaVacia() {
        when(datoRepository.findByPeriodo("2020-01")).thenReturn(List.of());

        assertThat(datoService.listarPorPeriodo("2020-01")).isEmpty();
    }

    @Test
    void listarPorTipo_normalizaMayusculas() {
        when(datoRepository.findByTipo("VENTA")).thenReturn(List.of(datoGuardado));

        List<DatoResponse> lista = datoService.listarPorTipo("venta");

        assertThat(lista).hasSize(1);
        verify(datoRepository).findByTipo("VENTA");
    }

    // ── buscarPorId ────────────────────────────────────────────────────────

    @Test
    void buscarPorId_existente_retornaResponse() {
        when(datoRepository.findById(1L)).thenReturn(Optional.of(datoGuardado));

        DatoResponse response = datoService.buscarPorId(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getDescripcion()).isEqualTo("Venta registrada en tienda");
    }

    @Test
    void buscarPorId_noExiste_lanzaExcepcion() {
        when(datoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> datoService.buscarPorId(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Dato no encontrado con id: 99");
    }

    // ── marcarProcesado ────────────────────────────────────────────────────

    @Test
    void marcarProcesado_existente_actualizaFlag() {
        when(datoRepository.findById(1L)).thenReturn(Optional.of(datoGuardado));
        when(datoRepository.save(datoGuardado)).thenReturn(datoGuardado);

        datoService.marcarProcesado(1L);

        assertThat(datoGuardado.getProcesado()).isTrue();
        verify(datoRepository).save(datoGuardado);
    }

    @Test
    void marcarProcesado_noExiste_lanzaExcepcion() {
        when(datoRepository.findById(55L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> datoService.marcarProcesado(55L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Dato no encontrado con id: 55");
    }
}
