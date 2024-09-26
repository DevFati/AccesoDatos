package creardepartamentosxml;

public class Dep {

	private int id;
	private String nombre;
	private String localidad;
	private int numemple;
	private float salario;
	public Dep(int id, String nombre, String localidad, int numemple, float salario) {
		super();
		this.id = id;
		this.nombre = nombre;
		this.localidad = localidad;
		this.numemple = numemple;
		this.salario = salario;
	}
	public Dep() {
		super();
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getNombre() {
		return nombre;
	}
	public void setNombre(String nombre) {
		this.nombre = nombre;
	}
	public String getLocalidad() {
		return localidad;
	}
	public void setLocalidad(String localidad) {
		this.localidad = localidad;
	}
	public int getNumemple() {
		return numemple;
	}
	public void setNumemple(int numemple) {
		this.numemple = numemple;
	}
	public float getSalario() {
		return salario;
	}
	public void setSalario(float salario) {
		this.salario = salario;
	} 
	
	
}
