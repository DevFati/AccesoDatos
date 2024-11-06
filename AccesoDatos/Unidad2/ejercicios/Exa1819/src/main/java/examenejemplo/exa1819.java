package examenejemplo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class exa1819 {
	private static Connection conexion = Conexiones.getOracle("EXA1819", "dam");

public static void main(String[] args) {
	
	/*    1. Realiza un método java que reciba un código de empresa, y visualice todos los departamentos de la empresa con sus empleados. 
Si la empresa no existe, visualizar que el código de empresa no existe en la BD.
Y si la empresa existe se debe de visualizar este informe:

COD-EMPRESA: xxxxxxx   NOMBRE: xxxxxxxxxxxxxx  
DIRECCIÓN: xxxxxxxxxxxxxxx    Número de departamentos:xxxxxxx
-----------------------------------------------------------------------------------  
COD-DEPARTAMENTO: xxxxxxx   NOMBRE: xxxxxxxxxxxxxx  LOCALIDAD: xxxxxxxxxxxxxxx   
COD-EMLEADO    NOMBRE            OFICIO        NOMBRE ENCARGADO    
-----------    --------------    ----------    ----------------
Xxxxxxxxxxx    xxxxxxxxxxxxxx    xxxxxxxxxx    xxxxxxxxxxxxxxxx
Xxxxxxxxxxx    xxxxxxxxxxxxxx    xxxxxxxxxx    xxxxxxxxxxxxxxxx
-----------    --------------    ----------    ----------------
Número de empleados por departamento:  xxxxxxxxxxx  
Nombre del jefe del departamento: xxxxxxxxxxxxx

 COD-DEPARTAMENTO: xxxxxxx   NOMBRE: xxxxxxxxxxxxxx  LOCALIDAD: xxxxxxxxxxxxxxx   
COD-EMLEADO    NOMBRE            OFICIO        NOMBRE ENCARGADO    
-----------    --------------    ----------    ----------------
Xxxxxxxxxxx    xxxxxxxxxxxxxx    xxxxxxxxxx    xxxxxxxxxxxxxxxx
Xxxxxxxxxxx    xxxxxxxxxxxxxx    xxxxxxxxxx    xxxxxxxxxxxxxxxx
-----------    --------------    ----------    ----------------
Número de empleados por departamento:  xxxxxxxxxxx  
Nombre del jefe del departamento: xxxxxxxxxxxxx
 . . . . . . . . . . . . .
 . . . . . . . . . . . . .*/
	
	verempresa(1);
}
	private static void verempresa(int codempresa) {
		String sql1="select nombre, direccion from empresas where codempre=?";

		String sql2="select count(*) from departamentos where codempre=?";
		
		try {
			PreparedStatement sentencia;
			sentencia = conexion.prepareStatement(sql1);
			sentencia.setInt(1, codempresa);
			ResultSet resul = sentencia.executeQuery();
			if (resul.next()) {
				//empresa existe
				PreparedStatement sentencia2;
				sentencia2 = conexion.prepareStatement(sql2);
				sentencia2.setInt(1, codempresa);
				ResultSet resul2 = sentencia2.executeQuery();
				resul2.next();
				//Visualizamos 
				
			System.out.println("COD-EMPRESA: "+codempresa+"            NOMBRE: "+resul.getString(1));  
				System.out.println("DIRECCIÓN: "+resul.getString(2)+"    Número de departamentos: "+resul2.getInt(1));
			
				System.out.println("------------------------------------------------------------------------------------------------------ ");
				if(resul2.getInt(1)>0) {
					listardepartamentos(codempresa);
				
				}
				
				resul2.close();
				sentencia2.close();
			} else {
				//empresa no existe
				System.out.println("---------------------------------");
				System.out.println("Codigo de empresa no existe: "+codempresa);
				System.out.println("---------------------------------");

			}
			
			resul.close();
			sentencia.close();

		} catch (SQLException e) {
			e.printStackTrace();
		}
		
	
}
	
	
	private static void listardepartamentos(int codempresa) {
		// TODO Auto-generated method stub
		/*COD-DEPARTAMENTO: xxxxxxx   NOMBRE: xxxxxxxxxxxxxx  LOCALIDAD: xxxxxxxxxxxxxxx   
COD-EMLEADO    NOMBRE            OFICIO        NOMBRE ENCARGADO    
-----------    --------------    ----------    ----------------
Xxxxxxxxxxx    xxxxxxxxxxxxxx    xxxxxxxxxx    xxxxxxxxxxxxxxxx
Xxxxxxxxxxx    xxxxxxxxxxxxxx    xxxxxxxxxx    xxxxxxxxxxxxxxxx*/
		
		String sql1="select coddepart, nombre, localidad,codjefedepartamento from departamentos where codempre=?";
		String sql2="select count(*) from empleados where coddepart=?";
		String sql3="select nombre from empleados where codemple=?";
		
		try {
			PreparedStatement sentencia;
			sentencia = conexion.prepareStatement(sql1);
			sentencia.setInt(1, codempresa);
			ResultSet resul = sentencia.executeQuery();
			while (resul.next()) {
				
				//hay departamentos 
				
				//Visualizamos 
				
			System.out.println("COD-DEPARTAMENTO: "+resul.getInt(1)+"  NOMBRE: "+resul.getString(2) +   "      LOCALIDAD: "+resul.getString(3));
			
				System.out.println("------------------------------------------------------------------------------------------------------ ");
			
					listarempleados(resul.getInt(1));
				
					//sacar num empleados 
					PreparedStatement sentencia2;
					sentencia2 = conexion.prepareStatement(sql2);
					sentencia2.setInt(1, resul.getInt(1));
					ResultSet resul2 = sentencia2.executeQuery();
					resul2.next();
					// sacar nombre empleado jefe 
					PreparedStatement sentencia3;
					sentencia3 = conexion.prepareStatement(sql3);
					sentencia3.setInt(1, resul.getInt(4));
					ResultSet resul3 = sentencia3.executeQuery();	
					resul3.next();
					System.out.println("Número de empleados por departamento: "+resul2.getInt(1));
					System.out.println("Nombre del jefe del departamento: "+resul3.getString(1));
					System.out.println();
					System.out.println();
					resul3.close();
					sentencia3.close();
					
					resul2.close();
					sentencia2.close();
			} 
			
			
			/*Número de empleados por departamento:  xxxxxxxxxxx  
Nombre del jefe del departamento: xxxxxxxxxxxxx*/
			
			
			resul.close();
			sentencia.close();

		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		
		
		
	}
	private static void listarempleados(int coddept) {
		/*
		COD-EMLEADO    NOMBRE            OFICIO        NOMBRE ENCARGADO    
		-----------    --------------    ----------    ----------------
		Xxxxxxxxxxx    xxxxxxxxxxxxxx    xxxxxxxxxx    xxxxxxxxxxxxxxxx
		Xxxxxxxxxxx    xxxxxxxxxxxxxx    xxxxxxxxxx    xxxxxxxxxxxxxxxx*/
				
				String sql1="select codemple, nombre, codoficio, codencargado from empleados where coddepart=?";
				String sql2="select nombre from empleados where codemple=?";
				String sql3="select nombre from oficios where codoficio=?";
				try {
					PreparedStatement sentencia;
					sentencia = conexion.prepareStatement(sql1);
					sentencia.setInt(1, coddept);
					ResultSet resul = sentencia.executeQuery();
					while (resul.next()) {
						//hay empleados 
						//Sacar nombre encargado 
						PreparedStatement sentencia2;
						sentencia2 = conexion.prepareStatement(sql2);
						sentencia2.setString(1, resul.getString(4));
						ResultSet resul2 = sentencia2.executeQuery();
						//sacar nombre de oficio 
						PreparedStatement sentencia3;
						sentencia3 = conexion.prepareStatement(sql3);
						sentencia3.setInt(1, resul.getInt(3));
						ResultSet resul3 = sentencia3.executeQuery();
						resul3.next();
						if(resul2.next()) {
							//Visualizamos 
							
							System.out.println("COD-EMPLEADO: "+resul.getInt(1)+"  NOMBRE: "+resul.getString(2) +   "      OFICIO: "+resul3.getString(1)+   "      NOMBRE ENCARGADO: "+resul2.getString(1));
							
								
							}else{
								System.out.println("COD-EMPLEADO: "+resul.getInt(1)+"  NOMBRE: "+resul.getString(2) +   "      OFICIO: "+resul3.getString(1)+   "      NOMBRE ENCARGADO: NO TIENE");
								
							
							}
						resul3.close();
						sentencia3.close();
						
						resul2.close();
						sentencia2.close();
						}
					
						 
					resul.close();
					sentencia.close();

				} catch (SQLException e) {
					e.printStackTrace();
				}
		
	}
}
