package nuevosdept;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="Nuevosdepartamentos")
public class Nuevosdepartamentos {

	private ArrayList<Departamento> lista;

	public Nuevosdepartamentos() {}
	
	public Nuevosdepartamentos(ArrayList<Departamento> lista) {
		super();
		this.lista = lista;
	}

	@XmlElement(name="departamento")
	public ArrayList<Departamento> getLista() {
		return lista;
	}

	public void setLista(ArrayList<Departamento> lista) {
		this.lista = lista;
	}
	
}
