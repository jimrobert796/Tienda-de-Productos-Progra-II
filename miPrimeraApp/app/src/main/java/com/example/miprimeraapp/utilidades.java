package com.example.miprimeraapp;

import java.util.Base64;

public class utilidades {
    static String url_consulta = "http://10.142.35.151:5984/wilfredo/_design/wilfredo/_view/wilfredo";

    // "http://10.148.226.151:5984/tienda_producto/_design/tienda_producto/_view/tienda_producto"
    static String url_mantenimiento = "http://10.142.35.151:5984/wilfredo"; // CRUD Insertar, Actualizar, Borrar y Buscar
    //http://10.148.226.151:5984/tienda_producto
    //http://10.142.35.151/
    static String user = "jimbo";
    static String passwd = "070906";
    static String credencialesCodificadas = Base64.getEncoder().encodeToString((user + ":" + passwd).getBytes());

    public String generarUnicoId() {
        return java.util.UUID.randomUUID().toString();
    }
}

