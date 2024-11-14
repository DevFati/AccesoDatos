package principal;

import java.util.Set;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.hibernate.SessionFactory;

import clases.C1Centros;
import clases.C1Profesores;

public class Principal {
	private static SessionFactory factori;

	public static void main(String[] args) {
		LogManager.getLogManager().reset();
		Logger globalLogger = Logger.getLogger(java.util.logging.Logger.GLOBAL_LOGGER_NAME);
		globalLogger.setLevel(java.util.logging.Level.OFF);

		factori = Conexion.getSession(); // Creo la sessionFactory una única vez.

		verdatosdecentro(1000);
		System.out.println("-----------");
		verdatosdecentro(1111);
		System.out.println("-----------");
		verdatosdecentro(1050);
		
		
		factori.close();

	}

	private static void verdatosdecentro(int cod) {

		Session session = factori.openSession();
		C1Centros cen = (C1Centros) session.get(C1Centros.class, cod);

		if (cen == null) {
			System.out.println("Cod Centro no existe: " + cod);

		} else {
			System.out.println("Cod Centro: " + cen.getCodCentro() +
					"            Nombre: " + cen.getNomCentro());
			Set<C1Profesores> lista = cen.getC1Profesoreses();
			if (lista.size() == 0)
				System.out.println("    Centro sin profesores.");

			else {
				mostrardatosprofescentro(lista);
			}

			// Cod NombreProfesor NombrEspecialidad Nombre Jefe NúmAsig que imparte
			// --- -------------- ----------------- ------------ --------------------

		}

		session.close();

	}

	private static void mostrardatosprofescentro(Set<C1Profesores> lista) {
		// TODO Auto-generated method stub

		// Cod NombreProfesor NombrEspecialidad Nombre Jefe NúmAsig que imparte
		// --- -------------- ----------------- ------------ --------------------

		System.out.printf("%5s %-30s %-30s %-30s %-20s%n",
				"Cod","NombreProfesor","NombrEspecialidad","Nombre Jefe","NúmAsig que imparte");		
		System.out.printf("%5s %-30s %-30s %-30s %-20s%n",
				"-----","--------------------","--------------------","--------------------",
				"--------------------");
		for (C1Profesores p : lista) {
			
			String jefe="NO TIENE";
			String espe="NO TIENE";
			if (p.getC1Especialidad()!=null) {
				espe = p.getC1Especialidad().getNombreEspe();
			}
			if( p.getC1Profesores()!=null) {
				jefe = p.getC1Profesores().getNombreApe();
			}
			
			System.out.printf("%5s %-30s %-30s %-30s %-20s%n",
			  p.getCodProf(), p.getNombreApe(), 
			  espe,  jefe, 	  p.getC1Asignaturases().size());
			  
			}
	
	}

}


	