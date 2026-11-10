package com.donateblood.demo.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "usuarios")
public class Usuario {

    @Id
    private String id;
    private String nombre;

    @Indexed(unique = true)
    private String username;
    private String tipoDocumento;
    private String documento;
    private String numPhone;
    private Integer edad;
    private String email;
    private String password;
    private String rol;

    // Solo aplica a ROLE_BANCO: médicos/cuartos = donantes máximos por fecha y hora
    private Integer capacidadAtencion;

    public Usuario(){}

    public Usuario(String username, String password, String rol) {
        this.username = username;
        this.password = password;
        this.rol = rol;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(String tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public String getDocumento() {
        return documento;
    }

    public void setDocumento(String documento) {
        this.documento = documento;
    }

    public String getNumPhone() {
        return numPhone;
    }

    public void setNumPhone(String numPhone) {
        this.numPhone = numPhone;
    }

    public Integer getEdad() {
        return edad;
    }

    public void setEdad(Integer edad) {
        this.edad = edad;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public Integer getCapacidadAtencion() {
        return capacidadAtencion;
    }

    public void setCapacidadAtencion(Integer capacidadAtencion) {
        this.capacidadAtencion = capacidadAtencion;
    }

    /** Un donante tiene el perfil completo cuando ya registró documento, teléfono y edad. */
    public boolean tienePerfilCompleto() {
        return documento != null && !documento.isBlank()
                && numPhone != null && !numPhone.isBlank()
                && edad != null;
    }
}
