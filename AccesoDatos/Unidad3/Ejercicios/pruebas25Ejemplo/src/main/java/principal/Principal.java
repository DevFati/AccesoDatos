package principal;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import clases.*;

public class Principal {

	private static SessionFactory factori;

	public static void main(String[] args) {
		LogManager.getLogManager().reset();
		Logger globalLogger = Logger.getLogger(java.util.logging.Logger.GLOBAL_LOGGER_NAME);
		globalLogger.setLevel(java.util.logging.Level.OFF);

		factori = Conexion.getSession(); // Creo

//		consultasobjetos();
//
//		consultatotales();
//		
//		consultaconobjetos();

		insertardepardenuevos();
		
		insertardual();
		

		factori.close();
	}

	private static void insertardual() {
		
		Session session = factori.openSession();
		System.out.println("\nInsertar nuevos departamentos\n-----------------------");
		Transaction tx = session.beginTransaction();

		try {
			String hqlInsert = "insert into Departamentos (deptNo, dnombre, loc)"
					+ " select 200, 'Nuevo200', 'Talavera' from dual";

			Query cons = (Query) session.createMutationQuery(hqlInsert);
			int filascreadas = cons.executeUpdate();

			tx.commit(); // valida la transacción

			System.out.printf("FILAS INSERTADAS: %d%n", filascreadas);
			
		} catch (java.lang.IllegalArgumentException e) {
			System.out.println("ERROR. NO EXISTE LA TABLA ORIGEN.");
			System.out.println(e.getMessage());
		}

		session.close();
	}

	private static void insertardepardenuevos() {
		Session session = factori.openSession();
		System.out.println("\nInsertar nuevos departamentos\n-----------------------");
		Transaction tx = session.beginTransaction();

		try {
			String hqlInsert = "insert into Departamentos (deptNo, dnombre, loc)"
					+ " select n.deptNo, n.dnombre, n.loc from Nuevos n";

			Query cons = (Query) session.createMutationQuery(hqlInsert);
			int filascreadas = cons.executeUpdate();

			tx.commit(); // valida la transacción

			System.out.printf("FILAS INSERTADAS: %d%n", filascreadas);
			
		} catch (org.hibernate.exception.ConstraintViolationException e) {
			System.out.println("ERROR. SE HA ENCONTRADO CLAVES REPETIDAS.");
			System.out.println(e.getErrorMessage());
		}

		session.close();
	}

	private static void consultaconobjetos() {
		Session session = factori.openSession();
		Query cons = session.createQuery("select d.deptNo, count(em.empNo), " + " coalesce(avg(em.salario),0), "
				+ " d.dnombre from Departamentos d left join d.empleadoses em  "
				+ " group by d.deptNo,d.dnombre order by d.deptNo", Object.class);

		System.out.printf("%n%10s %-15s %14s %-14s", "NUMERO DEP", "NOMBRE", "SALARIO MEDIO", "NUM EMPLES");
		System.out.printf("%n%10s %-15s %14s %-14s", "----------", "---------------", "--------------",
				"--------------");

		List filas = cons.list();
		for (int i = 0; i < filas.size(); i++) {
			Object[] filaActual = (Object[]) filas.get(i); // Acceso a una fila
			System.out.printf("%n%10s %-15s %14.2f %-14s", filaActual[0], filaActual[3], filaActual[2], filaActual[1]);
		}
	}

