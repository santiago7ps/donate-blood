package com.donateblood.demo.controller;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.donateblood.demo.ml.Prediccion;
import com.donateblood.demo.ml.ResultadoCita;
import com.donateblood.demo.model.Cita;
import com.donateblood.demo.repository.CitaRepository;
import com.donateblood.demo.service.ModeloPredictivoService;

/** Todas las citas pendientes actuales con su probabilidad de donar y paginación. */
@Controller
public class PrediccionController {

    @Autowired
    private CitaRepository citaRepository;

    @Autowired
    private ModeloPredictivoService modeloService;

    @GetMapping("/banco/prediccion")
    public String ver(
            @RequestParam(required = false) String buscar,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "25") int tamanio,
            Model model) {

        List<Cita> todas = citaRepository.findAll();
        Prediccion prediccion = Prediccion.desde(todas);
        LocalDate hoy = LocalDate.now(ZoneId.of("America/Bogota"));

        // Solo citas pendientes de hoy en adelante
        List<Cita> pendientesProximas = todas.stream()
                .filter(c -> "PENDIENTE".equals(c.getEstado()) && !esPasada(c, hoy))
                .collect(Collectors.toList());

        // Modelo entrenado. Si no está disponible, se usa el respaldo por historial.
        Map<String, Double> delModelo = modeloService.probabilidadesAsistir(
                pendientesProximas, todas);

        Map<String, ResultadoCita> resultados = new HashMap<>();
        int conModelo = 0;

        for (Cita c : pendientesProximas) {
            Double p = delModelo == null ? null : delModelo.get(c.getId());

            if (p != null) {
                resultados.put(c.getId(), ResultadoCita.delModelo(
                        p, prediccion.resumen(c.getUsuario())));
                conModelo++;
            } else {
                resultados.put(c.getId(), ResultadoCita.delHistorial(
                        prediccion, c.getUsuario()));
            }
        }

        // Ordenar todas las citas pendientes próximas
        List<Cita> proximas = pendientesProximas.stream()
                .sorted(Comparator.comparing((Cita c) -> c.getFecha())
                        .thenComparing(c -> c.getHora() == null ? "" : c.getHora()))
                .collect(Collectors.toList());

        // Indicadores generales: siempre se calculan con todas las citas, no con la página.
        long altas = proximas.stream()
                .filter(c -> ResultadoCita.ALTA.equals(resultados.get(c.getId()).getEtiqueta()))
                .count();

        long medias = proximas.stream()
                .filter(c -> ResultadoCita.MEDIA.equals(resultados.get(c.getId()).getEtiqueta()))
                .count();

        double esperadas = proximas.stream()
                .mapToInt(c -> resultados.get(c.getId()).getPorcentaje())
                .sum() / 100.0;

        // Buscar exclusivamente por nombre completo, nunca por nombre de usuario.
        String texto = normalizarBusqueda(buscar);

        List<Cita> filtradas = texto.isEmpty()
                ? proximas
                : proximas.stream()
                        .filter(c -> coincideNombre(c.getNombre(), texto))
                        .collect(Collectors.toList());

        // Validar el tamaño de página
        if (tamanio != 25 && tamanio != 50 && tamanio != 100) {
            tamanio = 25;
        }

        // Paginación
        int totalPaginas = (int) Math.ceil((double) filtradas.size() / tamanio);

        if (totalPaginas == 0) {
            pagina = 0;
        } else {
            pagina = Math.max(0, Math.min(pagina, totalPaginas - 1));
        }

        int desde = pagina * tamanio;
        int hasta = Math.min(desde + tamanio, filtradas.size());
        List<Cita> pendientesPagina = filtradas.subList(desde, hasta);

        int paginaInicio = Math.max(0, pagina - 2);
        int paginaFin = Math.min(totalPaginas - 1, pagina + 2);

        model.addAttribute("total", proximas.size());
        model.addAttribute("altas", altas);
        model.addAttribute("medias", medias);
        model.addAttribute("bajas", proximas.size() - altas - medias);
        model.addAttribute("esperadas", Math.round(esperadas));

        model.addAttribute("encontradas", filtradas.size());
        model.addAttribute("pendientes", pendientesPagina);
        model.addAttribute("buscar", buscar == null ? "" : buscar);
        model.addAttribute("resultados", resultados);

        model.addAttribute("paginaActual", pagina);
        model.addAttribute("totalPaginas", totalPaginas);
        model.addAttribute("tamanioPagina", tamanio);
        model.addAttribute("paginaInicio", paginaInicio);
        model.addAttribute("paginaFin", paginaFin);
        model.addAttribute("desde", filtradas.isEmpty() ? 0 : desde + 1);
        model.addAttribute("hasta", hasta);

        model.addAttribute("usaModelo", conModelo > 0);
        model.addAttribute("conHistorial", proximas.size() - conModelo);
        model.addAttribute("precisionModelo", ModeloPredictivoService.PRECISION_APROX);
        model.addAttribute("umbralAlta", ResultadoCita.UMBRAL_ALTA);
        model.addAttribute("umbralMedia", ResultadoCita.UMBRAL_MEDIA);

        return "banco_prediccion";
    }

    private static boolean esPasada(Cita c, LocalDate hoy) {
        try {
            return LocalDate.parse(c.getFecha()).isBefore(hoy);
        } catch (Exception e) {
            return true;
        }
    }

    // Unificar comas, guiones, puntos y espacios repetidos.
    private static String normalizarBusqueda(String texto) {
        if (texto == null || texto.isBlank()) {
            return "";
        }

        return texto.trim()
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .trim()
                .toLowerCase();
    }

    // Todas las palabras escritas deben aparecer en el nombre, en cualquier orden.
    private static boolean coincideNombre(String nombre, String busqueda) {
        String nombreNormalizado = normalizarBusqueda(nombre);

        if (nombreNormalizado.isEmpty()) {
            return false;
        }

        for (String palabra : busqueda.split("\\s+")) {
            if (!nombreNormalizado.contains(palabra)) {
                return false;
            }
        }

        return true;
    }
}
