package principal;

import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import clases.T3clientes;
import clases.T3habitaciones;
import clases.T3reservas;



public class Principal {
	private static SessionFactory sesion;

	public static void main(String[] args) {
		Logger.getLogger("org.hibernate").setLevel(Level.OFF);

		sesion=Conexion.getSession(); 
		//Método para insertar datos en la tabla Reservas 
		
		insertarReservas("01013",1,0,"29-05-2025","21-05-2025");
		
		listadoclientes();
		
		sesion.close();
	}

	private static void listadoclientes() {
		Session session = sesion.openSession();
		T3clientes clientes=new T3clientes();
		String con="from T3clientes";
		Query q = session.createQuery(con,T3clientes.class);
		List<T3clientes> lista=q.getResultList();
		int num=lista.size();
		
		for(int i=0; i<num;i++) {
			//extraemos el objeto 
			clientes=(T3clientes) lista.get(i);
			System.out.println("Cod cliente:   "+clientes.getCodigocliente()+"     Nombre: "+clientes.getNombrecliente()+"    Apellido: "+clientes.getApellido());
			
			Set<T3reservas> listareservas = clientes.getT3reservases();
			if(listareservas.size()>0) {
				System.out.printf("%-10s %-10s %-10s %15s %15s %10s %10s %10s %10s %10s%n","CODRES","NUMHABIT","FECHAENTRADA", "FECHASALIDA", "TOTALSUPLE","DIAS","PVPDIA","IMPORTE","IMPORDEDESC","IMPORTOTAL");
				 System.out.printf("%-10s %-10s %-10s %15s %15s %10s %10s %10s %10s %10s%n","----------","----------","----------", "---------------", "---------------","----------","----------","----------","----------","----------");
				 double max=0;
				 String maxS="";
				for (T3reservas r : listareservas) {
					// Calcular la diferencia en días
					long diferenciaEnMilisegundos = r.getFechasalida().getTime() - r.getFechaentrada().getTime();
					long dias = TimeUnit.MILLISECONDS.toDays(diferenciaEnMilisegundos);
					
					double precio=r.getT3habitaciones().getT3tiposhabitaciones().getPrecio();
					int camasS=0;
					if(r.getCamassupletorias()!=null) {
						 camasS= (r.getCamassupletorias().intValue()*10);
					}
					
					double importeT=(camasS+dias)*precio;
					double desc=(importeT * r.getDescuento().doubleValue()) /100;
					double importeTotal=importeT-desc;
					System.out.printf("%-10s %-10s %-10s %15s %15s %10s %10s %10s %10s %10s%n",r.getCodreserva(),r.getT3habitaciones().getNumhabitacion(),r.getFechaentrada(), r.getFechasalida(),camasS,dias,precio,importeT,desc,importeTotal);

					if(importeTotal>=max) {
						
						if(importeTotal==max) {
							maxS=maxS+r.getCodreserva()+" . ";
						}else {
							maxS=r.getCodreserva()+" . ";
							
						}
						
						max=importeTotal;
					}
					
					
				}
				System.out.println("TOTALES: ");
				System.out.println("Número de reserva con mas importe total: "+maxS);
				System.out.println();
			}else {
				System.out.println("CLIENTE SIN RESERVAS");
			}
			
		}

		session.close();
		
	}

