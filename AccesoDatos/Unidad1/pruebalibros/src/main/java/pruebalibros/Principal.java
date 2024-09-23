package pruebalibros;

import java.io.File;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;


public class Principal {


	private static final String MIARCHIVO_XML = "./unlibro.xml";


	public static void main(String[] args) {
		Libro milibro = new Libro("Entornos de Desarrollo", 
                "Alicia Ramos","Garceta","978-84-1545-297-3" );	
		try {
			JAXBContext context = JAXBContext.newInstance(Libro.class);
			Marshaller m = context.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			m.marshal(milibro, System.out);
			m.marshal(milibro, new File(MIARCHIVO_XML));
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


}
