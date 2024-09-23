package verDir;

import java.io.File;

public class VerDir {
	public static void main(String[] args) {
		String dir="."; //El punto se refiere al directorio actual desde donde 
		// se esta ejecutando el programa. 
		File f=new File(dir);
		//creo un objeto File llamado f que representa el directorio actual 
		//(.). La clase File se usa para interactuar con archivos y directorios
		String archivos[]=f.list();
		//LLama al metodo list() del objeto f. Este metodo devuelve un 
		//array de tipo String con los nombres de todos los archivos 
		// y subdirectorios en el directorio representado por f. La lista se 
		//almacena en el array archivos. 
		System.out.printf("Ficheros en el directorio actual: %d %n",
                archivos.length);
		//imprime en la consola el numero de archivos y directorios contenidos en 
		//el directorio actual. Archivos.lenght devuelve el numero de elementos en el array 
		// de archivos. 
			for (int i = 0; i < archivos.length; i++) {
		//Inicia el bucle for que recorre todos los elementos del array de archivos.
		
				File f2 = new File(f, archivos[i]);
		//Crea un objeto File llamado f2 que representa el archivo o subdirectorio en la posicion i
				// del array archivos. El constructor new File(f,archivos[i]) utiliza f como directorio 
				//base y archivos[i] como nombre del archivo o subdirectorio. 
				System.out.printf("Nombre: %s, es fichero?: %b, es directorio?: %b %n", archivos[i], f2.isFile(),f2.isDirectory());
			}
		}
}
