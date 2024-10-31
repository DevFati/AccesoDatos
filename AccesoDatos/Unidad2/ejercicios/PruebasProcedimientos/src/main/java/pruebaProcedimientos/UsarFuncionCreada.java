package pruebaProcedimientos;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.concurrent.Callable;

public class UsarFuncionCreada {
public static void main(String[] args) {
	Connection conexion=Conexiones.getOracle("ejemplo", "dam");
	
	String sql="Select dept_no, dnombre, loc from departamentos order by dept_no";
	//devuelve media FACTIVIDAD12 (d NUMBER, num out number) 
			String fun = "{? = call FACTIVIDAD12 ( ?, ? ) }";
		
			
			try {
				Statement sentencia = conexion.createStatement();
			    ResultSet resul = sentencia.executeQuery(sql);
			    // cabeceras
			    System.out.printf("%7s %15s %15s %12s %20s%n",
			    "DEPT-NO","NOMBRE","LOCALIDAD","MEDIASALARIO","CONTADOREMPLES");
			    System.out.printf("%7s %15s %15s %12s %20s%n",
					    "-------","--------------","--------------",
					    "------------","---------------------");
				CallableStatement llamada = null;
				float totalmedia=0;
				int totalcontador=0;
			    while (resul.next()) {
			    	// 
			    	llamada = conexion.prepareCall(fun);

			        llamada.registerOutParameter(1, Types.FLOAT);
					llamada.setInt(2, resul.getInt(1));
					llamada.registerOutParameter(3, Types.INTEGER);
			    	
					llamada.executeUpdate();
					
					 System.out.printf("%7s %15s %15s %12.2f %20s%n",
							 resul.getInt(1), resul.getString(2),
							 resul.getString(3), llamada.getFloat(1),
							 llamada.getInt(3));
					 totalmedia=totalmedia + llamada.getFloat(1);
					 totalcontador = totalcontador +  llamada.getInt(3);		   
			    }
			    llamada.close();
			    System.out.printf("%7s %15s %15s %12s %20s%n",
					    "-------","--------------","--------------",
					    "------------","---------------------");
			
			    System.out.printf("%39s %12.2f %20s%n",
					    "TOTALES:", totalmedia,totalcontador);
			    
			    resul.close();
			    sentencia.close();
			    
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
		}

}
