package ventanas;

import java.awt.EventQueue;

import javax.swing.JDialog;
import javax.swing.JLabel;

import java.awt.Font;

import javax.swing.JPanel;

import java.awt.Color;

import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.JButton;

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

import java.awt.TextArea;

import datos.Ventas;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigDecimal;

public class VentanaDialogoArticc  extends JDialog  {
	private JTextField codarticulo;
	JTextField denominacionartic;
	JTextField pvpartic;
	JLabel Mensaje;
	TextArea textArea;
	JTextField stockartic;
	int codarti;
	String denom; 
	int stock; 
	float pvp;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					VentanaDialogoArticc dialog = new VentanaDialogoArticc();
					dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
					dialog.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

//////////////////////////////////////////////////////////////////////////////////
	public VentanaDialogoArticc() {
		setModal(true);
		setTitle("Ejercicio Neodatis. Mantenimiento articulos");
		setBounds(100, 100, 1107, 358);
		getContentPane().setLayout(null);
		
		JLabel lblEntradaDeVentas = new JLabel("ENTRADA DE DATOS DE ART\u00CDCULOS");
		lblEntradaDeVentas.setHorizontalAlignment(SwingConstants.CENTER);
		lblEntradaDeVentas.setFont(new Font("Tahoma", Font.BOLD, 13));
		lblEntradaDeVentas.setBounds(102, 11, 259, 22);
		getContentPane().add(lblEntradaDeVentas);
		
		JPanel panel = new JPanel();
		panel.setBackground(new Color(255, 218, 185));
		panel.setBounds(33, 55, 426, 143);
		getContentPane().add(panel);
		panel.setLayout(null);
		
		JLabel lblCdigodeart = new JLabel("C\u00F3digo de art\u00EDculo:");
		lblCdigodeart.setForeground(new Color(0, 0, 128));
		lblCdigodeart.setBounds(23, 11, 156, 15);
		panel.add(lblCdigodeart);
		lblCdigodeart.setFont(new Font("Tahoma", Font.BOLD, 12));
		
		codarticulo = new JTextField();
		codarticulo.setHorizontalAlignment(SwingConstants.CENTER);
		codarticulo.setFont(new Font("Tahoma", Font.BOLD, 11));
		codarticulo.setBounds(177, 9, 63, 24);
		panel.add(codarticulo);
		codarticulo.setColumns(10);
		
		denominacionartic = new JTextField();
	
		denominacionartic.setFont(new Font("Tahoma", Font.BOLD, 11));
		denominacionartic.setHorizontalAlignment(SwingConstants.CENTER);
		denominacionartic.setBounds(147, 44, 190, 26);
		panel.add(denominacionartic);
		denominacionartic.setColumns(10);
		
		pvpartic = new JTextField();
		pvpartic.setFont(new Font("Tahoma", Font.BOLD, 11));
		pvpartic.setHorizontalAlignment(SwingConstants.CENTER);
		pvpartic.setBounds(123, 75, 86, 26);
		panel.add(pvpartic);
		pvpartic.setColumns(10);
		
		JButton btnConsultar = new JButton("Consultar");
		btnConsultar.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Mensaje.setText("");
				try{	
				    codarti = Integer.parseInt(codarticulo.getText());
				    consultar();
				}catch (NumberFormatException er)
				{
					Mensaje.setText("C�digo de art�culo err�neo. ");
					denominacionartic.setText("");
					pvpartic.setText("");
					stockartic.setText("");
				}

			}
		});
	
		btnConsultar.setBounds(272, 8, 112, 23);
		panel.add(btnConsultar);
		
		JLabel lblPvp = new JLabel("PVP:");
		lblPvp.setHorizontalAlignment(SwingConstants.CENTER);
		lblPvp.setFont(new Font("Tahoma", Font.BOLD, 12));
		lblPvp.setBounds(35, 77, 46, 14);
		panel.add(lblPvp);
		
		JLabel lbstock = new JLabel("STOCK:");
		lbstock.setHorizontalAlignment(SwingConstants.CENTER);
		lbstock.setFont(new Font("Tahoma", Font.BOLD, 12));
		lbstock.setBounds(35, 109, 46, 14);
		panel.add(lbstock);
		
		stockartic = new JTextField();
		stockartic.setHorizontalAlignment(SwingConstants.CENTER);
		stockartic.setFont(new Font("Tahoma", Font.BOLD, 11));
		stockartic.setColumns(10);
		stockartic.setBounds(111, 106, 86, 26);
		panel.add(stockartic);
		
		JLabel lblDenominacin = new JLabel("Denominaci\u00F3n:");
		lblDenominacin.setHorizontalAlignment(SwingConstants.CENTER);
		lblDenominacin.setFont(new Font("Tahoma", Font.BOLD, 12));
		lblDenominacin.setBounds(35, 37, 101, 30);
		panel.add(lblDenominacin);
		
		Mensaje = new JLabel("-------------------------------------------------------");
		Mensaje.setFont(new Font("Tahoma", Font.BOLD, 11));
		Mensaje.setForeground(new Color(255, 0, 0));
		Mensaje.setHorizontalAlignment(SwingConstants.CENTER);
		Mensaje.setBounds(21, 209, 475, 37);
		getContentPane().add(Mensaje);
		
		JButton btnInsertarArtic = new JButton("Insertar art\u00EDculo");
		btnInsertarArtic.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {		
				if(validardatos())
				      insertararticulo();
			}
		});
	
		btnInsertarArtic.setFont(new Font("Tahoma", Font.BOLD, 11));
		btnInsertarArtic.setBounds(33, 246, 137, 23);
		getContentPane().add(btnInsertarArtic);
		
		JButton Borrarartic = new JButton("Borrar art\u00EDculo");
		Borrarartic.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				if (validardatos())
				     modificarborrarartic(1); // 1 borra
			}
		});
	
		Borrarartic.setFont(new Font("Tahoma", Font.BOLD, 11));
		Borrarartic.setBounds(180, 246, 137, 23);
		getContentPane().add(Borrarartic);
		
		JButton btnModificarArt = new JButton("Modificar art\u00EDculo");
		btnModificarArt.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (validardatos())
				     modificarborrarartic(0); // 0 modifica
			}
		});
	
		btnModificarArt.setFont(new Font("Tahoma", Font.BOLD, 11));
		btnModificarArt.setBounds(327, 246, 147, 22);
		getContentPane().add(btnModificarArt);
		
		textArea = new TextArea();
		textArea.setEditable(false);
		textArea.setBackground(new Color(255, 255, 102));
		textArea.setBounds(515, 40, 568, 256);
		textArea.setFont(new java.awt.Font("Courier", 1, 14));
		getContentPane().add(textArea);
		
		JButton btnVisualizarArt = new JButton("Visualizar Art\u00EDculos");
		btnVisualizarArt.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				visualizararticulos();
			}
		});
	
		btnVisualizarArt.setFont(new Font("Tahoma", Font.BOLD, 12));
		btnVisualizarArt.setBounds(515, 11, 184, 23);
		getContentPane().add(btnVisualizarArt);
		
		JButton btnLimpiarPantalla = new JButton("Limpiar pantalla");
		btnLimpiarPantalla.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				limpiarpantalla();		
			}
		});
		btnLimpiarPantalla.setFont(new Font("Tahoma", Font.BOLD, 12));
		btnLimpiarPantalla.setBounds(899, 11, 184, 23);
		getContentPane().add(btnLimpiarPantalla);
		
		JButton btnIrAVentas = new JButton("Ir a Ventas");
		btnIrAVentas.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				VentanaDialogoVentas dialog = new VentanaDialogoVentas();
				dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
				dialog.setVisible(true);
			}
		});
		btnIrAVentas.setFont(new Font("Tahoma", Font.BOLD, 12));
		btnIrAVentas.setBounds(744, 12, 122, 23);
		getContentPane().add(btnIrAVentas);
}
//////////////////////////////////////////////////////////////////////////////////
protected boolean validardatos() {
	Mensaje.setText("");
	String mensaje="";
	boolean OK = true;
	try{	
	    codarti = Integer.parseInt(codarticulo.getText());
	}catch (NumberFormatException e)
	{
		mensaje ="C�digo de art�culo err�neo. ";
	}
	
    denom = denominacionartic.getText();
    
	try{
		stock = Integer.parseInt(stockartic.getText());
     }catch (NumberFormatException e)
    	{
    	 mensaje = mensaje + "Stock de art�culo err�neo. ";
	    }
	try{
	     pvp = Float.parseFloat(pvpartic.getText());
	  }catch (NumberFormatException e)
		{
		  mensaje = mensaje + "Precio de art�culo err�neo. ";
		}
	if (mensaje.compareTo("")!=0)    OK=false;
	Mensaje.setText(mensaje);	
	return OK;
}

