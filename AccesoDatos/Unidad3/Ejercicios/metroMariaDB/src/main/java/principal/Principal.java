package principal;

import java.util.Set;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.hibernate.SessionFactory;


import clases.*;

public class Principal {
	private static SessionFactory factori;

	public static void main(String[] args) {
		LogManager.getLogManager().reset();
		Logger globalLogger = Logger.getLogger(java.util.logging.Logger.GLOBAL_LOGGER_NAME);
		globalLogger.setLevel(java.util.logging.Level.OFF);

		factori = Conexion.getSession(); // Creo la sessionFactory una única vez.

		System.out.println("----------------");
		verlinea(1); // de 1 a 7
		
		System.out.println("----------------");
		verlinea(5); // linea sin estaciones
		
		System.out.println("----------------");
		verlinea(50); // linea no existe
		factori.close();
	}

	private static void verlinea(int id) {
	

		Session session = factori.openSession();
		TLineas lin = (TLineas) session.get(TLineas.class, id);
		
		if(lin  == null) {
			System.out.println("Cod línea no existe: " + id);
		}
		else {
			System.out.println("COD LINEA: " + lin.getCodLinea() + "   NOMBRE: " +  lin.getNombre());
			System.out.println(" * Estaciones de la línea: " +  lin.getTLineaEstacions().size());
			if (lin.getTLineaEstacions().size() > 0)
			{
				System.out.printf("    %10s %-30s %-30s %n","CODIGO","NOMBRE","DIRECCION");
			System.out.printf("    %10s %-30s %-30s %n","---------","---------------","----------------");;
			Set<TLineaEstacion> lista= lin.getTLineaEstacions();
			for (TLineaEstacion ll : lista) {
				System.out.printf("    %10s %-30s %-30s %n",
						ll.getTEstaciones().getCodEstacion() 
						 , ll.getTEstaciones().getNombre() 
						, ll.getTEstaciones().getDireccion() );
						 
				}
			

			}
			System.out.println(" * Trenes de la línea: " +  lin.getTTreneses().size());
			if(lin.getTTreneses().size()> 0) {
				Set <TTrenes> listatrenes= lin.getTTreneses();
				System.out.printf("    %10s %-30s %-15s %10s %-30s%n",
						"CODIGO","NOMBRE","TIPO","CODCOCHERA","NOMBRE_COCHERA");
				System.out.printf("    %10s %-30s %-15s %10s %-30s%n",
						"----------","------------------------------",
						"--------------------","----------","------------------------------");
				for (TTrenes ll : listatrenes) {
					System.out.printf("    %10s %-30s %-15s %10s %-30s%n",
							ll.getCodTren(), ll.getNombre(), ll.getTipo(),
							ll.getTCocheras().getCodCochera(),
							ll.getTCocheras().getNombre());
							 
					}
			}
						
			
		}
		
		
		
	}

}


