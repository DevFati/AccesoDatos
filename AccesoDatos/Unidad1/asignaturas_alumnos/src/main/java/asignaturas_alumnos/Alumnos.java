package asignaturas_alumnos;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="ListaAlumnos")
public class Alumnos {

	private ArrayList<Alumno> alumnos;

	public Alumnos(ArrayList<Alumno> alumnos) {
		super();
		this.alumnos = alumnos;
	}

	public Alumnos() {
		super();
	}

	@XmlElement(name = "Alum")
	public ArrayList<Alumno> getAlumnos() {
		return alumnos;
	}

	public void setAlumnos(ArrayList<Alumno> alumnos) {
		this.alumnos = alumnos;
	}
	
	
	
}
