package prueba1;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class pruebass {
public static void main(String[] args) throws SQLException, ClassNotFoundException {
	/*Tomando como base el programa que ilustra los pasos de funcionamiento de JDBC 
	 * obtener el APELLIDO, OFICIO y SALARIO de los empleados del departamento 10. 
Realiza otro programa Java que visualice el Apellido del empleado con máximo salario, 
visualiza también su SALARIO y el nombre de su departamento. 

*/
	
	
	System.out.println("Prueba Mysql!");
	Class.forName("com.mysql.cj.jdbc.Driver");
	Connection conexion = null;
	conexion = DriverManager.getConnection("jdbc:mysql://localhost:3306/ejemplo", "root", "");
	System.out.println("Empleados del departamento 10: ");
	Statement sentencia;
	try {
		sentencia = conexion.createStatement();
		String sql = "select apellido, oficio, salario from empleados where dept_no=10";
		ResultSet resul = sentencia.executeQuery(sql);

		// Recorremos el resultado para visualizar cada fila
		// Se hace un bucle mientras haya registros y se van mostrando

		while (resul.next()) {
			System.out.printf("%S, %s, %d %n", resul.getString(1), resul.getString(2), resul.getInt(3));
		}
		
		System.out.println("Empleado con el mayor salario");
		 sql = "SELECT apellido, SALARIO, dnombre  from departamentos JOIN  empleados ON (departamentos.dept_no= empleados.dept_no)\r\n"
		 		+ "where SALARIO=(SELECT MAX(SALARIO) FROM EMPLEADOS)\r\n"
		 		+ "";
		 resul = sentencia.executeQuery(sql);

		// Recorremos el resultado para visualizar cada fila
		// Se hace un bucle mientras haya registros y se van mostrando

		while (resul.next()) {
			System.out.printf("%s, %d, %s %n", resul.getString(1), resul.getInt(2), resul.getString(3));
		}

		resul.close(); // Cerrar ResultSet
		sentencia.close(); // Cerrar Statement
		conexion.close();
	} catch (SQLException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	
	
	
	
}
}
