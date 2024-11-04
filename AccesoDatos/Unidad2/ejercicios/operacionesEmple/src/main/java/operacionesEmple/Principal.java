package operacionesEmple;

import java.sql.Connection;
import java.sql.SQLException;


public class Principal {

	public static void main(String[] args) {
		OperacionesEmple o= new OperacionesEmple();
	//	Connection conexion=Conexiones.getOracle("ejemplo", "dam");

			Connection conexion=Conexiones.getMysql("ejemplo", "root","1234");
			//		Connection conexion=Conexiones.getSQLite(".\\basedatos\\SQLITE\\ejemplo.db");
					
			if(conexion!=null) {
				//probar comprobaremple
				int emp_no=122;
				if(o.comprobarEmple(conexion,emp_no)) {
					System.out.println("Empleado "+emp_no+" existe");
				}else {
					System.out.println("Empleado "+emp_no+" no existe");
				}
			}else {
				System.out.println("ERROR EN LA CONEXION");
			}
			
			
			// probar borraremple 
			
			System.out.println(o.borraremple(conexion, 123));
			System.out.println(o.borraremple(conexion, 1238));
			System.out.println(o.borraremple(conexion, 7566));
			
			//cargar fecha 
			java.util.Date utilDate=new java.util.Date();
			java.sql.Date sqlDate=new java.sql.Date(utilDate.getTime());
			//MODIFICAR 
			System.out.println(o.modificaremple(conexion, 124, "NuevoNombre", "PROGRAMADOR",
					1600f, 0.0f, sqlDate, 20, 7369)); //OK 
			
			System.out.println(o.modificaremple(conexion, 1247, "NuevoNombre", "PROGRAMADOR",
					1600f, 0.0f, sqlDate, 20, 7369)); //no se puede actualizar 
			
			System.out.println(o.modificaremple(conexion, 124, "NuevoNombre", "PROGRAMADOR",
					1600f, 0.0f, sqlDate, 80, 7369)); //error dep no existe 
			

			//INSERTAR 
			System.out.println("---------------------------------------");
			System.out.println(o.insertarEmple(conexion, 1234, "Nuevo1234", "PROGRAMADOR", 1600f, 0.0f, sqlDate, 20, 7369)); //OK
			System.out.println(o.insertarEmple(conexion, 1234, "Nuevo1234", "PROGRAMADOR", 1600f, 0.0f, sqlDate, 20, 7369)); //YA EXISTE 
			System.out.println(o.insertarEmple(conexion, 1235, "Nuevo1235", "PROGRAMADOR", 1300f, 0.0f, sqlDate, 80, 7369)); //Error departamento
			
			//VER DATOS 
			
			o.verempleados(conexion);
			
			//VER DATOS DE UN EMPLEADO 
			o.verunempleados(conexion, 124); //existe
			o.verunempleados(conexion, 1244); //no existe
			
			try {
				conexion.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
	}
}
