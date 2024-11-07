package mortahilchachoufatima;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;


public class Principal {
	private static Connection conexion = Conexiones.getOracle("examen", "examen");

	public static void main(String[] args) {
		Scanner teclado = new Scanner(System.in);
		int operacion;
		do {
			menu();
			operacion=teclado.nextInt();
			switch (operacion) {

			case 1:
		
				//cargar fecha 
				java.util.Date utilDate=new java.util.Date();
				java.sql.Date sqlDate=new java.sql.Date(utilDate.getTime());
				insertarestudiante("DAVID HUTTON","C/La Luz 4. Mostoles. ESP","925666777",sqlDate);
				
				break;
			case 2:
			
				listarproyecto(5);
				
				break;
			case 3:
				añadircolumnas();
		
				break;
			case 4:
			
				break;
			

			default:
				System.out.println("Opción invalida!");

			}

		} while (operacion != 0);
	}

	
	private static void añadircolumnas() {
		
	String crearcolumna1="ALTER TABLE proyectos ADD numempre number(5)";
	String crearcolumna2="ALTER TABLE proyectos ADD IMPORTEEMP float(10)";
	String crearcolumna3="ALTER TABLE proyectos ADD NUMALUM number(5)";
	String crearcolumna4="ALTER TABLE proyectos ADD GASTOALUM float(15)";
	String crearcolumna5="ALTER TABLE proyectos ADD GASTORECUR float(15)";
	String commit="COMMIT";
	
	String actualiza1="update proyectos p set numempre = (select coalesce(count(*),0) from patrocina where codigoproyecto=p.codigoproyecto)";
	String actualiza2="update proyectos p set importeemp=(select coalesce(sum(importeaportacion),0) from patrocina where codigoproyecto=p.codigoproyecto)";
	String actualiza3="update proyectos p set gastoalum= (select coalesce(sum(numaportaciones)*p.extraaportacion,0) from participa  where codigoproyecto=p.codigoproyecto)";
	String actualiza4="update proyectos p set gastorecur=(select coalesce(sum(pvp* cantidad),0) from usa join recursos using (codrecurso) where codigoproyecto=p.codigoproyecto  )";
	String actualiza5="update proyectos p set numalum=(select coalesce(count(*),0) from participa where codigoproyecto=p.codigoproyecto  )";

	

		try {
			PreparedStatement sent = conexion.prepareStatement(crearcolumna1);
			sent.executeUpdate();
			PreparedStatement sent2 = conexion.prepareStatement(crearcolumna2);
			sent2.executeUpdate();
			PreparedStatement sent3 = conexion.prepareStatement(crearcolumna3);
			sent3.executeUpdate();
			PreparedStatement sent4 = conexion.prepareStatement(crearcolumna4);
			sent4.executeUpdate();
			PreparedStatement sent5 = conexion.prepareStatement(crearcolumna5);
			sent5.executeUpdate();
			System.out.println("---------------------------------");
			System.out.println("Columnas creadas."); 
			
			sent.close();
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			System.out.println("Atención ya existen las columnas: ");
			System.out.println(e.getMessage());
			
		}
			
		try {
			PreparedStatement sent1 = conexion.prepareStatement(actualiza1);
			int lin1 = 	sent1.executeUpdate();
			PreparedStatement sent2 = conexion.prepareStatement(actualiza2);
			int lin2 = 	sent2.executeUpdate();
			PreparedStatement sent3 = conexion.prepareStatement(actualiza3);
			int lin3 = 	sent3.executeUpdate();
			PreparedStatement sent4 = conexion.prepareStatement(actualiza4);
			int lin4 = 	sent4.executeUpdate();
			PreparedStatement sent5 = conexion.prepareStatement(actualiza5);
			int lin5 = 	sent5.executeUpdate();
			System.out.println("Columnas actualizadas"); 
			
			
			
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				System.out.println("Atención error en la actualización: ");
				System.out.println(e.getMessage());
				
			}
		
	}


