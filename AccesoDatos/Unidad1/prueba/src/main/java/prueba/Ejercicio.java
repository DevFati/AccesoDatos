package prueba;



import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.io.RandomAccessFile;

import java.util.Scanner;



public class Ejercicio {

public static void main(String[] args) throws IOException {

	Scanner teclado=new Scanner(System.in);

	int opcion=0;

	String nombre="Practica";

	 final int LONG=72;

	

	do {

		menu();

		 opcion=teclado.nextInt();

		 switch(opcion) {

		 case 1: 

			 System.out.println("Introduce el nombre del fichero: ");

			  

			 try {

				System.out.println(crearFichero(nombre));

			} catch (IOException e) {

				// TODO Auto-generated catch block

				e.printStackTrace();

			}

			 

			 break;

		 case 2: 

			 System.out.println("Introduce el codigo de departamento: ");

			 int dep=teclado.nextInt();

			 try {

				if(consultarregistro(dep)) {

					 System.out.println("Existe el departamento"+dep);

				 }else {

					 System.out.println("No existe el departamento: "+dep);

				 }

			} catch (IOException e) {

				// TODO Auto-generated catch block

				e.printStackTrace();

			}

			 

			 break; 

		 case 3: 

			 System.out.println("Introduce un código de departamento: ");

			 int dept=teclado.nextInt();

			 System.out.println("Introduce un nombre: ");

			 String nom=teclado.next();

			 System.out.println("Introduce una localidad: ");

			 String loc=teclado.next();

			 System.out.println("Numero de empleados: ");

			 int num=teclado.nextInt();

			 System.out.println("Media salario: ");

			 float sal=teclado.nextFloat();

			 try {
				System.out.println(insertarregistro(dept,nom,loc,num,sal));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			 

			 break;
		 case 4: 
			 System.out.println("Introduce un codigo de departamento: ");
			 int dept4=teclado.nextInt();
			 
					visualizaregistro(dept4);
				
			 
			 break;
		 case 5: 
			 System.out.println("Introduce un codigo de departamento: ");
			 int dep5=teclado.nextInt();
			 System.out.println("Introduce una localidad:  ");
			  loc=teclado.next();
			 System.out.println("Introduce la media de salario");
			 sal=teclado.nextFloat();
			 modificaregistro(dep5,loc,sal);
			 break;
			 
		 case 6: 
			 listar();

		 }

		

	}while(opcion!=0);

}



private static void listar() throws IOException {
	
	File fichero = new File(".\\Practica.dat");
	
	RandomAccessFile file = new RandomAccessFile(fichero, "r"); //r significa que esta en modo lectura 
	//
	int cod, num, posicion;
	float media;
	char nombre[] = new char[15], aux;
	char localidad[] = new char[15], aux1;

	posicion = 0; // para situarnos al principio

	for (;;) { // recorro el fichero
		file.seek(posicion); // nos posicionamos en posicion
		cod = file.readInt(); // obtengo id de empleado
		for (int i = 0; i < nombre.length; i++) {
			aux = file.readChar();// recorro uno a uno los caracteres del apellido
			nombre[i] = aux; // los voy guardando en el array
		}
		String nombreS = new String(nombre);// convierto a String el array
		
		for (int i = 0; i < localidad.length; i++) {
			aux = file.readChar();// recorro uno a uno los caracteres del apellido
			localidad[i] = aux; // los voy guardando en el array
		}
		String localidadS = new String(localidad);// convierto a String el array
		num = file.readInt();// obtengo dep
		// dep=0;
		media = file.readFloat(); // obtengo salario

		// salario=0d;
		System.out.println(
				"COD_DEP: " + cod + ", Nombre: " + nombreS + ", Localidad: " + localidadS + ", Numero_Empleados: " + num+", Media_Salario"+media);
		posicion = posicion + 72; // me posiciono para el sig empleado
									// Cada empleado ocupa 36 bytes (4+20+4+8)
		// Si he recorrido todos los bytes salgo del for
		if (posicion >= file.length() || file.getFilePointer() == file.length())
			break; //Basicamente lo que hago aqui es ver si la posicion en la que estoy es igual al tamaño del fichero, de ser asi. Salgo

	} // fin bucle for
	file.close(); // cerrar fichero
}



private static String modificaregistro(int dept, String loc, float sal) throws IOException {
	String mensaje="";
	
 if(consultarregistro(dept)) {
		
		File fichero = new File(".\\Practica.dat");
		try {
			RandomAccessFile file = new RandomAccessFile(fichero, "rw");
			long posicion=(dept-1)*72;
			
				file.seek(posicion+30+4); // nos posicionamos
				//posicion=posicion+(15*2)+4;
				//file.seek(posicion);
				StringBuffer buffer = null;				
						buffer = new StringBuffer( loc );      
						      buffer.setLength(15); 
					file.writeChars(buffer.toString());
					posicion=posicion+4;
					file.seek(posicion);
					file.writeFloat(sal); 
				
		
			file.close(); // cerrar fichero
			mensaje="Registro "+dept+" modificado";
			
			
					} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace(); 
			mensaje="Error registro no se puede modificar";
					
					}
	}else {
		mensaje="Error registro no se puede modificar";
	}
 return mensaje;
}



private static void visualizaregistro(int dept4) {
	try {
		if(consultarregistro(dept4)) {
			
			int dep,num;
			String nom,loc;
			float sal;
			File fichero = new File(".\\Practica.dat");
		
			try {
				RandomAccessFile file = new RandomAccessFile(fichero, "rw");
				long posicion=(dept4-1)*72;
					file.seek(posicion); 
					 dep=file.readInt();
					if(dep==dept4) {
						file.seek(posicion);
						dep =file.readInt();
						 nom = "";
						for (int i = 0; i < 15; i++) {
							nom = nom + file.readChar();
						}
						
						 loc = "";
						for (int i = 0; i < 15; i++) {
							loc = loc + file.readChar();
						}
						

						num=file.readInt();
						sal=file.readFloat();
						
				System.out.println("COD_DEPT: "+dep+"   NOMBRE: "+nom+"   LOCALIDAD: "+loc+"   "+"   NUM_EMPLEADOS: "+num+"   MEDIA_SALARIO: "+sal);		
					
				}else {
					System.out.println("Codigo de departamento "+dept4+" no existe, es un hueco");
				}
				

				
				file.close(); // cerrar fichero
				
				
				
						} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace(); 
						}
			
			
			
			
		}else {
			System.out.println("Departamento no existe");
		}
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	return;
}



private static String insertarregistro(int dept, String nom, String loc, int num, float sal) throws IOException {
	String mensaje="";
	if(dept<1 || dept>100) {
		mensaje="Error el departamento debe estar entre 1 y 100";
		
	}else if(consultarregistro(dept)) {
		mensaje="Error el departamento ya existe no se puede insertar";
	}else {
		
		File fichero = new File(".\\Practica.dat");
		try {
			RandomAccessFile file = new RandomAccessFile(fichero, "rw");
			long posicion=(dept-1)*72;
				file.seek(posicion); // nos posicionamos
				file.writeInt(dept);
				StringBuffer buffer = null;
				
				buffer = new StringBuffer( nom );      
				      buffer.setLength(15); 
				      file.writeChars(buffer.toString());
				 						
						buffer = new StringBuffer( loc );      
						      buffer.setLength(15); 
					file.writeChars(buffer.toString());
					file.writeInt(num); 
					file.writeFloat(sal); 
				
		
			file.close(); // cerrar fichero
			mensaje="Registro insertado";
			
			
					} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace(); 
			
			mensaje="Error registro no insertado";
					
					}
	}
	return mensaje;

}



