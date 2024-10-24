package operacionesEmple;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class OperacionesEmple {
	public  boolean comprobarEmple(Connection conexion, int id) {
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

	public  boolean comprobardir(Connection conexion, int id) {
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

	public  String borraremple(Connection conexion, int id) {
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
	
	public  String modificaremple(Connection conexion, int empno, String ape, String ofi, float sal, float comi, java.sql.Date fecha, int dep, int dir) {
		String mensaje="";
		
		if(comprobarEmple(conexion, empno)) {
			//existe, se puede modificar
			String sql="UPDATE EMPLEADOS SET apellido=?, oficio=?, dir=?,fecha_alt=?,salario=?,comision=?,dept_no=? WHERE emp_no=?";
			
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

	
	public  String insertarEmple(Connection conexion, int empno, String ape, String ofi, float sal, float comi, java.sql.Date fecha, int dep, int dir) {
		String mensaje="";
		
		if(!comprobarEmple(conexion, empno)) {
			//no existe, se puede insertar 
			String	sql="insert into empleados(emp_no, apellido, oficio, dir,fecha_alt,salario,comision,dept_no)values(?,?,?,?,?,?,?,?) ";
			try {
				PreparedStatement sentencia = conexion.prepareStatement(sql);
				sentencia.setInt(1, empno);
				sentencia.setString(2, ape);
				sentencia.setString(3,ofi);
				sentencia.setInt(4, dir);
				sentencia.setDate(5, fecha);
				sentencia.setFloat(6, sal);
				sentencia.setFloat(7, comi);
				sentencia.setInt(8, dep);
			
				
				int linea = sentencia.executeUpdate();
				mensaje = "EMPLEADO INSERTADO: " + empno;
				sentencia.close();

			} catch (SQLException e) {
				// TODO Auto-generated catch block
			//	e.printStackTrace();
				mensaje=e.getMessage();
			}
			
		}else {

			mensaje="EMPLEADO YA EXISTE: "+empno+", NO SE INSERTA";
		}
		
		return mensaje;
		
	}
	
	
	public static void verempleados(Connection conexion) {
		String	sql="select emp_no, apellido, oficio, dir,fecha_alt,salario,comision,dept_no from empleados ";
		try {
			PreparedStatement sentencia = conexion.prepareStatement(sql);
			
			ResultSet resul=sentencia.executeQuery();
			System.out.printf("%10s %15s %15s %10s %10s %10s %10s %10s %n","EMP_NO","APELLIDO","OFICIO","DIRECCION","FECHA ALTA "," SALARIO","COMISION","DEPARTAMENTO");
			System.out.printf("%10s %15s %15s %10s %10s %10s %10s %10s %n","----------","----------","----------","----------","----------","----------","----------","----------");
			while(resul.next()) {
				System.out.printf("%10s %15s %15s %10s %10s %10s %10s %10s %n",resul.getInt(1),resul.getString(2),resul.getString(3),resul.getInt(4),resul.getDate(5),resul.getFloat(6),resul.getFloat(7),resul.getInt(8));

				System.out.printf("%10s %15s %15s %10s %10s %10s %10s %10s %n","----------","----------","----------","----------","----------","----------","----------","----------");

				
			}

			resul.close();
			sentencia.close();
			
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
		//	e.printStackTrace();
	
			System.out.println(e.getMessage());
		}
	}
	
	
	public  void verunempleados(Connection conexion,int empno) {
		if(comprobarEmple(conexion,empno)) {
			
		
		String	sql="select emp_no, apellido, oficio, dir,fecha_alt,salario,comision,dept_no from empleados where emp_no=? ";
		try {
			PreparedStatement sentencia = conexion.prepareStatement(sql);
			sentencia.setInt(1, empno);
			ResultSet resul=sentencia.executeQuery();
			resul.next();
		
			System.out.println("DATOS DEL EMPLEADO: "+empno);
			System.out.println("Apellido: "+resul.getString(2));
			System.out.println("Oficio: "+resul.getString(3));
			System.out.println("Director: "+resul.getInt(4));
			System.out.println("Fecha alta: "+resul.getDate(5));
			System.out.println("Salario: "+resul.getFloat(6));
			System.out.println("Comision: "+resul.getFloat(7));
			System.out.println("Depart: "+resul.getInt(8));
			System.out.println("-----------------------------");

			resul.close();
			sentencia.close();
			
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
		//	e.printStackTrace();
	
			System.out.println(e.getMessage());
		}
		}else {
			System.out.println("Empleado "+empno+" no existe");
		}
	}

}
