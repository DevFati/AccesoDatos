package principal;

import java.math.BigInteger;
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
