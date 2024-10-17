package pruebasSentencias;





import java.sql.*;



public class Principal {
	
	public static void main(String[] args) {
		Connection conexion=Conexiones.getMysql("ejemplo", "root","1234");

		System.out.println("PRUEBA verempleados MYSQL");

		verempleadosdep(conexion, 10);
		
		 conexion=Conexiones.getDerby("basedatos/DERBY/ejemplo");

		System.out.println("PRUEBA verempleados  Derby");
		verempleadosdep(conexion, 20);
	}

	public static void mainInsertarEmpleados(String[] args) {

		Connection conexion=Conexiones.getMysql("ejemplo", "root","1234");

		System.out.println("PRUEBA INSERTAR DEP MYSQL");

		System.out.println(insertarDepartamento(conexion, 12,"DEP 12", "TOLEDO"));

		

		

		conexion=Conexiones.getSQLite(".\\basedatos\\SQLITE\\ejemplo.db");

		System.out.println("PRUEBA INSERTAR DEP SQLITE");

		System.out.println(insertarDepartamento(conexion, 12,"DEP 12", "TOLEDO"));

		

		

		conexion=Conexiones.getOracle("ejemplo", "dam");

		System.out.println("PRUEBA INSERTAR EN ORACLE");

		System.out.println(insertarDepartamento(conexion, 12,"DEP 12", "TOLEDO"));

	}

	public static void mainCrearVista(String[] args) {

		Connection conexion=Conexiones.getMysql("ejemplo", "root","1234");

		System.out.println("PRUEBA CREAR VISTA MYSQL");

		String vista="CREATE OR REPLACE VIEW totales (dep, dnombre, nemp, media) AS SELECT d.dept_no, dnombre, COUNT(emp_no), AVG(salario) FROM departamentos d LEFT JOIN empleados e ON e.dept_no = d.dept_no GROUP BY d.dept_no, dnombre";

		crearVista(conexion,vista);

		

		

		

		conexion=Conexiones.getOracle("ejemplo", "dam");

		System.out.println("PRUEBA SUBIDA EN ORACLE");

		crearVista(conexion, vista);

	}

	

	public static void mainSubirSalario(String[] args) {

		//Subir el salario a los empleados de un departamento

		Connection conexion=Conexiones.getMysql("ejemplo", "root","1234");

		System.out.println("PRUEBA SUBIDA EN MYSQL");

		subirSalario(conexion,100,10);

		

		conexion=Conexiones.getOracle("ejemplo", "dam");

		System.out.println("PRUEBA SUBIDA EN ORACLE");

		subirSalario(conexion, 100, 10);

		

		conexion=Conexiones.getSQLite(".\\basedatos\\SQLITE\\ejemplo.db");

		System.out.println("PRUEBA SUBIDA EN SQLITE");

		subirSalario(conexion, 100, 10);

		

	}



	private static void subirSalario(Connection conexion, int subida, int departamento) {

		String sql="UPDATE empleados SET salario= salario+"+subida+" WHERE dept_no="+departamento;

		System.out.println(sql);

		try {

			Statement sentencia = conexion.createStatement();

			int filas = sentencia.executeUpdate(sql);  

			System.out.printf("Empleados modificados: %d %n", filas);

			conexion.close();

		} catch (SQLException e) {

			e.printStackTrace();

		}

	}

	

	private static void crearVista(Connection conexion, String vista) {

		System.out.println("Vista a crear: "+vista);

		try {

			Statement sentencia = conexion.createStatement();

			int filas = sentencia.executeUpdate(vista);  

			System.out.println("Vista creada");

			conexion.close();

		} catch (SQLException e) {

			e.printStackTrace();

		}

	}

	public static String insertarDepartamento(Connection conexion, int dept, String nom, String loc) {

		String mensaje = "";

		try {

			String sql = "INSERT INTO departamentos VALUES("

	                  + dept + ", '" + nom + "', '" + loc + "')";

			Statement sentencia = conexion.createStatement();

			int filas = sentencia.executeUpdate(sql);

			mensaje = "Registro Insertado. Filas afectadas: " + filas;

			sentencia.close();

			conexion.close();

		} catch (SQLException e) {

		 mensaje = "Código de error: " + e.getErrorCode() + "\nMensaje de error: " + e.getMessage();

		}

		return mensaje;

	}
	
public static void verempleadosdep(Connection conexion, int dept) {
		
		try {
		
			String sql = "select emp_no, apellido, oficio, salario from empleados where dept_no = " + dept;
			Statement sentencia = conexion.createStatement();
			ResultSet resul = sentencia.executeQuery(sql);
			System.out.println("Datos de los empleados del dep : " + dept);
			while (resul.next()) {
				System.out.println("Emp-no:" +  resul.getInt(1) +
						". Apellido: " +  resul.getString(2) +
						". Oficio: " +  resul.getString(3) +
						". Salario: " +  resul.getFloat(4) );				
			}
			resul.close();
			sentencia.close();
			
			
		
		} catch (SQLException e) {
			// e.printStackTrace();
			System.out.println("Código de error: " + e.getErrorCode() + 
					"\nMensaje de error: " + e.getMessage());

		}
		
	}


}