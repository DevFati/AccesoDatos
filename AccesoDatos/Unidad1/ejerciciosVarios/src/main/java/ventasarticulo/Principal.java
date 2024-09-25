package ventasarticulo;

import java.io.File;
import java.util.ArrayList;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;


public class Principal {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		JAXBContext context;
		try {
			context = JAXBContext.newInstance(Ventasarticulos.class);
			Unmarshaller unmars = context.createUnmarshaller();
			Ventasarticulos objeto = (Ventasarticulos) unmars.unmarshal(new File(".\\ventasarticulos.xml"));
	
			Articulo articulo= objeto.getArticulo();
		    ArrayList<Venta> listaventas = objeto.getListaventas();
		    
		    
		    System.out.println("Código: " +articulo.getCodigo()+
		    		"   Nombre: " + articulo.getDenominacion() + 
		    		"   PVP: " + articulo.getPrecio());
		    System.out.println("Número de ventas: " + listaventas.size());
		    
		    
		    System.out.printf("%8s %11s %25s %8s %8s%n","NUMVENTA","FECHA VENTA","NOM-CLIENTE", "UNIDADES","IMPORTE");
		    System.out.printf("%8s %11s %25s %8s %8s%n","--------","-----------","------------------------", "--------","--------");
			float sumaimporte=0f;
			int sumauni=0;
			
		    for (Venta v: listaventas ) {
		    	float imp= v.getUnidades() * articulo.getPrecio();
		    	System.out.printf("%8s %11s %25s %8s %8s%n",
		    			 v.getNumventa(),v.getFecha(),v.getNombrecliente(),
		    			 v.getUnidades(),imp);
		  		
		    	sumauni=sumauni+v.getUnidades();
		    	sumaimporte=sumaimporte+imp;
		    	
		    }
		    //visualizar lineas de pie
		    System.out.printf("%8s %11s %25s %8s %8s%n","--------","-----------","------------------------", "--------","--------");
		    System.out.printf("%8s %11s %25s %8s %8s%n","TOTALES: ","  ","  ",sumauni,sumaimporte);

		   	
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}