package com.cordillera.MS_reportes.factory.impl;

import com.cordillera.MS_reportes.dto.DatoDto;
import com.cordillera.MS_reportes.dto.KpiDto;
import com.cordillera.MS_reportes.dto.ReporteDto;
import com.cordillera.MS_reportes.factory.ReporteContenido;
import com.cordillera.MS_reportes.factory.TipoReporte;
import com.cordillera.MS_reportes.util.RangoKpi;

import java.util.List;

/** ConcreteProduct – genera contenido orientado a KPIs. */
public class ReporteKpiContenido implements ReporteContenido {

    @Override
    public String construirContenido(ReporteDto dto, List<KpiDto> kpis, List<DatoDto> datos) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== REPORTE DE KPIs ===\n");
        sb.append("Titulo: ").append(dto.getTitulo()).append("\n");
        sb.append("Periodo: ").append(dto.getPeriodo()).append("\n\n");

        if (kpis.isEmpty()) {
            sb.append("No se encontraron KPIs para el periodo indicado.\n");
        } else {
            sb.append("KPIs calculados:\n");
            sb.append("Rango (PROMEDIO/SUMA): 🟢 ÓPTIMO · 🟡 REGULAR · 🔴 MALO\n");
            kpis.forEach(k -> sb.append(String.format(
                    "  - %s [%s]: %s %s%s\n",
                    k.getNombre(), k.getTipoCalculo(),
                    k.getValor() != null ? k.getValor().toPlainString() : "N/A",
                    k.getUnidad() != null ? k.getUnidad() : "",
                    RangoKpi.etiquetaPara(k)
            )));
        }

        if (dto.getDescripcionAdicional() != null) {
            sb.append("\nObservaciones: ").append(dto.getDescripcionAdicional());
        }
        return sb.toString();
    }

    @Override
    public TipoReporte getTipo() {
        return TipoReporte.KPI;
    }
}
