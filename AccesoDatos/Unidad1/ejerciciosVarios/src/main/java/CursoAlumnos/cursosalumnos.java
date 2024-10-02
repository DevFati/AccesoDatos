package CursoAlumnos;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "cursosalumnos")
public class cursosalumnos {

	private ArrayList<Curso>cursos;

	public cursosalumnos(ArrayList<Curso> cursos) {
		super();
		this.cursos = cursos;
	}

	public cursosalumnos() {
		super();
	}

	
	@XmlElement(name = "curso")
	public ArrayList<Curso> getCursos() {
		return cursos;
	}

	public void setCursos(ArrayList<Curso> cursos) {
		this.cursos = cursos;
	}
	
	
}



