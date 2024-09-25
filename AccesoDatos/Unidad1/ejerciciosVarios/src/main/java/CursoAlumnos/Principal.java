package CursoAlumnos;

import java.io.File;
import java.util.ArrayList;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;


public class Principal {
	
	public static void main(String[] args) {
	JAXBContext context;

	try {
		context = JAXBContext.newInstance(cursosalumnos.class);
		Unmarshaller unmars = context.createUnmarshaller();
		cursosalumnos objeto = (cursosalumnos) unmars.unmarshal(new File("cursosalumnosVer2.xml"));
		
		ArrayList<Curso> cursos=objeto.getCursos(); 
		
		
		System.out.println("Número de cursos: "+cursos.size());
		
		for(Curso v: cursos) {
			System.out.println("CURSO: "+v.getNombre());
			System.out.println();
			float suma=0;
			float contador=0;
			System.out.printf("%10s %20s %11s%n","  ","NOMBRE","NOTA MEDIA");
			System.out.printf("%10s %20s %11s%n","  ","-------------","-------------");
			for(Alumno a:v.getAlumnos()) {
				
			
				System.out.printf("%10s %20s %11s%n","  ",a.getNombre(),a.getNotamedia());
				suma=suma+a.getNotamedia();
				contador++;
			}
			System.out.printf("%10s %20s %11s%n","  ","-------------","-------------");
		//	System.out.println(suma);
			//System.out.println(contador);
			float media=(float)Math.round((suma/contador)*100)/100;
			System.out.printf("%10s %20s %11s%n","  ","MEDIA:  ",media);
			System.out.println();
			
			
		}
	    
	   
	   	
	} catch (JAXBException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}}
}
