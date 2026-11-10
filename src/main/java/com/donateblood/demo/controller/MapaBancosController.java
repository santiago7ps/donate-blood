package com.donateblood.demo.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.donateblood.demo.model.BancoMapa;

/**
 * Entrega al mapa del donante la lista de bancos de sangre autorizados de Cartagena
 * (Hemocaribe, Cruz Roja Seccional Bolívar y Nuevo Hospital Bocagrande, según el
 * directorio oficial del Ministerio de Salud / INS) y un puesto fijo de donación.
 * Por ahora es una lista fija (prototipo): no se lee de MongoDB.
 *
 * Las coordenadas son aproximadas; para afinarlas, clic derecho sobre el
 * lugar en Google Maps y copiar los dos números.
 */
@RestController
public class MapaBancosController {

    private static final List<BancoMapa> BANCOS = List.of(
            new BancoMapa(
                    "Banco de Sangre Hemocaribe",
                    "Transversal 54 #30-769, C.C. Plaza Colón, Locales 14 y 15, El Bosque, Cartagena",
                    "605 677 5082",
                    10.4000, -75.5110),
            new BancoMapa(
                    "Cruz Roja Colombiana Seccional Bolívar",
                    "Calle 30 #44D-71, Barrio España, Cartagena",
                    "605 662 72 02",
                    10.4065, -75.5185),
            new BancoMapa(
                    "Banco de Sangre Nuevo Hospital Bocagrande",
                    "Carrera 5 #6-49, Castillogrande, Cartagena",
                    "605 693 3052",
                    10.3985, -75.5565),
            new BancoMapa(
                    "Puesto de donación Hemocaribe - Hospital Universitario del Caribe",
                    "Calle 29 #50-50, Zaragocilla, Cartagena",
                    "605 677 5082",
                    10.4030, -75.5010)
    );

    @GetMapping("/api/bancos")
    public List<BancoMapa> listarBancos() {
        return BANCOS;
    }
}
