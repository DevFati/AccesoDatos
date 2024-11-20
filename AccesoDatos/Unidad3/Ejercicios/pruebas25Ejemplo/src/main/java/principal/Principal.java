package principal;
import java.math.BigInteger;
import java.util.Set;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import clases.*;
public class Principal {

	private static SessionFactory factori;

	public static void main(String[] args) {
		LogManager.getLogManager().reset();
		Logger globalLogger = Logger.getLogger(java.util.logging.Logger.GLOBAL_LOGGER_NAME);
		globalLogger.setLevel(java.util.logging.Level.OFF);



		factori = Conexion.getSession(); // Creo la sessionFactory una única vez.

		//insertardep();
		//insertarunempleado();
		cargardeparget(10);
		
		cargardeparget(100);
		
		cargardeparget(61);
		System.out.println("-----------empleado no existe");
		actualizardepalempleado(1111, 10); //empleado no existe
		System.out.println("-----------dep no existe");
		actualizardepalempleado(4450, 99); //dept no existe
		System.out.println("-----------ok");
		actualizardepalempleado(4450, 30); //correcta
		
		
		System.out.println("-----------Añadir al set de empleados");
		System.out.println("-----------error dep ");
		insertaempleadoalsetdedepartamento( 999, 4455) ;
		System.out.println("-----------error emple ");
		insertaempleadoalsetdedepartamento( 30, 445599) ;
		
		System.out.println("-----------OK ");
		insertaempleadoalsetdedepartamento( 30, 4455) ;
		
		factori.close();

	}

	
	private static void insertaempleadoalsetdedepartamento(int nu, int emp) {
		Session session = factori.openSession();

		Departamentos dep = (Departamentos) session.get(Departamentos.class, nu);
		if (dep == null) {
			System.out.println("El departamento no existe. No se puede insertar empleado: "+nu);
		} else {
			// compruebo empleado
			Empleados emple = (Empleados) session.get(Empleados.class, BigInteger.valueOf(emp));
			if (emple == null) {
				System.out.println("El Empleado no existe. No se puede insertar: "+ emp);
			} else {
				// lo añado al set
				Transaction tx = session.beginTransaction();
				dep.getEmpleadoses().add(emple);
				System.out.println("Empleado " + emp + " añadido al departamento " + nu);
				//session.update(dep);     
				session.merge(dep);    
				tx.commit();
			}
		}
		session.close();

	}

	
	private static void actualizardepalempleado(int emp, int nu ) {
		
		Session session = factori.openSession();
		Empleados emple = (Empleados) session.get(Empleados.class, emp);
		if (emple == null) {
			System.out.println("El Empleado no existe. No se puede actualizar: "+emp);
		} else {
			Departamentos dep = (Departamentos) session.get(Departamentos.class, nu);
			if (dep == null) 
				System.out.println("El departamento no existe. No se puede actualizar: "+nu);
				else {
					Transaction tx = session.beginTransaction();
					emple.setDepartamentos(dep);
					System.out.println("Empleado " + emp + " actualizado al departamento " + nu);
					// en desuso session.update(emple);
					session.merge(emple);
					tx.commit();
					}
			}
			session.close();
		}


	
	
	
	private static void cargardeparget(int nu) {
		Session session = factori.openSession();
		Departamentos dep = (Departamentos) session.get(Departamentos.class, nu);
		if (dep == null) {
			System.out.println("El departamento no existe" +  nu);
		} else {
			System.out.println("Nombre Dep:" + dep.getDnombre());
			System.out.println("Localidad:" + dep.getLoc());
			
			Set<Empleados> listaemple = dep.getEmpleadoses();
			
			System.out.println("Número de empleados: " + listaemple.size());
			
			for (Empleados emple : listaemple) {
			   System.out.println(emple.getApellido() + " * " + emple.getSalario());
			}

			
		}
		session.close();
	}


	
	private static void insertarunempleado() {
		Session session = factori.openSession(); // creo una sesión de trabajo
		Transaction tx = session.beginTransaction();

		Empleados em = new Empleados(); // creo un objeto empleados
		em.setEmpNo(BigInteger.valueOf(4450));
		em.setApellido("JUAN");
		em.setOficio("VENDEDOR");
		em.setSalario(2000.0);
		em.setComision(100.0);

		Departamentos d = new Departamentos(); // creo un objeto Departamentos
		d.setDeptNo(BigInteger.valueOf(10)); // el número de dep es 10
		em.setDepartamentos(d);

		// fecha de alta
		java.util.Date hoy = new java.util.Date();
		java.sql.Date fecha = new java.sql.Date(hoy.getTime());
		em.setFechaAlt(fecha);

		try {
			session.persist(em);
			tx.commit();
			System.out.println("EMPLEADO INSERTADO EN EL DEPARTAMENTO 10.");

		} catch (org.hibernate.exception.ConstraintViolationException e) {
			System.out.println("EMPLEADO NO INSERTADO.");
			System.out.println(e.getMessage());

		} catch (org.hibernate.exception.GenericJDBCException e1) {
			System.out.println("EMPLEADO NO INSERTADO.");
			System.out.println(e1.getMessage());

		} catch (java.lang.IllegalStateException e2) {
			System.out.println("EMPLEADO NO INSERTADO.");
			System.out.println(e2.getMessage());
		}

		session.close();

	}

	private static void insertardep() {

		Session session = factori.openSession(); // creo una sesión de trabajo
		Transaction tx = session.beginTransaction();

		try {
			Departamentos dep = new Departamentos();
			dep.setDeptNo(BigInteger.valueOf(62));
			dep.setDnombre("Mmmmm");
			dep.setLoc("GUADALAJARA");

			session.persist(dep);
			tx.commit();
			System.out.println("Departamento insertado");

		} catch (org.hibernate.exception.ConstraintViolationException e) {
			System.out.println(e.getMessage());

		} catch (org.hibernate.exception.GenericJDBCException e1) {
			System.out.println(e1.getMessage());
		}
		session.close();

	}

}
