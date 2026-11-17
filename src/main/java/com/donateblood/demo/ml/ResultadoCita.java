package com.donateblood.demo.ml;

/**
 * Resultado de una cita pendiente para mostrar en pantalla: probabilidad de donar (en %),
 * nivel (Alta, Media o Baja) y una frase corta con el pronóstico.
 *
 * Dos orígenes:
 *  - delModelo:    probabilidad del modelo entrenado (servicio Python).
 *  - delHistorial: respaldo con la fórmula simple de Prediccion (si el modelo no está disponible).
 *
 * Ambos usan los mismos umbrales, para que el nivel signifique lo mismo en los dos casos.
 */
public class ResultadoCita {

    /** Umbrales (en %). Se ajustan aquí si se reentrena el modelo. */
    public static final int UMBRAL_ALTA = 70;   // desde aquí: probabilidad ALTA
    public static final int UMBRAL_MEDIA = 40;  // desde aquí hasta ALTA: MEDIA; por debajo: BAJA

    public static final String ALTA = "Alta";
    public static final String MEDIA = "Media";
    public static final String BAJA = "Baja";

    private final int porcentaje;
    private final String etiqueta;     // Alta | Media | Baja  (probabilidad de donar)
    private final String riesgo;       // solo para el color de la etiqueta: bajo (verde) | medio | alto (rojo)
    private final String interpretacion;
    private final boolean delModelo;

    private ResultadoCita(int porcentaje, String historial, boolean delModelo) {
        this.porcentaje = porcentaje;
        this.delModelo = delModelo;
        String frase;
        if (porcentaje >= UMBRAL_ALTA) {
            etiqueta = ALTA;
            riesgo = "bajo";
            frase = "Es muy probable que complete su donación";
        } else if (porcentaje >= UMBRAL_MEDIA) {
            etiqueta = MEDIA;
            riesgo = "medio";
            frase = "Puede asistir o no: no es seguro";
        } else {
            etiqueta = BAJA;
            riesgo = "alto";
            frase = "Es poco probable que complete su donación";
        }
        this.interpretacion = frase + " (" + historial + ").";
    }

    /**
     * @param probAsistir probabilidad de REALIZADA, entre 0 y 1
     * @param historial   texto corto del historial, por ejemplo "cumplió 3 de 4 citas"
     */
    public static ResultadoCita delModelo(double probAsistir, String historial) {
        return new ResultadoCita((int) Math.round(probAsistir * 100), historial, true);
    }

    public static ResultadoCita delHistorial(Prediccion formula, String donante) {
        return new ResultadoCita(formula.porcentaje(donante), formula.resumen(donante), false);
    }

    public int getPorcentaje() { return porcentaje; }
    public String getEtiqueta() { return etiqueta; }
    public String getRiesgo() { return riesgo; }
    public String getInterpretacion() { return interpretacion; }
    public boolean isDelModelo() { return delModelo; }
}