	private static void listarproyecto(int codproyecto) {
		float aportaciones=0;
		float prepTotal=0;
		float numaportaciones=0;
		float totalap=0;
		String sql1="select nombre, fechainicio, fechafin, presupuesto, extraaportacion from proyectos where codigoproyecto=?";
		try {
			PreparedStatement sentencia;
			sentencia = conexion.prepareStatement(sql1);
			sentencia.setInt(1, codproyecto);
			ResultSet resul = sentencia.executeQuery();
			resul.next();
			
		
				
				System.out.println("    COD-PROYECTO: "+codproyecto+"    NOMBRE: "+resul.getString(1)+"    \nFECHAINICIO: "+resul.getDate(2)+",       FECHA FIN: "+resul.getDate(3)+"        \nPRESUPUESTO"+resul.getFloat(4)+"      EXTRAAPORTACIÓN: "+resul.getFloat(5));
			System.out.println("-----------------------------------------------------------------------------------------------------------------------------");
				
				System.out.printf("      %10s %30s %-10s %10s %10s %10s %n",
						"---------",  "---------","---------------------------------------------","---------","---------","---------","---------");
				
				
			
				
				
				String sql2="select importeaportacion, fechaaportacion, codentidad from patrocina where codigoproyecto=?";
				
				PreparedStatement sentencia2;
				sentencia2= conexion.prepareStatement(sql2);
				sentencia2.setInt(1, codproyecto);
				ResultSet resul2 = sentencia2.executeQuery();
				if(resul2.next()) {
					System.out.println("LISTA DE ENTIDADES QUE PATROCINA EL PROYECTO");
					
					System.out.printf("      %10s %30s %10s %10s %n",
							"Código",  "Descripción","Importe aportación ","Fecha aportación");
					
					System.out.printf("      %10s %30s %10s %10s %n",
							"----------",  "----------","---------- ","----------");
				
				
				do {
					String sql3="select codentidad, descripcion from entidades where codentidad=?";
					
					PreparedStatement sentencia3;
					sentencia3= conexion.prepareStatement(sql3);
					sentencia3.setInt(1, resul2.getInt(3));
					ResultSet resul3 = sentencia3.executeQuery();
				while(resul3.next()) {	
					
					System.out.printf("      %10s %30s %10s %10s %n",
						resul2.getInt(3)	,  resul3.getString(2),resul2.getFloat(1),resul2.getDate(2));
					
					aportaciones=aportaciones+resul2.getFloat(1);
				}
				}while(resul2.next());
				
				prepTotal=aportaciones+resul.getFloat(4);
				
				System.out.printf("      %30s %30s  %n",
						"TOTAL APORTACIONES: ",aportaciones);
				
				System.out.printf("      %30s %30s  %n",
						"PRESUPUESTO TOTAL: ",prepTotal);
				}else {
					System.out.println("NINGUNA ENTIDAD PATROCINA ESTE PROYECTO");
				}
				System.out.println("--------------------------------------------------------------------------------------------------------------------------");
				
				
				
				String sql4="select codestudiante, nombre, direccion from estudiantes where codestudiante in (select codestudiante from participa where codigoproyecto=?)";
				PreparedStatement sentencia4;
				sentencia4= conexion.prepareStatement(sql4);
				sentencia4.setInt(1, codproyecto);
				ResultSet resul4= sentencia4.executeQuery();
				if(resul4.next()) {
					System.out.println("LISTA DE ESTUDIANTES QUE PARTICIPAN EN EL PROYECTO ");
					
					System.out.printf("      %10s %30s %-10s %10s %20s %20s %20s  %n",
							"Cod",  "Nombre","Direccion ","CodPar","Tipo aportación","NumApt","TotAport");
					
					System.out.printf("      %10s %30s %-10s %10s %20s %20s %20s  %n",
							"----------",  "----------","---------- ","----------","----------","----------","----------");
				
				do {
					String sql5="select codparticipacion, tipoparticipacion, numaportaciones from participa where codestudiante=? and codigoproyecto=?";
					PreparedStatement sentencia5;
					sentencia5= conexion.prepareStatement(sql5);
					sentencia5.setInt(1, resul4.getInt(1));
					sentencia5.setInt(2, codproyecto);
					ResultSet resul5= sentencia5.executeQuery();
					resul5.next();
					
					System.out.printf("      %10s %30s %-10s %10s %20s %20s %20s  %n",
							resul4.getInt(1), resul4.getString(2),resul4.getString(3),resul5.getInt(1),resul5.getString(2),resul5.getInt(3),resul.getFloat(5)*resul5.getInt(3)); 
					
					
					numaportaciones=numaportaciones+resul5.getFloat(3);
					totalap=totalap+resul.getFloat(5)*resul5.getInt(3);
				}while(resul4.next());
				
				System.out.printf("      %10s  %60s %20s %30s  %n",
						"TOTALES: ", "",numaportaciones,totalap);
				
		}else {
			System.out.println("NINNGUN ESTUDIANTE PERTENECE A ESTE PROYECTO");
		}
				
				
				
				
			
				
			

		} catch (SQLException e) {
			e.printStackTrace();
		}
		
	}


