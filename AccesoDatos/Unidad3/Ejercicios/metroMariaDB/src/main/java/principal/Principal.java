package principal;

import java.util.ArrayList;
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

		factori = Conexion.getSession(); // Creo la sessionFactory una única vez.

		listarlineasestacionesaccesos();
		veraccesosporestacion();
		numTrenesxtipo();
		
		
//		System.out.println("-----Estacion existe");
//		veraccesosestacion(2);
//
//		System.out.println("-----Estacion sin accesos");
//		veraccesosestacion(21);
//
//		System.out.println("-----Estacion no existe");
//		veraccesosestacion(210);
//
//		// Lista de parámetros
//		List<String> tipos = new ArrayList<String>();
//		tipos.add("SERIE 3000");
//		tipos.add("SERIE 8400");
//
//		vertrenesportipo(tipos);
//		
//		tipos = new ArrayList<String>();
//		tipos.add("SERIE 5000");
//		tipos.add("SERIE 9000");
//		tipos.add("SERIE 8000");
//		
//		vertrenesportipo(tipos);
		
		factori.close();

	}

	private static void numTrenesxtipo() {
		/*    • Hacer un método java que visualice el número de trenes de cada tipo, y el tipo con más trenes. La salida ordenada por tipo
TIPO   Número de trenes
-----  ----------------  
. . .  . . . . . . .
-----  ----------------  
Tipo o tipos con más trenes: . . . .
Media de trenes por Tipo: . . . . .
*/
		Session session = factori.openSession();
		String con="select count(t), t.tipo from TTrenes t\r\n"
				+ "group by t.tipo";
		Query q = session.createQuery(con,Object.class);
		List<Object[]> lista=q.list();
		
		System.out.printf("%-30s %-30s %n",
	               "TIPO","NÚMERO DE TRENES");
		
		System.out.printf("%-30s %-30s %n",
	               "------------------------------","------------------------------");
		//Media de trenes por tipo 
		//Maxima serie con trenes 
		int contador=0; 
		float suma=0; 
		long max=0; 
		String maxS="";
		for (int i = 0; i < lista.size(); i++) {
			Object[] par = (Object[]) lista.get(i);
			Long num=(Long) par[0];
			String tip=(String) par[1];
	
			suma=suma+num;
			contador++;
			System.out.printf("%-30s %-30s %n",
		              tip,num);
			if(num>=max) {
				if(num==max) {
					maxS=maxS+tip+". ";
				
			}else {
				max=num;
				maxS=tip+". ";
			}
			}
			
		}
	
		float media=suma/contador; 
		System.out.println("Media de trenes por tipo: "+media);
		System.out.println("Nombre de tipo de trenes con mas trenes: "+maxS);
		System.out.println("Con un número de trenes máximo de: "+max);
		session.close();
		
		
	}

	private static void veraccesosporestacion() {
		Session session = factori.openSession();
		//Accesosporestacion
		String con = "select new clases.Accesosporestacion(e.codEstacion, e.nombre, e.direccion, count(a) ) "
				+ "   from TEstaciones e left join e.TAccesoses a"
				+ "   group by e.codEstacion, e.nombre, e.direccion"
				+ "   order by  e.codEstacion";
			
		Query q = session.createQuery(con, Accesosporestacion.class);
		List<Accesosporestacion> lista = q.list();
		//Cod_estación  Nombre  Dirección  Número de accesos
		System.out.printf("%6s %-30s %-30s %6s %n",
	               "CODEST","NOMBRE", "DIRECCIÓN","NUMACC");
			System.out.printf("%6s %-30s %-30s %6s %n",
		               "------","------------------------------", "------------------------------",
		               "------");
		String nombremax="";
		Long max=0l;
		int contador=0; 
		Float suma=0f; 
		for (Accesosporestacion acc:lista) {
			//System.out.println(acc.toString());
			System.out.printf("%6s %-30s %-30s %6s %n",
					acc.getCodEstacion(), acc.getNombre(),
					acc.getDireccion(), acc.getContador());
			if(acc.getContador()>=max) {
				if(acc.getContador()==max) {
					nombremax=nombremax+acc.getNombre()+". ";
				
			}else {
				max=acc.getContador();
				nombremax=acc.getNombre()+". ";
			}
			}
			contador ++;
			suma=suma+acc.getContador();
		} 
		System.out.printf("%6s %-30s %-30s %6s %n",
	               "------","------------------------------", "------------------------------",
	               "------");
		
		
		System.out.println("Nombre de estación con mas accesos: "+nombremax);
		System.out.println("Media de accesos por estación: "+suma/contador);
		
		
		
		
		session.close();
		
		
	}

	private static void listarlineasestacionesaccesos() {
		Session session = factori.openSession();
		String con = "from TLineas l join l.TLineaEstacions lt join lt.TEstaciones.TAccesoses ta order by l.codLinea";
		
		Query q = session.createQuery(con, Object.class);
		List<Object[]> lista = q.list();
		
		System.out.printf("%6s %-30s %6s %-30s %6s %-30s%n",
               "CODLIN","NOMBRELIN", "CODEST","NOMBREESTACION","CODACC","DESCR.ACCESO");
		System.out.printf("%6s %-30s %6s %-30s %6s %-30s%n",
	               "------","------------------------------", "------","------------------------------",
	               "------","------------------------------");
		
		
		for (int i = 1; i < lista.size(); i++) {
			Object[] par = (Object[]) lista.get(i);
			TLineas lin = (TLineas) par[0]; // objeto empleado el primero
			TLineaEstacion liest = (TLineaEstacion) par[1];
			
			//TEstaciones  acc= (TEstaciones) par[2];
			
			TAccesos acc = (TAccesos) par[3];
			
			System.out.printf("%6s %-30s %6s %-30s %6s %-30s%n",
					lin.getCodLinea(), lin.getNombre(),
					liest.getTEstaciones().getCodEstacion(),
					liest.getTEstaciones().getNombre(),
					//acc.getCodEstacion(), acc.getNombre()
			        acc.getCodAcceso(), acc.getDescripcion()
					);
		}
		
		
		System.out.println();	System.out.println();
		
		for (Object linaes : lista) {
			
			Object[] par = (Object[]) linaes;
			TLineas lin = (TLineas) par[0]; 
			TLineaEstacion liest = (TLineaEstacion) par[1];
			//TAccesos  acc= (TAccesos) par[2];
			
			System.out.println("linaes "+ linaes.toString());
				
		}
		
		session.close();
	}

	private static void vertrenesportipo(List<String> tipos) {
		Session session = factori.openSession();
		String con = "from TTrenes  e where e.tipo in (:lista)";

		Query q = session.createQuery(con, TTrenes.class);
		q.setParameterList("lista", tipos);
		List<TTrenes> lista = q.list();
		
		System.out.println("**** Trenes de los tipos: " + tipos.toString());
		
		for (TTrenes tren : lista) {
			System.out.printf("%5s %20s %15s%n", tren.getCodTren(), tren.getNombre(),
					tren.getTipo());
		}
		
		
		session.close();
	}

	private static void veraccesosestacion(int codestacion) {
		Session session = factori.openSession();

		// comprobar si la estación existe
		TEstaciones est = session.get(TEstaciones.class, codestacion);

		if (est == null) {

			System.out.println("**** Estación NO EXISTE: " + codestacion);

		} else {

			String con = "from TAccesos e where e.TEstaciones.codEstacion = :idestacion " + " order by e.codAcceso";

			Query q = session.createQuery(con, TAccesos.class);
			q.setParameter("idestacion", codestacion);
			List<TAccesos> lista = q.list();

			System.out.println("Accesos a la estación con código: " + codestacion);

			if (lista.size() > 0) {
				System.out.printf("%15s %20s %n", "CÓD DE ACCESO", "DESCRIPCIÓN");
				System.out.printf("%15s %20s %n", "---------------", "---------------");
				for (TAccesos acc : lista) {
					System.out.printf("%15s %20s %n", acc.getCodAcceso(), acc.getDescripcion());
				}

			} else {
				System.out.println("**ESTACIÓN SIN ACCESOS * ");
			}

		}
		session.close();
	}

	public static void main2(String[] args) {
		LogManager.getLogManager().reset();
		Logger globalLogger = Logger.getLogger(java.util.logging.Logger.GLOBAL_LOGGER_NAME);
		globalLogger.setLevel(java.util.logging.Level.OFF);

		factori = Conexion.getSession(); // Creo la sessionFactory una única vez.

//		System.out.println("----------------");
//		verlinea(1); // de 1 a 7
//		
//		System.out.println("----------------");
//		verlinea(5); // linea sin estaciones
//		
//		System.out.println("----------------");
//		verlinea(50); // linea no existe

		// actualizar línea estación línea, estación y orden
		System.out.println("---Linea-estacion existe ");
		actualizarlineaestacion(1, 1, 14);

		System.out.println("---Linea-estacion No existe ");
		actualizarlineaestacion(2, 1, 14);

		factori.close();
	}

	private static void actualizarlineaestacion(int codlin, int codest, int orden) {

		Session session = factori.openSession();
		TLineaEstacionId clave = new TLineaEstacionId(codlin, codest);

		TLineaEstacion liest = session.get(TLineaEstacion.class, clave);

		if (liest == null) {
			System.out.println("---Linea-estación No existe: (" + codlin + " , " + codest + ")");
		} else {
			Transaction tx = session.beginTransaction();
			liest.setOrden(orden);
			session.merge(liest);
			tx.commit();
			System.out.println("---Linea-estación-orden actualizado: (" + codlin + " , " + codest + ")");

		}

		session.close();

	}

	private static void verlinea(int id) {

		Session session = factori.openSession();
		TLineas lin = (TLineas) session.get(TLineas.class, id);

		if (lin == null) {
			System.out.println("Cod línea no existe: " + id);
		} else {
			System.out.println("COD LINEA: " + lin.getCodLinea() + "   NOMBRE: " + lin.getNombre());
			System.out.println(" * Estaciones de la línea: " + lin.getTLineaEstacions().size());
			if (lin.getTLineaEstacions().size() > 0) {
				System.out.printf("    %10s %-30s %-30s %n", "CODIGO", "NOMBRE", "DIRECCION");
				System.out.printf("    %10s %-30s %-30s %n", "---------", "---------------", "----------------");
				;
				Set<TLineaEstacion> lista = lin.getTLineaEstacions();
				for (TLineaEstacion ll : lista) {
					System.out.printf("    %10s %-30s %-30s %n", ll.getTEstaciones().getCodEstacion(),
							ll.getTEstaciones().getNombre(), ll.getTEstaciones().getDireccion());

				}

			}
			System.out.println(" * Trenes de la línea: " + lin.getTTreneses().size());
			if (lin.getTTreneses().size() > 0) {
				Set<TTrenes> listatrenes = lin.getTTreneses();
				System.out.printf("    %10s %-30s %-15s %10s %-30s%n", "CODIGO", "NOMBRE", "TIPO", "CODCOCHERA",
						"NOMBRE_COCHERA");
				System.out.printf("    %10s %-30s %-15s %10s %-30s%n", "----------", "------------------------------",
						"--------------------", "----------", "------------------------------");
				for (TTrenes ll : listatrenes) {
					System.out.printf("    %10s %-30s %-15s %10s %-30s%n", ll.getCodTren(), ll.getNombre(),
							ll.getTipo(), ll.getTCocheras().getCodCochera(), ll.getTCocheras().getNombre());

				}
			}

		}

	}

}
