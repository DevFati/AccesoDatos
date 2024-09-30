package Ejercicio;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement()
public class Productos {
	private ArrayList<Producto> lista;

	public Productos(ArrayList<Producto> lista) {
		super();
		this.lista = lista;
	}

	public Productos() {
		super();
	}

	public ArrayList<Producto> getLista() {
		return lista;
	}

	public void setLista(ArrayList<Producto> lista) {
		this.lista = lista;
	}
	
}