	private static void insertarReservas(String numHabitacion, int codcli, int camasSupletorias, String fechaEntrada, String fechaSalida) {
		
		Session session=sesion.openSession();
		Transaction tx=session.beginTransaction();
		T3habitaciones habit=(T3habitaciones) session.get(T3habitaciones.class, numHabitacion);
		String mensaje="";
		boolean error=false;
		BigInteger descuento=BigInteger.valueOf(0);
		Date entrada = null, salida=null;
		if(habit==null) {
			mensaje=mensaje+"La habitacion "+numHabitacion+" no existe \n";
			error=true;
			
		}
		
		T3clientes cli=(T3clientes) session.get(T3clientes.class, codcli);
		if(cli==null) {
			mensaje=mensaje+"El cliente "+codcli+" no existe \n";
			error=true;
			
		}
		
		//Vemos si las fechas tienen el formato correcto: 
		boolean fechaValida=true;
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
		try {
		    LocalDate fecha = LocalDate.parse(fechaEntrada, formatter);
		} catch (DateTimeParseException e) {
		    mensaje += "FECHA " + fechaEntrada + " TIENE FORMATO ERRONEO \n";
		    fechaValida=false;
		    error=true;
		}
		
		try {
		    LocalDate fecha = LocalDate.parse(fechaSalida, formatter);
		} catch (DateTimeParseException e) {
		    mensaje += "FECHA " + fechaSalida + " TIENE FORMATO ERRONEO \n";
		    fechaValida=false;
		    error=true;
		}

		//si las fechas son validas, comprobar que no esten incluidas en alguna reserva 
		
		if(fechaValida) {
			SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
            
			try {
					 entrada = sdf.parse(fechaEntrada);
				  salida = sdf.parse(fechaSalida);
				// 1 o un valor positivo: Si la fecha entrada es posterior a la fecha salida.
				 // 0 si son iguales
			
		            if (entrada.compareTo(salida) > 0) {
		            	error=true;
		                mensaje=mensaje+"La fecha de entrada debe ser menor que la fecha de salida \n";
		            }
		            
		            if (entrada.compareTo(salida) == 0) {
		            	error=true;
		                mensaje=mensaje+"La fechas no pueden ser iguales \n";
		            }
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
			
			String hql = " from T3reservas r "
					+ " where r.t3habitaciones.numhabitacion = :numhab"
					+ " and (  :fechaEntrada between r.fechaentrada and r.fechasalida"
					+ "      or :fechaSalida between r.fechaentrada and r.fechasalida"
					+ "      or (r.fechaentrada between :fechaEntrada and :fechaSalida)"
					+ "      or (r.fechasalida between :fechaEntrada and :fechaSalida)"
					+ "  )";
			
			
			Query cons = session.createQuery(hql, T3reservas.class);
			cons.setParameter("numhab", numHabitacion);
			cons.setParameter("fechaEntrada", entrada);
			cons.setParameter("fechaSalida", salida);
			List<T3reservas> lista=cons.list();
			
			if(lista.size()>0) {
				error=true;
				mensaje=mensaje+"HABITACION RESERVADA EN DICHAS FECHAS \n";
			}
			
			
			
			
           
			
			 descuento=BigInteger.valueOf(obtenerDescuento(fechaEntrada));
            
		}
		
		
		String hql="select max( codreserva+1) from T3reservas";
		Query cons = session.createQuery(hql, BigInteger.class);
		BigInteger codreserva= (BigInteger) cons.getSingleResult();
		
		if(!error) {
			T3reservas re=new T3reservas();
			re.setCodreserva(codreserva);
			re.setT3clientes(cli);
			re.setDescuento(descuento);
			re.setT3habitaciones(habit);
			re.setFechaentrada(entrada);
			re.setFechasalida(salida);
			
			session.persist(re); //para guardar los cambios en la bbdd
			
			tx.commit();
			System.out.println("La inserción se realizo con exito");
			session.close();
		}else {
			System.out.println("NO SE PUDO REALIZAR LA INSERCIÓN POR LOS SIGUIENTES ERRORES: ");
			System.out.println(mensaje);
		}
		
	}

	private static int obtenerDescuento(String fechaEntrada) {
		// Extraer el mes de la cadena de texto
        String mesTexto = fechaEntrada.substring(3, 5); // Extraer caracteres del mes
        int mes = Integer.parseInt(mesTexto); // Convertir el mes a entero

        int descuento;

        // Determinar el trimestre y asignar descuento
        if (mes >= 1 && mes <= 3) { // Primer trimestre
            descuento = 10;
        } else if (mes >= 4 && mes <= 6) { // Segundo trimestre
            descuento = 4;
        } else if (mes >= 7 && mes <= 9) { // Tercer trimestre
            descuento = 0;
        } else { // Cuarto trimestre (octubre-diciembre)
            descuento = 5;
        }

        return descuento;
	}

}
