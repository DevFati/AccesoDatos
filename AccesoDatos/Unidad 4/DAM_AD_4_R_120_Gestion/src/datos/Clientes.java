package datos;

public class Clientes {

	private int numcli ;
	private String nombre;
    private String pobla;
    private int descuento; // 0 si no tiene, 1 si sí

    public Clientes(){}
    
	public Clientes(int numcli, String nombre, String pobla) {
		super();
		this.numcli = numcli;
		this.nombre = nombre;
		this.pobla = pobla;
		this.descuento=(int)Math.random()*2;

	}
	
	public Clientes(int numcli, String nombre, String pobla, int descuento) {
		super();
		this.numcli = numcli;
		this.nombre = nombre;
		this.pobla = pobla;
		this.descuento = descuento;
	}


     
	public int getNumcli() {
		return numcli;
	}
	public void setNumcli(int numcli) {
		this.numcli = numcli;
	}
	public String getNombre() {
		return nombre;
	}
	public void setNombre(String nombre) {
		this.nombre = nombre;
	}
	public String getPobla() {
		return pobla;
	}
	public void setPobla(String pobla) {
		this.pobla = pobla;
	}
	public int getDescuento() {
		return descuento;
	}

	public void setDescuento(int descuento) {
		this.descuento = descuento;
	}
	

}
