package Ejercicio;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

@XmlType(propOrder = {"codigo", "nombre", "existencias", "precio","listaventas"})

public class Producto2 {
	

	private int codigo;
	private String nombre;
	private int existencias;
	private double precio;
	
	private ArrayList<Venta> listaventas;
	public Producto2(int codigo, String nombre, int existencias, double precio) {
		super();
		this.codigo = codigo;
		this.nombre = nombre;
		this.existencias = existencias;
		this.precio = precio;
		this.listaventas=new ArrayList<Venta>();
	}
	
	public Producto2(int codigo, String nombre, int existencias, double precio, ArrayList<Venta> listaventas) {
		super();
		this.codigo = codigo;
		this.nombre = nombre;
		this.existencias = existencias;
		this.precio = precio;
		this.listaventas = listaventas;
	}

	public Producto2() {
		super();
	}

	public int getCodigo() {
		return codigo;
	}

	public void setCodigo(int codigo) {
		this.codigo = codigo;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public int getExistencias() {
		return existencias;
	}

	public void setExistencias(int existencias) {
		this.existencias = existencias;
	}

	public double getPrecio() {
		return precio;
	}

	public void setPrecio(double precio) {
		this.precio = precio;
	}

	@XmlElementWrapper(name = "listaventa")  
	@XmlElement(name = "venta")
	public ArrayList<Venta> getListaventas() {
		return listaventas;
	}

	public void setListaventas(ArrayList<Venta> listaventas) {
		this.listaventas = listaventas;
	}
	
	
	
	
	
}
