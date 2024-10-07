package asignaturas_alumnos;

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
static final int LONGALUMNOS=92;
static String fichalumnos= ".\\Alumnos.dat";
static String fichnotas= ".\\Notas.dat";
static final int LONGNOTAS=48;

	public static void main(String[] args) throws IOException {
		Scanner teclado=new Scanner (System.in);
		int opcion=0;
		do {
			
			menu();
			System.out.println("Elige una opcion: ");
			opcion=teclado.nextInt();
			
			switch(opcion) {
			case 1: 
				listaralumnos();
				break;
			case 2: 
				listarnotas();
				break; 
			
			case 3: 
				actualizarAlumnos();
				break; 
			case 4: 
				crearXML();
				break; 

			}
		}while(opcion!=5);
		
		
		
		
	}

	
	
	private static void crearXML() throws IOException {
	Alumnos ListaAlumnos = new Alumnos();
		
	
		ArrayList<Alumno> alumn  = cargaralumnos();

		cargarnotas(alumn);
		ListaAlumnos.setAlumnos(alumn);

		// crear el XML
		JAXBContext context;
		try {
			context = JAXBContext.newInstance(Alumnos.class);

			Marshaller m = context.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			m.marshal(ListaAlumnos, System.out);
			m.marshal(ListaAlumnos, new File(".\\alumnos.xml"));
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}



	private static ArrayList<Alumno> cargarnotas(ArrayList<Alumno> alumn) throws IOException {
		
		
		File fiche = new File(fichnotas);
		try {
			RandomAccessFile file = new RandomAccessFile(fiche, "r");

			long posicion = 0;

			for (;;) {

			file.seek(posicion); // nos posicionamos en posicion
				int cod = file.readInt();
				if (cod != 0) {
					
					for (Alumno a : alumn) {
						
						if(cod==a.getNumalumno()) {
							String nom = "";
							for (int i = 0; i < 20; i++) {
								nom = nom + file.readChar();
							}	
							float nota = file.readFloat();
							
							a.getListanotas().add(new Nota(nom.trim(),nota));
							
						}
						
					}
					
					
				}

				posicion = posicion + LONGNOTAS;
				if (posicion >= file.length())
					break;

			}
			
			file.close();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		
		return alumn;
	
	}



	private static ArrayList<Alumno> cargaralumnos() throws IOException {
		ArrayList<Alumno> alumn = new ArrayList<Alumno>();
		
		File fiche = new File(fichalumnos);
		try {
			RandomAccessFile file = new RandomAccessFile(fiche, "r");

			long posicion = 0;

			for (;;) {

			file.seek(posicion); // nos posicionamos en posicion
				int cod = file.readInt();
				if (cod != 0) {
					String nom = "";
					for (int i = 0; i < 20; i++) {
						nom = nom + file.readChar();
					}

					String loc = "";
					for (int i = 0; i < 20; i++) {
						loc = loc + file.readChar();
					}

					int num = file.readInt();

					float nota = file.readFloat();

					
					Alumno al= new Alumno(cod, nom.trim(), loc.trim(), num, nota );
					alumn.add(al);	
					System.out.println("Alumno " + cod + " añadido a la lista");
				}

				posicion = posicion + LONGALUMNOS;
				if (posicion >= file.length())
					break;

			}
			
			file.close();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
		
		
		
		
		
		return alumn;
	}



	private static void actualizarAlumnos() throws IOException {

				File fichero = new File(fichnotas);
				
				RandomAccessFile file = new RandomAccessFile(fichero, "r");
				
				int codAlumno, posicion;
				float nota;
				

				posicion = 0;

				for (;;) { // recorro el fichero
					file.seek(posicion);
					codAlumno = file.readInt(); 
					if (codAlumno != 0) {
					file.seek(posicion+(20*2)+4);
						
						nota = file.readFloat(); 
					
						if (ejercicio2(codAlumno)) {
					
							
							actualizaralumno(codAlumno,nota );
							System.out.println("Actualizado dep: " + codAlumno);
							
						}
					}

					posicion = posicion + LONGNOTAS;

					// Si he recorrido todos los bytes salgo del for
					if (posicion >= file.length() )
						break;

				} // fin bucle for
				notamedia();
				file.close(); // cerrar fichero
		
	}



	private static void notamedia() throws IOException {
		File fiche = new File(fichalumnos);
	
			RandomAccessFile file = new RandomAccessFile(fiche, "rw");
			float notas=0;
			int alumnos=0;
			int posicion=0;
			float max=Integer.MIN_VALUE;
			char nombre[] = new char[20], aux;
			String nombreS="";
			for (;;) { // recorro el fichero
				file.seek(posicion);
				int codAlumno = file.readInt(); 
				if (codAlumno != 0) {
				file.seek(posicion+(20*2)+4+(20*2));
					
					int num = file.readInt(); 
					
					float nota=file.readFloat();
					
					file.seek(posicion+4+80+4);
					file.writeFloat(nota/num);
					notas=notas+(nota/num);
					alumnos++;
					
					if(max<(nota/num)) {
						max=nota/num;
						file.seek(posicion+4);
						for (int i = 0; i < nombre.length; i++) {
							aux = file.readChar();// recorro uno a uno los caracteres 
							nombre[i] = aux; // los voy guardando en el array
						}
						 nombreS = new String(nombre);
					}
					
				}

				posicion = posicion + LONGALUMNOS;

				// Si he recorrido todos los bytes salgo del for
				if (posicion >= file.length() )
					break;

			 // fin bucle for
			
			}
			listaralumnos();
			System.out.println("Alumno con nota media máxima: "+nombreS);
			System.out.println("Media de nota total: "+notas/alumnos);
	
	}



	private static void actualizaralumno(int codAlumno, float nota) throws IOException {
		File fiche = new File(fichalumnos);
		try {
			RandomAccessFile file = new RandomAccessFile(fiche, "rw");

			long posicion = (codAlumno - 1) * LONGALUMNOS;
			file.seek(posicion + 84); 
		    int con=file.readInt();
		    con=con+1;
			file.seek(posicion + 84); 
			file.writeInt(con); 

			
			float saa=file.readFloat();
			float media = (saa + nota);
			file.seek(posicion + 88); 
			file.writeFloat(media); 
	
			file.close();
			

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}



	private static boolean ejercicio2(int id) throws IOException {
		
		File fichero = new File(fichalumnos);
		boolean existe = false;
		try {
			RandomAccessFile file = new RandomAccessFile(fichero, "r");
			int posicion = (id - 1) * LONGALUMNOS;
			if (posicion >= file.length()) {
				existe = false;
			}

			else {
				file.seek(posicion);// nos posicionamos
				int ident = file.readInt(); 
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


	private static void listarnotas() throws IOException {
		File fiche = new File(fichnotas);
		try {
			RandomAccessFile file = new RandomAccessFile(fiche, "r");

			long posicion = 0;

			System.out.printf("%10s %-25s %-25s %10s %n", "REGIS", "NUMALUM", "ASIGNATURA", "NOTA");
			System.out.printf("%10s %-25s %-25s %10s %n", "------", "---------------", "---------------",
					"---------");
			int indice=1;
			for (;;) {
				
				file.seek(posicion); // nos posicionamos en posicion
				int numalum = file.readInt();
			
				if (numalum != 0) {
					String asignatura = "";
					for (int i = 0; i < 20; i++) {
						asignatura = asignatura + file.readChar();
					}

				
			

					float nota = file.readFloat();

					System.out.printf("%10s %-25s %-25s %10s %n", indice, numalum, asignatura, nota);
					indice++;
				}

				posicion = posicion + LONGNOTAS;
				if (posicion >= file.length())
					break;

			}
			System.out.printf("%10s %-25s %-25s %10s %n", "------", "---------------", "---------------", "------"
				);

			file.close();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}



	private static void listaralumnos() throws IOException {

		File fiche = new File(fichalumnos);
		try {
			RandomAccessFile file = new RandomAccessFile(fiche, "r");

			long posicion = 0;

			System.out.printf("%10s %-25s %-25s %10s %10s %n", "NUMALUM", "NOMBRE", "LOCALIDAD", "NUMASIG", "NOTA MEDIA");
			System.out.printf("%10s %-25s %-25s %10s %10s %n", "------", "---------------", "---------------", "------",
					"---------");

			for (;;) {

				file.seek(posicion); // nos posicionamos en posicion
				int cod = file.readInt();
			
				if (cod != 0) {
					String nom = "";
					for (int i = 0; i < 20; i++) {
						nom = nom + file.readChar();
					}

					String loc = "";
					for (int i = 0; i < 20; i++) {
						loc = loc + file.readChar();
					}

					int num = file.readInt();

					float med = file.readFloat();

					System.out.printf("%10s %-25s %-25s %10s %10s %n", cod, nom, loc, num, med);
				}

				posicion = posicion + LONGALUMNOS;
				if (posicion >= file.length())
					break;

			}
			System.out.printf("%10s %-25s %-25s %10s %10s %n", "------", "---------------", "---------------", "------",
					"---------");

			file.close();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}



	private static void menu() {
		System.out.println("---------------------------------------------------");
		System.out.println("MENÚ DE OPERACIONES");
		System.out.println("1.	Ejercicio 1. Listar alumnos");
		System.out.println("2.	Ejercicio 2. Listar notas");
		System.out.println("3.	Ejercicio 3. Actualizar el fichero Alumnos");
		System.out.println("4.	Ejercicio 4. Generar el fichero Alumnos.xml");
		System.out.println("5.	Salir");
		
		System.out.println("---------------------------------------------------");
	}
}
