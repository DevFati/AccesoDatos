package principal;

import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import clases.Alumnos;
import clases.Asignaturas;

public class Principal {
	private static SessionFactory sesion;

	public static void main(String[] args) {
		Logger.getLogger("org.hibernate").setLevel(Level.OFF);

		sesion=Conexion.getSession(); 

		
		Scanner teclado=new Scanner(System.in);
		int opcion=0; 
		do {
			menu();
			
			System.out.println("Elige un opcion ");
			opcion=teclado.nextInt();
			switch(opcion) {
			
			case 1: 
				insertarEvaluacion(1,1010,1,4);
				break; 
			
			case 2:
				break; 
			
			case 3:
				break; 
			
			case 4: 
				break; 
			
			case 5: 
				break; 
			
			case 6: 
				System.out.println("Adios!");
				break; 
			
			default: 
				System.out.println("Opción invalida");
			}
			
		}while(opcion!=6);
		
		
		
		sesion.close();

	}

	private static void insertarEvaluacion(int codEv, int codAl, int codAs, double nota) {
		
		String mensaje="";
		
		if(!(codEv==1 || codEv==2 || codEv==3)) {
			mensaje=mensaje+"CodEvaluacion incorrecto \n";
		}
		
		if (!esNotaValida(nota)) {
			mensaje=mensaje+"Nota invalida \n";			

		}
		
		
		Session session=sesion.openSession();
		Transaction tx=session.beginTransaction();
		Asignaturas asig=(Asignaturas) session.get(Asignaturas.class, codAs);
		
		if(asig==null) {
			mensaje=mensaje+"No existe la asignatura \n";
		}
		
		Alumnos alu=(Alumnos) session.get(Alumnos.class, codAl);
		
		if(alu==null) {
			mensaje=mensaje+"No existe el alumno \n";
		}
		
		String hql="select e.nota from Evaluaciones e where "
				+ "e.alumnos.numAlumno = :codAlu "
				+ "and e.asignaturas = :codAsig";
		
		
		
		
		
		
		
	}
	
	 public static boolean esNotaValida(double nota) {
	        // Verificar que la nota esté entre 1 y 10 (inclusive)
	        if (nota < 1 || nota > 10) {
	            return false;
	        }

	        // Verificar que los decimales sean exactamente 2
	        String notaTexto = String.valueOf(nota);
	        if (!notaTexto.matches("\\d+\\.\\d{2}")) {
	            return false;
	        }

	        return true; // Si pasa ambas validaciones, la nota es válida
	    }

	private static void menu() {
		System.out.println("OPERACIONES CON PROYECTOS. Realizado por Fátima Mortahil");
		
		System.out.println("1. EJERCICIO 1: Insertar evaluaciones");
		System.out.println("2. EJERCICIO 2: Actualizar contadores.");
		System.out.println("3. EJERCICIO 3: Mostrar datos de todos los cursos.");
		System.out.println("4. EJERCICIO 4: Mostrar estadística de centros.");
		System.out.println("5. EJERCICIO 5: Realizar consultas.");
		System.out.println("6. Salir");
		
	}
	
	

}
