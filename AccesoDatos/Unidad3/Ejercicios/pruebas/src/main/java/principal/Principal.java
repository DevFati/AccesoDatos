package principal;

import java.math.BigInteger;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.JDBCException;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.Query;

import clases.Departamentos;
import clases.Empleados;
import jakarta.persistence.TypedQuery;
import oracle.jdbc.driver.json.JakartaExceptionFactory;

public class Principal {
private static SessionFactory sesion;

public static void main(String[] args) {
	Logger.getLogger("org.hibernate").setLevel(Level.OFF);

	sesion=Conexion.getSession(); 
	//probamos a cargardepar con load 
	cargardepar(10);
	cargardepar(88);
	//probamos cargar departamento con get 
	cargardeparget(10);
	cargardeparget(88);
	System.out.println("==================================================");
	//ver empleados de un departamento 
	verdepartamento(10);
	verdepartamento(88);
	
	//actualizar depart, sino existe lo creamos 
	
	insertarmodifdepart(15,"DEP INFOR","TALAVERA");
	
	System.out.println("===================================");
	actualizardepalempleado(1111, 10); //empleado no existe
	
	actualizardepalempleado(7000, 99); //dept no existe
	
	actualizardepalempleado(7000, 10); //correcta
	
	System.out.println("-----------Añadir al set de empleados");
	System.out.println("-----------error dep ");
	insertaempleadoalsetdedepartamento( 999, 4455) ;
	System.out.println("-----------error emple ");
	insertaempleadoalsetdedepartamento( 30, 445599) ;
	
	System.out.println("-----------OK ");
	insertaempleadoalsetdedepartamento( 20, 7000) ;
	
	//Borramos un departamento 
	borrardepar(10);
	
	
	
	//listardepartamentos2 (usando getresultlist)
	listardepartamentos2();
	
	//listar empleados de un dep 
	
	listaremplesdeundep(10);
	
	//listar departamentos 
		System.out.println("LISTAMOS LOS DEPARTAMENTOS: ");
		listardepartamentos();
	
		//listardepartamentos pero usando set de empleados en vez 
		//de sacarlos con consulta
		System.out.println("LISTAMOS LOS DEPARTAMENTOS SET: ");
		listardepartamentosSet();

		//Consulta uniqueResult cuando se devuelve un unico objeto 
		//
		System.out.println("USAMOS UNIQUE RESULT: ");
		consultauniqueresul();
	
	sesion.close();
	
}

private static void consultauniqueresul() {
	
	Session session = sesion.openSession();
	//Visualiza los datos del departamento 10  
	System.out.println("------------------------");
	Departamentos depart = (Departamentos) session.createQuery(
		    "from Departamentos as dep where dep.deptNo = 10").uniqueResult();
	System.out.println(depart.getLoc() +"*"+depart.getDnombre());	

	//Visualiza los datos del departamento con nombre CONTABILIDAD
	System.out.println("------------------------");
	depart = (Departamentos) session.createQuery("from Departamentos as dep where dep.dnombre = 'CONTABILIDAD'").uniqueResult();
	System.out.println(depart.getLoc() +"*"+depart.getDeptNo());

	System.out.println("------------------------");
	Long cont =  (Long) session.createQuery("select count(*) from Empleados ").uniqueResult();
	System.out.println("Número de empleados: " + cont);

	System.out.println("------------------------");
	Double media =  (double) session.createQuery("select avg(salario) from Empleados ").uniqueResult();
	System.out.println("Media de salario de empleados: " + media);

		
	System.out.println("------------------------");
	Double maxi =  (Double) session.createQuery("select max(salario) from Empleados ").uniqueResult();
	System.out.println("Máximo de salario de empleados: " + maxi);

	session.close();
		
	}

private static void listardepartamentosSet() {
	Session session = sesion.openSession();
	Departamentos depar = new Departamentos();
	
	Query<Departamentos> q = session.createQuery("from Departamentos");
	List<Departamentos> lista = q.getResultList(); //)list();
	int num = lista.size();
	System.out.println("Número de departamentos: " + num);
	for (int i = 0; i < num; i++) {
		// extraer el objeto
		depar = (Departamentos) lista.get(i);
		// obtenemos empleados
		Set<Empleados> listaemple = depar.getEmpleadoses();
		Iterator<Empleados> it = listaemple.iterator();
		System.out.println();
		System.out.println("Num dep: " + depar.getDeptNo() +
			" Nombre Dep:" + depar.getDnombre() +
			"  Localidad:" + depar.getLoc() +
			"  Número de empleados: "+ depar.getEmpleadoses().size());
		System.out.printf("%10s %15s %15s %15s %15s %n",
					"EMPNO", "APELLIDO","OFICIO","FECHAALTA", "SALARIO");
		System.out.printf("%10s %15s %15s %15s %15s %n",
					"----------", "---------------","---------------",
"---------------", "---------------");
		float totalsalario=0;
		while (it.hasNext()) {
			Empleados emple = new Empleados();
			emple = it.next();
			System.out.printf("%10s %15s %15s %15s %15s %n",
				emple.getEmpNo(), emple.getApellido(),emple.getOficio(),
				emple.getFechaAlt(), emple.getSalario());
			totalsalario = (float) (totalsalario +  emple.getSalario());
		}
		System.out.printf("%10s %15s %15s %15s %15s %n","----------", 
 "---------------","---------------","---------------", "---------------");
		System.out.printf("%-26s %15s %15s %15s %n",
					"Total salario: ","","", totalsalario);
		System.out.printf("%10s %15s %15s %15s %15s %n","----------",
 "---------------","---------------","---------------", "---------------");
	}
		
		session.close();
}


private static void listaremplesdeundep(int dep) {
	Session session = sesion.openSession();
	Empleados emp = new Empleados();
	System.out.println("-------------------------------");

 	Query <Empleados> q= session.createQuery("from Empleados e where e.departamentos.deptNo =:id");
	q.setParameter("id", dep);
	
 	List<Empleados> lista = q.list();
	
	int num = lista.size();
	System.out.println("Número de empleados del departamento " + dep+" : "+num);
	for (int i = 0; i < num; i++) {
		// extraer el objeto
		emp = (Empleados) lista.get(i);
		System.out.println(emp.getEmpNo() + "*" + emp.getApellido());
	}
	
	session.close();
	
}


private static void listardepartamentos2() {
	Session session = sesion.openSession();
	Departamentos depar = new Departamentos();
	System.out.println("-------------------------------");

 	TypedQuery <Departamentos> q= session.createQuery("from Departamentos");
	List<Departamentos> lista = q.getResultList();
	
	int num = lista.size();
	System.out.println("Número de departamentos: " + num);
	for (int i = 0; i < num; i++) {
		// extraer el objeto
		depar = (Departamentos) lista.get(i);
		System.out.println(depar.getDeptNo() + "*" + depar.getDnombre());
	}
	
	session.close();
}

