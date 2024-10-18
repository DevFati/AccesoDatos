package pruebasSentencias;





import java.sql.*;



public class Principal {
	
	
	public static void main(String[] args) {
	
		Connection conexion=Conexiones.getMysql("ejemplo", "root","1234");

		System.out.println("PRUEBA verempleados MYSQL");
		
		//error en empleado, dir y dep 
		System.out.println(insertarEmpleado(conexion,7369,"","",222,-1500,100,45));
		System.out.println("----------------------");
		// error en el empleado 
		System.out.println(insertarEmpleado(conexion,7369,"EMPLE123","INFORMATICO",7499,1500,100,10));
		System.out.println("----------------------");
		//error en dep
		System.out.println(insertarEmpleado(conexion,123,"EMPLE123",null,7499,1500,100,45));
		System.out.println("----------------------");
		//no hay error
		System.out.println(insertarEmpleado(conexion,123,"EMPLE123","INFORMATICO",7499,1500,100,10));

		
		
	}
	
	private static String insertarEmpleado(Connection conexion, int emp_no, String apellido,String oficio, int dir , float salario, float comision, int dept_no) {

		String mensaje="";
		int error =0; // si ocurre error la ponemos a 1
		
		if(salario<=0) {
			error=1;
			mensaje=mensaje+"EL SALARIO ES NEGATIVO, ERROR NO PUEDE SER NEGATIVO.\n";
		}
		
		if(apellido==null || apellido.equals("")) {
			error=1;
			mensaje=mensaje+"EL APELLIDO NO PUEDE SER NULO.\n";
		}
		
		if(oficio==null || oficio.equals("")) {
			error=1;
			mensaje=mensaje+"EL OFICIO NO PUEDE SER NULO.\n";
		}
		
		//Comprobar si el empleado existe
		String sql1="select count(*) from empleados where emp_no="+emp_no;
		Statement sentencia;
		try {
			sentencia=conexion.createStatement();
			ResultSet resul=sentencia.executeQuery(sql1);
			resul.next();
			int cuenta=resul.getInt(1);
			if(cuenta==1) {
				//empleado existe, no se puede insertar
				mensaje=mensaje+"EL NUM DE EMPLEADO YA EXISTE,"+emp_no+", ERROR AL INSERTAR.\n";
				error=1;
			}
			
			//Comprobar si el director existe 
			 sql1="select count(*) from empleados where emp_no="+dir;
			sentencia=conexion.createStatement();
			resul=sentencia.executeQuery(sql1);
			resul.next();
			cuenta=resul.getInt(1);
			if(cuenta==0) {
				//director no existe
				mensaje=mensaje+"EL DIRECTOR ("+dir+") NO EXISTE EN EMPLEADOS, ERROR AL INSERTAR.\n";
				error=1;
			}
			
			//Comprobar si el dep existe 
			
			 sql1="select count(*) from departamentos where dept_no="+dept_no;
				sentencia=conexion.createStatement();
				resul=sentencia.executeQuery(sql1);
				resul.next();
				cuenta=resul.getInt(1);
				if(cuenta==0) {
					//director no existe
					mensaje=mensaje+"EL NUM DE DEPARTAMENTO("+dept_no+") NO EXISTE EN LA TABLA DEPARTAMENTOS.\n";
					error=1;
				}
				
				//Una vez finalizadas las comprobaciones se pregunta si ha habido algún error
				if(error==1) {
					//no se inserta
					mensaje=mensaje+"REGISTRO NO INSERTADO";
				}else {
					//A insertar el empleado
				//	mensaje=mensaje+"DATOS CORRECTOS. A INSERTAR";
					
					mensaje=insertaremple( conexion,  emp_no, 
							 apellido, oficio,  dir ,  salario, 
							 comision,  dept_no);
				}
			
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return mensaje;
	}
	
	
	private static String insertaremple(Connection conexion, int emp_no, 
			String apellido,String oficio, int dir , float salario, 
			float comision, int dept_no) {
		String mensaje="";
		//cargar fecha 
		java.util.Date utilDate=new java.util.Date();
		java.sql.Date sqlDate=new java.sql.Date(utilDate.getTime());
		System.out.println(utilDate);
		System.out.println(sqlDate);
		
	return mensaje;
	}

	
	
	

	public static void verempleados(String[] args) {
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