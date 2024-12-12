package principal;

import java.math.BigInteger;
import java.util.List;
import java.util.Scanner;
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
		
		factory=Conexion.getSession();
		Scanner s=new Scanner(System.in);
		boolean salir=false;
		do {
			menu();
			int opcion=s.nextInt();
			switch(opcion) {
				case 1:
					llenarResumenCamisetas();
					break;
				case 2:
					listarCiclistasEquipo(11);
					break;
				case 3:
					consulta1();
					System.out.println();
					consulta2();
					break;
				case 0:
					salir=true;
					break;
				default:
					System.out.println("Opcion incorrecta");
					break;
			}
		}while(!salir);
	}
	
	private static void menu() {
		System.out.println("Elige una opcion");
		System.out.println("1 - Llenar RESUMEN_CAMISETAS");
		System.out.println("2 - Resumen ciclistas de un equipo");
		System.out.println("3 - Consultas");
		System.out.println("0 - Salir");
	}
	
	private static void llenarResumenCamisetas() {
		Session session=factory.openSession();
		String hqlEquipos="select distinct e.codigoequipo, e.nombreequipo from Ciclistas c join c.equipos e join c.llevas l join l.camisetas ca order by e.codigoequipo";
		String hql="select c.codigociclista, c.nombreciclista, ca.codigocamiseta,\r\n"
				+ "count(ca.codigocamiseta), ca.importepremio*count(ca.codigocamiseta)\r\n"
				+ "from Ciclistas c join c.equipos e join c.llevas l join l.camisetas ca\r\n"
				+ "where e.codigoequipo=:codigoequipo\r\n"
				+ "group by e.codigoequipo, c.codigociclista, c.nombreciclista, ca.codigocamiseta,ca.importepremio\r\n"
				+ "order by c.codigociclista, ca.codigocamiseta\r\n";
		Query q=session.createQuery(hqlEquipos,Object.class);
		List<Object>lista=q.list();
		Transaction tx=session.beginTransaction();
		for(int i=0;i<lista.size();i++) {
			Object[]filaActual=(Object[]) lista.get(i);
			System.out.printf("%-50s %15s %15s %15s %n","Equipo: "+filaActual[0]+", "+filaActual[1],"CAMISETA","N VECES","IMPORTE PREMIO");
			System.out.printf("%-50s %15s %15s %15s %n","--------------------------------------------------","---------------","---------------","---------------");
			Query q2=session.createQuery(hql,Object.class);
			q2.setParameter("codigoequipo", filaActual[0]);
			List<Object>listaCiclistas=q2.list();
			for(int j=0;j<listaCiclistas.size();j++) {
				Object[]ciclistaActual=(Object[])listaCiclistas.get(j);
				System.out.printf("%-50s %15s %15s %15s %n","     Insertado: "+ciclistaActual[0]+" "+ciclistaActual[1],ciclistaActual[2],ciclistaActual[3],ciclistaActual[4]);
				ResumenCamisetasId idNuevo=new ResumenCamisetasId();
				idNuevo.setCodigoequipo((BigInteger) filaActual[0]);
				idNuevo.setCodigociclista((BigInteger) ciclistaActual[0]);
				idNuevo.setCodigocamiseta((BigInteger) ciclistaActual[2]);
				ResumenCamisetas nuevo=new ResumenCamisetas();
				nuevo.setId(idNuevo);
				nuevo.setEquipos(session.get(Equipos.class, filaActual[0]));
				nuevo.setCiclistas(session.get(Ciclistas.class, ciclistaActual[0]));
				nuevo.setCamisetas(session.get(Camisetas.class, ciclistaActual[2]));
				BigInteger numVeces=BigInteger.valueOf((long) ciclistaActual[3]);
				nuevo.setNumveces(numVeces);
				nuevo.setImportepremio((BigInteger) ciclistaActual[4]);
				session.persist(nuevo);
			}
			System.out.println();
		}
		tx.commit();
		session.close();
	}
	
	private static void listarCiclistasEquipo(int codigoEquipo) {
		Session session =factory.openSession();	
		Equipos equipo=session.get(Equipos.class, codigoEquipo);
		if(equipo!=null) {
			String consultaJefe="select distinct c.ciclistas.nombreciclista from Ciclistas c where c.equipos.codigoequipo=:codEquipo";
			Query<String>q2 = session.createQuery(consultaJefe,String.class);
			q2.setParameter("codEquipo",codigoEquipo);
			String jefeEquipo=(String) q2.uniqueResult();
			if (jefeEquipo==null) jefeEquipo="NO TIENE";
			System.out.println("COD-EQUIPO: "+equipo.getCodigoequipo()+"   Nombre: "+equipo.getNombreequipo());
			System.out.println("PAIS: "+equipo.getPais()+"   Jefe de equipo: "+jefeEquipo);
			System.out.println("-----------------------------------------------------------------------------");
			if(equipo.getCiclistases().size()>0) {
				System.out.printf("%10s %30s %15s %15s %15s %n","CODIGO","NOMBRE","ETAP GANADAS","TRAMOS GANADOS","N VECES CAMISETA");
				System.out.printf("%10s %30s %15s %15s %15s %n","----------","------------------------------","---------------","---------------","---------------");
				//SI LOS PIDE ORDENADOS NO SE PUEDE HACER CON SET, SE NECESITA CONSULTA
				String consultaCiclistas="from Ciclistas c where c.equipos.codigoequipo=:codigoEquipo order by c.codigociclista";
				Query qCiclistas = session.createQuery(consultaCiclistas,Ciclistas.class);
				qCiclistas.setParameter("codigoEquipo",codigoEquipo);
				List<Ciclistas>ciclistas=qCiclistas.list();
				String ciclistaMaximoEtapas="";
				String ciclistaMaximoTramos="";
				int maximoEtapas=0, maximoTramos=0;
				for(Ciclistas c:ciclistas) {
					System.out.printf("%10s %30s %15s %15s %15s %n",c.getCodigociclista(),c.getNombreciclista(),c.getEtapases().size(),c.getTramospuertoses().size(),c.getResumenCamisetases().size());
					if(c.getEtapases().size()>maximoEtapas) {
						maximoEtapas=c.getEtapases().size();
						ciclistaMaximoEtapas=c.getNombreciclista();
					}
					else if (c.getEtapases().size()==maximoEtapas) ciclistaMaximoEtapas+="    "+c.getNombreciclista();
					if(c.getTramospuertoses().size()>maximoTramos) {
						maximoTramos=c.getTramospuertoses().size();
						ciclistaMaximoTramos=c.getNombreciclista();
					}
					else if (c.getTramospuertoses().size()==maximoTramos) ciclistaMaximoTramos+="    "+c.getNombreciclista();
				}
				System.out.printf("%10s %30s %15s %15s %15s %n","----------","------------------------------","---------------","---------------","---------------");
				if(maximoEtapas==0) ciclistaMaximoEtapas="NO HAY";
				if(maximoTramos==0) ciclistaMaximoTramos="NO HAY";
				System.out.println("Nombre/s de corredor/es con mas etapas ganadas: "+ciclistaMaximoEtapas);
				System.out.println("Nombre/s de corredor/es con mas tramos de montaña ganados: "+ciclistaMaximoTramos);
			
			}
			else System.out.println("EQUIPO SIN CICLISTAS");
		}
		else System.out.println("EQUIPO "+codigoEquipo+" NO EXISTE");
	}
	private static void consulta1() {
		Session session=factory.openSession();
		String hql="select distinct e.codigoetapa, e.km, e.pobsalida, e.pobllegada, e.ciclistas.nombreciclista\r\n"
				+ "from Etapas e join e.tramospuertoses\r\n"
				+ "where e.pobsalida=e.pobllegada \r\n"
				+ "order by e.codigoetapa";
		Query cons = session.createQuery(hql,Object.class);
		List datos = cons.list();
		System.out.println("CONSULTA 1");
		System.out.printf("%13s %10s %30s %30s %30s %n","CODIGOETAPA","KM","POBSALIDA","POBLLEGADA","NOMBRECICLISTA");
		System.out.printf("%13s %10s %30s %30s %30s %n","-------------","----------","------------------------------","------------------------------","------------------------------");
		for (int i = 0; i < datos.size(); i++) {
			Object[] filaActual = (Object[]) datos.get(i); // Acceso a una fila
			System.out.printf("%13s %10s %30s %30s %30s %n",filaActual[0],filaActual[1],filaActual[2],filaActual[3],filaActual[4]);
		}
		session.close();
	}
	private static void consulta2() {
		Session session=factory.openSession();
		String hql="select c.codigociclista, c.nombreciclista, e.codigoetapa, e.tipoetapa, t.codigotramo, t.nombretramo, t.categoria\r\n"
				+ "from Ciclistas c join c.etapases e join c.tramospuertoses t\r\n"
				+ "where t.pendiente like '%5,5%'\r\n"
				+ "order by c.codigociclista,e.codigoetapa";
		Query cons = session.createQuery(hql,Object.class);
		List datos = cons.list();
		System.out.println("CONSULTA 2");
		System.out.printf("%13s %30s %30s %30s %30s %30s %30s %n","CODIGOCICLISTA","NOMBRECICLISTA","CODETAPA","TIPO","CODTRAMO","NOMBRETRAMO","CATEGORIA");
		System.out.printf("%13s %30s %30s %30s %30s %30s %30s %n","-------------","------------------------------","------------------------------","------------------------------","------------------------------","------------------------------","------------------------------");
		for (int i = 0; i < datos.size(); i++) {
			Object[] filaActual = (Object[]) datos.get(i); // Acceso a una fila
			System.out.printf("%13s %30s %30s %30s %30s %30s %30s %n",filaActual[0],filaActual[1],filaActual[2],filaActual[3],filaActual[4],filaActual[5],filaActual[6]);
		}
		session.close();
	}
}
