package ejercicioJardineria;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Scanner;

public class Principal {
	private static Connection conexion = Conexiones.getOracle("jardineria", "dam");

	public static void main(String[] args) {

		Scanner teclado = new Scanner(System.in);
		int operacion;
		do {
			menu();
			operacion=teclado.nextInt();
			switch (operacion) {

			case 1:
				System.out.println(insertarEmple("Maria", "Castro", "Blanco", 1234, "mar@gardering.com", "TAL-ES", 1,
						"Representante Ventas"));
				
				break;
			case 2:
				verpedidoscliente(2); //no existe
				verpedidoscliente(6); // no tiene pedidos 
				verpedidoscliente(4); //si tiene pedidos 
				
				
				break;
			case 3:
				crearclientessinpedido();
				break;
			case 4:
				clientesporempleado();
				break;
			case 5:
				stockactualizado();
				break;
			case 6:
				ejeciciooficinas("BCN-ES"); // existe y tienen empleados
                ejeciciooficinas("oooooo"); // oficina no existe
                ejeciciooficinas("NUEVA"); // oficina existe sin empleados
				break;
			case 7:
				verpedidostodos();
				break;
			case 8:
				tratarnuevosempleados();
				
				break;

			default:
				System.out.println("Opción invalida!");

			}

		} while (operacion != 0);
	}

	

