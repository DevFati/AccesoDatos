package ejercicioJardineria;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
				break;
			case 3:
				break;
			case 4:
				break;
			case 5:
				break;
			case 6:
				break;
			case 7:
				break;
			case 8:
				break;

			default:
				System.out.println("Opción invalida!");

			}

		} while (operacion != 0);
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
