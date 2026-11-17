package com.donateblood.demo.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.donateblood.demo.model.Cita;
import com.donateblood.demo.model.Donacion;
import com.donateblood.demo.repository.DonacionRepository;

/**
 * Consulta al microservicio Python (modelo_api/app.py) la probabilidad de que cada cita
 * PENDIENTE se realice, usando el modelo entrenado (árbol de decisión).
 *
 * Las 10 variables se calculan igual que en el cuaderno de entrenamiento: el historial del
 * donante usa solo registros ANTERIORES a la cita. Si el microservicio no responde, la app
 * sigue funcionando: se devuelve null y la pantalla usa la estimación simple por historial.
 */
@Service
public class ModeloPredictivoService {

    /** Acierto aproximado del modelo en la prueba (%). Actualizar si se reentrena. */
    public static final int PRECISION_APROX = 82;

    private static final Logger log = LoggerFactory.getLogger(ModeloPredictivoService.class);
    private static final Pattern HORA = Pattern.compile("^(\\d{1,2}):(\\d{2})$");

    private final DonacionRepository donacionRepository;
    private final RestTemplate http;
    private final String url;

    public ModeloPredictivoService(
            DonacionRepository donacionRepository,
            @Value("${prediccion.url:http://localhost:5000}") String url) {

        this.donacionRepository = donacionRepository;
        this.url = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;

        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(1500);
        fabrica.setReadTimeout(5000);
        this.http = new RestTemplate(fabrica);
    }

