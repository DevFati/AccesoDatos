package prueba1;

import java.sql.*;
import java.util.Scanner;

public class DataBaseMetadata {

	static String consulta = "SELECT * FROM EMPLEADOS JOIN DEPARTAMENTOS USING (DEPT_NO)";

	public static void main(String[] args) {
		Scanner s = new Scanner(System.in);
		boolean salir = false;
		try {
			Connection conexion = null;
			do {
				menu();
				int opcion = s.nextInt();
				switch (opcion) {
				case 1:
					Class.forName("com.mysql.cj.jdbc.Driver");
					conexion = DriverManager.getConnection("jdbc:mysql://localhost:3306/ejemplo", "root", "1234");
					// catalogo,esquema,patronDeTabla,tipos[]
					verMetadatos(conexion, "ejemplo", null, null, null);
					verResultSetMetadata(conexion);
					conexion.close(); // Cerrar conexión
					break;
				case 2:
					Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
					conexion = DriverManager.getConnection("jdbc:derby:basedatos/DERBY/ejemplo");
					// catalogo,esquema,patronDeTabla,tipos[]
					verMetadatos(conexion, null, "APP", null, null);
					verResultSetMetadata(conexion);
					break;
				case 3:
					Class.forName("org.hsqldb.jdbcDriver");
					conexion = DriverManager.getConnection("jdbc:hsqldb:file:.\\basedatos\\bdhsqldb2\\ejemplo\\ejemplo");
					// catalogo,esquema,patronDeTabla,tipos[]
					verMetadatos(conexion, "PUBLIC", "PUBLIC", null, null);
					verResultSetMetadata(conexion);
					break;
				case 4:
					Class.forName("org.sqlite.JDBC");
					conexion = DriverManager.getConnection("jdbc:sqlite:.\\basedatos\\SQLITE\\ejemplo.db");
					// catalogo,esquema,patronDeTabla,tipos[]
					verMetadatos(conexion, null, null, null, null);
					verResultSetMetadata(conexion);
					break;
				case 5:
					Class.forName("oracle.jdbc.driver.OracleDriver");
					conexion = DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:XE", "ejemplo", "dam");
					// catalogo,esquema,patronDeTabla,tipos[]
					verMetadatos(conexion, null, "EJEMPLO", null, null);
					verResultSetMetadata(conexion);
					break;
				case 6:
					Class.forName("org.mariadb.jdbc.Driver");
					conexion = DriverManager.getConnection("jdbc:mariadb://localhost:3306/ejemplo", "root", "1234");
					// catalogo,esquema,patronDeTabla,tipos[]
					verMetadatos(conexion, "ejemplo", null, null, null);
					verResultSetMetadata(conexion);
					break;
				case 7:
					salir = true;
					break;
				default:
					System.out.println("Opcion incorrecta");
					break;
				}
			} while (!salir);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void menu() {
		System.out.println("");
		System.out.println("OPERACIONES DATABASEMETADATA: ");
		System.out.println("1. Prueba mysql");
		System.out.println("2. Prueba Derby");
		System.out.println("3. Prueba HSQLDB");
		System.out.println("4. Prueba SQLite");
		System.out.println("5. Prueba Oracle");
		System.out.println("6. Prueba MariaDB");
		System.out.println("7. Salir");
		System.out.println("");
	}

	private static void verMetadatos(Connection conexion, String catalogo, String esquema, String patronDeTabla,
			String tipos[]) {
		try {
			DatabaseMetaData dbmd = conexion.getMetaData();
			ResultSet resul = null;
			String nombre = dbmd.getDatabaseProductName();
			String driver = dbmd.getDriverName();
			String url = dbmd.getURL();
			String usuario = dbmd.getUserName();

			System.out.println("INFORMACIÓN SOBRE LA BASE DE DATOS:");
			System.out.println("===================================");
			System.out.printf("Nombre : %s %n", nombre);
			System.out.printf("Driver : %s %n", driver);
			System.out.printf("URL    : %s %n", url);
			System.out.printf("Usuario: %s %n", usuario);
			System.out.printf("MajorVersion: %s %n", dbmd.getDatabaseMajorVersion());

			// Obtener información de las tablas y vistas que hay
			resul = dbmd.getTables(catalogo, esquema, patronDeTabla, tipos);
			System.out.println("INFORMACION getTables(): ");
			System.out.println("===================================");
			while (resul.next()) {
				String catalogo2 = resul.getString(1);// columna 1
				String esquema2 = resul.getString(2); // columna 2
				String tabla = resul.getString(3); // columna 3
				String tipo = resul.getString(4); // columna 4
				// String ref=resul.getString(9); //columna 9
				System.out.printf("%s - Catalogo: %s, Esquema: %s,Nombre: %s %n", tipo, catalogo2, esquema2, tabla);

				// MOSTRAR COLUMNAS DE LA TABLA
				System.out.println("    COLUMNAS TABLA " + tabla + ": ");
				System.out.println("    ===================================");
				ResultSet columnas = dbmd.getColumns(catalogo, esquema, tabla, null);
				while (columnas.next()) {
					String nombCol = columnas.getString("COLUMN_NAME"); // getString(4)
					String tipoCol = columnas.getString("TYPE_NAME"); // getString(6)
					String tamCol = columnas.getString("COLUMN_SIZE"); // getString(7)
					String nula = columnas.getString("IS_NULLABLE"); // getString(18)
					System.out.printf("    Columna: %s, Tipo: %s, Tamaño: %s, ¿Puede ser Nula:? %s %n", nombCol,
							tipoCol, tamCol, nula);
				}
				System.out.println("");
				columnas.close();

				if (tipo.equals("TABLE")) { // Para que lo haga solo con las tablas, porque con SYSTEM TABLE da error
					System.out.println("    Clave primaria de " + tabla + ": ");
					System.out.println("    ===================================");
					ResultSet pk = dbmd.getPrimaryKeys(catalogo, esquema, tabla);
					String pkDep = "", separador = "";
					while (pk.next()) {
						pkDep = pkDep + separador + pk.getString("COLUMN_NAME");// getString(4)
						separador = "+";
						System.out.println("    Clave Primaria: " + pkDep);
					}
					System.out.println("");
					pk.close();

					System.out.println("    Claves exportadas de " + tabla + ": ");
					System.out.println("    ===================================");
					ResultSet fk = dbmd.getExportedKeys(catalogo, esquema, tabla);
					while (fk.next()) {
						String fk_name = fk.getString("FKCOLUMN_NAME"); // getString(8)
						String pk_name = fk.getString("PKCOLUMN_NAME"); // getString(4)
						String pk_tablename = fk.getString("PKTABLE_NAME"); // getString(3)
						String fk_tablename = fk.getString("FKTABLE_NAME"); // getString(7)
						System.out.printf("    Tabla PK: %s, Clave Primaria: %s %n", pk_tablename, pk_name);
						System.out.printf("    Tabla FK: %s, Clave Ajena: %s %n", fk_tablename, fk_name);
					}
					System.out.println("");
					fk.close();

				}

			}

			resul.close();

		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	private static void verResultSetMetadata(Connection conexion) {
		try {
			System.out.println("METADATOS DE LA CONSULTA: "+consulta);
			System.out.println("===================================");
			Statement sentencia = conexion.createStatement();
			ResultSet rs = sentencia.executeQuery(consulta);

			ResultSetMetaData rsmd = rs.getMetaData();

			int nColumnas = rsmd.getColumnCount();
			String nula;
			System.out.printf("Número de columnas recuperadas: %d%n", nColumnas);
			for (int i = 1; i <= nColumnas; i++) {
				System.out.printf("Columna %d: %n ", i);
				System.out.printf("  Nombre: %s %n   Tipo: %s %n ", rsmd.getColumnName(i), rsmd.getColumnTypeName(i));
				if (rsmd.isNullable(i) == 0)
					nula = "NO";
				else
					nula = "SI";

				System.out.printf("  Puede ser nula?: %s %n ", nula);
				System.out.printf("  Máximo ancho de la columna: %d %n", rsmd.getColumnDisplaySize(i));
				System.out.printf("  Nombre de la tabla: %s %n", rsmd.getTableName(i));
			} // for

			sentencia.close();
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
