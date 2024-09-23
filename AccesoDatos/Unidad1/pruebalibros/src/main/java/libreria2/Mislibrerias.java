package libreria2;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "MISLIBRERIAS")
public class Mislibrerias {

	private ArrayList<Libreria> libreria;

	public Mislibrerias() {
	}

	public Mislibrerias(ArrayList<Libreria> libreria) {
		super();
		this.libreria = libreria;
	}

    @XmlElement(name = "Libreria")
    @XmlElementWrapper(name="Mislibrerias")
	public ArrayList<Libreria> getLibreria() {
		return libreria;
	}

	public void setLibreria(ArrayList<Libreria> libreria) {
		this.libreria = libreria;
	}

}
