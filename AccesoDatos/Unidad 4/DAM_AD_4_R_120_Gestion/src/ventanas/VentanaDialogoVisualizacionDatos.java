package ventanas;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.SwingConstants;

import java.awt.Color;
import java.awt.TextArea;

import javax.swing.UIManager;

import org.neodatis.odb.ODB;
import org.neodatis.odb.ODBFactory;
import org.neodatis.odb.ODBRuntimeException;
import org.neodatis.odb.ObjectValues;
import org.neodatis.odb.Objects;
import org.neodatis.odb.Values;
import org.neodatis.odb.core.query.IQuery;
import org.neodatis.odb.core.query.criteria.Where;
import org.neodatis.odb.impl.core.query.criteria.CriteriaQuery;
import org.neodatis.odb.impl.core.query.values.ValuesCriteriaQuery;

import datos.Articulos;
import datos.Clientes;
import datos.Ventas;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.math.BigDecimal;
import java.math.BigInteger;

public class VentanaDialogoVisualizacionDatos extends JDialog {
	private TextArea textArea;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			VentanaDialogoVisualizacionDatos dialog = new VentanaDialogoVisualizacionDatos();
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create the dialog.
	 */
	public VentanaDialogoVisualizacionDatos() {
		setTitle("Ejercicio Neodatis. Visualizaci\u00F3n de datos");
		setModal(true);
		setBounds(100, 100, 875, 424);
		getContentPane().setLayout(null);
	
		JButton btnVisualizarArticulos = new JButton("Visualizar Art\u00EDculos");
		btnVisualizarArticulos.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent arg0) {
					visualizararticulos();
			}
		});
		btnVisualizarArticulos.setFont(new Font("Tahoma", Font.BOLD, 13));
		btnVisualizarArticulos.setBounds(86, 47, 202, 30);
		getContentPane().add(btnVisualizarArticulos);

		JButton btnVisualizarClientes = new JButton("Visualizar Clientes");
		btnVisualizarClientes.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				visualizarclientes();
			}
		});
		btnVisualizarClientes.setFont(new Font("Tahoma", Font.BOLD, 13));
		btnVisualizarClientes.setBounds(297, 47, 202, 30);
		getContentPane().add(btnVisualizarClientes);

		JButton estadisticas = new JButton("Visualizar Estadisticas");
		estadisticas.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				
				visualizarestadisticas();
			}
		});
		estadisticas.setFont(new Font("Tahoma", Font.BOLD, 13));
		estadisticas.setBounds(510, 47, 202, 30);
		getContentPane().add(estadisticas);
		
		JLabel lblVisualizacinDeDatos = new JLabel("VISUALIZACI\u00D3N DE DATOS");
		lblVisualizacinDeDatos.setForeground(new Color(72, 61, 139));
		lblVisualizacinDeDatos.setFont(new Font("Mangal", Font.BOLD, 14));
		lblVisualizacinDeDatos.setHorizontalAlignment(SwingConstants.CENTER);
		lblVisualizacinDeDatos.setBounds(193, 11, 285, 25);
		getContentPane().add(lblVisualizacinDeDatos);

		textArea = new TextArea();
		textArea.setFont(new java.awt.Font("Courier", 1, 14));
		textArea.setBackground(new Color(221, 160, 221));
		textArea.setEditable(false);
		textArea.setBounds(32, 93, 781, 269);
		getContentPane().add(textArea);

}
///////////////////////////////////////////
protected void visualizarestadisticas() {
 /*
Nombre de art�culo/s m�s vendidos (m�s n�mero de ventas): _______________________
Media de importe de ventas por art�culo (importes por art�culo/numero de art�culos): ______________
Nombre de cliente/s que m�s ha gastado (total importe de cliente m�ximo): ______________
Nombre de cliente/s con m�s ventas (m�s n�mero de ventas): _
*/
	textArea.setText(""); // limpiamos el textarea
	ODB odb = ODBFactory.open("ARTICULOS.DAT");
	Values  groupby = odb.getValues(new ValuesCriteriaQuery(Ventas.class)
	    .field("codarti.codarti")
	    .count("codventa").groupBy("codarti.codarti" ));  
	int max =0 ;
  // calculo eml m�ximo de n�mero de ventas	
  while(groupby.hasNext()){
     ObjectValues objetos= (ObjectValues) groupby.next();  
     System.out.println( objetos.getByIndex(0)+ " * "+objetos.getByIndex(1)  );
     BigInteger conta = (BigInteger) objetos.getByIndex(1);
     if (conta.intValue() > max) max =conta.intValue(); 
  }	// fin while
 
  // vuelvo a crear la consulta
  groupby = odb.getValues(new ValuesCriteriaQuery(Ventas.class)
         .field("codarti.codarti")
         .count("codventa").groupBy("codarti.codarti" ));
 // recorro de nuevo para ver el articulo cuyo contador coincida con el m�ximo
 String cadena=String.format("%nNombre de art�culo/s m�s vendido/s (m�s n�mero de ventas= %d): ", max);
 textArea.append(cadena);
 while(groupby.hasNext()){
	    ObjectValues objetos= (ObjectValues) groupby.next();  
	    BigInteger conta = (BigInteger) objetos.getByIndex(1);
	    IQuery query = new CriteriaQuery(Articulos.class, Where.equal("codarti", objetos.getByIndex(0)));
		Articulos  arti = (Articulos) odb.getObjects(query).getFirst();
	    if (conta.intValue()== max)
	    {
	    	 textArea.append(arti.getDenom()+". ");
	    } 
	    
	}
	
	IQuery query = new CriteriaQuery(Ventas.class);  
	Objects<Ventas> objects2 = odb.getObjects(query);	    
	if (objects2.size()==0)
	  {
		  textArea.append("\nNO HAY Ventas...." );
	  }
	  else		  
	  {   
		  //media art�culos
		  mediaimportearticulos( odb);	  
		  
		  // media ventas, cada fila es una venta
		  float totalimpor=0,   nuventas = 0;
		  while (objects2.hasNext()){
				Ventas  vven= objects2.next();
				nuventas+=1;
         		totalimpor=totalimpor +
						(vven.getUniven() * vven.getCodarti().getPvp());	  
	  }	   
		  float media = totalimpor/nuventas;
		  textArea.append("\nMedia de importe de ventas (importes de las ventas / n�mero de ventas): " + media);	    
		
	  }
	  
	  // cliente que m�s ha gastado
	  clientemasgasto(odb);
	  // cliente con m�s ventas
	  clientemasventas(odb);
	  
 odb.close();	
}

