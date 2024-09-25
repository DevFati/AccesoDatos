package ventasarticulo;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "ventasarticulos")
public class Ventasarticulos {
	
    private Articulo articulo;
	private ArrayList<Venta> listaventas;
	
	
	public Ventasarticulos() {}
	
	public Ventasarticulos(Articulo articulo, ArrayList<Venta> listaventas) {
		super();
		this.articulo = articulo;
		this.listaventas = listaventas;
	}


	public Articulo getArticulo() {
		return articulo;
	}


	public void setArticulo(Articulo articulo) {
		this.articulo = articulo;
	}


	@XmlElementWrapper(name = "ventas")  
	@XmlElement(name = "venta")
	public ArrayList<Venta> getListaventas() {
		return listaventas;
	}
	
	
	public void setListaventas(ArrayList<Venta> listaventas) {
		this.listaventas = listaventas;
	}
}
