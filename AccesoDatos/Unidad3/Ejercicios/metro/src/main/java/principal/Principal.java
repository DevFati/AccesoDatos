package principal;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;


import clases.TAccesos;
import clases.TCocheras;
import clases.TEstaciones;
import clases.TLineaEstacion;
import clases.TLineas;
import clases.TTrenes;


public class Principal {
	private static SessionFactory sesion;

public static void main(String[] args) {
	Logger.getLogger("org.hibernate").setLevel(Level.OFF);

	sesion=Conexion.getSession(); 
	lineaEstacion(1,1);
	System.out.println("======================================");
	detallesLinea(3);
	
//	actualizarTren(1,"COCHE 3000 , 1","SERIE 3000",4,2);
	
	insertarTren(2002,"COCHE 3000 , 1","SERIE 3000",2,2);
	
	borrarTren(1);
	
	borrarestacion(1);
	
	//Ver por cada estacion su numero d accesos y numero de lineas 
	
	detallesEstacion();
	
	// veraccesosporestacion();

	System.out.println("VER ACCESSOS POR ESTACION: ");
	System.out.println("-----Estacion existe");
	veraccesosestacion(2);

	System.out.println("-----Estacion sin accesos");
	veraccesosestacion(21);

	System.out.println("-----Estacion no existe");
	veraccesosestacion(210);
	
	//Obtener los trenes de la serie 5000, 6000 y 8000
	
System.out.println("LISTA DE PARAMETROS: ");
	List<String> tipos = new ArrayList<String>();
	tipos.add("SERIE 3000");
	tipos.add("SERIE 8400");

	vertrenesportipo(tipos);
	
	List<String> tipo2s = new ArrayList<String>();

	tipo2s = new ArrayList<String>();
	tipo2s.add("SERIE 5000");
	tipo2s.add("SERIE 9000");
	tipo2s.add("SERIE 8000");
	
	vertrenesportipo(tipo2s);

	sesion.close();
}

private static void vertrenesportipo(List<String> tipos) {	
	Session session = sesion.openSession();
	TTrenes tren=new TTrenes();
	String hql="from TTrenes e where e.tipo in  (:listatren) ";
	Query q=session.createQuery(hql,TTrenes.class);
	q.setParameterList("listatren", tipos);
	List<TTrenes> lista3=q.list();
	System.out.println("----------------------------");
	
	
	for(int i=0;i<lista3.size();i++) {
		tren =lista3.get(i);
		System.out.println("Tren tipo:  "+tren.getTipo()+ "  Nombre tren: "+tren.getNombre());
	}
	
	session.close();
	
}

private static void veraccesosestacion(int codestacion) {
	Session session = sesion.openSession();
	TAccesos accesos=new TAccesos();
	TEstaciones est=session.get(TEstaciones.class, codestacion);
	if(est==null) {
		System.out.println("ESTACION NO EXISTE: "+codestacion);
	}else {
	
	String con="from TAccesos t where t.TEstaciones.codEstacion= :cod";
	Query q = session.createQuery(con,TAccesos.class);
	q.setParameter("cod", codestacion);
	List<TAccesos> lista=q.list();
	int num=lista.size();
	
	 System.out.println("CODESTACION:       "+codestacion);
	
if(num>0) {
	 System.out.printf("%-20s %-30s%n", "CODIGOACCESO","DESCRIPCION");
	 System.out.printf("%-20s %-30s %n","--------------------","------------------------------");
	 for(int i=0; i<num;i++) {
		 accesos= (TAccesos) lista.get(i);
		 System.out.printf("%-20s %-30s%n", accesos.getCodAcceso(),accesos.getDescripcion());
	 }
}else {
	System.out.println("SIN ACCESOS");
}
	
	}
	session.close();
	
}

private static void detallesEstacion() {
	Session session = sesion.openSession();
	TEstaciones estacion=new TEstaciones();
	String con="from TEstaciones";
	Query q = session.createQuery(con,TEstaciones.class);
	
	List<TEstaciones> lista=q.getResultList();
	int num=lista.size();
	 System.out.printf("%-20s %-30s %-30s %20s %20s%n","COD ESTACION:","NOMBRE","DIRECCION", "NUMERO ACCESOS", "NUMERO DE LINEAS");
	 System.out.printf("%-20s %-30s %-30s %20s %20s%n","--------------------","------------------------------","------------------------------", "--------------------", "--------------------");

	for(int i=0; i<num;i++) {
		//extraemos el objeto 
		estacion=(TEstaciones) lista.get(i);
		 System.out.printf("%-20s %-30s %-30s %20s %20s%n",estacion.getCodEstacion(),estacion.getNombre(),estacion.getDireccion(), estacion.getTAccesoses().size(), estacion.getTLineaEstacions().size());

	}

	session.close();
	
}

private static void borrarestacion(int cod) {
	Session session=sesion.openSession();
	TEstaciones est=session.get(TEstaciones.class, cod);
	Transaction tx=session.beginTransaction();
	
	try {
		System.out.println("Voy a borrar: "+cod+", "+est);
		if(est!=null) {
			session.remove(est);
			tx.commit();
			System.out.println("Estacion Borrada: "+cod);
		}else {
			System.out.println("Estacion NO EXISTE: "+cod);
		}
	}catch(jakarta.persistence.PersistenceException  e) {
		System.out.println("ERROR DE PERSISTENCIA");
		System.out.println("NO SE PUEDE BORRAR HAY REGISTROS RELACIONADOS");
		
	}
	session.close();
	
}

private static void borrarTren(int codTren) {
	

	Session session=sesion.openSession();
	TTrenes tren=(TTrenes) session.get(TTrenes.class, codTren);

	if(tren==null) {
		System.out.println("El tren no existe. No se puede borrar");
	}else {
		try {
			Transaction tx=session.beginTransaction();
			session.remove(tren);
			tx.commit();
			System.out.println("TREN BORRADO");
		}catch(jakarta.persistence.PersistenceException e) {
				System.out.println("NO SE PUEDE BORRAR. TIENE REGISTROS RELACIONADOS");
			
				 System.out.println("Mensaje : "+e.getMessage() );
		}
		
	}
	
session.close();
}

private static void insertarTren(int codTren, String nombre, String tipo, int codLinea, int codCochera) {
	System.out.println("===================================");
	Session session=sesion.openSession();
	Transaction tx=session.beginTransaction();
	TTrenes tren=(TTrenes) session.get(TTrenes.class, codTren);
	TLineas linea;
	TCocheras cochera;
	if(tren==null) {
		System.out.println("El tren no existe. Lo insertamos");
		 linea=(TLineas) session.get(TLineas.class, codLinea);
		 cochera=(TCocheras) session.get(TCocheras.class, codCochera);

		if(linea == null || cochera ==null) {
			if(linea==null) {
				System.out.println("La línea no existe.");

			}
			
			if(cochera==null) {
				System.out.println("La cochera no existe.");

			}
			return; 
		}else {
			tren=new TTrenes();
			tren.setCodTren(codTren);
			tren.setNombre(nombre);
			tren.setTipo(tipo);
			tren.setTCocheras(cochera);
			tren.setTLineas(linea);
		
			

			session.persist(tren);
		}
		try {
			tx.commit();
			System.out.println("TREN INSERTADO CORRECTAMENTE");
		}catch(Exception e) {
			   System.out.println("Mensaje : "+e.getMessage() );	

		}
		
	}else {
		System.out.println("Tren ya existe");
	}
	
	
	
	session.close();
	
}

private static void actualizarTren(int codTren, String nombre, String tipo, int codLinea, int codCochera) {
	System.out.println("===================================");
	Session session=sesion.openSession();
	Transaction tx=session.beginTransaction();
	TTrenes tren=(TTrenes) session.get(TTrenes.class, codTren);
	TLineas linea;
	TCocheras cochera;
	if(tren==null) {
		System.out.println("El tren no existe.");
	}else {
		System.out.println("El tren existe.");
		 linea=(TLineas) session.get(TLineas.class, codLinea);
		 cochera=(TCocheras) session.get(TCocheras.class, codCochera);

		if(linea == null || cochera ==null) {
			if(linea==null) {
				System.out.println("La línea no existe.");

			}
			
			if(cochera==null) {
				System.out.println("La cochera no existe.");

			}
		}else {
			tren.setNombre(nombre);
			tren.setTipo(tipo);
			tren.setTCocheras(cochera);
			tren.setTLineas(linea);
		
			

			session.persist(tren);
			System.out.println("TREN MODIFICADO CORRECTAMENTE");
		}
		
	}
	
	
	tx.commit();
	session.close();
	
}

private static void detallesLinea(int cod) {
 Session session=sesion.openSession();
 
 String con="from TLineas l join l.TLineaEstacions "
 		+ "lt join l.TTreneses tre join tre.TCocheras "
 		+ "where l.codLinea=:id";
 
 Query<Object[]>q=session.createQuery(con,Object[].class);
 
 q.setParameter("id", cod);
 List<Object[]> lista = q.list();
 
 
 System.out.printf("%-10s %-30s %n","Cod Linea:","Nombre");
 System.out.printf("%-10s %-30s %n","----------","------------------------------");
 if(lista.size()>0) {
	 Object[] param=(Object[]) lista.get(0);
		TLineas lin = (TLineas) param[0];
		 System.out.printf("%-10s %-30s %n",lin.getCodLinea(),lin.getNombre());

	

		 
	 
	 System.out.println("Estaciones de la línea: ");
	 System.out.printf("%-10s %-30s %-30s %n","CODIGO:","NOMBRE","DIRECCION");
	 System.out.printf("%-10s %-30s %-30s %n","----------","------------------------------","------------------------------");
	 
//	Set<TEstaciones> estacionesSinDup= new HashSet<>();
	//usamos un linkedhashset para conservar el orden 
	 
	LinkedHashSet<TEstaciones> estacionesSinDup= new LinkedHashSet<>();
	 
	 for (int i=0; i<lista.size();i++) {
		 Object[] p=(Object[]) lista.get(i);
		 TLineaEstacion liest = (TLineaEstacion) p[1];
		 estacionesSinDup.add(liest.getTEstaciones());				
		}
	 
	 //Imprimimos 
	 
	 for(TEstaciones e: estacionesSinDup) {
		 System.out.printf("%-10s %-30s %-30s %n",e.getCodEstacion(),e.getNombre(),e.getDireccion());
 
	 }
	 
	 System.out.println("Trenes de la línea: ");
	 System.out.printf("%-10s %-30s %-30s %-15s %-30s %n","CODIGO:","NOMBRE","TIPO", "COD_COCHERA","NOMBRE_COCHERA");
	 System.out.printf("%-10s %-30s %-30s %-15s %-30s %n","----------","------------------------------","------------------------------", "---------------","------------------------------");
	 
		LinkedHashSet<String> trenes= new LinkedHashSet<>();

	 for (int i=0; i<lista.size();i++) {
		 Object[] params=(Object[]) lista.get(i);
			TTrenes tren=(TTrenes) params[2];
				TCocheras coch=(TCocheras) params[3];	
				 String txt= String.format("%-10s %-30s %-30s %-15s %-30s %n",tren.getCodTren(),tren.getNombre(),tren.getTipo(), coch.getCodCochera(),coch.getNombre());

				 trenes.add(txt);
				 
		}
	 
	 for(String t: trenes) {
		 System.out.print(t);
	 }
	 
	 
 }else{
	 System.out.println("La línea no existe");
 }



 session.close();
 
	
}

private static void lineaEstacion(int l, int e) {
	Session session=sesion.openSession();
	
		//La consulta selecciona multiples columnas 
	//en lugar de toda una entidad 

		String con= "select orden ,TEstaciones.nombre, TLineas.nombre from TLineaEstacion "
				+ "where TLineas.codLinea= :linea and "
				+ "TEstaciones.codEstacion = :estacion";
		
		Query<Object[]> q=session.createQuery(con,Object[].class);
	
		q.setParameter("linea", l);
		q.setParameter("estacion", e);
		Object[] r=  q.uniqueResult();
		
		
		if(r == null) {
			System.out.println("No existe ninguna linea ni estacion correspondientes");
		}else {
			Integer orden=(Integer) r[0];
			String nE=(String) r[1];
			String nL=(String) r[2];
			
			System.out.println("El numero de orden "+orden+" pertenece a la estacion "+nE+" y la linea "+nL);
		}
		
		
	session.close();	

	
	
}
}