	private static void insertarestudiante(String nom, String dir, String tel, Date sqlDate) {
		
		String mensaje = "";
		int error = 0;
		int codestudiante=0;

		try {
			

			if (!comprobarNombre(nom)) {
				//existe el nombre
				error = 1;
				mensaje = mensaje + "\n  Ya existe un estudiante con ese nombre, no se ha podido insertar: " + nom;
			}

			if(nom.isEmpty()) {
				error = 1;
				mensaje = mensaje + "\n  EL NOMBRE NO PUEDE ESTAR VACÍO O BLANCO ";
			}
			
			if(dir.isEmpty()) {
				error = 1;
				mensaje = mensaje + "\n  LA DIRECCION NO PUEDE ESTAR VACÍA O BLANCA";
			}
			
			if(tel.isEmpty()) {
				error = 1;
				mensaje = mensaje + "\n  EL TELEFONO NO PUEDE ESTAR VACÍO O BLANCO ";
			}
			
			if(sqlDate.equals("")) {
				error = 1;
				mensaje = mensaje + "\n  LA FECHA NO PUEDE ESTAR VACÍA O BLANCA. ";
			}

			if (error == 0) {
				// a insertar
				//creamos el codestudiante 
				String sql1="select max(codestudiante+1) from estudiantes";
				PreparedStatement sentencia;
				sentencia = conexion.prepareStatement(sql1);
				ResultSet resul = sentencia.executeQuery();
				resul.next();
				codestudiante=resul.getInt(1);
				
				
				String sql2 = "insert into estudiantes (codestudiante,  nombre,  direccion,  tlf,  fechaalta)"
						+ " values (?, ?,?, ?,? )";

				PreparedStatement sentencia2 = conexion.prepareStatement(sql2);
				sentencia2 = conexion.prepareStatement(sql2);
				sentencia2.setInt(1, codestudiante);
				sentencia2.setString(2, nom);
				sentencia2.setString(3, dir);
				sentencia2.setString(4, tel);
				sentencia2.setDate(5, sqlDate);

				int filas = sentencia2.executeUpdate();

				System.out.println("Estudiante insertado correctamente con el código " + codestudiante);
				resul.close();
				sentencia.close();
				sentencia2.close();
				
			}

			else {
				System.out.println("Error, no inserta el estudiante. Errores: \n" + mensaje);

			}

		} catch (SQLException e) {
			// TODO Auto-generated catch block
		//	e.printStackTrace();
			mensaje =  e.getMessage();
		}
		
	}

	private static boolean comprobarNombre(String nom) {
		boolean existe = false;
		nom="'"+nom+"'";
		String sql = "select * from estudiantes where nombre = ?";
		try {
			PreparedStatement sentencia;
			sentencia = conexion.prepareStatement(sql);
			
			sentencia.setString(1,nom );
			ResultSet resul = sentencia.executeQuery();
			if (resul.next()) {
				existe = true;

			} else {
				existe = false;
			}
			resul.close();
			sentencia.close();

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return existe;
	}


	private static void menu() {
		System.out.println("OPERACIONES CON PROYECTOS. Realizado por Fatima");
		System.out.println("1. EJERCICIO1: Insertar estudiantes");
		System.out.println("2. EJERCICIO2: Listar proyecto");
		System.out.println("3. EJERCICIO3: Añadir columnas y actualizar con datos nuevos");
		System.out.println("4. Salir");
		
	}

		
	
	
	
	
}
