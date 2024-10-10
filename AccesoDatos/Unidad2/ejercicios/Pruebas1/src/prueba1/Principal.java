package prueba1;

import java.sql.*;
import java.util.Scanner;

public class Principal {

	public static void main(String[] args) {

		try {
			Connection conexion = null;
			Scanner sc = new Scanner(System.in);
			int opcion = 0;

			do {
				mostrarMenu();
				opcion = sc.nextInt();
				switch (opcion) {
				case 1:
					// Mysql
					System.out.println("Prueba Mysql!");
					Class.forName("com.mysql.cj.jdbc.Driver");
					conexion = DriverManager.getConnection("jdbc:mysql://localhost:3306/ejemplo", "root", "1234");
					consultadepartamentos(conexion);
					conexion.close();

					break;
				case 2:
					System.out.println("Prueba Derby!");
					Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
					conexion = DriverManager.getConnection("jdbc:derby:basedatos/DERBY/ejemplo");
					consultadepartamentos(conexion);
					conexion.close();
					break;
				case 3:
					System.out.println("Prueba HSQLDB!");
					Class.forName("org.hsqldb.jdbcDriver");
					conexion = DriverManager.getConnection("jdbc:hsqldb:file:.\\basedatos\\bdhsqldb2\\ejemplo\\ejemplo");
					consultadepartamentos(conexion);
					conexion.close();
					break;
				case 4:
					System.out.println("Prueba SQLITE!");
					Class.forName("org.sqlite.JDBC");
					conexion = DriverManager.getConnection("jdbc:sqlite:.\\basedatos\\SQLITE\\ejemplo.db");
					consultadepartamentos(conexion);
					conexion.close();
					break;

				case 5:
					System.out.println("Prueba Oracle!");
					Class.forName("oracle.jdbc.driver.OracleDriver");
					conexion = DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:xe", "ejemplo",
							"dam");
					consultadepartamentos(conexion);
					conexion.close();
					break;

				case 0:
					System.out.println("FIN DE MENÚ!");
					break;
				default:
					System.out.println("Seleccione una opción válida!");
					break;
				}
			} while (opcion != 0);

			sc.close();

		} catch (ClassNotFoundException cn) {
			cn.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}// fin de main

	private static void mostrarMenu() {
		System.out.println("------------------------------------------------------");
		System.out.println("OPERACIONES CON DEPARTAMENTOS");
		System.out.println("  1. Prueba mysql.");
		System.out.println("  2. Prueba Derby.");
		System.out.println("  3. Prueba HSQLDB.");
		System.out.println("  4. Prueba Sqlite.");
		System.out.println("  5. Prueba Oracle.");
		System.out.println("  0. Salir");
		System.out.println("------------------------------------------------------");
	}

	private static void consultadepartamentos(Connection conexion) throws SQLException {
		// Preparamos la consulta
		Statement sentencia = conexion.createStatement();
		String sql = "SELECT * FROM departamentos";
		ResultSet resul = sentencia.executeQuery(sql);

		// Recorremos el resultado para visualizar cada fila
		// Se hace un bucle mientras haya registros y se van mostrando

		while (resul.next()) {
			System.out.printf("%d, %s, %s %n", resul.getInt(1), resul.getString(2), resul.getString(3));
		}

		resul.close(); // Cerrar ResultSet
		sentencia.close(); // Cerrar Statement
	}

}