//////////////////////////////////////////////////////////////////////////////////

public void insertararticulo() {

	  ODB odb = ODBFactory.open("ARTICULOS.DAT");
	  IQuery query = new CriteriaQuery(Articulos.class, Where.equal("codarti", codarti));
	  try{
		  Articulos  arti = (Articulos) odb.getObjects(query).getFirst();
		  String cadena=String.format("Articulo %d YA EXISTE",codarti);
		  Mensaje.setText(cadena) ; 
	
		  denominacionartic.setText(arti.getDenom());
		  pvpartic.setText( String.valueOf(arti.getPvp()));;
		  stockartic.setText(String.valueOf(arti.getStock()));;

		}catch (IndexOutOfBoundsException i){ 	
		    // a�adimos el articulo
			Articulos artinue = new Articulos (codarti,denom,stock,pvp);
			odb.store(artinue);
			odb.commit();
			String cadena=String.format("Articulo %d a�adido.",codarti);
			Mensaje.setText(cadena) ; 
		    System.out.printf (cadena) ;
	    }
	odb.close(); 	
}

void limpiarpantalla() {
	denominacionartic.setText("");
	pvpartic.setText("");
	stockartic.setText("");
	textArea.setText("");
	codarticulo.setText("");
	Mensaje.setText("");
	
}

