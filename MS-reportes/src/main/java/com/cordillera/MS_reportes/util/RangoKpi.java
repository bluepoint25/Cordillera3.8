package com.cordillera.MS_reportes.util;

import com.cordillera.MS_reportes.dto.KpiDto;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Rango (semáforo) para los KPIs de PROMEDIO y SUMA en los reportes:
 * 🟢 ÓPTIMO · 🟡 REGULAR · 🔴 MALO.
 *
 * Los umbrales se definen por TIPO DE DATO porque las magnitudes son muy
 * distintas (ventas en millones, producción en miles, etc.). El tipo de dato no
 * se persiste en el KPI, así que se infiere del nombre/descripción.
 *
 * Para COSTOS "menos es mejor" (masEsMejor = false); para el resto, más es mejor.
 * Ajusta la tabla UMBRALES según el negocio.
 */
public enum RangoKpi {

    OPTIMO("🟢", "ÓPTIMO"),
    REGULAR("🟡", "REGULAR"),
    MALO("🔴", "MALO");

    private final String emoji;
    private final String etiqueta;

    RangoKpi(String emoji, String etiqueta) {
        this.emoji = emoji;
        this.etiqueta = etiqueta;
    }

    public String getEmoji()    { return emoji; }
    public String getEtiqueta() { return etiqueta; }

    /** Umbrales de un tipo de dato. Si masEsMejor=false, un valor menor es mejor (ej. COSTOS). */
    private record Umbral(double optimo, double regular, boolean masEsMejor) {}

    // === Tabla de umbrales por tipo de dato (ajustable) ===
    private static final Map<String, Umbral> UMBRALES = Map.of(
            "VENTAS",     new Umbral(1_500_000, 1_000_000, true),
            "COSTOS",     new Umbral(  800_000, 1_000_000, false),
            "PRODUCCION", new Umbral(   10_000,     8_000, true),
            "INVENTARIO", new Umbral(2_000_000, 1_500_000, true)
    );

    /**
     * Devuelve el sufijo de etiqueta para una línea de KPI (ej. "  🟢 ÓPTIMO").
     * Solo aplica a PROMEDIO y SUMA; devuelve "" si no aplica o no se puede clasificar.
     */
    public static String etiquetaPara(KpiDto kpi) {
        if (kpi == null || kpi.getTipoCalculo() == null) return "";
        String tipoCalculo = kpi.getTipoCalculo().toUpperCase();
        if (!tipoCalculo.equals("PROMEDIO") && !tipoCalculo.equals("SUMA")) return "";

        RangoKpi rango = evaluar(inferirTipoDato(kpi), kpi.getValor());
        return rango == null ? "" : "  " + rango.emoji + " " + rango.etiqueta;
    }

    /** Clasifica un valor según los umbrales de su tipo de dato. */
    public static RangoKpi evaluar(String tipoDato, BigDecimal valor) {
        if (tipoDato == null || valor == null) return null;
        Umbral u = UMBRALES.get(tipoDato);
        if (u == null) return null;

        double v = valor.doubleValue();
        if (u.masEsMejor()) {
            if (v >= u.optimo())  return OPTIMO;
            if (v >= u.regular()) return REGULAR;
            return MALO;
        } else {
            if (v <= u.optimo())  return OPTIMO;
            if (v <= u.regular()) return REGULAR;
            return MALO;
        }
    }

    /** Infiere el tipo de dato (VENTAS, COSTOS, ...) desde el nombre/descripción del KPI. */
    public static String inferirTipoDato(KpiDto kpi) {
        String texto = ((kpi.getNombre() != null ? kpi.getNombre() : "") + " "
                + (kpi.getDescripcion() != null ? kpi.getDescripcion() : "")).toUpperCase();
        if (texto.contains("VENTA"))      return "VENTAS";
        if (texto.contains("COSTO"))      return "COSTOS";
        if (texto.contains("PRODUCC"))    return "PRODUCCION";
        if (texto.contains("INVENTARIO")) return "INVENTARIO";
        return null;
    }
}
