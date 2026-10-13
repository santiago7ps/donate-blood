package com.donateblood.demo;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class Router {

    @GetMapping("/")
    public String landing() {
       return "vistasPagina/landing";
    }

    @GetMapping("/info")
    public String info() {
        return "vistasPagina/projectInfo";
    }

    @GetMapping("/verBancos")
    public String verBancos() {
        return "verBancos";
    }

    @GetMapping("/pedidos")
    public String pedidos() {
        return "pedidos";
    }
}