	private static void consultatotales() {
		Session session = factori.openSession();

		Query cons4 = session.createQuery("select new clases.Totales(" + " d.deptNo, count(em.empNo),  "
				+ " coalesce(avg(em.salario),0),  d.dnombre )" + " from Departamentos d left join d.empleadoses em "
				+ " group by  d.deptNo,d.dnombre order by d.deptNo", Totales.class);

		System.out.printf("%n%10s %-15s %14s %-14s", "NUMERO DEP", "NOMBRE", "SALARIO MEDIO", "NUM EMPLES");

		System.out.printf("%n%10s %-15s %14s %-14s", "----------", "---------------", "--------------",
				"--------------");

		List<Totales> filas4 = cons4.list();
		for (int i = 0; i < filas4.size(); i++) {
			Totales tot = (Totales) filas4.get(i);
			System.out.printf("%n%10s %-15s %14.2f %-14s", tot.getNumero(), tot.getNombre(), tot.getMedia(),
					tot.getCuenta());

		}
		System.out.printf("%n%10s %-15s %14s %-14s", "----------", "---------------", "--------------",
				"--------------");

		session.close();
	}

	public static void main2(String[] args) {
		LogManager.getLogManager().reset();
		Logger globalLogger = Logger.getLogger(java.util.logging.Logger.GLOBAL_LOGGER_NAME);
		globalLogger.setLevel(java.util.logging.Level.OFF);

		factori = Conexion.getSession(); // Creo la sessionFactory una única vez.

		// insertardep();
		// insertarunempleado();
		cargardeparget(10);

		cargardeparget(100);

		cargardeparget(61);
		System.out.println("-----------empleado no existe");
		actualizardepalempleado(1111, 10); // empleado no existe
		System.out.println("-----------dep no existe");
		actualizardepalempleado(4450, 99); // dept no existe
		System.out.println("-----------ok");
		actualizardepalempleado(4450, 30); // correcta

		System.out.println("-----------Añadir al set de empleados");
		System.out.println("-----------error dep ");
		insertaempleadoalsetdedepartamento(999, 4455);
		System.out.println("-----------error emple ");
		insertaempleadoalsetdedepartamento(30, 445599);

		System.out.println("-----------OK ");
		insertaempleadoalsetdedepartamento(30, 4455);

		factori.close();

	}

	private static void consultasobjetos() {
		Session session = factori.openSession();
		String hql = "from Empleados e, Departamentos d where  e.departamentos.deptNo=d.deptNo order by e.apellido";
		Query cons = session.createQuery(hql, Object.class);
		List datos = cons.list();

		for (int i = 1; i < datos.size(); i++) {
			Object[] par = (Object[]) datos.get(i);
			Empleados em = (Empleados) par[0]; // objeto empleado el primero
			Departamentos de = (Departamentos) par[1]; // objeto departamento el segundo
			System.out.println(em.getApellido() + "*" + em.getSalario() + "*" + de.getDnombre() + "*" + de.getLoc());
		}
		session.close();

	}

	private static void insertaempleadoalsetdedepartamento(int nu, int emp) {
		Session session = factori.openSession();

		Departamentos dep = (Departamentos) session.get(Departamentos.class, nu);
		if (dep == null) {
			System.out.println("El departamento no existe. No se puede insertar empleado: " + nu);
		} else {
			// compruebo empleado
			Empleados emple = (Empleados) session.get(Empleados.class, BigInteger.valueOf(emp));
			if (emple == null) {
				System.out.println("El Empleado no existe. No se puede insertar: " + emp);
			} else {
				// lo añado al set
				Transaction tx = session.beginTransaction();
				dep.getEmpleadoses().add(emple);
				System.out.println("Empleado " + emp + " añadido al departamento " + nu);
				// session.update(dep);
				session.merge(dep);
				tx.commit();
			}
		}
		session.close();

	}

	private static void actualizardepalempleado(int emp, int nu) {

		Session session = factori.openSession();
		Empleados emple = (Empleados) session.get(Empleados.class, emp);
		if (emple == null) {
			System.out.println("El Empleado no existe. No se puede actualizar: " + emp);
		} else {
			Departamentos dep = (Departamentos) session.get(Departamentos.class, nu);
			if (dep == null)
				System.out.println("El departamento no existe. No se puede actualizar: " + nu);
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
			System.out.println("El departamento no existe" + nu);
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
