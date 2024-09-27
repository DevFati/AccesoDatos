package Ejercicio;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Scanner;

public class Principal {
	static String fichdep = ".\\Productos.dat";
	 static int LON = 46;

	public static void main(String[] args) throws IOException {
		listar();
	//	accederdatosVentas();
		
	}
	
	public static void listar() throws IOException {
		File fichero = new File(".\\DatosdeVentas.dat");
		// declara el fichero de acceso aleatorio
		RandomAccessFile file = new RandomAccessFile(fichero, "r");
		//
		int cod, uniVen, posicion;
		
		char fecha[] = new char[10], aux;

		posicion = 0; // para situarnos al principio

		for (;;) { // recorro el fichero
			file.seek(posicion); // nos posicionamos en posicion
			cod = file.readInt(); // obtengo id de empleado
			for (int i = 0; i < fecha.length; i++) {
				aux = file.readChar();// recorro uno a uno los caracteres del apellido
				fecha[i] = aux; // los voy guardando en el array
			}
			String fechaS = new String(fecha);// convierto a String el array
			uniVen = file.readInt();// obtengo dep
			// dep=0;
		

			// salario=0d;
			System.out.println(
					"Codigo: " + cod + ", UNI VEN: " + uniVen + ", FECHA: " + fecha );
			posicion = posicion + 38; // me posiciono para el sig empleado
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
				for (int i = 0; i < fecha.length; i++) {
					aux = file.readChar();// recorro uno a uno los caracteres del apellido
					fecha[i] = aux; // los voy guardando en el array
				}
				String fechaS = new String(fecha);// convierto a String el array
				uniVen = file.readInt();// obtengo dep
			

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

			posicion = posicion + 46;

			// Si he recorrido todos los bytes salgo del for
			if (posicion >= file.length() )
				break;

		} // fin bucle for
		file.close(); // cerrar fichero
	}
	private static void actualizarprod(int cod, int ventas) throws IOException {
		
		File fiche = new File(fichdep);
		try {
			RandomAccessFile file = new RandomAccessFile(fiche, "rw");

			long posicion = (cod - 1) * LON;
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
		
	
}
