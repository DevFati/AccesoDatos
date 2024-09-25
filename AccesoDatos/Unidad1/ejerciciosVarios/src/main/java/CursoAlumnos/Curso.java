package CursoAlumnos;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

public class Curso {

	private String nombre;
	private ArrayList<Alumno> alumnos;
	public Curso(String nombre, ArrayList<Alumno> alumnos) {
		super();
		this.nombre = nombre;
		this.alumnos = alumnos;
	}
	public Curso() {
		super();
	}
	public String getNombre() {
		return nombre;
	}
	public void setNombre(String nombre) {
		this.nombre = nombre;
	}
	
	@XmlElementWrapper(name = "alumnos")  
	@XmlElement(name = "alumno")
	public ArrayList<Alumno> getAlumnos() {
		return alumnos;
	}
	public void setAlumnos(ArrayList<Alumno> alumnos) {
		this.alumnos = alumnos;
	}
	
	
	
}
