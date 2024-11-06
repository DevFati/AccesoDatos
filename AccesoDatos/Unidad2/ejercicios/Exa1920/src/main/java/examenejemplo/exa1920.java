package examenejemplo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;



public class exa1920 {
	private static Connection conexion = Conexiones.getOracle("EXA19", "dam");

public static void main(String[] args) {

	creaeestadisticaciudades();
	System.out.println();
	System.out.println();
	creaeestadisticaciudades2();
}

private static void creaeestadisticaciudades() {
	
	String sql1="create table estadisticaciudades"
			+ "  ( ciudad varchar(20) primary key,"
			+ "	    nombrepais varchar(25),"
			+ "	    numviajesdestino number(5),"
			+ "	    numviajesprocedencia number(5)"
			+ "  )";
	
	String sql2="insert into estadisticaciudades "
			+ "select ciudad, nombre, 0 , 0 from ciudades join paises using(codpais) order by ciudad";
	
	String sql3=" update estadisticaciudades es\r\n"
			+ " set NUMVIAJESDESTINO=(select count(*) from viajes where ciudaddestino=es.ciudad),\r\n"
			+ " numviajesprocedencia=(   select count(*) from viajes where ciudadorigen=es.ciudad)";
	
	try {
		// crear tabla
		PreparedStatement sent = conexion.prepareStatement(sql1);
		sent.executeUpdate();
		System.out.println("-----------------");
		System.out.println("Tabla creada");
		
		// Añadir ciudades
		sent = conexion.prepareStatement(sql2);
		int lin=sent.executeUpdate();
		System.out.println("-----------------");
		System.out.println("Añadidas "+lin+" ciudades");
		
		//actualizar contadores
		sent = conexion.prepareStatement(sql3);
		lin=sent.executeUpdate();
		System.out.println("-----------------");
		System.out.println("Actualizadas "+lin+" ciudades");	
		
		sent.close();
				
				
	} catch (SQLException e) {
		// TODO Auto-generated catch block
		// e.printStackTrace();
		System.out.println("Atención ya existe la Tabla.");
		System.out.println(e.getMessage());

	}
	

}


	
	
	
	
private static void creaeestadisticaciudades2() {
	String sql1="create table estadisticaciudades55"
			+ "  ( ciudad varchar(20) primary key,"
			+ "	    nombrepais varchar(25),"
			+ "	    numviajesdestino number(5),"
			+ "	    numviajesprocedencia number(5)"
			+ "  )";
	
	String sql2="select ciudad, nombre from ciudades join paises using(codpais) order by ciudad";
	
	try {
		// crear tabla
		PreparedStatement sent = conexion.prepareStatement(sql1);
		sent.executeUpdate();
		System.out.println("-----------------");
		System.out.println("Tabla creada");
	
		// recorrer las ciudades e insertar
		
		sent = conexion.prepareStatement(sql2);
		ResultSet res = sent.executeQuery();
		String destino="select count(*) from viajes where ciudaddestino= ?";
		String proc="select count(*) from viajes where ciudadorigen= ?";
		
		String insert="insert into estadisticaciudades55 values(?, ?, ? ,?)";
		
		while(res.next()) {
			PreparedStatement sent2 = conexion.prepareStatement(destino);
			sent2.setString(1,res.getString(1));
			ResultSet resdestin = sent2.executeQuery();
			resdestin.next();
			int contdestino = resdestin.getInt(1);
			
			sent2 = conexion.prepareStatement(proc);
			sent2.setString(1,res.getString(1));
			ResultSet respro= sent2.executeQuery();
			respro.next();
			int contpro = respro.getInt(1);
			
			//contador origen
			sent2 = conexion.prepareStatement(insert);
			sent2.setString(1,res.getString(1));
			sent2.setString(2,res.getString(2));
			sent2.setInt(3,contdestino);
			sent2.setInt(4,contpro);
			
			sent2.executeUpdate();
			System.out.println("-----------------");
			System.out.println("Insertada ciudad: "+res.getString(1));
			
			
			
			
		}
		
		
	
	
	} catch (SQLException e) {
		// TODO Auto-generated catch block
		// e.printStackTrace();
		System.out.println("Atención ya existe la Tabla.");
		System.out.println(e.getMessage());

	}
	
	
	
	
}


}