    /**
     * @param aPredecir citas pendientes a predecir
     * @param todas     todas las citas (para armar el historial de cada donante)
     * @return mapa id de cita -> probabilidad de asistir (0 a 1); vacío si no hay nada que
     *         predecir; null si el microservicio no está disponible.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Map<String, Double> probabilidadesAsistir(List<Cita> aPredecir, List<Cita> todas) {

        Map<String, Double> resultado = new HashMap<>();
        if (aPredecir.isEmpty()) {
            return resultado;
        }

        // Todas las citas válidas por donante (como el cuaderno: citas_previas cuenta cualquier estado)
        Map<String, List<Cita>> citasPorUsuario = new HashMap<>();
        for (Cita c : todas) {
            if (esValida(c)) {
                citasPorUsuario.computeIfAbsent(c.getUsuario().trim(), k -> new ArrayList<>()).add(c);
            }
        }

        // Donaciones COMPLETADAS por donante
        Map<String, List<LocalDate>> donacionesPorDonante = new HashMap<>();
        for (Donacion d : donacionRepository.findAll()) {
            LocalDate f = parseFecha(d.getFecha());
            if (d.getDonante() != null && f != null && "COMPLETADA".equals(limpiarEstado(d.getEstado()))) {
                donacionesPorDonante.computeIfAbsent(d.getDonante().trim(), k -> new ArrayList<>()).add(f);
            }
        }

        List<Map<String, Object>> filas = new ArrayList<>();
        for (Cita cita : aPredecir) {
            if (cita.getId() == null || !esValida(cita)) {
                continue;
            }
            filas.add(construirVariables(cita, citasPorUsuario, donacionesPorDonante));
        }
        if (filas.isEmpty()) {
            return resultado;
        }

        try {
            Map<String, Object> cuerpo = new HashMap<>();
            cuerpo.put("citas", filas);

            ResponseEntity<Map> respuesta = http.postForEntity(url + "/predecir", cuerpo, Map.class);
            Map<String, Object> datos = respuesta.getBody();
            if (datos == null || !(datos.get("predicciones") instanceof List)) {
                log.warn("Respuesta inesperada del servicio del modelo: {}", datos);
                return null;
            }

            for (Object o : (List<Object>) datos.get("predicciones")) {
                Map<String, Object> p = (Map<String, Object>) o;
                resultado.put(String.valueOf(p.get("id")), ((Number) p.get("probRealizada")).doubleValue());
            }
            return resultado;

        } catch (Exception e) {
            log.warn("Servicio del modelo no disponible en {}: {}", url, e.getMessage());
            return null;
        }
    }

    /** Mismas variables y definiciones que el cuaderno de entrenamiento. */
    private Map<String, Object> construirVariables(
            Cita cita,
            Map<String, List<Cita>> citasPorUsuario,
            Map<String, List<LocalDate>> donacionesPorDonante) {

        LocalDate fecha = parseFecha(cita.getFecha());
        double hora = parseHora(cita.getHora());
        String usuario = cita.getUsuario().trim();

        // citas_previas: TODAS las citas anteriores del donante (cualquier estado).
        // prop_realizadas: realizadas / resueltas (REALIZADA o CANCELADA) entre esas anteriores.
        int citasPrevias = 0;
        int resueltas = 0;
        int realizadas = 0;
        Set<String> vistas = new HashSet<>(); // una sola vez cada (fecha, hora), como el cuaderno
        for (Cita otra : citasPorUsuario.getOrDefault(usuario, List.of())) {
            LocalDate f = parseFecha(otra.getFecha());
            double h = parseHora(otra.getHora());
            boolean anterior = f.isBefore(fecha) || (f.isEqual(fecha) && h < hora);
            if (!anterior || !vistas.add(f + "|" + otra.getHora().trim())) {
                continue;
            }
            citasPrevias++;
            String estado = limpiarEstado(otra.getEstado());
            if ("REALIZADA".equals(estado)) {
                resueltas++;
                realizadas++;
            } else if ("CANCELADA".equals(estado)) {
                resueltas++;
            }
        }
        double propRealizadas = resueltas == 0 ? 0.5 : (double) realizadas / resueltas;

        // donaciones_previas y dias_ultima_donacion: COMPLETADAS con fecha anterior a la cita
        int donacionesPrevias = 0;
        LocalDate ultima = null;
        for (LocalDate f : donacionesPorDonante.getOrDefault(usuario, List.of())) {
            if (f.isBefore(fecha)) {
                donacionesPrevias++;
                if (ultima == null || f.isAfter(ultima)) {
                    ultima = f;
                }
            }
        }
        int sinDonacionPrevia = ultima == null ? 1 : 0;
        double diasUltimaDonacion = ultima == null ? 0.0 : (double) ChronoUnit.DAYS.between(ultima, fecha);

        Map<String, Object> fila = new LinkedHashMap<>();
        fila.put("id", cita.getId());
        fila.put("tipoSangre", cita.getTipoSangre().trim());
        fila.put("lugar", cita.getLugar().trim());
        fila.put("franja", hora < 12 ? "MANANA" : "TARDE");
        fila.put("dia_semana", fecha.getDayOfWeek().getValue() - 1); // lunes = 0, como pandas
        fila.put("mes", fecha.getMonthValue());
        fila.put("citas_previas", citasPrevias);
        fila.put("prop_realizadas", propRealizadas);
        fila.put("donaciones_previas", donacionesPrevias);
        fila.put("dias_ultima_donacion", diasUltimaDonacion);
        fila.put("sin_donacion_previa", sinDonacionPrevia);
        return fila;
    }

    /** Cita con todos los datos que el cuaderno exige (el resto se descartaba al entrenar). */
    private static boolean esValida(Cita c) {
        return c != null
                && !esVacio(c.getUsuario()) && !esVacio(c.getLugar()) && !esVacio(c.getTipoSangre())
                && !esVacio(c.getEstado())
                && parseFecha(c.getFecha()) != null
                && parseHora(c.getHora()) != null;
    }

    private static boolean esVacio(String s) {
        return s == null || s.isBlank();
    }

    private static String limpiarEstado(String estado) {
        return estado == null ? "" : estado.trim().toUpperCase();
    }

    private static LocalDate parseFecha(String fecha) {
        if (fecha == null) return null;
        try {
            return LocalDate.parse(fecha.trim());
        } catch (Exception e) {
            return null;
        }
    }

    /** "08:30" -> 8.5 (igual que hora_num del cuaderno); null si no tiene formato HH:MM. */
    private static Double parseHora(String hora) {
        if (hora == null) return null;
        Matcher m = HORA.matcher(hora.trim());
        if (!m.matches()) return null;
        return Integer.parseInt(m.group(1)) + Integer.parseInt(m.group(2)) / 60.0;
    }
}
