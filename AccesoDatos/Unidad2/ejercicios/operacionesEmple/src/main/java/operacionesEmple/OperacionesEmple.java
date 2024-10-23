package operacionesEmple;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class OperacionesEmple {
	public static boolean comprobarEmple(Connection conexion, int id) {
		boolean existe = false;

		String sql = "select * from empleados where emp_no=?";
		try {
			PreparedStatement sentencia;
			sentencia = conexion.prepareStatement(sql);
			sentencia.setInt(1, id);
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

	public static boolean comprobardir(Connection conexion, int id) {
		boolean existe = false;

		String sql = "select * from empleados where dir=?";
		try {
			PreparedStatement sentencia;
			sentencia = conexion.prepareStatement(sql);
			sentencia.setInt(1, id);
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

	public static String borraremple(Connection conexion, int id) {
		String mensaje = "";

		if (comprobarEmple(conexion, id)) {
			// empleado existe, se puede borrar, comprobar antes que no sea director
			// no sea director

			if (comprobardir(conexion, id)) {
				// el empleado es dir
				mensaje = "EMPLEADO NO BORRADO, ES DIRECTOR DE OTRO: "+id;
			} else {
				// se borra el empleado porque el empleado existe y no es dir
				String sql = "delete from empleados where emp_no=?";

				try {
					PreparedStatement sentencia = conexion.prepareStatement(sql);
					sentencia.setInt(1, id);
					int linea = sentencia.executeUpdate();
					mensaje = "EMPLEADO BORRADO: " + id;
					sentencia.close();

				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					mensaje=e.getMessage();
				}

			} //fin borrar 

		} else {
			mensaje = "EMPLEADO NO BORRADO, NO EXISTE";
		}

		return mensaje;
	}
	
	public static String modificaremple(Connection conexion, int empno, String ape, String ofi, float sal, float comi, java.sql.Date fecha, int dep, int dir) {
		String mensaje="";
		
		if(comprobarEmple(conexion, empno)) {
			//existe, se puede modificar
			String sql="UPDATE EMPLEADOS SET emp_no=?, apellido=?, oficio=?, dir=?,fecha_alt=?,salario=?,comision=?,dept_no=? WHERE emp_no=?";
			
			try {
				PreparedStatement sentencia = conexion.prepareStatement(sql);
				sentencia.setString(1, ape);
				sentencia.setString(2,ofi);
				sentencia.setInt(3, dir);
				sentencia.setDate(4, fecha);
				sentencia.setFloat(5, sal);
				sentencia.setFloat(6, comi);
				sentencia.setInt(7, dep);
				sentencia.setInt(8, empno);
				
				int linea = sentencia.executeUpdate();
				mensaje = "EMPLEADO ACTUALIZADO: " + empno;
				sentencia.close();

			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				mensaje=e.getMessage();
			}
			
		}else {
			//empleado no existe 
			mensaje="EMPLEADO NO EXISTE: "+empno+", NO SE ACTUALIZA";
		}
		
		return mensaje;
		
	}


}
