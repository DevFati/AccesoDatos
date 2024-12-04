package clases;

import java.math.BigInteger;

public class TotalCliente {

	private BigInteger codcliente;
	private String nombre;
	private Long contador; 
	private Double suma;
	public BigInteger getCodcliente() {
		return codcliente;
	}
	public void setCodcliente(BigInteger codcliente) {
		this.codcliente = codcliente;
	}
	public String getNombre() {
		return nombre;
	}
	public void setNombre(String nombre) {
		this.nombre = nombre;
	}
	public Long getContador() {
		return contador;
	}
	public void setContador(Long contador) {
		this.contador = contador;
	}
	public Double getSuma() {
		return suma;
	}
	public void setSuma(Double suma) {
		this.suma = suma;
	}
	public TotalCliente(BigInteger codcliente, String nombre, Long contador, Double suma) {
		super();
		this.codcliente = codcliente;
		this.nombre = nombre;
		this.contador = contador;
		this.suma = suma;
	}
	public TotalCliente() {
		super();
	}
	@Override
	public String toString() {
		return "TotalCliente [codcliente=" + codcliente + ", nombre=" + nombre + ", contador=" + contador + ", suma="
				+ suma + "]";
	} 
	
	
	
	
}
