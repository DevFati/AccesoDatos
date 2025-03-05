/*
 * Decompiled with CFR 0.152.
 */
package compVuelos;

import java.io.Serializable;

public class Vuelos
implements Serializable {
    private String identificador;
    private String aeropuertoorigen;
    private String nombreorigen;
    private String paisorigen;
    private String aeropuertodestino;
    private String nombredestino;
    private String paisdestino;
    private String tipovuelo;
    private String fechavuelo;
    private int descuento;
    private int numero;

    public Vuelos() {
    }

    public Vuelos(String identificador, String aeropuertoorigen, String nombreorigen, String paisorigen, String aeropuertodestino, String nombredestino, String paisdestino, String tipovuelo, String fechavuelo, int descuento, int numero) {
        this.identificador = identificador;
        this.aeropuertoorigen = aeropuertoorigen;
        this.nombreorigen = nombreorigen;
        this.paisorigen = paisorigen;
        this.aeropuertodestino = aeropuertodestino;
        this.nombredestino = nombredestino;
        this.paisdestino = paisdestino;
        this.tipovuelo = tipovuelo;
        this.fechavuelo = fechavuelo;
        this.descuento = descuento;
        this.numero = numero;
    }

    public String getIdentificador() {
        return this.identificador;
    }

    public void setIdentificador(String identificador) {
        this.identificador = identificador;
    }

    public String getAeropuertoorigen() {
        return this.aeropuertoorigen;
    }

    public void setAeropuertoorigen(String aeropuertoorigen) {
        this.aeropuertoorigen = aeropuertoorigen;
    }

    public String getNombreorigen() {
        return this.nombreorigen;
    }

    public void setNombreorigen(String nombreorigen) {
        this.nombreorigen = nombreorigen;
    }

    public String getPaisorigen() {
        return this.paisorigen;
    }

    public void setPaisorigen(String paisorigen) {
        this.paisorigen = paisorigen;
    }

    public String getAeropuertodestino() {
        return this.aeropuertodestino;
    }

    public void setAeropuertodestino(String aeropuertodestino) {
        this.aeropuertodestino = aeropuertodestino;
    }

    public String getNombredestino() {
        return this.nombredestino;
    }

    public void setNombredestino(String nombredestino) {
        this.nombredestino = nombredestino;
    }

    public String getPaisdestino() {
        return this.paisdestino;
    }

    public void setPaisdestino(String paisdestino) {
        this.paisdestino = paisdestino;
    }

    public String getTipovuelo() {
        return this.tipovuelo;
    }

    public void setTipovuelo(String tipovuelo) {
        this.tipovuelo = tipovuelo;
    }

    public String getFechavuelo() {
        return this.fechavuelo;
    }

    public void setFechavuelo(String fechavuelo) {
        this.fechavuelo = fechavuelo;
    }

    public int getDescuento() {
        return this.descuento;
    }

    public void setDescuento(int descuento) {
        this.descuento = descuento;
    }

    public int getNumero() {
        return this.numero;
    }

    public void setNumero(int numero) {
        this.numero = numero;
    }
}