private void clientemasventas(ODB odb) {
	
	  IQuery query = new CriteriaQuery(Clientes.class);
	  
	  Objects<Clientes> objects = odb.getObjects(query);
	    
	  if (objects.size()==0)
	  {
		  textArea.setText("NO HAY CLIENTES MAS VENTAS...." );
	  }
	  else
	  { 
		 int max = 0, cuen;
		 Clientes climax=null;
		 while (objects.hasNext()){
			 Clientes clien = objects.next();
			// obtengo el n�mero de ventas
			Values valores = odb.getValues(new ValuesCriteriaQuery(
					   Ventas.class, Where.equal("numcli.numcli",clien.getNumcli()))
					   .count("codventa")
			           .groupBy("numcli.numcli")
			          );
			if (valores.size() != 0)
			{			
				ObjectValues objeto1= (ObjectValues) valores.next();   
			    BigInteger contador=(BigInteger) objeto1.getByIndex(0);
				cuen = contador.intValue();
			    if (cuen > max){
			    	max=cuen;
			    }		    	
			  }	   			
		} // fin while
		// si hay varios
		 String cadena=String.format("%nCliente/s con m�s n�mero de ventas (max=%d): ", max);
		 textArea.append(cadena);
		 objects = odb.getObjects(query);
		 while (objects.hasNext()){
			 Clientes clien = objects.next();
			// obtengo el n�mero de ventas
			Values valores = odb.getValues(new ValuesCriteriaQuery(
					   Ventas.class, Where.equal("numcli.numcli",clien.getNumcli()))
					   .count("codventa")
			           .groupBy("numcli.numcli")
			          );
			if (valores.size() != 0)
			{			
				ObjectValues objeto1= (ObjectValues) valores.next();   
			    BigInteger contador=(BigInteger) objeto1.getByIndex(0);
				cuen = contador.intValue();
			    if (cuen == max)   	textArea.append( clien.getNombre() + ". ");		    	
			 }	    
		 } 
		 
					  
	  } // fin else
	 
 
	}
