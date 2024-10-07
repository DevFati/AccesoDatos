package asignaturas_alumnos;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;


@XmlType(propOrder = {"numalumno", "nombre","localidad","numasig","notamedia","listanotas"})

public class Alumno {

	private int numalumno;
	private String nombre;
	private String localidad;
	private int numasig;
	private float notamedia;
	private ArrayList<Nota> listanotas;
	
	
	public Alumno(int numalumno, String nombre, String localidad, int numasig, float notamedia,
			ArrayList<Nota> listanotas) {
		super();
		this.numalumno = numalumno;
		this.nombre = nombre;
		this.localidad = localidad;
		this.numasig = numasig;
		this.notamedia = notamedia;
		this.listanotas = listanotas;
	}
	public Alumno() {
		super();
	}
	public int getNumalumno() {
		return numalumno;
	}
	public void setNumalumno(int numalumno) {
		this.numalumno = numalumno;
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
	public int getNumasig() {
		return numasig;
	}
	public void setNumasig(int numasig) {
		this.numasig = numasig;
	}
	public float getNotamedia() {
		return notamedia;
	}
	public void setNotamedia(float notamedia) {
		this.notamedia = notamedia;
	}
	@XmlElementWrapper(name = "ListaNotas")  
	@XmlElement(name = "notaasig")
	public ArrayList<Nota> getListanotas() {
		return listanotas;
	}
	public void setListanotas(ArrayList<Nota> listanotas) {
		this.listanotas = listanotas;
	}
	public Alumno(int numalumno, String nombre, String localidad, int numasig, float notamedia) {
		super();
		this.numalumno = numalumno;
		this.nombre = nombre;
		this.localidad = localidad;
		this.numasig = numasig;
		this.notamedia = notamedia;
		this.listanotas=new ArrayList<Nota>();
	}
	
	
	
	
}