public void visualizararticulos() {
	try
	 {
	  ODB odb = ODBFactory.open("ARTICULOS.DAT");
	  IQuery query = new CriteriaQuery(Articulos.class);
	  Objects<Articulos> objects = odb.getObjects(query);  
	  if (objects.size()==0)
	  {
		  Mensaje.setText("NO HAY ART�CULOS." );
	  }
    else
    {  
	 textArea.setText(" "); // limpiamos el textarea
	
	 textArea.append("CODARTI DENOMINACION            PVP        STOCK     SUMAUNIDADES  DIFERENCIA  IMPORTE      \n");
	 textArea.append("======= ======================= ========== ========= ============ ============ ==========    \n");
	 int totstock=0, totuni=0, totstockact=0;
	 float totim=0, maximpor=0;
	 int maxuni=0, contmaxim=0, contmaxuni=0;
	 while (objects.hasNext())
	 {   	  
		 Articulos art = objects.next();
		 // Calculamos la suma
		 Values val = odb.getValues(new ValuesCriteriaQuery(Ventas.class, Where.equal("codarti.codarti", art.getCodarti())).sum("univen"));
		 ObjectValues ov = val.nextValues(); 
		 BigDecimal unidades = (BigDecimal) ov.getByAlias("univen");
		 float impor = art.getPvp() * unidades.floatValue();
		 int stcac = art.getStock() - unidades.intValue();
	     String cad=String.format("%7d %-23s %8.2f %10d %8d %15d %12.2f %n", art.getCodarti(), art.getDenom(), art.getPvp(), art.getStock(),
	    		 unidades.intValue(), stcac, impor );
	     textArea.append(cad); 
	     totuni = totuni + unidades.intValue();
	      totstock=totstock + art.getStock();
		  totstockact=totstockact + stcac;
		  totim=totim + impor ;
		  if (impor>maximpor) {contmaxim=contmaxim + 1; maximpor=impor;}
		  if (unidades.intValue()>maxuni) {contmaxuni = contmaxuni+1; maxuni=unidades.intValue();}
		  
	  }	
	 textArea.append("======= ======================= ========== ========= ============ ============ ==========    \n");
     // Totales
	    String cad=String.format("%7s %-23s %8s %10d %8d %15d %12.2f %n", " ", "TOTALES = = =>>>", " ",totstock,
	    		totuni, totstockact, totim );
	  textArea.append(cad); 	 
	  textArea.append("======= ======================= ========== ========= ============ ============ ==========    \n");
	 
	
	 // Art�culos con m�s unidades
	if (contmaxuni>0 ) // si es 0 es que no hay art�culos
	{	  String cadena= String.format("%nArt�culo(s) con m�s unidades vendidas (max = %d): ",maxuni); 
		  textArea.append(cadena);
		  IQuery consart = new CriteriaQuery(Articulos.class);
		  Objects<Articulos> artt = odb.getObjects(consart); 
		  while (artt.hasNext())
			 {   	  
				 Articulos art = artt.next();
				 Values val = odb.getValues(new ValuesCriteriaQuery(Ventas.class, Where.equal("codarti.codarti", art.getCodarti())).sum("univen"));
				 ObjectValues ov = val.nextValues(); 
				 BigDecimal unidades = (BigDecimal) ov.getByAlias("univen");
				 if (unidades.intValue() == maxuni) textArea.append(" " +  art.getDenom() + "." );	 	
			 }

		}
	 
	 //Art�culos con m�s importe
	if (contmaxim>0) 
	{
		String cadena= String.format("%nArt�culo con m�s importe (max = %8.2f):  ",maximpor); 
	    textArea.append(cadena);
	    IQuery consart = new CriteriaQuery(Articulos.class);
		Objects<Articulos> artt = odb.getObjects(consart); 
		 while (artt.hasNext())
			 {   	  
				 Articulos art = artt.next();
				 Values val = odb.getValues(new ValuesCriteriaQuery(Ventas.class, Where.equal("codarti.codarti", art.getCodarti())).sum("univen"));
				 ObjectValues ov = val.nextValues(); 
				 BigDecimal unidades = (BigDecimal) ov.getByAlias("univen");
				 float impor = art.getPvp() * unidades.floatValue();
				 if (impor == maximpor) textArea.append(" " +  art.getDenom() + "." );	 	
			 }	  
			  
	}
		
		
	 
	 
   }//fin else objects.size	
  odb.close();
} 
	 catch (ODBRuntimeException  e ) {
		 Mensaje.setText("ERROR BD, COMPRUEBA QUE EST� CERRADA.");
	 }
	
}

