package clases;

public class Accesosporestacion {

	private int codEstacion; 
	private String nombre; 
	private String direccion; 
	private long contador;
	public int getCodEstacion() {
		return codEstacion;
	}
	public void setCodEstacion(int codEstacion) {
		this.codEstacion = codEstacion;
	}
	public String getNombre() {
		return nombre;
	}
	public void setNombre(String nombre) {
		this.nombre = nombre;
	}
	public String getDireccion() {
		return direccion;
	}
	public void setDireccion(String direccion) {
		this.direccion = direccion;
	}
	public long getContador() {
		return contador;
	}
	public void setContador(long contador) {
		this.contador = contador;
	}
	public Accesosporestacion(int codEstacion, String nombre, String direccion, long contador) {
		super();
		this.codEstacion = codEstacion;
		this.nombre = nombre;
		this.direccion = direccion;
		this.contador = contador;
	}
	public Accesosporestacion() {
		super();
	}
	@Override
	public String toString() {
		return "Accesosporestacion [codEstacion=" + codEstacion + ", nombre=" + nombre + ", direccion=" + direccion
				+ ", contador=" + contador + "]";
	} 
	
	
	
	
}
