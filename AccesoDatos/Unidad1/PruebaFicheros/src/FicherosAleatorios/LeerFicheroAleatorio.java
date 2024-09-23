package FicherosAleatorios;

import java.io.*;

public class LeerFicheroAleatorio {

	public static void main(String[] args) {
		try {
			leerfichero();
			System.out.println("---------------------");
			consultarregistro(5); // reg existe
			System.out.println("---------------------");
			consultarregistro(10); // reg no existe
			// id, apellido,dep, salario

			System.out.println("---------------------");
			insertarregistro(6, "NUEVO 6", 10, 1000d); // Ya existe id
			System.out.println("---------------------");
			insertarregistro(20, "NUEVO 20", 10, 1000d); // Se inserta
			System.out.println("---------------------");

			insertarregistro(10, "NUEVO 10", 10, 1000d); // Hueco, se inserta

			System.out.println("---------------------");
			insertarregistro(15, "NUEVO 15", 15, 1000d); // Hueco, se inserta

			System.out.println("---------------------");
			System.out.println("------MODIFICAR---------------");

			// Sumar subida al salario de los emples de un dep
			System.out.println("------MODIFICAR EMPLES DE UN DEP---------------");
			modificartodoslosdeldep(10, 100d);

			modificartodoslosdeldep(100, 100d);
			
			System.out.println("------MODIFICAR UN EMPLE---------------");
			// modif un empleado sumar 100 a su salario
			modificaremple(5); // ok

			modificaremple(35); // No existe

			modificaremple(16); // No existe, es hueco
			
			
			System.out.println("--------------BORRAR---------------");
			borraemple(3); //ok 
			borraemple(35); //no existe, sobrepasa
			borraemple(16); //es hueco, no lo borra
			
			
			System.out.println("--------------LISTAR---------------");
			
			
			
			leerfichero();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static void borraemple(int identificador) throws IOException {
		
		// leer secuencialmente todos
					File fichero = new File(".\\AleatorioEmple.dat");
					// declara el fichero de acceso aleatorio
					try {
						RandomAccessFile file = new RandomAccessFile(fichero, "rw");
						long posicion=(identificador-1)*36;
						if(posicion>=file.length()) {
							System.out.println("ID "+identificador+"NO EXISTE EL EMPLEADO (sobrepasa)....");
						}else {
							file.seek(posicion); // nos posicionamos
							int id=file.readInt();
							if(id==identificador) {
								System.out.println("ID "+identificador+" existe, borrado logico ");
								file.seek(posicion); //se hace esto para poder modificar el id a 0, ya que antes se leyo el id para ver si coincidia con el proporcionado por el usuario
								//asi que la posicion ya no estaria en 0, por eso se vuelve a usar 
								//ponems a 0 el id
								file.writeInt(0);
								String apellido="          "; //fijar a 10 caraccteres
								file.writeChars(apellido); //insertar apellido
								file.writeInt(0); //insertar departamento
								file.writeDouble(0); //insertar salario
							
						}else {
							System.out.println("ID "+id+" no existe, es un hueco");
						}
						

						} // fin bucle for
						file.close(); // cerrar fichero
						
						
						
								} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace(); 
								}
		
	}

	/*private static void modificaremple(int identificador) throws IOException {
		
		// leer secuencialmente todos
				File fichero = new File(".\\AleatorioEmple.dat");
				// declara el fichero de acceso aleatorio
				try {
					RandomAccessFile file = new RandomAccessFile(fichero, "rw");
					int id, dep, posicion;
					Double salario;
					char apellido[] = new char[10], aux;
					posicion = 0; // para situarnos al principio
		            int contador=0;
					for (;;) { // recorro el fichero
						file.seek(posicion); // nos posicionamos en posicion
						id = file.readInt(); // obtengo id de empleado
						for (int i = 0; i < apellido.length; i++) {
							aux = file.readChar();// recorro uno a uno los caracteres del apellido
							apellido[i] = aux; // los voy guardando en el array
						}
						String apellidoS = new String(apellido);// convierto a String el array
									
						dep = file.readInt();// obtengo dep
						
						if (id==identificador) {
							//actualizar
							salario = file.readDouble(); // obtengo salario
		                    salario=salario+100;
		                    file.seek(posicion+4+20+4); 
		                    file.writeDouble(salario);
		                    System.out.println(
		    						"ID actualizado: " + id + ", Apellido: " 
		                    + apellidoS + ", Departamento: " + dep + ", Salario: " + salario);
		                    contador=contador+1;
						}
						
						posicion = posicion + 36; 
						// Si he recorrido todos los bytes salgo del for
						if (posicion >= file.length() || file.getFilePointer() == file.length())
							break;

					} // fin bucle for
					file.close(); // cerrar fichero
					
					
					
							} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}*/
	
	
	
	
	
	private static void modificaremple(int identificador) throws IOException {
	
	// leer secuencialmente todos
			File fichero = new File(".\\AleatorioEmple.dat");
			// declara el fichero de acceso aleatorio
			try {
				RandomAccessFile file = new RandomAccessFile(fichero, "rw");
				long posicion=(identificador-1)*36;
				if(posicion>=file.length()) {
					System.out.println("ID "+identificador+"NO EXISTE EL EMPLEADO....");
				}else {
					file.seek(posicion); // nos posicionamos
					int id=file.readInt();
					if(id==identificador) {
						System.out.println("ID "+identificador+" existe, actualizo salario");
						file.seek(posicion+4+20+4); //se posiciono en salario
						Double sal=file.readDouble();
						sal=sal+100;
						file.seek(posicion+4+20+4);
						file.writeDouble(sal);
					
				}else {
					System.out.println("ID "+id+" no existe, es un hueco");
				}
				

				} // fin bucle for
				file.close(); // cerrar fichero
				
				
				
						} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

			

	private static void modificartodoslosdeldep(int depart, double subida) throws IOException {
		// leer secuencialmente todos
		File fichero = new File(".\\AleatorioEmple.dat");
		// declara el fichero de acceso aleatorio
		try {
			RandomAccessFile file = new RandomAccessFile(fichero, "rw");
			int id, dep, posicion;
			Double salario;
			char apellido[] = new char[10], aux;
			posicion = 0; // para situarnos al principio
            int contador=0;
			for (;;) { // recorro el fichero
				file.seek(posicion); // nos posicionamos en posicion
				id = file.readInt(); // obtengo id de empleado
				for (int i = 0; i < apellido.length; i++) {
					aux = file.readChar();// recorro uno a uno los caracteres del apellido
					apellido[i] = aux; // los voy guardando en el array
				}
				String apellidoS = new String(apellido);// convierto a String el array
							
				dep = file.readInt();// obtengo dep
				
				if (dep==depart) {
					//actualizar
					salario = file.readDouble(); // obtengo salario
                    salario=salario+subida;
                    file.seek(posicion+4+20+4); 
                    file.writeDouble(salario);
                    System.out.println(
    						"ID actualizado: " + id + ", Apellido: " 
                    + apellidoS + ", Departamento: " + dep + ", Salario: " + salario);
                    contador=contador+1;
				}
				
				posicion = posicion + 36; 
				// Si he recorrido todos los bytes salgo del for
				if (posicion >= file.length() || file.getFilePointer() == file.length())
					break;

			} // fin bucle for
			file.close(); // cerrar fichero
			
			  System.out.println("Se han actualizado: "+contador +
						" empleados del dep: "+depart);
			
					} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static void insertarregistro(int id, String apellido, int dep, double salario) throws IOException {

		File fiche = new File(".\\AleatorioEmple.dat");
		// declara el fichero de acceso aleatorio
		try {
			RandomAccessFile file = new RandomAccessFile(fiche, "rw");
			long posicion = (id - 1) * 36; // calculamos la posicion

			if (posicion >= file.length()) {
				// id no existe, se añade
				System.out.println("ID: " + id + ", NO EXISTE. Se inserta:");
				file.seek(posicion); // nos posicionamos
				file.writeInt(id); // se escribe id
				StringBuffer buffer = new StringBuffer(apellido);
				buffer.setLength(10); // 10 caracteres para el apellido
				file.writeChars(buffer.toString());// insertar apellido

				file.writeInt(dep); // insertar departamento

				file.writeDouble(salario);// insertar salario

			}

			else { // ident existe o es un hueco
				System.out.println("ID: " + id + ", EXISTE O ES HUECO.");
				// comprobamos el id
				file.seek(posicion);
				int iden = file.readInt();
				if (iden == 0) {
					// id es hueco, nuevo registro se graba
					file.seek(posicion);
					file.writeInt(id); // se escribe id

					StringBuffer buffer = new StringBuffer(apellido);
					buffer.setLength(10); // 10 caracteres para el apellido
					file.writeChars(buffer.toString());// insertar apellido

					file.writeInt(dep); // insertar departamento

					file.writeDouble(salario);// insertar salario
					System.out.println("ID: " + id + ", ES HUECO, SE INSERTA.");

				} else {
					// id ya existe, escribimos mensaje
					System.out.println("ID: " + id + ", YA EXISTE. NO SE INSERTA.");

				}

			}
			file.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

	}

	private static void consultarregistro(int identificador) throws IOException {

		File fichero = new File(".\\AleatorioEmple.dat");
		// declara el fichero de acceso aleatorio
		try {
			RandomAccessFile file = new RandomAccessFile(fichero, "r");

			int posicion = (identificador - 1) * 36; // calculo donde empieza el registro
			if (posicion >= file.length())
				System.out.println("ID: " + identificador + ", NO EXISTE EMPLEADO...");
			else {
				// nos posicionamos, leemos y mostramos
				System.out.println("ID: " + identificador + " LOCALIZADO:");
				file.seek(posicion);// nos posicionamos
				int id = file.readInt(); // obtengo id de empleado
				String ape = "";
				for (int i = 0; i < 10; i++) {
					ape = ape + file.readChar();// recorro uno a uno los caracteres del apellido
				}
				int dep = file.readInt();// obtengo dep
				Double salario = file.readDouble(); // obtengo salario

				System.out.println(
						"ID: " + id + ", Apellido: " + ape + ", Departamento: " + dep + ", Salario: " + salario);

				file.close();

			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

	}

	public static void leerfichero() throws IOException {
		File fichero = new File(".\\AleatorioEmple.dat");
		// declara el fichero de acceso aleatorio
		RandomAccessFile file = new RandomAccessFile(fichero, "r"); //r significa que esta en modo lectura 
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
				break; //Basicamente lo que hago aqui es ver si la posicion en la que estoy es igual al tamaño del fichero, de ser asi. Salgo

		} // fin bucle for
		file.close(); // cerrar fichero
	}
}