	private static void listardepartamentos() {
	
		Session session=sesion.openSession();
		Departamentos depar=new Departamentos();
		System.out.println("--------------------------------------");
		Query<Departamentos> q=session.createQuery("from Departamentos");
		List<Departamentos> lista=q.list();
		int num=lista.size();
		System.out.println("Número de departamentos: "+num);
		
		Empleados emp=new Empleados();
		System.out.println("--------------------------------------");
		Query<Empleados> q2=session.createQuery("from Empleados t where t.departamentos.deptNo= :id ");
		
		
		
		
		for(int i=0; i<num;i++) {

			//extraer el objeto 
			depar=(Departamentos) lista.get(i);

			q2.setParameter("id", depar.getDeptNo());
			List<Empleados> listaEmp=q2.list();
			int num2=listaEmp.size();
			System.out.println("Num dep: "+depar.getDeptNo()+"  Nombre: "+depar.getDnombre()+"  Localidad: "+depar.getLoc()+" Número de empleados: "+num2);
			
			if(num2>0) {
				System.out.printf("%10s %-15s %14s %-14s %10s %n", "EMPNO", "APELLIDO", "OFICIO", "FECHAALTA","SALARIO");
				System.out.printf("%10s %-15s %14s %-14s %10s %n", "----------", "---------------", "--------------", "--------------","----------");
				double totalsalario=0;
				for(int j=0; j<num2;j++) {
					emp=(Empleados) listaEmp.get(j);
					System.out.printf("%10s %-15s %14s %-14s %10s %n", emp.getEmpNo(), emp.getApellido(), emp.getOficio(), emp.getFechaAlt(),emp.getSalario());
					totalsalario=totalsalario+emp.getSalario();
				}
				System.out.printf("%10s %-15s %14s %-14s %10s %n", "----------", "---------------", "--------------", "--------------","----------");
				System.out.printf("%-26s %15s %15s %15s %n",
						"Total salario: ","","", totalsalario);				System.out.printf("%10s %-15s %14s %-14s %10s %n", "----------", "---------------", "--------------", "--------------","----------");

			}else {
				System.out.println("El departamento no disponde de empleados");
			}
			

			
		}
		
		session.close();
	
}


