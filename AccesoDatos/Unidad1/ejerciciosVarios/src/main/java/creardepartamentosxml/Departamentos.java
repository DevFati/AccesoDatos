package creardepartamentosxml;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement()
public class Departamentos {

	private ArrayList<Dep> lista;

	public ArrayList<Dep> getLista() {
		return lista;
	}

	public void setLista(ArrayList<Dep> lista) {
		this.lista = lista;
	}

	public Departamentos(ArrayList<Dep> lista) {
		super();
		this.lista = lista;
	}

	public Departamentos() {
		super();
	}
	
	
}
