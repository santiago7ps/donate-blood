package com.donateblood.demo.controller;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.donateblood.demo.model.Encargo;
import com.donateblood.demo.repository.EncargoRepository;

@Controller
@RequestMapping("/centro")
public class CentroController {

    @Autowired
    private EncargoRepository encargoRepository;

    @GetMapping("/panel")
    public String panelCentro() {
        return "centropanel";
    }

    @GetMapping("/unidades")
    public String verUnidades(Model model) {

        model.addAttribute(
                "unidades",
                BancoController.getUnidadesSangre()
        );

        return "centro_unidades";
    }

    @GetMapping("/encargos")
    public String mostrarEncargos() {

        return "encargos";
    }

    @PostMapping("/encargos")
    public String realizarEncargo(

            @RequestParam(required = false) String tipo,

            @RequestParam(required = false) Integer cantidad,

            Model model) {

        if (tipo == null || tipo.isEmpty()
                || cantidad == null || cantidad <= 0) {

            model.addAttribute(
                    "error",
                    "Debes seleccionar un tipo y una cantidad válida"
            );

            return "encargos";
        }

        String resultado =
                BancoController.procesarPedido(tipo, cantidad);

        if ("ok".equals(resultado)) {

            Encargo encargo = new Encargo();

            encargo.setCentroMedico("Centro Médico");

            encargo.setBancoSangre("Banco Central");

            encargo.setTipoSangre(tipo);

            encargo.setCantidad(cantidad);

            encargo.setFecha(LocalDate.now().toString());

            encargo.setEstado("PENDIENTE");

            encargoRepository.save(encargo);

            model.addAttribute(
                    "exito",
                    "Encargo realizado correctamente"
            );

        } else {

            model.addAttribute("error", resultado);
        }

        return "encargos";
    }
}