///////////////////////////////////////////7

private void clientemasgasto(ODB odb) {

	  IQuery query = new CriteriaQuery(Clientes.class);  
	  Objects<Clientes> objects = odb.getObjects(query);
	  float maximporte=0; 
	  Clientes cli = null;
	  
	  if (objects.size()==0)
	  {
		  textArea.setText("\nNO HAY CLIENTES...." );
	  }
	  else
	  { 
		 while (objects.hasNext()){
			 Clientes clien = objects.next();
			// obtengo el n�mero de ventas
			Values valores = odb.getValues(new ValuesCriteriaQuery(
					   Ventas.class, Where.equal("numcli.numcli",clien.getNumcli()))
					   .count("codventa")
			           .groupBy("numcli.numcli")
			          );
			System.out.println( " Hago la primera consulta. ");
			if (valores.size()== 0)
			{
				// no tiene ventas
			}
			else
			{
				// calcular el total importe, obtengo las unidades y el precio 
				// de los articulos de ese cliente
				Values datos2 = odb.getValues(new ValuesCriteriaQuery(
							 Ventas.class, Where.equal("numcli.numcli",clien.getNumcli()))
							.field("univen")
							.field("codarti.pvp")
						 );
				System.out.println( " Hago la segunda consulta. ");
				float totalimporte=0;
				if (datos2.size() == 0)
					 System.out.println( " No tiene importe de venta. ");
				else
				{    
				   while(datos2.hasNext()){
					      ObjectValues objetos= (ObjectValues) datos2.next(); 
					      int uniddes =  (int) objetos.getByIndex(0);
					      Float precio=(Float) objetos.getByIndex(1);
					      totalimporte = totalimporte + (uniddes * precio.floatValue());			       
				   }	 
		    	   //Guardo el m�ximo si el totalimporte es mayor
				   if  (totalimporte > maximporte) maximporte = totalimporte;
    
			     }	 
		    
		   } // fin else (valores.size()
			
	  			
		} // fin while
		 
		 // compruebo a ver si hay varios m�ximos
		  objects = odb.getObjects(query);
		  String cadena2=String.format("%nNombre de cliente/s con m�s importe de ventas (max= %f): ", maximporte);
		  textArea.append(cadena2);
		  while (objects.hasNext()){
			    Clientes clien = objects.next();
				Values valores = odb.getValues(new ValuesCriteriaQuery(
						   Ventas.class, Where.equal("numcli.numcli",clien.getNumcli()))
						   .count("codventa")
				           .groupBy("numcli.numcli")
				          );
				if (valores.size()== 0)
				{
					// no tiene ventas
				}
				else
				{
					
					Values datos2 = odb.getValues(new ValuesCriteriaQuery(
								 Ventas.class, Where.equal("numcli.numcli",clien.getNumcli()))
								.field("univen")
								.field("codarti.pvp")
							 );
					float totalimporte=0;
					if (datos2.size() == 0)
						 System.out.println( " No tiene importe de venta. ");
					else
					{    
					   while(datos2.hasNext()){
						      ObjectValues objetos= (ObjectValues) datos2.next(); 
						      int uniddes =  (int) objetos.getByIndex(0);
						      Float precio=(Float) objetos.getByIndex(1);
						      totalimporte = totalimporte + (uniddes * precio.floatValue());			       
					   }	 
			    	   //pregunto si el m�ximo y el totalimporte son iguales, y visualizo
					   if  (totalimporte == maximporte)
						   textArea.append( clien.getNombre() + ". ");
					} 
		 
		  }// fin else		
					  
	  } // fin while
	
	}//else objects.size()==0		  
	
}// fin metodo

