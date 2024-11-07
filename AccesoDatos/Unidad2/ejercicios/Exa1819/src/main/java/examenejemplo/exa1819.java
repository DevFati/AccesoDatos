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
	
	//verempresa(1);
	//eliminarempresassindep();
	//crearColumnaOficios();
	//insertarEmple("Pedro","direccion","poblacion",13,11); //OK
	//insertarEmple("Pedro","direccion","poblacion",130,111); //fallan los dos
	//insertarEmple("Pedro","direccion","poblacion",13,111); //fallan departamento
	//insertarEmple("Pedro","direccion","poblacion",130,11); //fallan oficio
	//insertarEmple("Pedro","direccion","poblacion",13,43);  
	
	//mostrarOficios();
	
	//actualizarDepartamentos(1,"pedro","casa","pp",100,100); //no existe nada
//	actualizarDepartamentos(1,"pedro","casa","pp",100,1); //no existe departamento y jefe
//	actualizarDepartamentos(11,"pedro","casa","pp",100,1); //no existe jefe

	actualizarDepartamentos(11,"gg","gg","pp",104,3); //TODO BIEN

	
	
}
	private static void actualizarDepartamentos(int codigodep, String nom, String dir, String loc, int codjefe, int codempresa) {
	
		String mensaje = "";
		int error = 0;
		try {
			
			if (!comprobarDepart(codigodep)) {
				// no existe el departamento
				error = 1;
				mensaje = mensaje + "\n  DEPARTAMENTO NO EXISTE: " + codigodep;
			}

			// Comprobar si existe empleado jefe
			if (!comprobarEmple(codjefe)) {
				// no existe el empleado
				error = 1;
				mensaje = mensaje + "\n  EL JEFE NO EXISTE: " + codjefe;
			}
			
			// Comprobar si existe empresa 
						if (!comprobarEmpresa(codempresa)) {
							// no existe la empresa
							error = 1;
							mensaje = mensaje + "\n  EL CODEMPRESA NO EXISTE: " + codempresa;
						}
			
			

			if (error == 0) {
				// se actualiza
				// decir qué se ha actualizado, extraemos los datos del departamento para ver si se actualiza algo o no
				String sql1 = "select coddepart, nombre, direccion, localidad, codjefedepartamento, codempre from departamentos where coddepart= ?";
				PreparedStatement sentemple = conexion.prepareStatement(sql1);
				sentemple.setInt(1, codigodep);
				ResultSet res = sentemple.executeQuery();
				res.next(); 
				
				String cambios="";
                if (!nom.equals(res.getString(2))) {
                	cambios=cambios+"Se actualiza el nombre.\n";
                }
                if (!dir.equals(res.getString(3))) {
                	cambios=cambios+"Se actualiza el direccion.\n";
                }
                if (!loc.equals(res.getString(4))) {
                	cambios=cambios+"Se actualiza el localidad.\n";
                }
                if (codjefe!=res.getInt(5)) {
                	cambios=cambios+"Se actualiza el codigo jefe de departamento.\n";
                }
                if (codempresa!=res.getInt(6)) {
                	cambios=cambios+"Se actualiza el codigo de empresa.\n";
                }
              
                sentemple.close();
                res.close();
                
                if (cambios=="") {
                	System.out.println("* No se actualiza el departamento: " + codigodep + ". Sin cambios.");
                }
                else {
                	// actualizar
                	String sql2 = "update departamentos set nombre=?,  direccion=?,"
                			+ "  localidad=?, codjefedepartamento=?,  codempre=? where coddepart = ?";
    						                	
    				PreparedStatement sentupdate = conexion.prepareStatement(sql2);
    				sentupdate = conexion.prepareStatement(sql2);
    				sentupdate.setInt(6, codigodep);
    				sentupdate.setString(1, nom);
    				sentupdate.setString(2, dir);
    				sentupdate.setString(3, loc);
    				sentupdate.setInt(4, codjefe);
    				sentupdate.setInt(5, codempresa);

    				int filas = sentupdate.executeUpdate();
                	
    				System.out.println("* Departamento actualizado: " +
    				codigodep + ". Cambios: \n" + cambios);
    				sentupdate.close();
                	
                } //cambios ==""
            	
			} // error = 0
			else {
				// No se actualiza
				System.out.println("* Error, no se actualiza el departamento: " + codigodep + ". Errores: " + mensaje);

			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		
		
	
}
	private static boolean comprobarEmpresa(int codempresa) {
		boolean existe = false;

		String sql = "select * from empresas where codempre=?";
		try {
			PreparedStatement sentencia;
			sentencia = conexion.prepareStatement(sql);
			sentencia.setInt(1, codempresa);
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
	
	
	
	private static boolean comprobarEmple(int codjefe) {

		 boolean existe = false;

			String sql = " select * from empleados where codemple=?";
			try {
				PreparedStatement sentencia;
				sentencia = conexion.prepareStatement(sql);
				sentencia.setInt(1, codjefe);
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
	private static void mostrarOficios() {
	System.out.println("Datos de oficios ");
	System.out.printf("      %-15s  %-30s %-20s %-20s %-20s %-15s  %n",
			"CÓDIGO",  "NOMBRE","SALARIO MES","NÚMERO EMPLEADOS","PRECIO TRIENIO","TOTAL SALARIO");
	System.out.printf("      %-15s  %-30s %-20s %-20s %-20s %-15s  %n",
		"---------------",  "------------------------------","--------------------","--------------------","--------------------","---------------");
	
	
	try {
		String sql1="select codoficio, nombre, salariomes, preciotrienio from oficios";
		 PreparedStatement sentencia;
			sentencia = conexion.prepareStatement(sql1);
			ResultSet resul = sentencia.executeQuery();
			while(resul.next()) {
				String sql2="select count(*) from empleados where codoficio=?";
				 PreparedStatement sentencia2;
					sentencia2 = conexion.prepareStatement(sql2);
					sentencia2.setInt(1, resul.getInt(1));
					ResultSet resul2 = sentencia2.executeQuery();
					resul2.next();
						System.out.printf("      %-15s  %-30s %-20s %-20s %-20s %-15s  %n",
								resul.getInt(1),  resul.getString(2),resul.getFloat(3),resul2.getInt(1),resul.getFloat(4),resul.getFloat(3)*resul2.getInt(1));
							
						resul2.close();
						sentencia2.close();	
			}
			
		resul.close();
		sentencia.close();
	} catch (SQLException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	 
	
	
	
	
}
	private static void insertarEmple(String nom, String direc, String pob, int codofic, int coddep) {
		int codemple=0;
		int codencargado=0;
		//nombre, direccion, poblacion, codoficio, codigodepart
		//codemple, nombre, direccion, pob, fechaalta, codencargado, coddepart,codoficio
	
		
				String mensaje = "";
				int error = 0;
				try {
					
					if (!comprobarDepart(coddep)) {
						// no existe departamento
						error = 1;
						mensaje = mensaje + "\n EL DEPARTAMENTO NO EXISTE: " + coddep;
					}else {
						//codencargado 
						 String sql2="select codjefedepartamento from departamentos where coddepart=?";
						 PreparedStatement sentencia2;
							sentencia2 = conexion.prepareStatement(sql2);
							sentencia2.setInt(1, coddep);
							ResultSet resul2 = sentencia2.executeQuery();
							if (resul2.next()) {
								if(resul2.getInt(1)!=0) {
									codencargado=resul2.getInt(1);
								}else {
								error = 1;
								mensaje = mensaje + "\n EL DEPARTAMENTO TIENE CODJEFEDEPARTAMENTO NULO: " + coddep+". (Porque es fk)";
							}
							}
							
							resul2.close();
							sentencia2.close();
					}

					// Comprobar si existe oficio
					if (!comprobarOficio(codofic)) {
						// no existe la oficina
						error = 1;
						mensaje = mensaje + "\n    NO EXISTE CODIGO DE OFICIO: " + codofic;
					}

					if (error == 0) {
						//codemple
						String sql1=" select max(codemple+1) from empleados";
						PreparedStatement sentencia;
						sentencia = conexion.prepareStatement(sql1);
						ResultSet resul = sentencia.executeQuery();
						resul.next();
						codemple=resul.getInt(1);
						
						
						
						//fecha
						java.util.Date utilDate=new java.util.Date();
						java.sql.Date sqlDate=new java.sql.Date(utilDate.getTime());
						
						
						
						
						// a insertar
						String sql3 = "insert into empleados (codemple,  nombre,  direccion,  poblacion,  fechaalta,  codencargado, coddepart,   codoficio)"
								+ " values (?, ?,?, ?,?, ?,?, ? )";

						PreparedStatement sent = conexion.prepareStatement(sql3);
						sent = conexion.prepareStatement(sql3);
						sent.setInt(1, codemple);
						sent.setString(2, nom);
						sent.setString(3, direc);
						sent.setString(4, pob);
						sent.setDate(5, sqlDate);
						sent.setInt(6, codencargado);
						sent.setInt(7, coddep);
						sent.setInt(8, codofic);
					

						int filas = sent.executeUpdate();

						System.out.println("* Empleado nuevo insertado " + codemple);

						sent.close();
						
						
						resul.close();
						sentencia.close();
						
					}

					else {
						System.out.println("Error, no inserta el empleado. Errores: " + mensaje);

					}
					
					
					
				

				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

	
}
	private static boolean comprobarOficio(int codofic) {
		 
		 boolean existe = false;

			String sql = " select * from oficios where codoficio=?";
			try {
				PreparedStatement sentencia;
				sentencia = conexion.prepareStatement(sql);
				sentencia.setInt(1, codofic);
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
	private static boolean comprobarDepart(int coddep) {
		boolean existe = false;

		String sql = " select * from departamentos where coddepart=?";
		try {
			PreparedStatement sentencia;
			sentencia = conexion.prepareStatement(sql);
			sentencia.setInt(1, coddep);
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
	
	
	
	
	private static void crearColumnaOficios() {
		String crearcolumna="ALTER TABLE oficios ADD numemple number(5)";
		String actualiza="update oficios ofi set numemple = \r\n"
				+ " ( select count(*) from empleados where codoficio = ofi.codoficio)";

		try {
			PreparedStatement sent = conexion.prepareStatement(crearcolumna);
			sent.executeUpdate();
			System.out.println("---------------------------------");
			System.out.println("Columna creada."); 
			
			sent.close();
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			System.out.println("Atención ya existe la columna: ");
			System.out.println(e.getMessage());
			
		}
			
		try {
			PreparedStatement sent = conexion.prepareStatement(actualiza);
			int lin = 	sent.executeUpdate();
			System.out.println("Columna actualizada, reg: "+ lin); 
			
			sent.close();
			
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				System.out.println("Atención error en la actualización: ");
				System.out.println(e.getMessage());
				
			}
	
	
}
	private static void eliminarempresassindep() {
		//crear la tabla empresas sin depar
		
		
		String crear="  create table empresassindepar as select * from empresas where codempre not in "
				+ "( select codempre  from departamentos ) order by codempre";
		//añadir la constraint
		String alter="ALTER TABLE empresassindepar ADD CONSTRAINT CSP_PK PRIMARY KEY ( codempre )";
		//eliminar las empresas sin depar de la tabla empresas 
		String borrar="delete from empresas where codempre not in ( select codempre from departamentos )";
	
		try {
			PreparedStatement sent = conexion.prepareStatement(crear);
			sent.executeUpdate();
			System.out.println("---------------------------------");  
			System.out.println("Tabla empresassindepar creada con los registros");
		    // añadimos la PK
			sent = conexion.prepareStatement(alter);
			sent.executeUpdate();
			System.out.println("Añadida la PK en empresassindepar");
			
			// borrar esas empresas
			sent = conexion.prepareStatement(borrar);
			int l=sent.executeUpdate();
			System.out.println("Empresas sin departamentos, borrados de empresas: "+l);
			
			sent.close();
			
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			System.out.println("Tabla EMPRESASSINDEPAR ya creada, y empresas sin departamentos borrados de EMPRESAS.");
			//System.out.println(e.getMessage());
			
		}
	
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
			System.out.printf("      %12s  %-35s %-30s %-35s %n",
					"COD-EMPLEADO",  "NOMBRE","OFICIO","NOMBRE ENCARGADO");
			
			System.out.printf("      %12s  %-35s %-30s %-35s %n",
					"------------",  "-----------------------------------","------------------------------","-----------------------------------");
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
							System.out.printf("      %12s  %-35s %-30s %-35s %n",
									+resul.getInt(1), resul.getString(2),resul3.getString(1),resul2.getString(1));
							
								
							}else{
								
								System.out.printf("      %12s  %-35s %-30s %-35s %n",
										+resul.getInt(1), resul.getString(2),resul3.getString(1), "NOMBRE ENCARGADO: NO TIENE");
								
								
							
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
