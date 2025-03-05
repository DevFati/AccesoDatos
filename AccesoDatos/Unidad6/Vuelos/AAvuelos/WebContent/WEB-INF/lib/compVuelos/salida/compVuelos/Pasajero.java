/*
 * Decompiled with CFR 0.152.
 */
package compVuelos;

import java.io.Serializable;

public class Pasajero
implements Serializable {
    private int pasajerocod;
    private String nombre;
    private String tlf;
    private String direccion;
    private String pais;

    public Pasajero() {
    }

    public Pasajero(int pasajerocod, String nombre, String tlf, String direccion, String pais) {
        this.pasajerocod = pasajerocod;
        this.nombre = nombre;
        this.tlf = tlf;
        this.direccion = direccion;
        this.pais = pais;
    }

    public int getPasajerocod() {
        return this.pasajerocod;
    }

    public void setPasajerocod(int pasajerocod) {
        this.pasajerocod = pasajerocod;
    }

    public String getNombre() {
        return this.nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getTlf() {
        return this.tlf;
    }

    public void setTlf(String tlf) {
        this.tlf = tlf;
    }

    public String getDireccion() {
        return this.direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getPais() {
        return this.pais;
    }

    public void setPais(String pais) {
        this.pais = pais;
    }
}
