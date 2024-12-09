package principal;

import java.math.BigInteger;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import clases.C1Asignaturas;
import clases.C1Centros;
import clases.C1Profesores;

public class Principal {
	private static SessionFactory sesion;

	public static void main(String[] args) {
		Logger.getLogger("org.hibernate").setLevel(Level.OFF);
	
		sesion=Conexion.getSession(); 

	//	actualizarCentro(1501, "Centroos 1500",2000,"C/Las Palmeras 2"); //Centro nuevo, se crea

		actualizarCentro(1501, "Centro 1051",2000,"C/Las Palmeras 3"); //Centro existe, se actualiza

		//Asignatura al profesor
	//	asignarAsignaturaAProfesor(1000,"Nueva2"); //Nueva asignatura

	//	asignarAsignaturaAProfesor(1000,"IF0006"); //No tiene la asignatura, se le añade

		asignarAsignaturaAProfesor(9000,"IF0001"); //No existe profesor

		
		borrarAsignatura("IF0001"); //Existe y tiene registros

		borrarAsignatura("IF000666"); //No existe

		
		

		
	}

	private static void borrarAsignatura(String cod) {
		Session session=sesion.openSession();
		C1Asignaturas a=(C1Asignaturas) session.get(C1Asignaturas.class, cod);

		if(a==null) {
			System.out.println("La asignatura no existe. No se puede borrar");
		}else {
			try {
				Transaction tx=session.beginTransaction();
				session.remove(a);
				tx.commit();
				System.out.println("ASIGNATURA BORRADO");
			}catch(jakarta.persistence.PersistenceException e) {
					System.out.println("NO SE PUEDE BORRAR. TIENE REGISTROS RELACIONADOS");
				
			}
			
		}
		
	session.close();
		
	}

	private static void asignarAsignaturaAProfesor(int codP, String asig) {
		Session session=sesion.openSession();
		Transaction tx=session.beginTransaction();
		C1Profesores p=(C1Profesores) session.get(C1Profesores.class, codP);
		boolean asignaturaYaAsignada=false;
		if(p==null) {
			System.out.println("El codigo de profesor "+codP+" no existe");
		}else {		
			C1Asignaturas a=(C1Asignaturas) session.get(C1Asignaturas.class, asig);
			if(a==null) {
				System.out.println("La asignatura no existe, LO CREO");
				a=new C1Asignaturas();
				a.setCodAsig(asig);
				a.setNombreAsi("Asig Nueva");
				
				session.persist(a); //para guardar los cambios en la bbdd
			}else {
				System.out.println("La asignatura ya existe");

				//en caso de que ya exista
				//Comporbamos si la asignatura ya esta asociada al empleado. 
				Set<C1Asignaturas> listaasig=p.getC1Asignaturases();
				
				for (C1Asignaturas as : listaasig) {
					   if(as.getCodAsig().equals(asig)) {
						   asignaturaYaAsignada=true;
					   }
					}
				
			}
			
			
			if(asignaturaYaAsignada) {
				System.out.println("La asignatura ya se encuentra asignada al profesor "+codP);

			}else {
				System.out.println("La asignatura se asigno al profesor"+ codP);
				C1Asignaturas asign=(C1Asignaturas) session.get(C1Asignaturas.class, asig);

				p.getC1Asignaturases().add(asign);
				
				session.persist(p);	
			}
			
		}

		tx.commit();
		session.close();
		
	}

	private static void actualizarCentro(int codc, String nombre, int coddirector, String direccion) {

		Session session=sesion.openSession();
		Transaction tx=session.beginTransaction();
		C1Centros c=(C1Centros) session.get(C1Centros.class, codc);
		C1Profesores p=(C1Profesores) session.get(C1Profesores.class, coddirector);
		boolean codD=true;
		if(p==null) {
			codD=false; //no existe el coddirector
		}
		
		
		if(c==null) {
			System.out.println("El centro no existe, LO CREO");
			c=new C1Centros();
			c.setCodCentro((short)codc);
			c.setNomCentro(nombre);
			if(codD) {
				c.setDirector((short)coddirector);

			}
			c.setDireccion(direccion);			
			session.persist(c); //para guardar los cambios en la bbdd
		}else {
			System.out.println("El centro existe, LO MODIFICO");
			c.setNomCentro(nombre);
			if(codD) {
				c.setDirector((short)coddirector);

			}			
			c.setDireccion(direccion);	
			//Añadimos al set de profesores de este centro 
			//al profesor con codigo 1000
			C1Profesores e=(C1Profesores) session.get(C1Profesores.class, 1000);

			c.getC1Profesoreses().add(e);
			
			session.persist(c);
		}
		
		tx.commit();
		session.close();
		
		
	}

}
