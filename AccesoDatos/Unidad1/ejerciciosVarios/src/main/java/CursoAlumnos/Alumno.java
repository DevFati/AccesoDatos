package CursoAlumnos;

public class Alumno {

	private String nombre;
	private float notamedia;
	public Alumno(String nombre, float notamedia) {
		super();
		this.nombre = nombre;
		this.notamedia = notamedia;
	}
	public Alumno() {
		super();
	}
	public String getNombre() {
		return nombre;
	}
	public void setNombre(String nombre) {
		this.nombre = nombre;
	}
	public float getNotamedia() {
		return notamedia;
	}
	public void setNotamedia(float notamedia) {
		this.notamedia = notamedia;
	}
	
	
	
}
