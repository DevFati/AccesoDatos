package principal;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.hibernate.SessionFactory;

import clases.C1Asignaturas;
import clases.C1Centros;
import clases.C1Profesores;

public class Principal {
	private static SessionFactory sesion;

	public static void main(String[] args) {
		Logger.getLogger("org.hibernate").setLevel(Level.OFF);

		sesion=Conexion.getSession(); 
		verdetallesCentro(1000);

		verdetallesCentro(1111);
		verdetallesCentro(1050);


		mostrarDatosProfesor(1011); //Tiene subordinados

		System.out.println("--------------------------");

		mostrarDatosProfesor(2002); //No es jefe

		System.out.println("--------------------------");

		mostrarDatosProfesor(8888); //No existe;		
		
		
		sesion.close();
	}

	private static void mostrarDatosProfesor(int i) {
		Session session=sesion.openSession();
		
		C1Profesores profesor=(C1Profesores) session.get(C1Profesores.class, i);
		
		if(profesor==null) {
			System.out.println("El profesor no existe");
		}else {
			
			System.out.printf("%-30s %-20s %-30s %-20s %-20s %n", "NombreProfesor ", "NombreEspecialidad ","Nombre Jefe", "CodigoP Jefe","NomCentro");
			String jefe="No tiene";
			String codj="Sin codigo";
			if(profesor.getC1Profesores() !=null) {
				jefe=profesor.getC1Profesores().getNombreApe();
				codj=profesor.getC1Profesores().getCodProf()+"";
			}
			System.out.printf("%-30s %-20s %-30s %-20s %-20s %n", profesor.getNombreApe(), profesor.getC1Especialidad().getNombreEspe(), jefe,codj,profesor.getC1Centros().getNomCentro());
			
			
			System.out.println("Asignaturas : ");
			Set<C1Asignaturas> listaasig=profesor.getC1Asignaturases();
			
			if(listaasig.size()==0) {
				System.out.println("No imparte ninguna asignatura");
			}else {
				System.out.printf("%10s %-30s  %n", "Codigo ", "Nombre ");
				System.out.printf("%10s %-30s  %n", "----------", "------------------------------");

				for(C1Asignaturas l:listaasig) {
					
					
					System.out.printf("%10s %-30s  %n", l.getCodAsig(), l.getNombreAsi());


				}
			}
			
			//Nombre de los profesores subordinados si es jefe 
			boolean esJefe=false;
			Set<C1Profesores> listaj=profesor.getC1Profesoreses();
			for(C1Profesores l: listaj) {
				if(i==l.getC1Profesores().getCodProf()) {
					esJefe=true;
				}
			}
			
				if(esJefe) {
					System.out.println("Profesores subordinados : ");
					Set<C1Profesores> listap=profesor.getC1Profesoreses();
					
					System.out.printf("%10s  %n", "Nombre ");
					System.out.printf("%10s  %n", "----------");

					for(C1Profesores li:listap) {
						
						
						System.out.printf("%10s   %n", li.getNombreApe());


					
				
				}
				}

			
			
		}
		session.close();
		
	}

	private static void verdetallesCentro(int i) {
		Session session=sesion.openSession();

		C1Centros centro=(C1Centros) session.get(C1Centros.class, i);
		
		if(centro==null) {
			System.out.println("El centro no existe");
		}else {
			System.out.printf("%-11s %-30s %n", "Cod Centro: ", "Nombre: ");
			System.out.printf("%-11s %-30s %n", centro.getCodCentro(), centro.getNomCentro());
			System.out.println("Lista de Profesores del centro ");

			Set<C1Profesores> listaprofesores=centro.getC1Profesoreses();
			
			if(listaprofesores.size()==0) {
				System.out.println("El centro "+centro.getNomCentro()+" no tiene profesores");
			}else {
				
				System.out.printf("%10s %-30s %-35s %-30s %30s %n", "Cod ", "NombreProfesor", "NombreEspecialidad ","Nombre Jefe","NúmAsig que imparte");
				System.out.printf("%10s %-30s %-35s %-30s %30s %n", "----------", "------------------------------", "------------------------------ ","------------------------------","------------------------------");
				
				for(C1Profesores prof:listaprofesores) {
					String jefe="No tiene";
					if(prof.getC1Profesores() != null) {
						

						jefe=prof.getC1Profesores().getNombreApe();
					}
					
					System.out.printf("%10s %-30s %-35s %-30s %30s %n", prof.getCodProf(), prof.getNombreApe(), prof.getC1Especialidad().getNombreEspe(),jefe,prof.getC1Asignaturases().size());


				}
			}
			
		}
		

		System.out.println("======================");

		session.close();

		
	}

}