/////////////////////////////////////////////
protected Articulos articulo(int codigo, ODB odb) {
	 
	  IQuery query = new CriteriaQuery(Articulos.class, 
			    Where.equal("codarti", codigo));
	  
	  Objects<Articulos> objects = odb.getObjects(query);
	    
	  if (objects.size()==0)
	  {
		  return null;
	  }
	  else
	  {
		  Articulos venta = objects.next();		  
		 return venta;		  
	  }
	  
		
	}

//////////////////////////////////////////////////////////////////	
protected void visualizarclientes() {
	try
	 {
	  ODB odb = ODBFactory.open("ARTICULOS.DAT");
	  IQuery query = new CriteriaQuery(Clientes.class);
	  
	  Objects<Clientes> objects = odb.getObjects(query);
	    
	  if (objects.size()==0)
	  {
		  textArea.setText("NO HAY CLIENTES...." );
	  }
	  else
	  { 
		 textArea.setText(" "); // limpiamos el textarea
			
		 textArea.append("NUMCLI    NOMBRE                    POBLACI�N        TOTAL IMPORTE  NUM_VENTAS\n");
		 textArea.append("================================================================================ \n");
		
		 while (objects.hasNext()){
			 Clientes clien = objects.next();
			// obtengo el n�mero de ventas
			Values valores = odb.getValues(new ValuesCriteriaQuery(
					   Ventas.class, Where.equal("numcli.numcli",clien.getNumcli()))
					   .count("codventa")
			           .groupBy("numcli.numcli")
			          );
			System.out.println( " Hago la primera consulta. ");
			if (valores.size() == 0)
			{
				System.out.println( "Cliente sin compras. ");
                String patroncito="%5d \t%-22s \t %-15s \t%8.2f \t %5d";
			     
			    String verapantalla =String.format(patroncito,clien.getNumcli(),
			               clien.getNombre(),  clien.getPobla(), 
			               0.0,0   );
			    	  
			    textArea.append(verapantalla+"\n");
			}
			else
				{
				// calcular el total importe, obtengo las unidades y el precio 
				// de los articulos de ese cliente
				Values datos2 = odb.getValues(new ValuesCriteriaQuery(
						 Ventas.class, Where.equal("numcli.numcli",clien.getNumcli()))
						.field("univen")
						.field("codarti.pvp")
					 );
			System.out.println( " Hago la segunda consulta. ");
			float totalimporte=0;
			if (datos2.size() == 0)
				 System.out.println( " No tiene importe de venta. ");
			else
			{    
			     while(datos2.hasNext()){
			      ObjectValues objetos= (ObjectValues) datos2.next(); 
			      int uniddes =  (int) objetos.getByIndex(0);
			      Float precio=(Float) objetos.getByIndex(1);
			      totalimporte = totalimporte + 
			    		   uniddes * precio.floatValue();
			       
			      }	 
			}				
				ObjectValues objeto1= (ObjectValues) valores.next();   
			    BigInteger contador=(BigInteger) objeto1.getByIndex(0);
											
				String patron="%5d \t%-22s \t %-15s \t %8.2f \t %5d";
			     
			     String datos =String.format(patron,clien.getNumcli(),
			               clien.getNombre(),
			               clien.getPobla(),
			               totalimporte,
			               contador.intValue()
			               );
			    	  
			    textArea.append(datos+"\n");
		    
			  }	 
	  			
		}
					  
	  }
	  odb.close(); 
	 
	 } 
	 catch (ODBRuntimeException  e ) {
		 textArea.setText("ERROR BD, COMPRUEBA QUE EST� CERRADA.");;
	 }	
		
	}

//////////////////////////////////////////////////////////////77
	