public void consultar() {
	
	ODB odb = ODBFactory.open("ARTICULOS.DAT");
	IQuery query = new CriteriaQuery(Articulos.class, Where.equal("codarti", codarti));

		try{
			  Articulos  arti = (Articulos) odb.getObjects(query).getFirst();
			  denominacionartic.setText(arti.getDenom());
			  pvpartic.setText( String.valueOf(arti.getPvp()));;
			  stockartic.setText(String.valueOf(arti.getStock()));;
			  String cadena=String.format("Articulo %d EXISTENTE.",codarti);
		      Mensaje.setText(cadena) ; 

		}catch (IndexOutOfBoundsException i){ 	
			String cadena=String.format("Articulo %d NO EXISTE, EN LA BD",codarti);
			Mensaje.setText(cadena) ; 
			System.out.printf (cadena) ;
	}
	odb.close(); 		
}

public void modificarborrarartic(int accion) {
// accion 0 modificar, 1 borrar
ODB odb = ODBFactory.open("ARTICULOS.DAT");
IQuery query = new CriteriaQuery(Articulos.class, Where.equal("codarti", codarti));

	try{
		Articulos  arti = (Articulos) odb.getObjects(query).getFirst();
		if (accion == 0) { // Modifico
			arti.setCodarti(codarti);
			arti.setDenom(denom);
			arti.setPvp(pvp);
			arti.setStock(stock);
			odb.store(arti);
			odb.commit();
			String cadena=String.format("Articulo %d YA EXISTE, SE MODIFICA CON LOS DATOS.",codarti);
			Mensaje.setText(cadena) ;
	    }
		else // borro artic
		{
			// Antes de borrar hay que borrar las ventas de ese art�culo.
			IQuery query2 = new CriteriaQuery(Ventas.class, Where.equal("codarti.codarti", codarti));  
			Objects<Ventas> objects = odb.getObjects(query2);
			 if (objects.size()==0)
			  {
				  Mensaje.setText("NO HAY VENTAS QUE BORRAR." );
			  }
		     else
			
			 while (objects.hasNext())
			 {   	  
				 Ventas vent = objects.next();
				 odb.delete(vent);
			 }	
			 odb.delete(arti);
			 odb.commit();
			 limpiarpantalla();
			 String cadena=String.format("Articulo %d BORRADO, SUS VENTAS TAMBI�N.",codarti);
			 Mensaje.setText(cadena) ;
		}

	}catch (IndexOutOfBoundsException i){ 	
		String cadena=String.format("Articulo %d NO EXISTE, NO SE PUEDE MODIFICAR, o BORRAR.",codarti);
		Mensaje.setText(cadena) ; 
		System.out.printf (cadena) ;
}
odb.close(); 	

}
//////////////////////////////////////////////////////////////////////////////

}