	private static void borrardepar(int nu) {
	
		Session session=sesion.openSession();
		Departamentos dep=(Departamentos) session.get(Departamentos.class, nu);

		if(dep==null) {
			System.out.println("El departamento no existe. No se puede borrar");
		}else {
			try {
				Transaction tx=session.beginTransaction();
				session.remove(dep);
				tx.commit();
				System.out.println("DEPARTAMENTO BORRADO");
			}catch(jakarta.persistence.PersistenceException e) {
					System.out.println("NO SE PUEDE BORRAR. TIENE REGISTROS RELACIONADOS");
				
			}
			
		}
		
	session.close();
}


	private static void insertaempleadoalsetdedepartamento(int nu, int emp) {
		Session session=sesion.openSession();
		Transaction tx=session.beginTransaction();
		
		Departamentos dep=(Departamentos) session.get(Departamentos.class, nu);
		
		if(dep==null) {
			System.out.println("El departamento no existe. No se puede insertar");
		}else {
			Empleados e=(Empleados) session.get(Empleados.class, emp);
			if(e==null) {
				System.out.println("El empleado no existe. No se puede insertar");
			}else {
			
				//Se añade al set 
				dep.getEmpleadoses().add(e);
				System.out.println("Empleado "+emp+" añadido al departamento "+nu);
				session.merge(dep);
			}
		}
		tx.commit();
		session.close();
	
}


	private static void actualizardepalempleado(int emp, int nu) {
		Session session=sesion.openSession();
		Transaction tx=session.beginTransaction();
		Empleados e=(Empleados) session.get(Empleados.class, emp);
		
		if(e==null) {
			System.out.println("Empleado no existe. No se puede actualizar");
		}else {
			Departamentos dep=(Departamentos) session.get(Departamentos.class, nu);
			if(dep==null) {
				System.out.println("El departamento no existe. No se puede actualizar");
			}else {
				e.setDepartamentos(dep);
				System.out.println("Empleado "+emp+" actualizado al departamento "+nu);
				//guardamos los cambios 
				session.merge(e);
				tx.commit();
			}
		}
		
	}
	


	private static void insertarmodifdepart(int nu, String nom, String loc) {
	System.out.println("===================================");
		Session session=sesion.openSession();
		Transaction tx=session.beginTransaction();
		Departamentos dep=(Departamentos) session.get(Departamentos.class, nu);
		System.out.println("DATOS DEL DEPARTAMENTO "+nu);
		
		if(dep==null) {
			System.out.println("El departamento no existe, LO CREO");
			dep=new Departamentos();
			BigInteger n=BigInteger.valueOf(nu);
			dep.setDeptNo(n);
			dep.setDnombre(nom);
			dep.setLoc(loc);
			session.persist(dep); //para guardar los cambios en la bbdd
		}else {
			System.out.println("El departamento existe, LO MODIFICO");
			dep.setDnombre(nom);
			dep.setLoc(loc);
			session.persist(dep);
		}
		
		tx.commit();
		session.close();
	
}


