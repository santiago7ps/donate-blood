package com.donateblood.demo.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.springframework.stereotype.Service;

import com.donateblood.demo.model.Cita;
import com.donateblood.demo.model.Usuario;
import com.donateblood.demo.repository.CitaRepository;
import com.donateblood.demo.repository.UsuarioRepository;
import com.donateblood.demo.util.CitasYSangre;

/**
 * Control de cupos de las citas. Cada banco define:
 *  - los días de la semana que atiende,
 *  - cuántos donantes puede atender en cada franja de 30 minutos (si no define una, usa su capacidad base).
 * Las citas CANCELADAS liberan el cupo; PENDIENTE y REALIZADA lo ocupan.
 */
@Service
public class CapacidadService {

    /** Capacidad usada si un banco aún no ha configurado la suya. */
    public static final int CAPACIDAD_POR_DEFECTO = 5;
    public static final int CAPACIDAD_MAXIMA = 100;

    public static final String[] DIAS_CORTOS = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");

    /** Con 30 % o menos de la capacidad libre la franja se marca como "pocos cupos". */
    private static final int PORCENTAJE_POCOS = 30;

    /**
     * Estado de una franja para mostrarla: DISPONIBLE, POCOS, LLENA, CERRADA
     * (el banco no atiende ese día o esa hora) o PASADA (la hora ya empezó).
     */
    public record FranjaInfo(String hora, String fin, int capacidad, int ocupados, int libres, String estado) {
        // Getters estilo JavaBean para que las plantillas Thymeleaf lean f.hora, f.libres, etc.
        public String getHora() { return hora; }
        public String getFin() { return fin; }
        public int getCapacidad() { return capacidad; }
        public int getOcupados() { return ocupados; }
        public int getLibres() { return libres; }
        public String getEstado() { return estado; }
    }

    /** Resumen de un día para el selector de fechas: DISPONIBLE, LLENO o CERRADO. */
    public record DiaInfo(String fecha, int libres, String estado) {
        public String getFecha() { return fecha; }
        public int getLibres() { return libres; }
        public String getEstado() { return estado; }
    }

    private final CitaRepository citaRepository;
    private final UsuarioRepository usuarioRepository;

    // Evita que dos solicitudes simultáneas tomen el último cupo (una sola instancia de la app).
    private final Object lock = new Object();

