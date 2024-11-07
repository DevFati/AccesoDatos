package mortahilchachoufatima;
import java.sql.*;

public class Conexiones {


	
	public static Connection getOracle( String usuario, String password) {
		Connection conexion=null;
		try {
			Class.forName("oracle.jdbc.driver.OracleDriver");
			conexion = DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:xe", usuario,
					password);
		} catch (ClassNotFoundException  e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return conexion; 
		
	}
	

}
