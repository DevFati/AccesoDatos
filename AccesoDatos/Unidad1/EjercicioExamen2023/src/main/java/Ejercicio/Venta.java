package Ejercicio;

import javax.xml.bind.annotation.XmlType;

@XmlType(propOrder = {"unidadesvendidas","fecha","importe"})

public class Venta {

	private int unidadesvendidas;
	private String fecha;
	private double importe;
	public Venta(int unidadesvendidas, String fecha, double importe) {
		super();
		this.unidadesvendidas = unidadesvendidas;
		this.fecha = fecha;
		this.importe = importe;
	}
	public Venta() {
		super();
	}
	public int getUnidadesvendidas() {
		return unidadesvendidas;
	}
	public void setUnidadesvendidas(int unidadesvendidas) {
		this.unidadesvendidas = unidadesvendidas;
	}
	public String getFecha() {
		return fecha;
	}
	public void setFecha(String fecha) {
		this.fecha = fecha;
	}
	public double getImporte() {
		return importe;
	}
	public void setImporte(double importe) {
		this.importe = importe;
	}
	
	
	
	
}