	private static void verdepartamento(int nu) {
	Session session=sesion.openSession();
	System.out.println("Cargo departamento");
	
	Departamentos dep=(Departamentos) session.get(Departamentos.class, nu);
	System.out.println("================================");
	System.out.println("DATOS DEL DEPARTAMENTO "+nu);
	if(dep==null) {
		System.out.println("El departamento no existe");
	}else {
		System.out.println("Nombre Dep: "+dep.getDnombre());
		System.out.println("Localidad: "+dep.getLoc());
		System.out.println("=============================");
		System.out.println("EMPLEADOS DEL DEPARTAMENTO");
		
		//Se obtiene un conjunto set de empleados 
		//el metodo getEmpleadoses devuelve todos los empleados asociados 
		//a un departamento 
		Set<Empleados> listaemple=dep.getEmpleadoses();
		//obtenemos los empleados 
		
		//Se crea un iterador para recorrer los elementos del conjunto 
		//listaemple
	//	Iterator<Empleados> it=listaemple.iterator();
		
	//	System.out.println("Número de empleados: "+listaemple.size());
		//se recorre el conjunto de empleados usando el iterador 
		
	/*	while(it.hasNext()) {
			Empleados emple=new Empleados();
			//emple se asigna al siguiente elemento del iterador 
			emple=it.next();
			System.out.println(emple.getApellido()+" * "+emple.getSalario());
		}
		*/
		//podemos usar un foreach para recorrer el set 
		for (Empleados emple : listaemple) {
			   System.out.println(emple.getApellido() + " * " + emple.getSalario());
			}
		
	}
	
	System.out.println("======================");
	session.close();
	
	
}











	private static void cargardeparget(int nu) {
	
		Session session=sesion.openSession();
		Departamentos dep=(Departamentos) session.get(Departamentos.class, nu);
		if(dep==null) {
			System.out.println("El departamento no existe");
			
		}else {
			System.out.println("Nombre Dep: "+dep.getDnombre());
			System.out.println("Localidad: "+dep.getLoc());
		}
		
		session.close();
	
}











	private static void cargardepar(int nu) {
	Session session=sesion.openSession();
	try {
		Departamentos dep=(Departamentos) session.load(Departamentos.class, nu);
		System.out.println("Nombre: "+dep.getDnombre());
		System.out.println("Localidad: "+dep.getLoc());
		
	}catch (ObjectNotFoundException e) {
		System.out.println("NO EXISTE EL DEPARTAMENTO");
	}
	
	session.close();
	
}











	public static void main2(String[] args) {
		
		//Para que no salgan los mensajes que lanza hibernate por defecto
		Logger.getLogger("org.hibernate").setLevel(Level.OFF);
		sesion=Conexion.getSession(); 
		//insertardepartamento();
		insertarEmpleado();
		sesion.close();
		
		 
		
		
	}
	
