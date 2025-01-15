package datos;

import java.io.File;

import org.neodatis.odb.ODB;
import org.neodatis.odb.ODBFactory;

public class CrearBD {

	public static void main(String[] args) {

		File fichero = new File("ARTICULOS.DAT");
		if (fichero.delete())
			   System.out.println("BD Borrada");
		// creación de registros en la BD
		  ODB odb = ODBFactory.open("ARTICULOS.DAT"); 
		  
		// creo los artículos
		  Articulos ar1 = new Articulos(1,"Mesas", 30, (float)100.5);
		  Articulos ar2 = new Articulos(2,"Pupitres", 10, (float)150.7);
		  Articulos ar3 = new Articulos(6,"Cuadernos", 100, (float)4.5);
		  Articulos ar4 = new Articulos(8,"Tabletas", 10, (float)175.4);
		  Articulos ar5 = new Articulos(9,"Bolígrafos", 100, (float)3.5);
		  Articulos ar6 = new Articulos(10,"Lapiceros", 300, (float)2.5);
		  Articulos ar7 = new Articulos(14,"Sillas", 30, (float)120.5);
		  Articulos ar8 = new Articulos(16,"Portátil", 25, (float)400.5);
		
		  Articulos ar11 = new Articulos(17,"Espejo baño", 20, (float)100.5);
		  Articulos ar21 = new Articulos(18,"Reloj cocina", 10, (float)20.7);
		  Articulos ar31 = new Articulos(20,"Tarjetero", 50, (float)14.5);
		  Articulos ar41 = new Articulos(22,"Estuches", 110, (float)20.4);
		  Articulos ar51 = new Articulos(23,"Libro BD", 10, (float)23.5);
		  Articulos ar61 = new Articulos(24,"Tijeras", 30, (float)5.0);
		  Articulos ar71 = new Articulos(25,"Cubiertos", 130, (float)10.5);
		  Articulos ar81 = new Articulos(26,"Teclado", 25, (float)40.5);
				
		 
		  // Almacenamos artículos 16 artículos
		  odb.store(ar1); 
		  odb.store(ar2);
		  odb.store(ar3);
		  odb.store(ar4);
		  odb.store(ar5); 
		  odb.store(ar6);
		  odb.store(ar7);
		  odb.store(ar8);
		  odb.store(ar11); 
		  odb.store(ar21);
		  odb.store(ar31);
		  odb.store(ar41);
		  odb.store(ar51); 
		  odb.store(ar61);
		  odb.store(ar71);
		  odb.store(ar81);
		  
		  // Creo los Clientes 12 clientes, num, nom, pob
		  
		  Clientes cli1 = new Clientes(1,"Antonio Ruiz","Talavera");
		  Clientes cli2 = new Clientes(2,"La Alameda S.L.","Talavera");
		  Clientes cli3 = new Clientes(7,"Los molinos CB","Madrid");
		  Clientes cli4 = new Clientes(8,"Pedro Morán S.L.","Talavera");
		  Clientes cli5 = new Clientes(12,"Azulejos Martín S.L.","Talavera");
		  Clientes cli6 = new Clientes(15,"Bar Girasol","Oropesa");
		  Clientes cli7 = new Clientes(9,"Escuela Mayores","Talavera");
		  Clientes cli8 = new Clientes(17,"Galería Madrid S.L.","Madrid");
		  Clientes cli9 = new Clientes(19,"El corte Chino","Talavera");
		  Clientes cli10 = new Clientes(20,"UNICAS S.A.","Oropesa");
		  Clientes cli11 = new Clientes(21,"Deportivo SAS","Talavera");
		  Clientes cli12 = new Clientes(22,"Academia Padel","Madrid");
		  
		  odb.store(cli1);		 
		  odb.store(cli2);odb.store(cli3);odb.store(cli4);odb.store(cli5); 
		  odb.store(cli6);odb.store(cli7);odb.store(cli8);odb.store(cli9); 
		  odb.store(cli10);odb.store(cli11);odb.store(cli12);
		  
		  // crear unas ventas
		  
	
			Ventas v1 = new Ventas (1,ar1,cli1,5,"05/06/2014");
			Ventas v2 = new Ventas (2,ar1,cli2,4,"15/06/2014");
			Ventas v3 = new Ventas (3,ar1,cli6,3,"25/06/2014");
			
			Ventas v4 = new Ventas (4,ar2,cli6,5,"03/07/2014");
			Ventas v5 = new Ventas (5,ar2,cli7,4,"11/08/2014");
			
			Ventas v6 = new Ventas (6,ar3,cli1,3,"25/04/2014");
			
			odb.store(v1);
			odb.store(v2);
			odb.store(v3);
			odb.store(v4);
			odb.store(v5);
			odb.store(v6);
			
			
		  
		  
		  
		  odb.close();
          System.out.println("BASE DE DATOS CREADA");

	}

}