private static boolean consultarregistro(int dep) throws IOException {

	File fichero= new File(".\\Practica.dat");

	RandomAccessFile file=new RandomAccessFile(fichero,"r");
		boolean existe;
	if(fichero.length()!=0) {
		int posicion =0; 

		int ident=0;

		file.seek((dep-1)*72);

		ident=file.readInt();

		if(dep==ident) {
			
			existe= true;

		}else {

			existe= false;

		}
	}else {
		existe= false;
	}
	
	file.close();
	
	return existe;

}



private static String crearFichero(String nombre) throws IOException {

	String mensaje="";

	File f=new File(".\\"+nombre+".dat");

	if(f.exists()) {

		mensaje="El fichero ya existe";

	}else {

		f.createNewFile();

		mensaje="El fichero se ha creado correctamente";

		

	}

	
	
	

	return mensaje;

	

}



private static void menu() {

	System.out.println("MENU: ");

	System.out.println("Opcion 1: Crear fichero");

	System.out.println("Opcion 2: Consultar registro");

	System.out.println("Opcion 3: Insertar registro");
	System.out.println("Opcion 4: Visualizar registro");
	System.out.println("Opcion 5: Modificar resgistro");
	System.out.println("Opcion 6: Listar");
	System.out.println("Opcion 0: Salir");

	System.out.println("Elige una opcion");

	

	

}

}

