/*
 * Decompiled with CFR 0.152.
 */
package compVuelos;

import java.io.Serializable;

public class Pasaje
implements Serializable {
    private int idpasaje;
    private int pasajerocod;
    private String identificador;
    private int numasiento;
    private String clase;
    private float pvp;
    private String nombre;
    private String pais;

    public Pasaje() {
    }

    public Pasaje(int idpasaje, int pasajerocod, String identificador, int numasiento, String clase, float pvp, String nombre, String pais) {
        this.idpasaje = idpasaje;
        this.pasajerocod = pasajerocod;
        this.identificador = identificador;
        this.numasiento = numasiento;
        this.clase = clase;
        this.pvp = pvp;
        this.nombre = nombre;
        this.pais = pais;
    }

    public int getIdpasaje() {
        return this.idpasaje;
    }

    public void setIdpasaje(int idpasaje) {
        this.idpasaje = idpasaje;
    }

    public int getPasajerocod() {
        return this.pasajerocod;
    }

    public void setPasajerocod(int pasajerocod) {
        this.pasajerocod = pasajerocod;
    }

    public String getIdentificador() {
        return this.identificador;
    }

    public void setIdentificador(String identificador) {
        this.identificador = identificador;
    }

    public int getNumasiento() {
        return this.numasiento;
    }

    public void setNumasiento(int numasiento) {
        this.numasiento = numasiento;
    }

    public String getClase() {
        return this.clase;
    }

    public void setClase(String clase) {
        this.clase = clase;
    }

    public float getPvp() {
        return this.pvp;
    }

    public void setPvp(float pvp) {
        this.pvp = pvp;
    }

    public String getNombre() {
        return this.nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getPais() {
        return this.pais;
    }

    public void setPais(String pais) {
        this.pais = pais;
    }
}
