package EjercicioDepartamentos;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Scanner;

public class Principal {

	static String fichdep = ".\\AleatorioDepart.dat";
	static int LON = 72;

	public static void main(String[] args) throws IOException {
		Scanner sc = new Scanner(System.in);
		int opcion = 0;

		do {
			mostrarMenu();
			opcion = sc.nextInt();
			switch (opcion) {
			case 1:
				ejercicio1();
				break;
			case 2:
				System.out.println("Ejercicio 2. Consultar si existe");
				int dep = 5;
				if (ejercicio2(dep)) {
					System.out.println("DEPARTAMENTO " + dep + " EXISTE");
				} else {
					System.out.println("DEPARTAMENTO " + dep + "  NO EXISTE");
				}
				dep = 25;
				if (ejercicio2(25)) {
					System.out.println("DEPARTAMENTO " + dep + " EXISTE");
				} else {
					System.out.println("DEPARTAMENTO " + dep + "  NO EXISTE");
				}

				break;
			case 3:
				// insertar ejercicio3();
				// cod nombre loc num mediasal
				int cod = 10, num = 3;
				float mediasal = 1000f;
				String nombre = "VENTAS", loc = "TALAVERA";
				System.out.println(ejercicio3(cod, nombre, loc, num, mediasal));

				cod = 20;
				num = 2;
				mediasal = 1500f;
				nombre = "INFORMÁTICA";
				loc = "MADRID";
				System.out.println(ejercicio3(cod, nombre, loc, num, mediasal));

				cod = 120;
				num = 2;
				mediasal = 1500f;
				nombre = "INFORMÁTICA";
				loc = "MADRID";
				System.out.println(ejercicio3(cod, nombre, loc, num, mediasal));

				break;
			case 4:
				// Visualizar
				int cod1 = 3;
				ejercicio4(cod1);
				cod1 = 10;
				ejercicio4(cod1);
				break;

			case 5:
				// modificarregistro
				int cod2 = 10;
				float mediasal2 = 1500f;
				String loc2 = "TOLEDO";
				System.out.println(ejercicio5(cod2, loc2, mediasal2));

				cod2 = 15;
				mediasal2 = 1500f;
				loc2 = "TOLEDO";
				System.out.println(ejercicio5(cod2, loc2, mediasal2));

				break;

			case 6:
				// Borrar
				int cod3 = 3;
				System.out.println(ejercicio6(cod3));
				cod3 = 10;
				System.out.println(ejercicio6(cod3));
				break;

			case 7:
				ejercicio7();
				break;
			case 8:

				listarempleados();
				break;

			case 9:

				actualizardepartamentos();
				break;

			case 0:
				System.out.println("FIN DE MENÚ!");
				break;
			default:
				System.out.println("Seleccione una opción válida!");
				break;
			}
		} while (opcion != 0);

		sc.close();
	}

	private static void actualizardepartamentos() throws IOException {
		// actualizar. Recorremos secuencialmente el empleado
		// y vamos actualizando de maneta directa en departam

		File fichero = new File(".\\AleatorioEmple.dat");
		// declara el fichero de acceso aleatorio
		RandomAccessFile file = new RandomAccessFile(fichero, "r");
		//
		int id, dep, posicion;
		Double salario;
		char apellido[] = new char[10], aux;

		posicion = 0;

		for (;;) { // recorro el fichero
			file.seek(posicion);
			id = file.readInt(); // obtengo id de empleado
			if (id != 0) {
				for (int i = 0; i < apellido.length; i++) {
					aux = file.readChar();// recorro uno a uno los caracteres del apellido
					apellido[i] = aux; // los voy guardando en el array
				}
				String apellidoS = new String(apellido);// convierto a String el array
				dep = file.readInt();// obtengo dep
				salario = file.readDouble(); // obtengo salario

				// Comprobar si el dep existe para actualizar
				if (ejercicio2(dep)) {
					// el dep si existe en el fich dep
					actualizardep(dep,salario.floatValue() );
					System.out.println("Actualizado dep: " + dep);
				}
			}

			posicion = posicion + 36;

			// Si he recorrido todos los bytes salgo del for
			if (posicion >= file.length() )
				break;

		} // fin bucle for
		file.close(); // cerrar fichero

	}