protected void visualizararticulos() {
	
	 try
	 {
	  ODB odb = ODBFactory.open("ARTICULOS.DAT");
	  IQuery query = new CriteriaQuery(Articulos.class);
	  
	  Objects<Articulos> objects = odb.getObjects(query);
	    
	  if (objects.size()==0)
	  {
		  textArea.setText("NO HAY Articulos...." );
	  }
	  else
	  { 
		 textArea.setText(" "); // limpiamos el textarea
			
		 textArea.append("CODARTI       DENOMINACION         STOCK         PVP    SUMA_UNIVEN     SUMA_IMPORTE     NUM_VENTAS      DESCUENTO? \n");
		 textArea.append("==================================================================================================================== \n");
		
		 while (objects.hasNext()){
			Articulos arti = objects.next();
			
			Values valores = odb.getValues(new ValuesCriteriaQuery(
					   Ventas.class, Where.equal("codarti.codarti",arti.getCodarti()))
					   .count("codventa")
			           .sum("univen")
			           .groupBy("codarti.codarti")
			          );
			if (valores.size() == 0)
			{
				 System.out.println( " Articulo sin Ventas: "+ arti.getCodarti() );
                 String patronn="%5d \t %-15s \t %5d \t %8.2f \t %6.1f \t %8.2f \t %5d \t %15s";
			     
			     String datoss =String.format(patronn,arti.getCodarti(),
			               arti.getDenom(),
			               arti.getStock(),
			               arti.getPvp(),
			               0.0,0.0,0, arti.getDescuento() );
			    	  
			    textArea.append(datoss+"\n");
				 
				 
			}
			else
				{
				ObjectValues objeto1= (ObjectValues) valores.next();   
			    BigInteger contador=(BigInteger) objeto1.getByIndex(0);
				BigDecimal suma=(BigDecimal) objeto1.getByIndex(1);
				
			
				float totalimporte = suma.floatValue() * arti.getPvp();
				
				String patron="%5d \t %-15s \t %5d \t %8.2f \t %6.1f \t %8.2f \t %5d";
			     
			     String datos =String.format(patron,arti.getCodarti(),
			               arti.getDenom(),
			               arti.getStock(),
			               arti.getPvp(),
			               suma.floatValue(),
			               totalimporte,
			               contador.intValue()
			               );
			    	  
			    textArea.append(datos+"\n");
   
			  }	 		  			
		}
					  
	  }
	  odb.close(); 	 
	 } 
	 catch (ODBRuntimeException  e ) {
		 textArea.setText("ERROR BD, COMPRUEBA QUE EST� CERRADA.");;
	 }
		
	}
/////////////////////////////////////////////////////////////////
protected void mediaimportearticulos(ODB odb) {
	  
	  IQuery query = new CriteriaQuery(Articulos.class);
	  Objects<Articulos> objects = odb.getObjects(query);
	    
	  if (objects.size()==0)
	  {
		  textArea.setText("NO HAY Articulos...." );
	  }
	  else
	  { 
		 int numartic=0;
		 float totalimptodosartic =0, media=0;
		 while (objects.hasNext()){
			Articulos arti = objects.next();
			numartic+=1;
			Values valores = odb.getValues(new ValuesCriteriaQuery(
					   Ventas.class, Where.equal("codarti.codarti",arti.getCodarti()))
			           .sum("univen")
			           .groupBy("codarti.codarti")
			          );
			float totalimporte =0;
			if (valores.size() != 0) // solo habr� una fila
			{
				ObjectValues objeto1= (ObjectValues) valores.next();   
				BigDecimal suma=(BigDecimal) objeto1.getByIndex(0);				
				totalimporte = suma.floatValue() * arti.getPvp();
				totalimptodosartic = totalimptodosartic + totalimporte;
			  }	 		  			
		}// fin whilwe
	 
	   media = totalimptodosartic/numartic;
	   textArea.append("\nMedia de importe de ventas por art�culo (importes por art�culo/numero de art�culos): " + media);	
					  
	  }
	
	 } 
	
	}
//////////////////////////////////7
