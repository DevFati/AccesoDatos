package mortahilchachoufatima;

import javax.xml.bind.annotation.XmlType;

@XmlType(propOrder = { "codviaje", "nombreviaje", "precio", "numplazas", "operacion" })

public class Viaje {

	private int codviaje;
	private String nombreviaje;
	private int precio;
	private int numplazas;
	private String operacion;

	public Viaje(int codviaje, String nombreviaje, int precio, int numplazas, String operacion) {
		super();
		this.codviaje = codviaje;
		this.nombreviaje = nombreviaje;
		this.precio = precio;
		this.numplazas = numplazas;
		this.operacion = operacion;
	}

	public Viaje() {
		super();
	}

	public int getCodviaje() {
		return codviaje;
	}

	public void setCodviaje(int codviaje) {
		this.codviaje = codviaje;
	}

	public String getNombreviaje() {
		return nombreviaje;
	}

	public void setNombreviaje(String nombreviaje) {
		this.nombreviaje = nombreviaje;
	}

	public int getPrecio() {
		return precio;
	}

	public void setPrecio(int precio) {
		this.precio = precio;
	}

	public int getNumplazas() {
		return numplazas;
	}

	public void setNumplazas(int numplazas) {
		this.numplazas = numplazas;
	}

	public String getOperacion() {
		return operacion;
	}

	public void setOperacion(String operacion) {
		this.operacion = operacion;
	}

}
