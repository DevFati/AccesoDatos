package clases;

public class Paises {
	private int id;
	private String nombrePais;
	public Paises(int id, String nombrePais) {
		super();
		this.id = id;
		this.nombrePais = nombrePais;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getNombrePais() {
		return nombrePais;
	}
	public void setNombrePais(String nombrePais) {
		this.nombrePais = nombrePais;
	}
	@Override
	public String toString() {
		return nombrePais;
	}
}