	private static void tratarnuevosempleados() {
		  String sql="SELECT  codigoempleado,  nombre, apellido1, apellido2,  "+
			      " extension, email, codigooficina, codigojefe, puesto "
		     		+ " FROM nuevosempleados";
		     String mensaje = "";
			 int error = 0;
		     try {
					PreparedStatement sent = conexion.prepareStatement(sql);
					ResultSet res = sent.executeQuery();

					while (res.next()) {
						// comprobar si existe empleado
					   int codigoempleado = res.getInt(1);
						 
						// Comprobar si existe empleado 
						if (comprobarEmple(conexion,codigoempleado)==false) {
					         
							insertarempleado(res);
							
						}
						if (comprobarEmple(conexion,codigoempleado)==true) {
					       // si existe el empleado ir  a actualizar
							actualizarempleado(res);
						}
							
					
					}
		     
				res.close();
				sent.close();
				
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
	}



	private static void actualizarempleado(ResultSet res) {
	
		String mensaje = "";
		int error = 0;
		try {
			String codigooficina = res.getString(7);
			int codigojefe = res.getInt(8);

			if (!comprobarOficina(conexion,codigooficina)) {
				// no existe la oficina
				error = 1;
				mensaje = mensaje + "\n  LA OFICINA NO EXISTE: " + codigooficina;
			}

			// Comprobar si existe empleado jefe
			if (!comprobarEmple(conexion,codigojefe)) {
				// no existe la oficina
				error = 1;
				mensaje = mensaje + "\n  EL JEFE NO EXISTE: " + codigojefe;
			}

			if (error == 0) {
				// se actualiza
				// decir qué se ha actualizado, extraemos los datos del empleado
				String sql1 = "select * from empleados where codigoempleado= ?";
				PreparedStatement sentemple = conexion.prepareStatement(sql1);
				sentemple.setInt(1, res.getInt(1));
				ResultSet resemple = sentemple.executeQuery();
				resemple.next(); // datos del empleado a actualizar
				
				// codigoempleado, nombre, apellido1, apellido2, "+
				// " extension, email, codigooficina, codigojefe, puesto
				String cambios="";
                if (!res.getString(2).equals(resemple.getString(2))) {
                	cambios=cambios+"Se actualiza el nombre.\n";
                }
                if (!res.getString(3).equals(resemple.getString(3))) {
                	cambios=cambios+"Se actualiza el apellido1.\n";
                }
                if (!(res.getString(4)== resemple.getString(4))) {
                	cambios=cambios+"Se actualiza el apellido2.\n";
                }
                if (!res.getString(5).equals(resemple.getString(5))) {
                	cambios=cambios+"Se actualiza la extensión.\n";
                }
                if (!res.getString(6).equals(resemple.getString(6))) {
                	cambios=cambios+"Se actualiza el email.\n";
                }
                if (!res.getString(7).equals(resemple.getString(7))) {
                	cambios=cambios+"Se actualiza el codigooficina.\n";
                }
                if (!(res.getInt(8) == resemple.getInt(8))) {
                	cambios=cambios+"Se actualiza el codigojefe.\n";
                }
                if (!res.getString(9).equals(resemple.getString(9))) {
                	cambios=cambios+"Se actualiza el puesto.\n";
                }
                sentemple.close();
                resemple.close();
                
                if (cambios=="") {
                	System.out.println("* No se actualiza el empleado: " + res.getInt(1) + ". Sin cambios.");
                }
                else {
                	// actualizar
                	String sql2 = "update empleados set nombre=?,  apellido1=?,"
                			+ "  apellido2=?, extension=?,  email=?,   "
                			+ "codigooficina = ?,  codigojefe = ?, puesto = ?"
                			+ " where codigoempleado = ?";
    						                	
    				PreparedStatement sentupdate = conexion.prepareStatement(sql2);
    				sentupdate = conexion.prepareStatement(sql2);
    				sentupdate.setInt(9, res.getInt(1));
    				sentupdate.setString(1, res.getString(2));
    				sentupdate.setString(2, res.getString(3));
    				sentupdate.setString(3, res.getString(4));
    				sentupdate.setString(4, res.getString(5));
    				sentupdate.setString(5, res.getString(6));
    				sentupdate.setString(6, res.getString(7));
    				sentupdate.setInt(7, res.getInt(8));
    				sentupdate.setString(8, res.getString(9));

    				int filas = sentupdate.executeUpdate();
                	
    				System.out.println("* Empleado actualizado: " +
    				res.getInt(1) + ". Cambios: " + cambios);
    				sentupdate.close();
                	
                } //cambios ==""
            	
			} // error = 0
			else {
				// No se actualiza
				System.out.println("* Error, no se actualiza el empleado: " + res.getInt(1) + ". Errores: " + mensaje);

			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
	}



	private static void insertarempleado(ResultSet res) {
		// codigoempleado, nombre, apellido1, apellido2, "+
		// " extension, email, codigooficina, codigojefe, puesto
		String mensaje = "";
		int error = 0;
		// comprobar oficina

		try {
			String codigooficina = res.getString(7);
			int codigojefe = res.getInt(8);

			if (!comprobarOficina(conexion,codigooficina)) {
				// no existe la oficina
				error = 1;
				mensaje = mensaje + "\n  LA OFICINA NO EXISTE: " + codigooficina;
			}

			// Comprobar si existe empleado jefe
			if (!comprobarEmple(conexion,codigojefe)) {
				// no existe la oficina
				error = 1;
				mensaje = mensaje + "\n  EL JEFE NO EXISTE: " + codigojefe;
			}

			if (error == 0) {
				// a insertar
				String sql1 = "insert into empleados (codigoempleado,  nombre,  apellido1,  apellido2,  extension,  email,   codigooficina,  codigojefe, puesto)"
						+ " values (?, ?,?, ?,?, ?,?, ?,? )";

				PreparedStatement sent = conexion.prepareStatement(sql1);
				sent = conexion.prepareStatement(sql1);
				sent.setInt(1, res.getInt(1));
				sent.setString(2, res.getString(2));
				sent.setString(3, res.getString(3));
				sent.setString(4, res.getString(4));
				sent.setString(5, res.getString(5));
				sent.setString(6, res.getString(6));
				sent.setString(7, res.getString(7));
				sent.setInt(8, res.getInt(8));
				sent.setString(9, res.getString(9));

				int filas = sent.executeUpdate();

				System.out.println("* Empleado nuevo insertado " + res.getInt(1));

				sent.close();

			}

			else {
				System.out.println("Error, no inserta el empleado: " + res.getInt(1) + ". Errores: " + mensaje);

			}

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		
		
	}






	private static void verpedidostodos() {
	// 	verpedidoscliente(4); 
		
		String sql = " Select codigocliente from clientes order by codigocliente";
		
		try {
			PreparedStatement sent = conexion.prepareStatement(sql);
			ResultSet res = sent.executeQuery();

			while (res.next()) {
				
				System.out.println("***");
				verpedidoscliente(res.getInt(1));
				
			}
			res.close();
			sent.close();
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}



	private static void ejeciciooficinas(String codofi) {
		//
		// COD OFICINA CIUDAD PAIS REGION DIRECCION1 NUM EMPLES
		// ----------- ---------------- ----------------- -----------------
		// ---------------- ----------

		String llamad = "{ ? = call veroficina(?,?,?,?,?) }";
		System.out.printf("%12s %-30s %-50s %-50s %-50s %10s %n",
				"COD OFICINA", "CIUDAD", "PAIS", "REGION",
				"DIRECCION1", "NUM EMPLES");
		System.out.printf("%12s %-30s %-50s %-50s %-50s %10s %n", "------------", "------------------------",
				"------------------------", "------------------------", "------------------------", "----------");

	//	CallableStatement llamada;
		try {
			CallableStatement llamada = conexion.prepareCall(llamad);

			// Dar valor a los argumentos
			llamada.registerOutParameter(1, Types.INTEGER); // contador
			llamada.setString(2, codofi); // oficina
			llamada.registerOutParameter(3, Types.VARCHAR);// ciudad
			llamada.registerOutParameter(4, Types.VARCHAR); //pais
			llamada.registerOutParameter(5, Types.VARCHAR); //Región
			llamada.registerOutParameter(6, Types.VARCHAR); //dirección 1
			
			// Ejecutar el procedimiento
			llamada.executeUpdate();
		    // mostrar datos
			System.out.printf("%12s %-30s %-50s %-50s %-50s %10s %n",
					codofi, llamada.getString(3), llamada.getString(4), 
					llamada.getString(5),llamada.getString(6),
					llamada.getInt(1));
			
			llamada.close();
			

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		
	}



	private static void stockactualizado() {
		String crearcolumna="ALTER TABLE productos ADD STOCKACTUALIZADO number(5)";
		String actualiza="update productos p set STOCKACTUALIZADO = "
				+ "CANTIDADENSTOCK - (select coalesce(sum(cantidad),0) from detallepedidos where codigoproducto = p.codigoproducto)";
		String consulta="select codigoproducto, cantidadenstock , STOCKACTUALIZADO "
				+ " from productos where STOCKACTUALIZADO< 5 ";
		
		try {
			PreparedStatement sent = conexion.prepareStatement(crearcolumna);
			sent.executeUpdate();
			System.out.println("---------------------------------");
			System.out.println("Columna creada."); 
			sent.close();
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			System.out.println("Atención ya existe la columna.");
			//System.out.println(e.getMessage());
			
		}
		
		try {
			PreparedStatement sent = conexion.prepareStatement(actualiza);
			int lin = 	sent.executeUpdate();
			System.out.println("Columnas actualizadas, reg: "+ lin); 
			sent.close();
			
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				System.out.println("Atención error en la actualización: ");
				System.out.println(e.getMessage());
				
			}
	
		try {
			// LISTADO
			PreparedStatement sent = conexion.prepareStatement(consulta);
			ResultSet res = sent.executeQuery();
			System.out.printf("%15s %15s %16s%n","COD PRODUCTO","CANTIDADENSTOCK" , "STOCKACTUALIZADO"); 
			System.out.printf("%15s %15s %16s%n","---------------","---------------" , "----------------"); 
			while (res.next()){
				System.out.printf("%15s %15s %16s%n",
						res.getString(1),
				        res.getInt(2), res.getInt(3)); 
				
			}
			
			sent.close();
			
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				System.out.println("Atención error en la actualización: ");
				System.out.println(e.getMessage());
				
			}
		
		
	}



	private static void clientesporempleado() {
		String crearcolumna="ALTER TABLE empleados ADD NUMCLIENTES number(5)";
		String actualiza="update empleados emple set NUMCLIENTES = "
				+ " ( select count(*) from clientes where codigoempleadorepventas = emple.codigoempleado)";

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



	private static void crearclientessinpedido() {
		// crear tabla sin pedidos y con los clientes
				// create table clientessinpedido as
				// select * from clientes where codigocliente 
				//  not in ( select codigocliente  from pedidos );
				 
				 //Añadir la PK a la tabla
				 // ALTER TABLE clientessinpedido ADD CONSTRAINT AAA_PK PRIMARY KEY ( CODIGOCLIENTE );

				//borrar de clientes
				//delete from clientes where codigocliente 
				//not in ( select codigocliente  from pedidos );
				String crear=" create table clientessinpedido as select * from clientes where codigocliente " +
				" not in ( select codigocliente  from pedidos ) order by codigocliente";
				
				String alter="ALTER TABLE clientessinpedido ADD CONSTRAINT CSP_PK PRIMARY KEY ( CODIGOCLIENTE )";
				
				String borrar=" delete from clientes where codigocliente not in ( select codigocliente  from pedidos )";
			
				try {
					PreparedStatement sent = conexion.prepareStatement(crear);
					sent.executeUpdate();
					System.out.println("---------------------------------");  
					System.out.println("Tabla clientessinpedido creada con los registros");
				    // añadimos la PK
					sent = conexion.prepareStatement(alter);
					sent.executeUpdate();
					System.out.println("Añadida la PK en clientessinpedido");
					
					// borrar esos clientes
					sent = conexion.prepareStatement(borrar);
					int l=sent.executeUpdate();
					System.out.println("Clientes sin pedido, borrados de clientes: "+l);
					
					sent.close();
					
					
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
					System.out.println("Tabla CLIENTESSINPEDIDO ya creada, y clientes sin pedidos borrados de CLIENTES.");
					//System.out.println(e.getMessage());
					
				}
	}



	private static void verpedidoscliente(int codigocliente) {
		String sql1="select nombrecliente,lineadireccion1 from clientes where codigocliente=?";

		String sql2="select count(*) from pedidos where codigocliente=?";
		
		try {
			PreparedStatement sentencia;
			sentencia = conexion.prepareStatement(sql1);
			sentencia.setInt(1, codigocliente);
			ResultSet resul = sentencia.executeQuery();
			if (resul.next()) {
				//cliente existe 
				PreparedStatement sentencia2;
				sentencia2 = conexion.prepareStatement(sql2);
				sentencia2.setInt(1, codigocliente);
				ResultSet resul2 = sentencia2.executeQuery();
				resul2.next();
				//Visualizamos 
				
			System.out.println("COD-CLIENTE: "+codigocliente+"            NOMBRE: "+resul.getString(1));  
				System.out.println("DIRECCIÓN1: "+resul.getString(2)+"    Número de pedidos: "+resul2.getInt(1));
			
				System.out.println("------------------------------------------------------------------------------------------------------ ");
				if(resul2.getInt(1)>0) {
					listarPedidosCliente(codigocliente);
				}
			} else {
				//cliente no existe 
				System.out.println("---------------------------------");
				System.out.println("Codigo de cliente no existe: "+codigocliente);
				System.out.println("---------------------------------");

			}
			resul.close();
			sentencia.close();

		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		
		
		
	}



	private static void listarPedidosCliente(int codigocliente) {
		String sql1="select codigopedido, fechapedido, estado from pedidos where codigocliente=? order by codigocliente";
		try {
			PreparedStatement sentencia;
			sentencia = conexion.prepareStatement(sql1);
			sentencia.setInt(1, codigocliente);
			ResultSet resul = sentencia.executeQuery();
			
			float importemax=0;
			int pedidomax=0;
			Date fechamax=null;
			String nombreMaximo="", codmax="";
			int cantidadMaxima=0;
			
			
			
			while(resul.next()) {
				
				System.out.println("    COD-PEDIDO: "+resul.getInt(1)+"    FECHA PEDIDO: "+resul.getDate(2)+"    ESTADO DEL PEDIDO: "+resul.getString(3));
				System.out.printf("      %9s %9s %-40s %10s %10s %10s %n",
						"NUM-LINEA",  "COD-PROD","NOMBRE PRODUCTO","CANTIDAD","PREC-UNID","IMPORTE");
				
				System.out.printf("      %9s %9s %-40s %10s %10s %10s %n",
						"---------",  "---------","---------------------------------------------","---------","---------","---------","---------");
				
				//detalle de pedido
				
				String sql2="select numerolinea, codigoproducto, nombre, cantidad, preciounidad, preciounidad*cantidad from detallepedidos join productos "
						+ "using (codigoproducto) where codigopedido=? order by numerolinea ";
				
				int sumacantidad=0; 
				float sumaprecio=0, sumaimporte=0;
				
				PreparedStatement sentencia2;
				sentencia2 = conexion.prepareStatement(sql2);
				sentencia2.setInt(1, resul.getInt(1));
				ResultSet resul2 = sentencia2.executeQuery();
				while(resul2.next()) {
					System.out.printf("      %9s %9s %-40s %10s %10s %10s %n",
							resul2.getInt(1),  resul2.getString(2),resul2.getString(3),resul2.getInt(4),resul2.getFloat(5),resul2.getFloat(6));
					sumacantidad=sumacantidad+resul2.getInt(4);
					sumaprecio=sumaprecio+resul2.getFloat(5);
					sumaimporte=sumaimporte+resul2.getFloat(6);
					
					if(resul2.getInt(4)>cantidadMaxima) {
						cantidadMaxima=resul2.getInt(4);
						nombreMaximo=resul2.getString(3);
						codmax=resul2.getString(2);
					}
					
				}
					
					//totales por pedido 
				System.out.printf("      %61s %10s %10s %10s %n",
					    "TOTALES POR PEDIDO",sumacantidad,sumaprecio,sumaimporte);
			System.out.println();
					if(sumaimporte > importemax) {
						pedidomax=resul.getInt(1);
						fechamax=resul.getDate(2);
						importemax=sumaimporte;
					} 
					
				
				
				resul2.close();
				sentencia2.close();
				
			} //fin pedidos
			
			System.out.printf("      %9s  %9s %-40s %10s %10s %10s %n",
					"---------",  "---------","----------------------------------------","----------",
					"----------","----------");
			
			System.out.println("COD de PEDIDO y FECHA PEDIDO CON TOTAL IMPORTE MÁXIMO: " + pedidomax + ", " + fechamax);
			System.out.println("COD PRODUCTO y NOMBRE PRODUCTO, del producto más comprado: "+ codmax + ", " + nombreMaximo); 
			System.out.println("-------------------------------------------------------------------");  

			
			
			System.out.println();
		
			
				
			resul.close();
			sentencia.close();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}



	public static boolean comprobarEmple(Connection conexion, int id) {
		boolean existe = false;

		String sql = "select * from empleados where codigoempleado=?";
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

	public static boolean comprobarOficina(Connection conexion, String id) {
		boolean existe = false;

		String sql = "select * from oficinas where codigooficina=?";
		try {
			PreparedStatement sentencia;
			sentencia = conexion.prepareStatement(sql);
			sentencia.setString(1, id);
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

	public static String insertarEmple(String nombre, String ape1, String ape2, int extension, String mail,
			String codOfi, int codJefe, String puesto) {
		String mensaje = "";
		boolean insertar=true;
		
		if (!comprobarEmple(conexion, codJefe)) {
			//empleado jefe no existe 
			insertar=false;
			mensaje =mensaje+ "EMPLEADO JEFE NO EXISTE: " + codJefe + ", NO SE INSERTA\n";
		
		}

			if (!comprobarOficina(conexion, codOfi)) {
				// no existe la oficina, no  se puede insertar
				insertar=false;
				mensaje =mensaje+ "OFICINA NO EXISTE: " + codOfi + ", NO SE INSERTA\n";
			} 

			if(insertar) {
				
			
			
				String sql = "insert into empleados(codigoempleado, nombre, apellido1, apellido2,extension,email,codigooficina,codigojefe,puesto)values(?,?,?,?,?,?,?,?,?) ";
				try {
					int codemple = 0;
					String sql1 = "select max(codigoempleado)+1 from empleados ";
					PreparedStatement sentencia1 = conexion.prepareStatement(sql1);
					ResultSet resul = sentencia1.executeQuery();
					resul.next();
					codemple = resul.getInt(1);

					PreparedStatement sentencia = conexion.prepareStatement(sql);
					sentencia.setInt(1, codemple);
					sentencia.setString(2, nombre);
					sentencia.setString(3, ape1);
					sentencia.setString(4, ape2);
					sentencia.setInt(5, extension);
					sentencia.setString(6, mail);
					sentencia.setString(7, codOfi);
					sentencia.setInt(8, codJefe);
					sentencia.setString(9, puesto);

					int linea = sentencia.executeUpdate();
					mensaje = "EMPLEADO INSERTADO: " + codemple;
					sentencia.close();

				} catch (SQLException e) {
					// TODO Auto-generated catch block
					// e.printStackTrace();
					mensaje = e.getMessage();
				}

			}

		return mensaje;

	}

	public static void menu() {
		System.out.println("1. Insertar Empleado");
		System.out.println("2. Visualizar pedidos de un cliente");
		System.out.println("3. Crear clientes sin pedido ");
		System.out.println("4. Actualizar Clientes por empleado. ");
		System.out.println("5. Crear STOCKACTUALIZADO");
		System.out.println("6. Oficinas con función almacenada");
		System.out.println("7. Ver los pedidos de todos los clientes. ");
		System.out.println("8. Tratar nuevos empleados.");
		System.out.println("0. SALIR");
		System.out.println("TECLEA OPERACIÓN: ");
	}

}
