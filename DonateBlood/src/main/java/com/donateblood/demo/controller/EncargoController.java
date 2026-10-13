package com.donateblood.demo.controller;

import com.donateblood.demo.model.Encargo;
import com.donateblood.demo.repository.EncargoRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/encargos")
@CrossOrigin("*")
public class EncargoController {

    @Autowired
    private EncargoRepository repository;

    @GetMapping
    public List<Encargo> obtenerEncargos() {
        return repository.findAll();
    }

    @PostMapping
    public Encargo crearEncargo(@RequestBody Encargo encargo) {
        return repository.save(encargo);
    }
}