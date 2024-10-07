package mortahilchachoufatima;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Scanner;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

public class Principal {
	static String ficheroviajes = ".\\Viajes.dat";
	static final int LONGVIAJES = 74;
	static String ficheromovimientos = ".\\Movimientos.dat";

	public static void main(String[] args) throws IOException {

		Scanner teclado = new Scanner(System.in);
		int opcion = 0;
		do {
			menu();
			System.out.println("Elige una opción: ");
			opcion = teclado.nextInt();

			switch (opcion) {

			case 1:
				actualizarviajes();
				break;
			case 2:
				listarviajes();
				break;

			case 3:
				crearXML();
				break;
			case 4:
				listarmovimientos(); //para verlo yo
				break;

			}

		} while (opcion != 0);

	}

	private static void crearXML() throws IOException {
		Viajes listaviajes = new Viajes();

		// leer el fichero y cargar los departamentos en la lista
		ArrayList<Viaje> lista = cargarviajes();

		// cargo la lista en la clase raíz
		listaviajes.setListaviajes(lista);

		// crear el XML
		JAXBContext context;
		try {
			context = JAXBContext.newInstance(Viajes.class);

			Marshaller m = context.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			m.marshal(listaviajes, System.out);
			m.marshal(listaviajes, new File(".\\viajes.xml"));
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static ArrayList<Viaje> cargarviajes() throws IOException {
		ArrayList<Viaje> lista = new ArrayList<Viaje>();

		File fiche = new File(ficheroviajes);
		try {
			RandomAccessFile file = new RandomAccessFile(fiche, "r");

			long posicion = 0;

			for (;;) {

				file.seek(posicion); // nos posicionamos en posicion
				int cod = file.readInt();
				if (cod != 0) {
					String nom = "";
					for (int i = 0; i < 30; i++) {
						nom = nom + file.readChar();
					}

					int pvp = file.readInt();
					int plazas = file.readInt();

					String situacion = "";
					for (int i = 0; i < 1; i++) {
						situacion = situacion + file.readChar();
					}

					
					
					Viaje v = new Viaje(cod, nom.trim(), pvp, plazas, situacion.trim());
					lista.add(v);

				}

				posicion = posicion + LONGVIAJES;
				if (posicion >= file.length())
					break;

			}

			file.close();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return lista;

	}

	private static void listarmovimientos() throws IOException {
		File fiche = new File(ficheromovimientos);
		try {
			RandomAccessFile file = new RandomAccessFile(fiche, "r");
			System.out.println("ENTRA");
			long posicion = 0;

			System.out.printf("%10s %-30s %-15s %10s %10s %n", "CodViaje", "Nombre", "PVP", "PLAZAS", "SITUACION");
			System.out.printf("%10s %-30s %-15s %10s %10s %n", "------", "---------------", "---------------", "------",
					"---------");

			for (;;) {

				file.seek(posicion); // nos posicionamos en posicion
				int cod = file.readInt();

				if (cod != 0) {
					String nom = "";
					for (int i = 0; i < 30; i++) {
						nom = nom + file.readChar();
					}

					int pvp = file.readInt();
					int plazas = file.readInt();

					String situacion = "";
					for (int i = 0; i < 1; i++) {
						situacion = situacion + file.readChar();
					}

					System.out.printf("%10s %-30s %-15s %10s %10s %n", cod, nom, pvp, plazas, situacion);
				}

				posicion = posicion + LONGVIAJES;
				if (posicion >= file.length())
					break;

			}
			System.out.printf("%10s %-30s %-15s %10s %10s %n", "------", "---------------", "---------------", "------",
					"---------");

			file.close();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static void actualizarviajes() throws IOException {

		File fichero = new File(ficheromovimientos);
		// declara el fichero de acceso aleatorio

		RandomAccessFile file = new RandomAccessFile(fichero, "r");
		//
		int cod, pvp, plazas, posicion;

		char nombre[] = new char[30], aux;
		char operacion[] = new char[1], aux1;

		System.out.println("-------- PROCESO DE ACTUALIZACIÓN. Fichero movimientos -----------------");
		System.out.println();
		System.out.printf("%-15s %15s %20s %20s %20s %35s %n", "CodViaje", "Nombre", "PVP", "PLAZAS", "OPERACION",
				"RESULTADO");
		System.out.printf("%-15s %15s %20s %20s %20s %35s %n", "---------", "-------", "--", "-----", "----------",
				"---------");

		posicion = 0;
		String mensaje = "";
		String simbolo = "";
		for (;;) { // recorro el fichero
			file.seek(posicion);
			cod = file.readInt(); 
			if (cod != 0) {
				for (int i = 0; i < nombre.length; i++) {
					aux = file.readChar();
					nombre[i] = aux; // los voy guardando en el array
				}
				String nombreS = new String(nombre);// convierto a String el array

				pvp = file.readInt();

				plazas = file.readInt();

				for (int i = 0; i < operacion.length; i++) {
					aux1 = file.readChar();
					operacion[i] = aux1; // los voy guardando en el array
				}
				String situacionS = new String(operacion);// convierto a String el array

				if (situacionS.equalsIgnoreCase("A")) {

					simbolo = "A";

					mensaje = añadirviaje(cod, nombreS, pvp, plazas, simbolo);

				} else if (situacionS.equalsIgnoreCase("B")) {

					simbolo = "B";
					mensaje = borrarviaje(cod, simbolo);

				} else if (situacionS.equalsIgnoreCase("M")) {
					simbolo = "M";
					mensaje = modificarviaje(cod, simbolo, pvp, plazas);
				}

				System.out.printf("%-15s %15s %-20s %-20s %-20s %-40s %n", cod, nombreS, pvp, plazas, situacionS,
						mensaje);

			}

			posicion = posicion + LONGVIAJES;

			// Si he recorrido todos los bytes salgo del for
			if (posicion >= file.length())
				break;

		} // fin bucle for
		file.close(); // cerrar fichero
	}

	private static String modificarviaje(int cod, String simbolo, int pvp, int plazas) throws IOException {
		String mensaje = "";

		if (!ejercicio2(cod)) {
			return "ERROR. VIAJE NO EXISTE, NO SE PUEDE MODIFICAR";
		} else if (!modificacionrealizada(cod)) {
			return "";
		}

		// No existe el reg, se inserta
		File fiche = new File(ficheroviajes);
		try {
			RandomAccessFile file = new RandomAccessFile(fiche, "rw");
			long posicion = (cod - 1) * LONGVIAJES;

			file.seek(posicion + 64);
			int pvps = file.readInt();
			int pvpT = pvps + pvp;
			file.seek(posicion + 64);
			file.writeInt(pvpT);

			int plazass = file.readInt();
			int plazasT = plazass + plazas;
			file.seek(posicion + 68);
			file.writeInt(plazasT);

			StringBuffer buffer = new StringBuffer(simbolo);
			buffer.setLength(1);
			file.writeChars(buffer.toString());

			mensaje = "VIAJE MODIFICADO.";

			file.close();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return mensaje;

	}

	private static boolean modificacionrealizada(int cod) throws IOException {

		// comprobar si el cod existe en fichero viajes o no
		// Consultar id, devuelve true o false
		File fichero = new File(ficheroviajes);
		// declara el fichero de acceso aleatorio
		boolean existe = false;
		try {
			RandomAccessFile file = new RandomAccessFile(fichero, "r");
			int posicion = (cod - 1) * LONGVIAJES;
			if (posicion >= file.length()) {
				existe = false;
			}

			else {
				file.seek(posicion + 72);// nos posicionamos
				char operacion[] = new char[1], aux1;
				for (int i = 0; i < operacion.length; i++) {
					aux1 = file.readChar();
					operacion[i] = aux1; 
				}
				String situacionS = new String(operacion);

				if (!situacionS.equalsIgnoreCase("M")) {
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

	private static String borrarviaje(int cod, String simbolo) throws IOException {
		// borrar
		String mensaje = "";
		if (!ejercicio2(cod)) {
			mensaje = "ERROR. VIAJE NO EXISTE, NO SE PUEDE BORRAR";
		} else {
			
			File fiche = new File(ficheroviajes);
			try {
				RandomAccessFile file = new RandomAccessFile(fiche, "rw");

				long posicion = (cod - 1) * LONGVIAJES;
				file.seek(posicion);
				
				file.writeInt(0);
				StringBuffer buffer = new StringBuffer("");
				buffer.setLength(30);
				file.writeChars(buffer.toString());
				file.writeInt(0);
				file.writeInt(0);

				buffer = new StringBuffer(simbolo);
				buffer.setLength(1);
				file.writeChars(buffer.toString());

				file.close();
				mensaje = "VIAJE BORRADO";

			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		return mensaje;

	}

	private static String añadirviaje(int cod, String nombreS, int pvp, int plazas, String simbolo) throws IOException {
		String mensaje = "";

		if (ejercicio2(cod)) {
			return "ERROR. VIAJE A DAR DE ALTA YA EXISTE";
		}

		// No existe el reg, se inserta
		File fiche = new File(ficheroviajes);
		try {
			RandomAccessFile file = new RandomAccessFile(fiche, "rw");
			long posicion = (cod - 1) * LONGVIAJES;
			file.seek(posicion);
			file.writeInt(cod);
			StringBuffer buffer = new StringBuffer(nombreS);
			buffer.setLength(30);
			file.writeChars(buffer.toString());
			file.writeInt(pvp);
			file.writeInt(plazas);
			buffer = new StringBuffer(simbolo);
			buffer.setLength(1);
			file.writeChars(buffer.toString());

			mensaje = "VIAJE AÑADIDO A VIAJES";

			file.close();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return mensaje;

	}

	private static boolean ejercicio2(int cod) throws IOException {
		// comprobar si el cod existe en fichero viajes o no
		// Consultar id, devuelve true o false
		File fichero = new File(ficheroviajes);
		// declara el fichero de acceso aleatorio
		boolean existe = false;
		try {
			RandomAccessFile file = new RandomAccessFile(fichero, "r");
			int posicion = (cod - 1) * LONGVIAJES;
			if (posicion >= file.length()) {
				existe = false;
			}

			else {
				file.seek(posicion);// nos posicionamos
				int ident = file.readInt(); 
				if (ident == cod) {
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

	private static void listarviajes() throws IOException {
		File fiche = new File(ficheroviajes);
		float pvpT = 0;
		float plazasT = 0;
		int contador = 0;
		try {
			RandomAccessFile file = new RandomAccessFile(fiche, "r");

			long posicion = 0;

			System.out.printf("%10s %-30s %-15s %10s %10s %n", "CodViaje", "Nombre", "PVP", "PLAZAS", "SITUACION");
			System.out.printf("%10s %-30s %-15s %10s %10s %n", "------", "---------------", "---------------", "------",
					"---------");

			for (;;) {

				file.seek(posicion); // nos posicionamos en posicion
				int cod = file.readInt();

				if (cod != 0) {
					String nom = "";
					for (int i = 0; i < 30; i++) {
						nom = nom + file.readChar();
					}

					int pvp = file.readInt();
					pvpT = pvpT + pvp;
					int plazas = file.readInt();
					plazasT = plazasT + plazas;
					contador++;

					String situacion = "";
					for (int i = 0; i < 1; i++) {
						situacion = situacion + file.readChar();
					}

					System.out.printf("%10s %-30s %-15s %10s %10s %n", cod, nom, pvp, plazas, situacion);
				}

				posicion = posicion + LONGVIAJES;
				if (posicion >= file.length())
					break;

			}
			System.out.printf("%10s %-30s %-15s %10s %10s %n", "------", "---------------", "---------------", "------",
					"---------");
			System.out.printf("%10s %-30s %-15s %10s %10s %n", "MEDIAS: ", "", +(pvpT / contador) + "",
					+(plazasT / contador) + "", "");

			file.close();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static void menu() {
		System.out.println("OPERACIONES CON VIAJES");
		System.out.println("1.   Actualizar Viajes.dat");
		System.out.println("2.   Listar el fichero Viajes.dat");
		System.out.println("3.   Crear XML viajes.xml");
		System.out.println("0.   Salir");

	}
}