    public CapacidadService(CitaRepository citaRepository, UsuarioRepository usuarioRepository) {
        this.citaRepository = citaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    // ---------------------------------------------------------------- bancos

    /** "lugar" de la cita es el nombre del banco (o su username si no tiene nombre). */
    private Usuario buscarBanco(String lugar) {
        if (lugar == null || lugar.isBlank()) return null;
        Usuario banco = usuarioRepository.findFirstByRolAndNombre("ROLE_BANCO", lugar);
        if (banco == null) {
            Usuario porUsername = usuarioRepository.findByUsername(lugar);
            if (porUsername != null && "ROLE_BANCO".equals(porUsername.getRol())) {
                banco = porUsername;
            }
        }
        return banco;
    }

    private Usuario bancoPorUsername(String username) {
        Usuario banco = usuarioRepository.findByUsername(username);
        return (banco != null && "ROLE_BANCO".equals(banco.getRol())) ? banco : null;
    }

    /** Nombre con el que las citas identifican a este banco. */
    public String lugarDe(String username) {
        Usuario banco = usuarioRepository.findByUsername(username);
        if (banco == null) return username;
        return (banco.getNombre() != null && !banco.getNombre().isBlank()) ? banco.getNombre() : banco.getUsername();
    }

    // ------------------------------------------------------------ capacidad

    private int baseDe(Usuario banco) {
        Integer c = banco == null ? null : banco.getCapacidadAtencion();
        return (c != null && c > 0) ? c : CAPACIDAD_POR_DEFECTO;
    }

    /** Capacidad base del banco (la que se usa en las franjas que no tienen un valor propio). */
    public int capacidadDe(String lugar) {
        return baseDe(buscarBanco(lugar));
    }

    public int capacidadDeUsername(String username) {
        return baseDe(usuarioRepository.findByUsername(username));
    }

    /** Cupos configurados para una franja; sin configuración usa la capacidad base. */
    private int capacidadFranja(Usuario banco, String hora) {
        int base = baseDe(banco);
        if (banco == null || banco.getCuposPorFranja() == null) return base;
        Integer c = banco.getCuposPorFranja().get(hora);
        return c == null ? base : Math.max(0, Math.min(c, CAPACIDAD_MAXIMA));
    }

    private boolean atiendeEseDia(Usuario banco, LocalDate dia) {
        if (banco == null || banco.getDiasAtencion() == null || banco.getDiasAtencion().isEmpty()) return true;
        return banco.getDiasAtencion().contains(dia.getDayOfWeek().getValue());
    }

    /** Días que atiende el banco (1 = lunes ... 7 = domingo); si no configuró ninguno, todos. */
    public List<Integer> diasAtencionDe(String username) {
        Usuario banco = usuarioRepository.findByUsername(username);
        List<Integer> dias = banco == null ? null : banco.getDiasAtencion();
        if (dias == null || dias.isEmpty()) return List.of(1, 2, 3, 4, 5, 6, 7);
        return new ArrayList<>(new TreeSet<>(dias));
    }

    /** Cupos de todas las franjas del banco (rellena con la capacidad base las que no tienen valor). */
    public Map<String, Integer> cuposConfigurados(String username) {
        Usuario banco = usuarioRepository.findByUsername(username);
        Map<String, Integer> resultado = new LinkedHashMap<>();
        for (String hora : CitasYSangre.franjasValidas()) {
            resultado.put(hora, capacidadFranja(banco, hora));
        }
        return resultado;
    }

    /** Texto corto de los días de atención, p. ej. "Lun a Sáb" o "Lun, Mié, Vie". */
    public String diasTexto(String lugar) {
        Usuario banco = buscarBanco(lugar);
        List<Integer> dias = (banco == null || banco.getDiasAtencion() == null || banco.getDiasAtencion().isEmpty())
                ? List.of(1, 2, 3, 4, 5, 6, 7)
                : new ArrayList<>(new TreeSet<>(banco.getDiasAtencion()));
        if (dias.size() == 7) return "Todos los días";

        boolean consecutivos = true;
        for (int i = 1; i < dias.size(); i++) {
            if (dias.get(i) != dias.get(i - 1) + 1) {
                consecutivos = false;
                break;
            }
        }
        if (consecutivos && dias.size() > 2) {
            return DIAS_CORTOS[dias.get(0) - 1] + " a " + DIAS_CORTOS[dias.get(dias.size() - 1) - 1];
        }
        List<String> nombres = new ArrayList<>();
        for (int d : dias) nombres.add(DIAS_CORTOS[d - 1]);
        return String.join(", ", nombres);
    }

    // -------------------------------------------------------- disponibilidad

    private LocalDate parsear(String fecha) {
        try {
            return fecha == null ? null : LocalDate.parse(fecha);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean yaPaso(LocalDate dia, String hora) {
        LocalDate hoy = LocalDate.now(ZONA);
        if (dia.isBefore(hoy)) return true;
        if (dia.isAfter(hoy)) return false;
        return !LocalTime.parse(hora).isAfter(LocalTime.now(ZONA));
    }

    private String fin(String hora) {
        LocalTime t = LocalTime.parse(hora).plusMinutes(30);
        return String.format("%02d:%02d", t.getHour(), t.getMinute());
    }

    private Map<String, Long> ocupadosPorHora(String lugar, String fecha, String excluirId) {
        Map<String, Long> usados = new LinkedHashMap<>();
        for (Cita c : citaRepository.findByLugarAndFecha(lugar, fecha)) {
            if ("CANCELADA".equals(c.getEstado())) continue;
            if (excluirId != null && excluirId.equals(c.getId())) continue;
            usados.merge(c.getHora(), 1L, Long::sum);
        }
        return usados;
    }

    /** Todas las franjas válidas de ese banco y fecha, con capacidad, ocupación y estado. */
    public List<FranjaInfo> detalleHoras(String lugar, String fecha, String excluirId) {
        Usuario banco = buscarBanco(lugar);
        LocalDate dia = parsear(fecha);
        boolean atiende = dia != null && atiendeEseDia(banco, dia);
        Map<String, Long> usados = dia == null ? Map.of() : ocupadosPorHora(lugar, fecha, excluirId);

        List<FranjaInfo> filas = new ArrayList<>();
        for (String hora : CitasYSangre.franjasValidas()) {
            int capacidad = atiende ? capacidadFranja(banco, hora) : 0;
            int ocupados = usados.getOrDefault(hora, 0L).intValue();
            int libres = Math.max(0, capacidad - ocupados);

            String estado;
            if (capacidad == 0) {
                estado = "CERRADA";
                libres = 0;
            } else if (yaPaso(dia, hora)) {
                estado = "PASADA";
                libres = 0;
            } else if (libres == 0) {
                estado = "LLENA";
            } else if (libres * 100 <= capacidad * PORCENTAJE_POCOS) {
                estado = "POCOS";
            } else {
                estado = "DISPONIBLE";
            }
            filas.add(new FranjaInfo(hora, fin(hora), capacidad, ocupados, libres, estado));
        }
        return filas;
    }

    /** Próximos días (desde "desde") con los cupos que quedan en total; sirve para elegir fecha de un vistazo. */
    public List<DiaInfo> resumenDias(String lugar, LocalDate desde, int cantidad) {
        List<DiaInfo> dias = new ArrayList<>();
        for (int i = 0; i < cantidad; i++) {
            String fecha = desde.plusDays(i).toString();
            List<FranjaInfo> franjas = detalleHoras(lugar, fecha, null);
            int libres = franjas.stream().mapToInt(FranjaInfo::libres).sum();
            boolean abierto = franjas.stream().anyMatch(f -> !"CERRADA".equals(f.estado()) && !"PASADA".equals(f.estado()));
            String estado = !abierto ? "CERRADO" : (libres == 0 ? "LLENO" : "DISPONIBLE");
            dias.add(new DiaInfo(fecha, libres, estado));
        }
        return dias;
    }

    /** Estado de una franja concreta; null si la hora no es una franja válida. */
    public FranjaInfo franja(String lugar, String fecha, String hora, String excluirId) {
        if (lugar == null || fecha == null || hora == null) return null;
        return detalleHoras(lugar, fecha, excluirId).stream()
                .filter(f -> f.hora().equals(hora))
                .findFirst()
                .orElse(null);
    }

    public int cuposLibres(String lugar, String fecha, String hora, String excluirId) {
        FranjaInfo f = franja(lugar, fecha, hora, excluirId);
        return f == null ? 0 : f.libres();
    }

    /** Todas las franjas válidas con la cantidad de cupos que les quedan ese día (0 si están cerradas o ya pasaron). */
    public Map<String, Integer> cuposPorHora(String lugar, String fecha, String excluirId) {
        Map<String, Integer> resultado = new LinkedHashMap<>();
        for (FranjaInfo f : detalleHoras(lugar, fecha, excluirId)) {
            resultado.put(f.hora(), f.libres());
        }
        return resultado;
    }

    public List<String> horasConCupo(String lugar, String fecha, String excluirId) {
        List<String> libres = new ArrayList<>();
        cuposPorHora(lugar, fecha, excluirId).forEach((hora, cupos) -> {
            if (cupos > 0) libres.add(hora);
        });
        return libres;
    }

    /** Guarda la cita solo si la hora aún tiene cupo. Devuelve false si ya está llena, cerrada o pasada. */
    public boolean guardarSiHayCupo(Cita cita) {
        synchronized (lock) {
            if (cuposLibres(cita.getLugar(), cita.getFecha(), cita.getHora(), cita.getId()) <= 0) {
                return false;
            }
            citaRepository.save(cita);
            return true;
        }
    }

    // ---------------------------------------------------------- configuración

    public void actualizarCapacidad(String username, int capacidad) {
        Usuario banco = bancoPorUsername(username);
        if (banco == null) {
            throw new IllegalArgumentException("El usuario no es un banco de sangre");
        }
        if (capacidad < 1 || capacidad > CAPACIDAD_MAXIMA) {
            throw new IllegalArgumentException("La capacidad debe estar entre 1 y " + CAPACIDAD_MAXIMA);
        }
        banco.setCapacidadAtencion(capacidad);
        usuarioRepository.save(banco);
    }

    /**
     * Guarda la disponibilidad completa del banco: capacidad base, días de atención y cupos por franja.
     * Un cupo de 0 cierra esa franja.
     */
    public void actualizarDisponibilidad(String username, int capacidadBase, List<Integer> dias, Map<String, Integer> cupos) {
        Usuario banco = bancoPorUsername(username);
        if (banco == null) {
            throw new IllegalArgumentException("El usuario no es un banco de sangre");
        }
        if (capacidadBase < 1 || capacidadBase > CAPACIDAD_MAXIMA) {
            throw new IllegalArgumentException("La capacidad base debe estar entre 1 y " + CAPACIDAD_MAXIMA);
        }
        if (dias == null || dias.isEmpty()) {
            throw new IllegalArgumentException("Selecciona al menos un día de atención");
        }
        TreeSet<Integer> diasLimpios = new TreeSet<>();
        for (Integer d : dias) {
            if (d == null || d < 1 || d > 7) {
                throw new IllegalArgumentException("Día de atención inválido");
            }
            diasLimpios.add(d);
        }

        Map<String, Integer> limpios = new LinkedHashMap<>();
        boolean algunaAbierta = false;
        for (String hora : CitasYSangre.franjasValidas()) {
            Integer c = cupos == null ? null : cupos.get(hora);
            int valor = c == null ? capacidadBase : c;
            if (valor < 0 || valor > CAPACIDAD_MAXIMA) {
                throw new IllegalArgumentException("Los cupos de " + hora + " deben estar entre 0 y " + CAPACIDAD_MAXIMA);
            }
            if (valor > 0) algunaAbierta = true;
            limpios.put(hora, valor);
        }
        if (!algunaAbierta) {
            throw new IllegalArgumentException("Deja al menos una franja con cupos mayores a 0");
        }

        banco.setCapacidadAtencion(capacidadBase);
        banco.setDiasAtencion(new ArrayList<>(diasLimpios));
        banco.setCuposPorFranja(limpios);
        usuarioRepository.save(banco);
    }
}
