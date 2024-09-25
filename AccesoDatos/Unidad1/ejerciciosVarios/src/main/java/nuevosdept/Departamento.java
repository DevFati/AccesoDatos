package nuevosdept;

import javax.xml.bind.annotation.XmlType;

@XmlType(propOrder = {"deptno", "dnombre", "localidad"})
public class Departamento {

	private int deptno;
	private String dnombre ;
	private String localidad;
	
	public Departamento() {}
	
	public Departamento(int deptno, String dnombre, String localidad) {
		super();
		this.deptno = deptno;
		this.dnombre = dnombre;
		this.localidad = localidad;
	}
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
	public String getLocalidad() {
		return localidad;
	}
	public void setLocalidad(String localidad) {
		this.localidad = localidad;
	}
	
	
	
	
}