	private static void actualizardep(int dep, Float salario) throws IOException {
	
		File fiche = new File(fichdep);
		try {
			RandomAccessFile file = new RandomAccessFile(fiche, "rw");

			long posicion = (dep - 1) * LON;
			file.seek(posicion + 64); // nos posicionamos en contador
		    int con=file.readInt();
		    con=con+1;
			file.seek(posicion + 64); 
			file.writeInt(con); // sumo 1 al número de empleados

			file.seek(posicion + 68); // nos posicionamos en media sal y leer
			float saa=file.readFloat();
			float mediasal = (saa + salario)/2;
			file.seek(posicion + 68); 
			file.writeFloat(mediasal); //  actualizo el salario

			file.close();
			

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
	}

	public static void listarempleados() throws IOException {
		File fichero = new File(".\\AleatorioEmple.dat");
		// declara el fichero de acceso aleatorio
		RandomAccessFile file = new RandomAccessFile(fichero, "r");
		//
		int id, dep, posicion;
		Double salario;
		char apellido[] = new char[10], aux;

		posicion = 0; // para situarnos al principio

		for (;;) { // recorro el fichero
			file.seek(posicion); // nos posicionamos en posicion
			id = file.readInt(); // obtengo id de empleado
			for (int i = 0; i < apellido.length; i++) {
				aux = file.readChar();// recorro uno a uno los caracteres del apellido
				apellido[i] = aux; // los voy guardando en el array
			}
			String apellidoS = new String(apellido);// convierto a String el array
			dep = file.readInt();// obtengo dep
			// dep=0;
			salario = file.readDouble(); // obtengo salario

			// salario=0d;
			System.out.println(
					"ID: " + id + ", Apellido: " + apellidoS + ", Departamento: " + dep + ", Salario: " + salario);
			posicion = posicion + 36; // me posiciono para el sig empleado
										// Cada empleado ocupa 36 bytes (4+20+4+8)
			// Si he recorrido todos los bytes salgo del for
			if (posicion >= file.length() || file.getFilePointer() == file.length())
				break;

		} // fin bucle for
		file.close(); // cerrar fichero
	}

	private static String ejercicio6(int cod) throws IOException {
		// borrar
		String mensaje = "";
		if (!ejercicio2(cod)) {
			mensaje = "\nERROR EL DEPARTAMENTO NO EXISTE, NO SE BORRA: " + cod;
		} else {
			// existe el dep, se modifica
			File fiche = new File(fichdep);
			try {
				RandomAccessFile file = new RandomAccessFile(fiche, "rw");

				long posicion = (cod - 1) * LON;
				file.seek(posicion);
				// Grabar ceros
				file.writeInt(0);
				StringBuffer buffer = new StringBuffer("   ");
				buffer.setLength(15);
				file.writeChars(buffer.toString());

				buffer = new StringBuffer("   ");
				buffer.setLength(15);
				file.writeChars(buffer.toString());

				file.writeInt(0);

				file.writeFloat(0);

				file.close();
				mensaje = "\nEL DEPARTAMENTO SE HA BORRADO: " + cod;

			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		return mensaje;
	}

	private static String ejercicio5(int cod, String loc, float mediasal) throws IOException {

		String mensaje = "";
		if (!ejercicio2(cod)) {
			mensaje = "\nERROR EL DEPARTAMENTO NO EXISTE, NO SE MODIFICA: " + cod;
		} else {
			// existe el dep, se modifica
			File fiche = new File(fichdep);
			try {
				RandomAccessFile file = new RandomAccessFile(fiche, "rw");

				long posicion = (cod - 1) * LON;
				file.seek(posicion + 34); // nos posicionamos en loc y grabamos
				StringBuffer buffer = new StringBuffer(loc);
				buffer.setLength(15);
				file.writeChars(buffer.toString());

				file.seek(posicion + 68); // nos posicionamos en media sal y grabamos
				file.writeFloat(mediasal);

				file.close();
				mensaje = "\nEL DEPARTAMENTO SE HA ACTUALIZADO: " + cod;

			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		return mensaje;

	}

	private static boolean ejercicio4(int cod1) throws IOException {
		// visualizar
		// comprobamos si existe
		boolean existe = false;
		if (!ejercicio2(cod1)) {
			System.out.println("\nERROR EL DEPARTAMENTO NO EXISTE: " + cod1);
		} else {
			// dep si existe leemos y vidualizamos
			System.out.println("\nDEPARTAMENTO EXISTE: " + cod1);
			existe = true;
			File fiche = new File(fichdep);
			try {
				RandomAccessFile file = new RandomAccessFile(fiche, "r");

				long posicion = (cod1 - 1) * LON;
				file.seek(posicion); // nos posicionamos en posicion
				int cod = file.readInt();

				String nom = "";
				for (int i = 0; i < 15; i++) {
					nom = nom + file.readChar();
				}

				String loc = "";
				for (int i = 0; i < 15; i++) {
					loc = loc + file.readChar();
				}

				int num = file.readInt();

				float med = file.readFloat();

				System.out.printf("%6s %-15s %-15s %6s %9s %n", "COD", "NOMBRE", "LOCALIDAD", "NUM EMP", "MEDSAL");
				System.out.printf("%6s %15s %15s %6s %9s %n%n", cod, nom, loc, num, med);

				file.close();

			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		return existe;
	}

	private static void ejercicio7() throws IOException {

		File fiche = new File(fichdep);
		try {
			RandomAccessFile file = new RandomAccessFile(fiche, "r");

			long posicion = 0;

			System.out.printf("%6s %-15s %-15s %6s %9s %n", "CODDEP", "NOMBRE DEP", "LOC DEP", "NUMEMP", "MEDIA SAL");
			System.out.printf("%6s %-15s %-15s %6s %9s %n", "------", "---------------", "---------------", "------",
					"---------");

			for (;;) {

				file.seek(posicion); // nos posicionamos en posicion
				int cod = file.readInt();
				//System.out.println("llega");
				if (cod != 0) {
					String nom = "";
					for (int i = 0; i < 15; i++) {
						nom = nom + file.readChar();
					}

					String loc = "";
					for (int i = 0; i < 15; i++) {
						loc = loc + file.readChar();
					}

					int num = file.readInt();

					float med = file.readFloat();

					System.out.printf("%6s %15s %15s %6s %5.2f %n", cod, nom, loc, num, med);
				}

				posicion = posicion + LON;
				if (posicion >= file.length())
					break;

			}
			System.out.printf("%6s %-15s %-15s %6s %9s %n%n", "------", "---------------", "---------------", "------",
					"---------");

			file.close();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static String ejercicio3(int cod, String nombre, String loc, int num, float mediasal) throws IOException {

		String mensaje = "";
		if (cod < 1 || cod > 100) {
			return "ERROR EL DEPARTAMENTO DEBE ESTAR ENTRE 1 Y 100: " + cod;
		}

		if (ejercicio2(cod)) {
			return "ERROR EL DEPARTAMENTO YA EXISTE NO SE PUEDE INSERTAR: " + cod;
		}

		// No existe el reg, se inserta
		File fiche = new File(fichdep);
		try {
			RandomAccessFile file = new RandomAccessFile(fiche, "rw");
			long posicion = (cod - 1) * LON;
			file.seek(posicion);
			file.writeInt(cod);
			StringBuffer buffer = new StringBuffer(nombre);
			buffer.setLength(15);
			file.writeChars(buffer.toString());

			buffer = new StringBuffer(loc);
			buffer.setLength(15);
			file.writeChars(buffer.toString());

			file.writeInt(num);

			file.writeFloat(mediasal);

			mensaje = "REGISTRO INSERTADO. Cod: " + cod + ", " + nombre;

			file.close();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return mensaje;
	}

	private static boolean ejercicio2(int id) throws IOException {
		// Consultar id, devuelve true o false
		File fichero = new File(fichdep);
		// declara el fichero de acceso aleatorio
		boolean existe = false;
		try {
			RandomAccessFile file = new RandomAccessFile(fichero, "r");
			int posicion = (id - 1) * LON;
			if (posicion >= file.length()) {
				existe = false;
			}

			else {
				file.seek(posicion);// nos posicionamos
				int ident = file.readInt(); // obtengo id de dep
				if (ident == id) {
					existe = true;
				}
			}

			file.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return existe;
	}

	private static void ejercicio1() {
		// Crear el fichero
		File fichero = new File(fichdep);
		if (fichero.exists())
			System.out.println("Fichero ya está creado.\n");
		else {

			// declara el fichero de acceso aleatorio
			try {
				RandomAccessFile file = new RandomAccessFile(fichero, "rw");

				try {
					file.close();

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			System.out.println("Fichero creado.\n");
		}

	}

	private static void mostrarMenu() {
		System.out.println("------------------------------------------------------");
		System.out.println("OPERACIONES CON DEPARTAMENTOS");
		System.out.println("  1. Ejercicio 1. Crea fichero.");
		System.out.println("  2. Ejercicio 2. Consultar registro.");
		System.out.println("  3. Ejercicio 3. Insertar registro.");
		System.out.println("  4. Ejercicio 4. Visualizar registro.");
		System.out.println("  5. Ejercicio 5. Modificar registro.");
		System.out.println("  6. Ejercicio 6. Borrar registro.");
		System.out.println("  7. Ejercicio 7. Listar departamentos.");
		System.out.println("  8. Ejercicio 8. Listar empleados.");
		System.out.println("  9. Ejercicio 9. Actualizar.");
		
		System.out.println("  0. Salir");
		System.out.println("------------------------------------------------------");
	}
}


