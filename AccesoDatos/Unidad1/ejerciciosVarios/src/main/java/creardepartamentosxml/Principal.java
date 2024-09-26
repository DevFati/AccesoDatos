package creardepartamentosxml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

public class Principal {

	static String fichdep = ".\\AleatorioDepart.dat";
	static int LON = 72;

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub

		insertarregistros();

		crearxmldepartamentos();

	}

	private static void crearxmldepartamentos() throws IOException {

		Departamentos departs = new Departamentos();
		
		// leer el fichero y cargar los departamentos en la lista
		ArrayList<Dep> lista  = cargardepenlista();

		// cargo la lista en la clase raíz
		departs.setLista(lista);

		// crear el XML
		JAXBContext context;
		try {
			context = JAXBContext.newInstance(Departamentos.class);

			Marshaller m = context.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			m.marshal(departs, System.out);
			m.marshal(departs, new File(".\\departamentos.xml"));
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static ArrayList<Dep> cargardepenlista() throws IOException {
		
		ArrayList<Dep> lista = new ArrayList<Dep>();
		
		File fiche = new File(fichdep);
		try {
			RandomAccessFile file = new RandomAccessFile(fiche, "r");

			long posicion = 0;

			for (;;) {

			file.seek(posicion); // nos posicionamos en posicion
				int cod = file.readInt();
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

					// creamos el objeto Dep, y lo añadimos a la lista
					//usamos trim para que los espacios entre nom y loc no esten 
					Dep dep= new Dep(cod, nom.trim(), loc.trim(), num, med );
					lista.add(dep);	
					System.out.println("Dep " + cod + " añadido a la lista");
				}

				posicion = posicion + LON;
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

	private static void insertarregistros() throws IOException {
		// insertar varios registros
		int cod = 10, num = 3;
		float mediasal = 1000f;
		String nombre = "VENTAS", loc = "TALAVERA";
		System.out.println(ejercicio3(cod, nombre, loc, num, mediasal));

		System.out.println(ejercicio3(20, "INFORMÁTICA", "TALAVERA", 2, 1500.0f));

		System.out.println(ejercicio3(30, "CONTABILIDAD", "TOLEDO", 1, 1600.0f));

		System.out.println(ejercicio3(40, "COMPRAS", "TOLEDO", 2, 1400.0f));

		System.out.println(ejercicio3(50, "FORMACIÓN", "TALAVERA", 4, 1600.0f));
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

}

