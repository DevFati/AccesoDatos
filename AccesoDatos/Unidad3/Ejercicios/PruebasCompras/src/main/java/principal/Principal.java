package principal;

import java.math.BigInteger;
import java.util.List;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;


import clases.*;

public class Principal {

	private static SessionFactory factory;
	
	public static void main(String[] args) {
		
		LogManager.getLogManager().reset();
		Logger globalLogger = Logger.getLogger(java.util.logging.Logger.GLOBAL_LOGGER_NAME);
		globalLogger.setLevel(java.util.logging.Level.OFF);

		factory = Conexion.getSession(); //Creo la sessionFactory una única vez.

		conuniqueresultmaxproductoporcompra();
		//Utilizamos la clase auxiliar 
		
		listartotalclientes1();
		
		//Consulta from Clientes 
		listartotalclientes2();
		
		
		
		//Conseguir listado de productos con array de objetos 
		listartotalProductos1();
		
		//Conseguir un listado de productos con from 
		
				listartotalProductos2();
				
				
				//Obtener por cada producto sus datos 
				listarProductos();
		factory.close();


	}




private static void listarProductos() {
		
		
	}




private static void listartotalProductos2() {
		// TODO Auto-generated method stub
		
	}




private static void listartotalProductos1() {
	Session session = factory.openSession();
	String hql ="select p.codproducto, p.denominacion, p.pvp,\r\n"
			+ "coalesce(sum(det.unidades),0), coalesce(sum(det.unidades * p.pvp),0)\r\n"
			+ "from Productos p left join p.detcomprases det\r\n"
			+ "group by p.codproducto, p.denominacion, p.pvp\r\n"
			+ "order by p.codproducto";
	
	Query<Object> q = session.createQuery(hql, Object.class);
	List<Object> lista = q.list();
	System.out.printf("%10s %-30s %10s %10s %10s %n","CODPRODUCT",
			"DENOMINACION","PVP","SUMAUNI","TOTALIMPORTE");
	System.out.printf("%10s %-30s %10s %10s %10s %n","----------",
			"------------------------------","----------","----------","----------");
	
	int sumauni=0; 
	float sumaimp=0; 
	String prodmax="";
	int max=0;
	for(int i=0; i<lista.size(); i++) {
		
		Object[] fil=(Object[]) lista.get(i);

		System.out.printf("%10s %-30s %10s %10s %10s %n",fil[0],
				fil[1],fil[2],fil[3],fil[4]);
		
		BigInteger uni=(BigInteger) fil[3];
		Double imp=(Double) fil[4];
		sumauni=sumauni+uni.intValue();
		sumaimp=sumaimp+imp.floatValue();
		
		if(uni.intValue() >= max) {
			if(uni.intValue() == max ) {
				prodmax= prodmax + fil[1] +". ";
				
			}else {
				max=uni.intValue();
				prodmax= prodmax + fil[1] +". ";
			}
		}
		
		
	}
	System.out.printf("%10s %-30s %10s %10s %10s %n","----------",
			"------------------------------","----------","----------","----------");
	
	System.out.printf("%10s %-30s %10s %10s %10s %n","TOTALES=>",
			"","",sumauni,sumaimp);
	
	System.out.println("Producto/s más vendidos ("+max  + "): " + prodmax );
		
	session.close();
	}









private static void listartotalclientes2() {


	Session session = factory.openSession();
	String hql ="From Clientes c order by c.codcliente";
	Query<Clientes> q = session.createQuery(hql, Clientes.class);
	List<Clientes> lista = q.list();
	int num = lista.size();
	System.out.printf("%10s %-30s %10s %10s %n","CODCLIENTE",
			"NOMBRE CLIENTE","NUMCOMPRAS","TOTAL" );
	System.out.printf("%10s %-30s %10s %10s %n","----------",
			"------------------------------","----------","----------" );
	Long tcon=0l;
	Double tsuma = 0d;
	String hql2="select  sum(det.productos.pvp * det.unidades)"
			+ "	from Compras c join c.detcomprases det "
			+ "	where c.clientes.codcliente = :codcli";

	for (Clientes tt: lista){
		
		// calcular el total de las compras del cliente	
		//  select  sum(det.productos.pvp * det.unidades)
		//  from Compras c join c.detcomprases det
		//   where c.clientes.codcliente = :codcli
					
		Query<Double> q2 = session.createQuery(hql2, Double.class );
		q2.setParameter("codcli", tt.getCodcliente());
		Double suma = q2.uniqueResult();
		
		// preguntar por suma = null
		if (suma==null)
				suma=0d;
		
		System.out.printf("%10s %-30s %10s %10s %n",
				tt.getCodcliente(), tt.getNombre(),
				tt.getComprases().size(), suma);
							
		tcon = tcon + tt.getComprases().size();
		// preguntar por suma = null
		tsuma = tsuma + suma;
	}
	System.out.printf("%10s %-30s %10s %10s %n","----------",
			"------------------------------","----------","----------" );

	System.out.printf("%10s %-30s %10s %10s %n",
			"TOTALES =>","",tcon, tsuma);
	session.close();
	
	
		
	}

private static void listartotalclientes1() {
		
		Session session = factory.openSession();
		String hql = "select new clases.TotalCliente(c.codcliente, c.nombre , count(distinct con),"
				+ " sum(det.unidades * det.productos.pvp) ) "
				+ " from Clientes c left join c.comprases con left join con.detcomprases det"
				+ " group by c.codcliente, c.nombre "
				+ " order by c.codcliente";
		
		Query<TotalCliente> q = session.createQuery(hql, TotalCliente.class);
		List<TotalCliente> lista = q.list();
		int num = lista.size();
		
		
		System.out.printf("%10s %-30s %10s %10s %n","CODCLIENTE",
				"NOMBRE CLIENTE","NUMCOMPRAS","TOTAL" );
		System.out.printf("%10s %-30s %10s %10s %n","----------",
				"------------------------------","----------","----------" );
		Long tcon=0l;
		Double tsuma = 0d;
		for (TotalCliente tt: lista){
			
			//System.out.println(tt.toString());
			System.out.printf("%10s %-30s %10s %10s %n",
			tt.getCodcliente(), tt.getNombre(), tt.getContador(), tt.getSuma());
			tcon = tcon + tt.getContador();
			tsuma = tsuma + tt.getSuma();
			
		}
		System.out.printf("%10s %-30s %10s %10s %n","----------",
				"------------------------------","----------","----------" );
	
		System.out.printf("%10s %-30s %10s %10s %n",
				"TOTALES =>","",tcon, tsuma);
		session.close();
	}

