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

	private static SessionFactory factory;

	public static void main(String[] args) {

		LogManager.getLogManager().reset();
		Logger globalLogger = Logger.getLogger(java.util.logging.Logger.GLOBAL_LOGGER_NAME);
		globalLogger.setLevel(java.util.logging.Level.OFF);

		factory = Conexion.getSession(); // Creo la sessionFactory una única vez.

		// conuniqueresultmaxproductoporcompra();
		// Utilizando la clase auxiliar
		// listartotalclientes1();

		// Consulta from Clientes
		// listartotalclientes2();

		// consulta con array d3 objetos
		// listartotalproductos1();

		// actualizar stock de productos con menos de 70 unidades

		actualizarstock();
		// no borra tiee reg relacionados
		borrarcliente(1);
		// lo borra la 1ª vez, la seg no existe
		borrarcliente(6);
		// No exsite
		borrarcliente(100);

		// Consulta from productos
		// listartotalproductos2();

		// Detalles de productos
		// listardetalleproductos();

		factory.close();

	}

	private static void borrarcliente(int cli) {
		// TODO Auto-generated method stub
		Session session = factory.openSession();
		Transaction tx = session.beginTransaction();
		try {
			String hqlDel = "delete Clientes c where c.codcliente = ?1";
			int filas = session.createMutationQuery(hqlDel).setParameter(1, cli).executeUpdate();
			if (filas != 0) {
				System.out.println("CLIENTE BORRADO: " + cli);
			} else {
				System.out.println("CLIENTE NO EXISTE: " + cli);
			}
			tx.commit();

		} catch (org.hibernate.exception.ConstraintViolationException e) {
			System.out.println("Atención cliente " + cli + " no se puede borrar, tiene reg relacionados.");

		}

		session.close();

	}

	private static void actualizarstock() {
		Session session = factory.openSession();
		Transaction tx = session.beginTransaction();

		String hqlModif = "update Productos p set p.stock = p.stock + :subida " + " where p.stock <=70 ";
		int filasModif = session.createMutationQuery(hqlModif).setParameter("subida", 50).executeUpdate();
		tx.commit();
		System.out.println("FILAS MODIFICADAS: " + filasModif);

		session.close();

	}

	private static void listardetalleproductos() {
		Session session = factory.openSession();
		String hql = "from Productos p order by p.codproducto ";
		Query<Productos> q = session.createQuery(hql, Productos.class);
		List<Productos> lista = q.list();

		for (Productos p : lista) {
			System.out.println("****");
			System.out.println("Cod producto: " + p.getCodproducto());
			System.out.println("Denominación: " + p.getDenominacion() + "   Precio: " + p.getPvp());
			System.out.println("Stock actual: " + p.getStock());

			System.out.println("-----------------------------------------------------------------------");

			if (p.getDetcomprases().size() == 0) {
				System.out.println("   ** SIN COMPRAS ** ");
				System.out.println("-----------------------------------------------------------------------");
			} else {

				System.out.printf("%10s %10s %10s %-30s %10s %10s %n", "Num_compra", "FechaCompra", "CodCliente",
						"Nombre cliente", "Unidades", "Importe");
				System.out.printf("%10s %10s %10s %-30s %10s %10s %n", "----------", "----------", "----------",
						"------------------------------", "----------", "----------");

				// Set<Detcompras> detalle = p.getDetcomprases();

				// Para ordenar por número de compra hacer consulta
				String hql3 = "from Detcompras d where d.productos.codproducto=:cod order by id.numcompra";
				Query<Detcompras> q3 = session.createQuery(hql3, Detcompras.class);
				q3.setParameter("cod", p.getCodproducto());

				List<Detcompras> detalle = q3.list();

				int sumauni = 0;
				float timp = 0;
				for (Detcompras de : detalle) {

					float imp = de.getUnidades().intValue() * p.getPvp().floatValue();
					timp = timp + imp;
					sumauni = sumauni + de.getUnidades().intValue();
					System.out.printf("%10s %10s %10s %-30s %10s %10s %n", de.getCompras().getNumcompra(),
							de.getCompras().getFecha(), de.getCompras().getClientes().getCodcliente(),
							de.getCompras().getClientes().getNombre(), de.getUnidades(), imp);

				} // fin detalle
				System.out.printf("%10s %10s %10s %-30s %10s %10s %n", "----------", "----------", "----------",
						"------------------------------", "----------", "----------");
				System.out.printf("%10s %10s %10s %-30s %10s %10s %n", "TOTALES: ", "", "", "", sumauni, timp);

			} // else prod con compras

		} // fin productos

		session.close();
	}

	private static void listartotalproductos2() {
		Session session = factory.openSession();
		String hql = "from Productos p order by p.codproducto ";

		Query<Productos> q = session.createQuery(hql, Productos.class);
		List<Productos> lista = q.list();

		System.out.printf("%10s %-30s %10s %10s %10s%n", "CODPRODUCT", "DENOMINACIÓN", "PVP", "SUMAUNI", "TOTIMPORTE");
		System.out.printf("%10s %-30s %10s %10s %10s %n", "----------", "------------------------------", "----------",
				"----------", "----------");
		int totaluni = 0, max = 0;
		float totalimp = 0;
		String nommax = "";

		String hql2 = "select coalesce(sum( d.unidades ),0)  from Detcompras d where d.id.codproducto = :cod ";

		for (Productos p : lista) {

			// HAciendo consulta
			int sumauni = 0;

			Query<BigInteger> q2 = session.createQuery(hql2, BigInteger.class);
			q2.setParameter("cod", p.getCodproducto());
			BigInteger ss = q2.uniqueResult();
			sumauni = ss.intValue();

			// RECORRIENDO EL SET
//			Set<Detcompras> detalle = p.getDetcomprases();
//			sumauni = 0;
//			for (Detcompras de:detalle) {
//				sumauni = sumauni + de.getUnidades().intValue();
//			}

			float total = p.getPvp().floatValue() * sumauni;
			System.out.printf("%10s %-30s %10s %10s %10.2f %5s%n", p.getCodproducto(), p.getDenominacion(), p.getPvp(),
					sumauni, total, p.getDetcomprases().size());

			totaluni = totaluni + sumauni;
			totalimp = totalimp + total;

			if (sumauni >= max) {

				if (sumauni == max) {
					nommax = nommax + p.getDenominacion() + ". ";
				} else {
					max = sumauni;
					nommax = p.getDenominacion() + ". ";
				}

			}

		}

		System.out.printf("%10s %-30s %10s %10s %10s %n", "----------", "------------------------------", "----------",
				"----------", "----------");

		System.out.printf("%10s %-30s %10s %10s %10.2f %n", "TOTALES:", "", "", totaluni, totalimp);

		System.out.println("Producto/s más vendido/s(" + max + " ): " + nommax);

		session.close();
	}

	private static void listartotalproductos1() {
		Session session = factory.openSession();

		String hql = "select  p.codproducto, p.denominacion, p.pvp,"
				+ " coalesce(sum(det.unidades),0), coalesce(sum(det.unidades * p.pvp),0) "
				+ " from Productos p left join p.detcomprases det" + " group by p.codproducto, p.denominacion, p.pvp"
				+ " order by p.codproducto";
		Query<Object> q = session.createQuery(hql, Object.class);
		List<Object> lista = q.list();

		System.out.printf("%10s %-30s %10s %10s %10s%n", "CODPRODUCT", "DENOMINACIÓN", "PVP", "SUMAUNI", "TOTIMPORTE");
		System.out.printf("%10s %-30s %10s %10s %10s %n", "----------", "------------------------------", "----------",
				"----------", "----------");
		int sumauni = 0;
		float sumaimp = 0;
		String prodmax = "";
		int max = 0;

		for (int i = 0; i < lista.size(); i++) {

			Object[] fil = (Object[]) lista.get(i);

			System.out.printf("%10s %-30s %10s %10s %10s %n", fil[0], fil[1], fil[2], fil[3], fil[4]);

			BigInteger uni = (BigInteger) fil[3];
			Double imp = (Double) fil[4];
			sumauni = sumauni + uni.intValue();
			sumaimp = sumaimp + imp.floatValue();

			if (uni.intValue() >= max) {

				if (uni.intValue() == max) {
					prodmax = prodmax + fil[1] + ". ";
				} else {
					max = uni.intValue();
					prodmax = fil[1] + ". ";
				}

			}

		}
		System.out.printf("%10s %-30s %10s %10s %10s %n", "----------", "------------------------------", "----------",
				"----------", "----------");

		System.out.printf("%10s %-30s %10s %10s %10s %n", "TOTALES =>", "", "", sumauni, sumaimp);
		System.out.println("Producto/s más vendidos (" + max + "): " + prodmax);

		session.close();

	}

	private static void listartotalclientes2() {

		Session session = factory.openSession();
		String hql = "From Clientes c order by c.codcliente";
		Query<Clientes> q = session.createQuery(hql, Clientes.class);
		List<Clientes> lista = q.list();
		int num = lista.size();
		System.out.printf("%10s %-30s %10s %10s %n", "CODCLIENTE", "NOMBRE CLIENTE", "NUMCOMPRAS", "TOTAL");
		System.out.printf("%10s %-30s %10s %10s %n", "----------", "------------------------------", "----------",
				"----------");
		Long tcon = 0l;
		Double tsuma = 0d;
		String hql2 = "select  sum(det.productos.pvp * det.unidades)" + "	from Compras c join c.detcomprases det "
				+ "	where c.clientes.codcliente = :codcli";

		for (Clientes tt : lista) {

			// calcular el total de las compras del cliente
			// select sum(det.productos.pvp * det.unidades)
			// from Compras c join c.detcomprases det
			// where c.clientes.codcliente = :codcli

			Query<Double> q2 = session.createQuery(hql2, Double.class);
			q2.setParameter("codcli", tt.getCodcliente());
			Double suma = q2.uniqueResult();

			// preguntar por suma = null
			if (suma == null)
				suma = 0d;

			System.out.printf("%10s %-30s %10s %10s %n", tt.getCodcliente(), tt.getNombre(), tt.getComprases().size(),
					suma);

			tcon = tcon + tt.getComprases().size();
			// preguntar por suma = null
			tsuma = tsuma + suma;
		}
		System.out.printf("%10s %-30s %10s %10s %n", "----------", "------------------------------", "----------",
				"----------");

		System.out.printf("%10s %-30s %10s %10s %n", "TOTALES =>", "", tcon, tsuma);
		session.close();

	}

	private static void listartotalclientes1() {

		Session session = factory.openSession();
		String hql = "select new clases.TotalCliente(c.codcliente, c.nombre , count(distinct con),"
				+ " sum(det.unidades * det.productos.pvp) ) "
				+ " from Clientes c left join c.comprases con join con.detcomprases det"
				+ " group by c.codcliente, c.nombre " + " order by c.codcliente";

		Query<TotalCliente> q = session.createQuery(hql, TotalCliente.class);
		List<TotalCliente> lista = q.list();
		int num = lista.size();

		System.out.printf("%10s %-30s %10s %10s %n", "CODCLIENTE", "NOMBRE CLIENTE", "NUMCOMPRAS", "TOTAL");
		System.out.printf("%10s %-30s %10s %10s %n", "----------", "------------------------------", "----------",
				"----------");
		Long tcon = 0l;
		Double tsuma = 0d;
		for (TotalCliente tt : lista) {

			// System.out.println(tt.toString());
			System.out.printf("%10s %-30s %10s %10s %n", tt.getCodcliente(), tt.getNombre(), tt.getContador(),
					tt.getSuma());
			tcon = tcon + tt.getContador();
			tsuma = tsuma + tt.getSuma();

		}
		System.out.printf("%10s %-30s %10s %10s %n", "----------", "------------------------------", "----------",
				"----------");

		System.out.printf("%10s %-30s %10s %10s %n", "TOTALES =>", "", tcon, tsuma);
		session.close();
	}

	private static void conuniqueresultmaxproductoporcompra() {

		Session session = factory.openSession();

		Query<Compras> q = session.createQuery("from Compras c order by c.numcompra", Compras.class);

		List<Compras> lista = q.list();
		int num = lista.size();
		if (num > 0) {

			// NUMCOMPRA CODCLIENTE NOMBRE PROD MÁS CARO DELA COMPRA(con Uniqueresult)
			System.out.printf("%9s %10s %30s %30s %n", "NUMCOMPRA", "CODCLIENTE", "NOMBRE CLIENTE", "PROD MÁS CARO");
			System.out.printf("%9s %10s %30s %30s %n", "---------", "----------", "---------------------------",
					"---------------------------");

			for (Compras com : lista) {

				String con = " select d.productos.denominacion "
						+ " from Detcompras d where d.compras.numcompra = :numcom " + " and d.productos.pvp = "
						+ " (select max(d2.productos.pvp) "
						+ "  from Detcompras d2 where d2.compras.numcompra = :numcom ) ";

				Query q2 = session.createQuery(con, String.class);
				q2.setParameter("numcom", com.getNumcompra());
				String prod = (String) q2.uniqueResult();

				System.out.printf("%9s %10s %30s %30s %n", com.getNumcompra(), com.getClientes().getCodcliente(),
						com.getClientes().getNombre(), prod);

			}

			session.close();

		}

	}//

} // fin clase
