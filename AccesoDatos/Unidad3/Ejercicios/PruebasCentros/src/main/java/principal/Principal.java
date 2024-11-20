package principal;

import java.util.Set;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import clases.C1Asignaturas;
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
		
		System.out.println("-----------");
		mostrardatosprofescentro(1011); // tiene subordinados
		System.out.println("-----------");
		mostrardatosprofescentro(2002); // No es JEFE
		System.out.println("-----------");
		mostrardatosprofescentro(9999); // NO EXISTE
		
		System.out.println("------------------insertar/actualizar---");
		//  centro nuevo, no existe, se creará
		actualizarcentro(1500, "Centro 1500", 2000, "C/Las Palmeras 2");
		System.out.println("------Existe-------------");
		// centro existe, se actualiza
		actualizarcentro(1050, "Centro 1050", 2000, "C/Las Palmeras 2");
		
		
		System.out.println("------------------Asignar asig a profesor---");
		asignarasigaprofesor(1000, "Nueva"); // nueva asig
		System.out.println("------Añadir");
		asignarasigaprofesor(1000, "IF0003"); // añado asig 
		System.out.println("------Ya la tiene");
		asignarasigaprofesor(1000, "IF0001"); // ya tiene esa asig
		factori.close();

	}
     
	private static void asignarasigaprofesor(int codprof, String codasi) {
		Session session = factori.openSession();

		C1Profesores profe = (C1Profesores) session.get(C1Profesores.class, (short) codprof);
		//Transaction tx = session.beginTransaction();

		if (profe!=null) {
			// profe existe
			//se busca la asignatura
			C1Asignaturas asi = (C1Asignaturas) session.get(C1Asignaturas.class, codasi);
			if (asi==null) {
				//Nueva asignatura, se añade
				asi= new C1Asignaturas ();
				asi.setCodAsig(codasi);
				asi.setNombreAsi(codasi+" NOMBRE");
				session.persist(asi);
				
			}
			
			//asignar la asignarura al set
			
			
			
			
			
		}
		else {
			System.out.println(" ** PROFESOR NO EXISTE: "+codprof);
		}
		
		
		//tx.commit();
		session.close();
		
		
	}
	
	private static void actualizarcentro(int cod, String nom, int dir, String direcc){
		Session session = factori.openSession();

		C1Centros cc = (C1Centros) session.get(C1Centros.class, (short) cod);
		Transaction tx = session.beginTransaction();

		if (cc == null) {
			// CREAR UN NUEVO CENTRO
			cc = new C1Centros();
			cc.setCodCentro((short) cod);
			cc.setNomCentro(nom);
			cc.setDirector((short)dir);
			cc.setDireccion(direcc);
			session.persist(cc); // se crea el centro
			System.out.println("---Centro creado "+cod);
			
		}
		else {
			//actualizar el centro
			cc.setNomCentro(nom);
			cc.setDirector((short)dir);
			cc.setDireccion(direcc);
			session.merge(cc); // 
			System.out.println("---Centro actualizado "+cod);
		}
		// añadir el profesor 100 al centro, volverlo a cargar
		
		C1Profesores pro = (C1Profesores) session.get(C1Profesores.class, (short) 1000);
		if (pro!=null) {
			//se añade
			cc.getC1Profesoreses().add(pro);
			session.merge(cc);
			System.out.println("---Profesor 1000 añadido al centro "+cod);
		}
		
		
				
		tx.commit();
		session.close();
		
	}
	
	
	
	
	private static void mostrardatosprofescentro(int id) {
		
		Session session = factori.openSession();
		C1Profesores pf = (C1Profesores) session.get(C1Profesores.class, id);
		
		if(pf == null) {
			System.out.println("Cod profesor no existe: " + id);
		}
		else {
			
			System.out.println("Nombre profesor: "+pf.getNombreApe());
			System.out.println("  * Nombre Especialidad: "+ pf.getC1Especialidad().getNombreEspe());
			String jefe="SIN JEFE";
			String codigo="SIN COD";
			if (pf.getC1Profesores()!=null) {
				jefe = pf.getC1Profesores().getNombreApe();
				Short codi = pf.getC1Profesores().getCodProf();
				codigo=codi.toString();
				
			}
			System.out.println("  * Nombre jefe: "+ jefe + ", código: "+codigo);
			System.out.println("  * Nombre de centro: "+ pf.getC1Centros().getNomCentro());
			System.out.println("  * Imparte asignaturas: " + pf.getC1Asignaturases().size());
			if (pf.getC1Asignaturases().size()>0) {
				System.out.println("    COD ASIG   NOMBREASIG");
				System.out.println("    --------   ----------------");
				Set<C1Asignaturas> listaai= pf.getC1Asignaturases();
				for (C1Asignaturas ass : listaai) {
					   System.out.println("    " + ass.getCodAsig() + "  "+ass.getNombreAsi());
					}
				System.out.println("    --------   ----------------");
				
			}
			
			
			System.out.println("  * Jefe de profesores: " + pf.getC1Profesoreses().size());
			if (pf.getC1Profesoreses().size()>0) {
				System.out.println("     COD PROF   NOMBRE PROF");
				System.out.println("     --------   ----------------");
				Set<C1Profesores> listp= pf.getC1Profesoreses();
				for (C1Profesores pro : listp) {
					   System.out.println("     " + pro.getCodProf() + "  "+pro.getNombreApe());
					}
				System.out.println("     --------   ----------------");
				
			}
			
			
			
		}
		
		session.close();
		
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
