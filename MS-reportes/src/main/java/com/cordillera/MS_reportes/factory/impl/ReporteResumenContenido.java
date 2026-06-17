package com.cordillera.MS_reportes.factory.impl;

import com.cordillera.MS_reportes.dto.DatoDto;
import com.cordillera.MS_reportes.dto.KpiDto;
import com.cordillera.MS_reportes.dto.ReporteDto;
import com.cordillera.MS_reportes.factory.ReporteContenido;
import com.cordillera.MS_reportes.factory.TipoReporte;
import com.cordillera.MS_reportes.util.RangoKpi;

import java.util.List;

/** ConcreteProduct – genera un resumen ejecutivo consolidado. */
public class ReporteResumenContenido implements ReporteContenido {

    @Override
    public String construirContenido(ReporteDto dto, List<KpiDto> kpis, List<DatoDto> datos) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== RESUMEN EJECUTIVO ===\n");
        sb.append("Titulo: ").append(dto.getTitulo()).append("\n");
        sb.append("Periodo: ").append(dto.getPeriodo()).append("\n\n");

        sb.append("Total de datos ingresados: ").append(datos.size()).append("\n");
        sb.append("Total de KPIs calculados:  ").append(kpis.size()).append("\n\n");

        if (!kpis.isEmpty()) {
            sb.append("Principales indicadores (rango PROMEDIO/SUMA: 🟢 ÓPTIMO · 🟡 REGULAR · 🔴 MALO):\n");
            kpis.stream().limit(5).forEach(k ->
                    sb.append(String.format("  • %s: %s %s%s\n",
                            k.getNombre(),
                            k.getValor() != null ? k.getValor().toPlainString() : "N/A",
                            k.getUnidad() != null ? k.getUnidad() : "",
                            RangoKpi.etiquetaPara(k)))
            );
        }

        if (dto.getDescripcionAdicional() != null) {
            sb.append("\nConclusiones: ").append(dto.getDescripcionAdicional());
        }
        return sb.toString();
    }

    @Override
    public TipoReporte getTipo() {
        return TipoReporte.RESUMEN;
    }
}
