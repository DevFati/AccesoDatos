package pruebaProcedimientos;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Types;




public class Principal {
	
	public static void main(String[] args) {
		try {
			//Probamos de mysql 
			// CREATE PROCEDURE datos_dep
	        //   (d int, OUT nom VARCHAR(15), OUT locali VARCHAR(15)) 

			String dep = args[0]; // departamento

			Connection conexion=Conexiones.getMysql("ejemplo", "root","1234");

			String sql = "{ call datos_dep (?, ?, ?) } ";

			// Preparar la llamada
			CallableStatement llamada = conexion.prepareCall(sql);

			llamada.setInt(1, Integer.parseInt(dep)); // dep
			llamada.registerOutParameter(2, Types.VARCHAR);// nombre
			llamada.registerOutParameter(3, Types.VARCHAR);// localidad

			// Ejecutar el procedimiento
			llamada.executeUpdate();
			System.out.printf("Nombre Dep: %s, Localidad: %s %n", llamada.getString(2), llamada.getString(3));

			llamada.close();
			conexion.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
	}

	public static void main3(String[] args) {
		try {
		    
		        Connection conexion = Conexiones.getOracle("Ejemplo", "dam");
			//Connection conexion=Conexiones.getMysql("ejemplo", "root","1234");
		     // recuperar parametro de main
				String dep = args[0]; // departamento

				// Construir orden de llamada
				// CREATE OR REPLACE FUNCTION nombre_dep
				// (d NUMBER, locali OUT VARCHAR2) RETURN VARCHAR2 AS
				// nom VARCHAR2(15);

				String sql = "{ ? = call nombre_dep (?, ?) } "; // ORACLE

				// Preparar la llamada
				CallableStatement llamada = conexion.prepareCall(sql);

				// registrar parámetro de resultado, return
				llamada.registerOutParameter(1, Types.VARCHAR);// valor devuelto NOM

				llamada.setInt(2, Integer.parseInt(dep)); // param de entrada

				// Registrar parámetro de salida
				llamada.registerOutParameter(3, Types.VARCHAR);// parámetro OUT LOCALI

				// Ejecutar el procedimiento
				llamada.executeUpdate();
				System.out.printf("Nombre Dep: %s, Localidad: %s %n", llamada.getString(1), llamada.getString(3));
				llamada.close();
				conexion.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}// fin de main

		
		public static void main2(String[] args) {

			try {

				 Connection conexion = Conexiones.getOracle("Ejemplo", "dam");
					//Connection conexion=Conexiones.getMysql("ejemplo", "root","1234");

				subidasal(args, conexion);

			}

			catch (SQLException e) {
				e.printStackTrace();
			}

		}// fin de main

		private static void subidasal(String[] args, Connection conexion) throws SQLException {
			// recuperar parámetros de main
			String dep = args[0]; // departamento
			String subida = args[1]; // subida

			// construir orden de llamada
			String sql = "{ call subida_sal (?, ?) } ";

			// Preparar la llamada
			CallableStatement llamada = conexion.prepareCall(sql);

			// Dar valor a los argumentos
			llamada.setInt(1, Integer.parseInt(dep)); // primero
			llamada.setFloat(2, Float.parseFloat(subida)); // segundo

			// Ejecutar el procedimiento
			llamada.executeUpdate();
			System.out.println("Subida realizada....");

			llamada.close();
			conexion.close();
		}

	}
