package operacionesEmple;

import java.sql.Connection;
import java.sql.SQLException;


public class Principal {

	public static void main(String[] args) {
//		Connection conexion=Conexiones.getOracle("ejemplo", "dam");

				Connection conexion=Conexiones.getMysql("ejemplo", "root","1234");
			//		Connection conexion=Conexiones.getSQLite(".\\basedatos\\SQLITE\\ejemplo.db");
					
			if(conexion!=null) {
				//probar comprobaremple
				int emp_no=122;
				if(OperacionesEmple.comprobarEmple(conexion,emp_no)) {
					System.out.println("Emleado "+emp_no+" existe");
				}else {
					System.out.println("Empleado "+emp_no+" no existe");
				}
			}else {
				System.out.println("ERROR EN LA CONEXION");
			}
			
			
			// probar borraremple 
			
			System.out.println(OperacionesEmple.borraremple(conexion, 123));
			System.out.println(OperacionesEmple.borraremple(conexion, 1234));
			System.out.println(OperacionesEmple.borraremple(conexion, 7566));
			try {
				conexion.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}
}