	private static void conuniqueresultmaxproductoporcompra() {
		
		Session session = factory.openSession();
	
		Query<Compras> q = session.createQuery("from Compras c order by c.numcompra",Compras.class);
		
		List<Compras> lista = q.list();
		int num = lista.size();
		if (num>0) {
			
			//NUMCOMPRA   CODCLIENTE  NOMBRE   PROD MÁS CARO DELA COMPRA(con Uniqueresult)
			System.out.printf("%9s %10s %30s %30s %n","NUMCOMPRA","CODCLIENTE",
					"NOMBRE CLIENTE","PROD MÁS CARO");
			System.out.printf("%9s %10s %30s %30s %n","---------","----------",
					"---------------------------","---------------------------");
		
			for (Compras com: lista){
				
				String con = " select d.productos.denominacion "
						+ " from Detcompras d where d.compras.numcompra = :numcom " 
						+ " and d.productos.pvp = "
						+ " (select max(d2.productos.pvp) "
						+ "  from Detcompras d2 where d2.compras.numcompra = :numcom ) ";
				
				   Query q2 =  session.createQuery(con, String.class);
				   q2.setParameter("numcom", com.getNumcompra());
				   String prod = (String) q2.uniqueResult();
				
				System.out.printf("%9s %10s %30s %30s %n",
						com.getNumcompra(), com.getClientes().getCodcliente(),
						com.getClientes().getNombre(),
						prod);

			}
			
			session.close();
		
	}
	
}//
	
} // fin clase


