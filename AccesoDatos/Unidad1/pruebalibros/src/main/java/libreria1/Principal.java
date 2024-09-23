package libreria1;

import java.util.ArrayList;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

public class Principal {

    private static final String MIARCHIVO_XML = "./libreria.xml";
    
	public static void main(String[] args) {
		
        //Se crea la lista de libros
        ArrayList<Libro> libroLista = new ArrayList<Libro>();
        
        // Creamos dos libros y los añadimos
        Libro libro1 = new Libro("Entornos de Desarrollo", 
                 "Alicia Ramos","Garceta","978-84-1545-297-3" );
        libroLista.add(libro1);
       
        Libro libro2 = new Libro("Acceso a Datos","Maria Jesús Ramos",
                 "Garceta","978-84-1545-228-7" );
        libroLista.add(libro2);
 
        // Se crea La libreria y se le asigna la lista de libros
        Libreria milibreria = new Libreria();
        milibreria.setNombre("Prueba de libreria JAXB");
        milibreria.setLugar("Talavera, como no");
        milibreria.setListaLibro(libroLista);

        // CREAR XML
        // Creamos el contexto indicando la clase raíz
        JAXBContext context;
		try {
			context = JAXBContext.newInstance(Libreria.class);
			//Creamos el Marshaller, convierte el java bean en una cadena XML
	        Marshaller m = context.createMarshaller();
	       //Formateamos el xml para que quede bien
	        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
	        // Lo visualizamos con system out
	        m.marshal(milibreria, System.out);
	         // Escribimos en el archivo
	        m.marshal(milibreria, new File(MIARCHIVO_XML));

		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        
	}

}
