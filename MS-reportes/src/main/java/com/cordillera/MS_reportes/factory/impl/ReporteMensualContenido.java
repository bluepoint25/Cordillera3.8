package com.cordillera.MS_reportes.factory.impl;

import com.cordillera.MS_reportes.dto.DatoDto;
import com.cordillera.MS_reportes.dto.KpiDto;
import com.cordillera.MS_reportes.dto.ReporteDto;
import com.cordillera.MS_reportes.factory.ReporteContenido;
import com.cordillera.MS_reportes.factory.TipoReporte;
import com.cordillera.MS_reportes.util.RangoKpi;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** ConcreteProduct – genera un reporte mensual con datos agrupados por tipo. */
public class ReporteMensualContenido implements ReporteContenido {

    @Override
    public String construirContenido(ReporteDto dto, List<KpiDto> kpis, List<DatoDto> datos) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== REPORTE MENSUAL ===\n");
        sb.append("Titulo: ").append(dto.getTitulo()).append("\n");
        sb.append("Periodo: ").append(dto.getPeriodo()).append("\n\n");

        if (!datos.isEmpty()) {
            sb.append("--- DATOS DEL PERIODO ---\n");
            Map<String, List<DatoDto>> porTipo = datos.stream()
                    .collect(Collectors.groupingBy(DatoDto::getTipo));

            porTipo.forEach((tipo, lista) -> {
                BigDecimal total = lista.stream()
                        .map(DatoDto::getValor)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                sb.append(String.format("  %s: %d registros | Total: %s\n",
                        tipo, lista.size(), total.toPlainString()));
            });
            sb.append("\n");
        }

        if (!kpis.isEmpty()) {
            sb.append("--- KPIs DEL PERIODO ---\n");
            sb.append("Rango (PROMEDIO/SUMA): 🟢 ÓPTIMO · 🟡 REGULAR · 🔴 MALO\n");
            kpis.forEach(k -> sb.append(String.format(
                    "  %s: %s %s%s\n", k.getNombre(),
                    k.getValor() != null ? k.getValor().toPlainString() : "N/A",
                    k.getUnidad() != null ? k.getUnidad() : "",
                    RangoKpi.etiquetaPara(k)
            )));
        }

        return sb.toString();
    }

    @Override
    public TipoReporte getTipo() {
        return TipoReporte.MENSUAL;
    }
}
