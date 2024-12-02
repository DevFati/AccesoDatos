package clases;

import java.math.BigInteger;

public class Totales {

	private BigInteger numero;
	private Long cuenta;
	private Double media;
	private String nombre;
	
	public Totales() {}
	
	
	@Override
	public String toString() {
		return "Totales [numero=" + numero + ", cuenta=" + cuenta + ", media=" + media + ", nombre=" + nombre + "]";
	}
	public Totales(BigInteger numero, Long cuenta, Double media, String nombre) {
		super();
		this.numero = numero;
		this.cuenta = cuenta;
		this.media = media;
		this.nombre = nombre;
	}
	public BigInteger getNumero() {
		return numero;
	}
	public void setNumero(BigInteger numero) {
		this.numero = numero;
	}
	public Long getCuenta() {
		return cuenta;
	}
	public void setCuenta(Long cuenta) {
		this.cuenta = cuenta;
	}
	public Double getMedia() {
		return media;
	}
	public void setMedia(Double media) {
		this.media = media;
	}
	public String getNombre() {
		return nombre;
	}
	public void setNombre(String nombre) {
		this.nombre = nombre;
	}
	
	

	
}
