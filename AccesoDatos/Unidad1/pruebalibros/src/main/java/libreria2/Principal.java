package libreria2;

import java.io.File;
import java.util.ArrayList;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

public class Principal {

	public static void main(String[] args) {

		crearxml();

		// documento Librerias
		leerxml();

	}

	private static void leerxml() {

		JAXBContext context;
		try {
			context = JAXBContext.newInstance(Mislibrerias.class);
			Unmarshaller unmars = context.createUnmarshaller();
			Mislibrerias objeto = (Mislibrerias) unmars.unmarshal(new File("Librerias.xml"));
           
			// recuperar los datos de las librerias
		
			ArrayList<Libreria> listalibrerias= objeto.getLibreria();
			
			// recorrer las librerias
		     for (Libreria lib : listalibrerias) {
		    	 
		    	ArrayList <Libro> listalibros= lib.getListaLibro();
		    	String lugar=lib.getLugar();
		    	String nombre = lib.getNombre();
		    	
		    	System.out.println("Nombre libreria: "+ nombre + ", lugar: "+lugar);
		    	System.out.println("Número de libros: "+ listalibros.size());
		    	
		    	// Mostrar libros
		    	 for (Libro libro : listalibros) {
		    			System.out.println("Nombre: "+ libro.getNombre() + 
		    					", autor: "+ libro.getAutor());
			 		    		 
		    	 }
		    	
		    	
		    	 
		    	 
		     }
			
			
			
			
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void crearxml() {

		// Primera libreria
		// Se crea la lista de libros
		ArrayList<Libro> libroLista = new ArrayList<Libro>();
		// Creamos dos libros y los añadimos
		Libro libro1 = new Libro("Entornos de Desarrollo", "Alicia Ramos", "Garceta", "978-84-1545-297-3");
		libroLista.add(libro1);

		Libro libro2 = new Libro("Acceso a Datos", "Maria Jesús Ramos", "Garceta", "978-84-1545-228-7");
		libroLista.add(libro2);

		// Se crea La primera libreria y se le asigna la lista de libros
		Libreria milibreria = new Libreria();
		milibreria.setNombre("Prueba de libreria JAXB");
		milibreria.setLugar("Talavera, como no");
		milibreria.setListaLibro(libroLista);

		// Se crea La segunda libreria
		libroLista = new ArrayList<Libro>();

		libro1 = new Libro("Entornos de DesarrolloLIB2", "Alicia RamosLIB2", "Garceta", "978-84-1545-297-3");
		libroLista.add(libro1);

		libro2 = new Libro("Acceso a DatosLIB2", "Maria Jesús RamosLIB2", "Garceta", "978-84-1545-228-7");
		libroLista.add(libro2);

		Libreria milibreria2 = new Libreria();
		milibreria2.setNombre("Prueba libreria2");
		milibreria2.setLugar("Talavera, libre2");
		milibreria2.setListaLibro(libroLista);

		// creo la lista de las librerias
		Mislibrerias mislibrerias = new Mislibrerias();

		ArrayList<Libreria> lista = new ArrayList<Libreria>();
		lista.add(milibreria);
		lista.add(milibreria2);

		mislibrerias.setLibreria(lista);

		// CREAR XML
		// Creamos el contexto indicando la clase raíz
		JAXBContext context;
		try {
			context = JAXBContext.newInstance(Mislibrerias.class);

			Marshaller m = context.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			m.marshal(mislibrerias, System.out);
			m.marshal(mislibrerias, new File("Librerias.xml"));
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}

