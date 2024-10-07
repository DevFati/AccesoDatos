package mortahilchachoufatima;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "ListaViajes")
public class Viajes {

	private ArrayList<Viaje> listaviajes;

	public Viajes(ArrayList<Viaje> listaviajes) {
		super();
		this.listaviajes = listaviajes;
	}

	public Viajes() {
		super();
	}

	@XmlElement(name = "Viaje")
	public ArrayList<Viaje> getListaviajes() {
		return listaviajes;
	}

	public void setListaviajes(ArrayList<Viaje> listaviajes) {
		this.listaviajes = listaviajes;
	}

}
