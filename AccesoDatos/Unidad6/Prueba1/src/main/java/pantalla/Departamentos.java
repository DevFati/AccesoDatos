package pantalla;

public class Departamentos {
    //atributos del departamento
    private int deptno;
    private String dnombre;
    private String loc;
     
    //botones de los formularios
    String modificar;
    String eliminar;
    String insertar;
	public int getDeptno() {
		return deptno;
	}
	public void setDeptno(int deptno) {
		this.deptno = deptno;
	}
	public String getDnombre() {
		return dnombre;
	}
	public void setDnombre(String dnombre) {
		this.dnombre = dnombre;
	}
	public String getLoc() {
		return loc;
	}
	public void setLoc(String loc) {
		this.loc = loc;
	}
	public String getModificar() {
		return modificar;
	}
	public void setModificar(String modificar) {
		this.modificar = modificar;
	}
	public String getEliminar() {
		return eliminar;
	}
	public void setEliminar(String eliminar) {
		this.eliminar = eliminar;
	}
	public String getInsertar() {
		return insertar;
	}
	public void setInsertar(String insertar) {
		this.insertar = insertar;
	}
	public Departamentos(int deptno, String dnombre, String loc, String modificar, String eliminar, String insertar) {
		super();
		this.deptno = deptno;
		this.dnombre = dnombre;
		this.loc = loc;
		this.modificar = modificar;
		this.eliminar = eliminar;
		this.insertar = insertar;
	}
	public Departamentos() {
		super();
	}
   
    //constructores y métodos get set
    
    
	
    
}