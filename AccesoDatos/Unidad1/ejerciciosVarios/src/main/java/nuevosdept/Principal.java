 package nuevosdept;

import java.io.File;
import java.util.ArrayList;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;


public class Principal {

	public static void main(String[] args) {
		JAXBContext context;
		try {
			context = JAXBContext.newInstance(Nuevosdepartamentos.class);
			Unmarshaller unmars = context.createUnmarshaller();
			Nuevosdepartamentos objeto = (Nuevosdepartamentos) unmars.unmarshal(new File(".\\NuevosDep.xml"));
	           
			ArrayList<Departamento> lista = objeto.getLista();
			
		     for (Departamento dep : lista) {
		    	 
		    		System.out.println("Cod: "+ dep.getDeptno() + 
	    					", nombre: "+ dep.getDnombre()+
	    					", localidad: "+ dep.getLocalidad());
		 		    		 
		    	 
		    	 
		     }
	
			
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
