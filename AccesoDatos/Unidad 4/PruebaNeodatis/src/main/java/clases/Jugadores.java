package clases;

public class Jugadores {
	private String nombre;
	private String deporte;
	private String ciudad;
	private int edad;

	public Jugadores() {
	}

	public Jugadores(String nombre, String deporte, String ciudad, int edad) {
		this.nombre = nombre;
		this.deporte = deporte;
		this.ciudad = ciudad;
		this.edad = edad;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public String getNombre() {
		return nombre;
	}

	public void setDeporte(String deporte) {
		this.deporte = deporte;
	}

	public String getDeporte() {
		return deporte;
	}

	public void setCiudad(String ciudad) {
		this.ciudad = ciudad;
	}

	public String getCiudad() {
		return ciudad;
	}

	public void setEdad(int edad) {
		this.edad = edad;
	}

	public int getEdad() {
		return edad;
	}

}