	private static void insertarEmpleado() {
		Session session=sesion.openSession(); 
		Transaction tx=session.beginTransaction();
		
		Empleados emp=new Empleados(); 
		BigInteger n=BigInteger.valueOf(7000);
		emp.setEmpNo(n);
		emp.setApellido("RUIZ");
		emp.setOficio("ANALISTA");
		BigInteger d=BigInteger.valueOf(7698);
		emp.setDir(d);
		//fecha 
		java.util.Date hoy=new java.util.Date();
		java.sql.Date fecha=new java.sql.Date(hoy.getTime());
	
		emp.setFechaAlt(fecha);
		emp.setSalario(1500d);
		emp.setComision(10d);
		BigInteger depn=BigInteger.valueOf(10);
		Departamentos dep =new Departamentos(depn);
		emp.setDepartamentos(dep);
		try {
			session.persist(emp);
			tx.commit();
				       System.out.println("Reg INSERTADO.");

		// } catch (javax.persistence.PersistenceException e) { //Si no se dispara esta

		} catch (jakarta.persistence.PersistenceException e) {

					if (e.getMessage().contains("unique constraint")) {
						System.out.println("CLAVE DUPLICADA. EMPLEADO YA EXISTE");
					} else if (e.getMessage().contains("value too large")) {
						System.out.println("ERROR EN LOS DATOS DE EMPLEADO, DEMASIADOS CARACTERES");
					} else if (e.getMessage().contains("org.hibernate.exception.GenericJDBCException")) {
						System.out.println("ERROR JDBC. NO SE HA PODIDO EJECUATR LA CONSULTA");
					} else
						System.out.println("HA ocurrido un error: " + e.getMessage());

				} catch (Exception e) {
					System.out.println("ERROR NO CONTROLADO....");
					System.out.println(e.getMessage());
					e.printStackTrace();
				}

				session.close(); //cierro la sesión de trabajo
		
		
		
	}

	private static void insertardepartamento() {
		Session session = sesion.openSession(); //creo una sesión de trabajo
		Transaction tx = session.beginTransaction();

		Departamentos dep = new Departamentos();
		BigInteger n=BigInteger.valueOf(64);
		dep.setDeptNo(n);
		dep.setDnombre("MARKET");
		dep.setLoc("GUADALAJARA");

		try {
	session.persist(dep);
	tx.commit();
		       System.out.println("Reg INSERTADO.");

// } catch (javax.persistence.PersistenceException e) { //Si no se dispara esta

} catch (jakarta.persistence.PersistenceException e) {

			if (e.getMessage().contains("unique constraint")) {
				System.out.println("CLAVE DUPLICADA. DEPARTAMENTO YA EXISTE");
			} else if (e.getMessage().contains("value too large")) {
				System.out.println("ERROR EN LOS DATOS DE DEPARTAMENTO, DEMASIADOS CARACTERES");
			} else if (e.getMessage().contains("org.hibernate.exception.GenericJDBCException")) {
				System.out.println("ERROR JDBC. NO SE HA PODIDO EJECUATR LA CONSULTA");
			} else
				System.out.println("HA ocurrido un error: " + e.getMessage());

		} catch (Exception e) {
			System.out.println("ERROR NO CONTROLADO....");
			System.out.println(e.getMessage());
			e.printStackTrace();
		}

		session.close(); //cierro la sesión de trabajo

	}
	
	





	public static void main1(String[] args) {
	//Para que no salgan los mensajes que lanza hibernate por defecto
		Logger.getLogger("org.hibernate").setLevel(Level.OFF);
		 // Inicializa el entorno Hibernate 
		   Configuration cfg = new Configuration().configure();
	  	   // Crea el ejemplar de sesion factory (fabrica de sesiones)
		   SessionFactory sessionFactory = cfg.buildSessionFactory();
		   // Obtiene un objeto session
		   Session sesion = sessionFactory.openSession();
		   Transaction tx =sesion.beginTransaction();		
		
		   System.out.println("Inserto una fila en la tabla DEPARTAMENTOS." );	
		   
		   
		   Departamentos dep = new Departamentos();
		   dep.setDeptNo(BigInteger.valueOf(63));
		   dep.setDnombre("MARKETING");
		   dep.setLoc("GUADALAJARA");
		   try {
			sesion.save(dep);
			tx.commit();
	            System.out.println("REGISTRO GRABADO ");	
			}catch (JDBCException j)
			{
				   System.out.println("Codigo error: "+j.getErrorCode());	
				   System.out.println("Mensaje : "+j.getMessage() );	
			}
			catch(Exception e)
			{
				   System.out.println("Codigo error: "+e.hashCode() );	
				   System.out.println("Mensaje : "+e.getMessage() );	
			}
	          sesion.close();
                 sessionFactory.close();
		
		}


}


