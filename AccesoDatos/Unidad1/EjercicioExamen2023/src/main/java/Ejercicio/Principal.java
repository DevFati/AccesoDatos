package Ejercicio;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;



public class Principal {
	static String fichdep = ".\\Productos.dat";
	 static int LON = 46;

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
				Scanner sc = new Scanner(System.in);
				int opcion = 0;

				do {
					mostrarMenu();
					opcion = sc.nextInt();
					switch (opcion) {
					case 1:
						listarProductos();
						break;
					case 2:
						listar();
					
						break;
					case 3:
						//System.out.println(ejercicio2(13));
						accederdatosVentas();

						break;
					case 4:
						crearXML();
						break;
					case 5: 
						crearXML2();
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
	
	private static void crearXML2() throws IOException {
		Productos2 productos=new Productos2();
		
		ArrayList<Producto2> lista=cargarproductos2();
		 lista=datosventas2(lista);
		 productos.setLista(lista);
		 
		// crear el XML
			JAXBContext context;
			try {
				context = JAXBContext.newInstance(Productos2.class);

				Marshaller m = context.createMarshaller();
				m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
				m.marshal(productos, System.out);
				m.marshal(productos, new File(".\\Productos2.xml"));
			} catch (JAXBException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
	}

	private static ArrayList<Producto2> datosventas2(ArrayList<Producto2> lista) throws IOException {
		File fiche= new File("DatosdeVentas.dat");
		try {
			RandomAccessFile file = new RandomAccessFile(fiche, "r");

			long posicion = 0;
					
			for (;;) {

			file.seek(posicion); // nos posicionamos en posicion
				int cod = file.readInt();
				int uniV=file.readInt();
				if (cod != 0) {
					String fecha = "";
					for (int i = 0; i < 10; i++) {
						fecha = fecha + file.readChar();
					}

					

				
					for(Producto2 p: lista) {
						
						if(p.getCodigo()==cod) {
							p.getListaventas().add(new Venta(uniV,fecha,uniV*p.getPrecio()));
							
							
								
							
							
							
						}
					}
				}

				posicion = posicion + 28;
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

	private static ArrayList<Producto2> cargarproductos2() throws IOException {
		ArrayList<Producto2> lista= new ArrayList<Producto2>();
		File fiche= new File("Productos.dat");
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

					

					int exis = file.readInt();
					double precio=file.readDouble();
					Producto2 prod= new Producto2(cod, nom.trim(),exis,precio );
					lista.add(prod);	
					System.out.println("Producto " + cod + " añadido a la lista");
				}

				posicion = posicion + 46;
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

	private static void crearXML() throws IOException {
		Productos productos=new Productos();
		
		ArrayList<Producto> lista= cargarproductos();
		 lista=datosventas(lista);
		 productos.setLista(lista);
		 
		// crear el XML
			JAXBContext context;
			try {
				context = JAXBContext.newInstance(Productos.class);

				Marshaller m = context.createMarshaller();
				m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
				m.marshal(productos, System.out);
				m.marshal(productos, new File(".\\Productos.xml"));
			} catch (JAXBException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
	}



	




	

	private static ArrayList<Producto> datosventas(ArrayList<Producto> lista) throws IOException {
		
		File fiche= new File("DatosdeVentas.dat");
		try {
			RandomAccessFile file = new RandomAccessFile(fiche, "r");

			long posicion = 0;
					
			for (;;) {

			file.seek(posicion); // nos posicionamos en posicion
				int cod = file.readInt();
				int uniV=file.readInt();
				if (cod != 0) {
					String fecha = "";
					for (int i = 0; i < 10; i++) {
						fecha = fecha + file.readChar();
					}

					

				
					for(Producto p: lista) {
						if(p.getCodigo()==cod) {
							p.setUnidadesVendidas(uniV+p.getUnidadesVendidas());
							p.setImporte(p.getUnidadesVendidas()*p.getPrecio());
							if(p.getExistencias()-p.getUnidadesVendidas()<2) {
								p.setEstado("A reponer");
							}
							
						}
					}
				}

				posicion = posicion + 28;
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

	private static ArrayList<Producto> cargarproductos() throws IOException {
		ArrayList<Producto> lista= new ArrayList<Producto>();
		File fiche= new File("Productos.dat");
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

					

					int exis = file.readInt();
					double precio=file.readDouble();
					Producto prod= new Producto(cod, nom.trim(),exis,0,0,"", precio );
					lista.add(prod);	
					System.out.println("Producto " + cod + " añadido a la lista");
				}

				posicion = posicion + 46;
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

	private static void listarProductos() throws IOException {
		File fichero = new File(".\\Productos.dat");
		// declara el fichero de acceso aleatorio
		RandomAccessFile file = new RandomAccessFile(fichero, "r");
		//
		int cod,ex, posicion;
		Double pvp;
		char nombre[]=new char[15],aux;
		
		posicion = 0; // para situarnos al principio

		for (;;) { 
			file.seek(posicion);
			cod = file.readInt(); 
			for (int i = 0; i < nombre.length; i++) {
				aux = file.readChar();// recorro uno a uno los caracteres del apellido
				nombre[i] = aux; // los voy guardando en el array
			}
			
			String nombreS = new String(nombre);
			ex=file.readInt();
			pvp=file.readDouble();
			if(cod!=0) {
				System.out.println(
						"Codigo: " + cod + ", Nombre: " + nombreS + ", Existencias: " + ex+" , PVP: "+pvp );
			}
			
			posicion = posicion + 46; // me posiciono para el sig empleado
										// Cada empleado ocupa 38 bytes (4+20+4+8)
			// Si he recorrido todos los bytes salgo del for
			if (posicion >= file.length() || file.getFilePointer() == file.length())
				break;

		} // fin bucle for
		file.close(); // cerrar fichero
		
	}

	public static void listar() throws IOException {
		File fichero = new File(".\\DatosdeVentas.dat");
		// declara el fichero de acceso aleatorio
		RandomAccessFile file = new RandomAccessFile(fichero, "r");
		//
		int cod, uniVen, posicion;
		
		char fecha[] = new char[10], aux;

		posicion = 0; // para situarnos al principio

		for (;;) { 
			file.seek(posicion);
			cod = file.readInt(); 
			uniVen = file.readInt();
			for (int i = 0; i < fecha.length; i++) {
				aux = file.readChar();// recorro uno a uno los caracteres del apellido
				fecha[i] = aux; // los voy guardando en el array
			}
			
			String fechaS = new String(fecha);
			System.out.println(
					"Codigo: " + cod + ", UNI VEN: " + uniVen + ", FECHA: " + fechaS );
			posicion = posicion + 28; // me posiciono para el sig empleado
										// Cada empleado ocupa 38 bytes (4+20+4+8)
			// Si he recorrido todos los bytes salgo del for
			if (posicion >= file.length() || file.getFilePointer() == file.length())
				break;

		} // fin bucle for
		file.close(); // cerrar fichero
	}
	
	private static void accederdatosVentas() throws IOException {
		
		Scanner sc = new Scanner(System.in);
		// actualizar. Recorremos secuencialmente el empleado
		// y vamos actualizando de maneta directa en departam

		File fichero = new File(".\\DatosdeVentas.dat");
		// declara el fichero de acceso aleatorio
		RandomAccessFile file = new RandomAccessFile(fichero, "r");
		//
		int cod, uniVen,posicion;
		
		char fecha[] = new char[10], aux;

		posicion = 0;

		for (;;) { // recorro el fichero
			file.seek(posicion);
			cod = file.readInt(); // obtengo id de empleado
			
			if (cod != 0) {
				uniVen = file.readInt();// obtengo dep
				for (int i = 0; i < fecha.length; i++) {
					aux = file.readChar();// recorro uno a uno los caracteres del apellido
					fecha[i] = aux; // los voy guardando en el array
				}
				String fechaS = new String(fecha);// convierto a String el array
				
			

				// Comprobar si el cod existe para actualizar
				if(cod<99) {
					if (ejercicio2(cod)) {
						// el dep si existe en el fich dep
						actualizarprod(cod,uniVen );
						System.out.println("Actualizado producto con cod: " + cod);
					}else {
						System.out.println("El cod no existe");
					}
				}else {
					System.out.println("El producto es mayor que 99");
				}
				
			}

			posicion = posicion + 28;

			// Si he recorrido todos los bytes salgo del for
			if (posicion >= file.length() )
				break;

		} // fin bucle for
		file.close(); // cerrar fichero
	}
	private static void actualizarprod(int cod, int ventas) throws IOException {
		
		File fiche = new File(".\\Productos.dat");
		try {
			RandomAccessFile file = new RandomAccessFile(fiche, "rw");

			long posicion = (cod - 1) * 46;
			
			file.seek(posicion + 34); // nos posicionamos en existencias
		    int exs=file.readInt();
		    exs=exs-ventas;
			file.seek(posicion + 34); 
			file.writeInt(exs); //actualizo las existencias

			

			file.close();
			

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
	}
	
	private static boolean ejercicio2(int id) throws IOException {
		// Consultar id, devuelve true o false
		File fichero = new File(".\\Productos.dat");
		// declara el fichero de acceso aleatorio
		boolean existe = false;
		try {
			RandomAccessFile file = new RandomAccessFile(fichero, "r");
			int posicion = (id - 1) * 46;
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
	
	
	private static void mostrarMenu() {
		System.out.println("------------------------------------------------------");
		System.out.println("OPERACIONES CON DEPARTAMENTOS");
		System.out.println("  1. Ejercicio 1. Listar productos.");
		System.out.println("  2. Ejercicio 2. Listar ventas.");
		System.out.println("  3. Ejercicio 3. Actualizar.");
		System.out.println("  4. Ejercicio 4. Crear XML.");	
		System.out.println("  5. Ejercicio 5. Crear XML2.");
		System.out.println("  0. Salir");
		System.out.println("------------------------------------------------------");
	}
	
}
