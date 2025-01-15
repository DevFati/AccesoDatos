package ventanas;

import java.awt.EventQueue;

import javax.swing.JDialog;
import javax.swing.JLabel;

import java.awt.Font;

import javax.swing.JPanel;

import java.awt.Color;

import javax.swing.JTextField;
import javax.swing.JComboBox;
import javax.swing.SwingConstants;
import javax.swing.JButton;

import org.neodatis.odb.ODB;
import org.neodatis.odb.ODBFactory;
import org.neodatis.odb.ODBRuntimeException;
import org.neodatis.odb.Objects;
import org.neodatis.odb.core.query.IQuery;
import org.neodatis.odb.core.query.criteria.Where;
import org.neodatis.odb.impl.core.query.criteria.CriteriaQuery;

import datos.Articulos;
import datos.Clientes;
import datos.Ventas;

import java.awt.TextArea;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class VentanaDialogoVentas extends JDialog {
	private JTextField codventa;
	private JTextField univen;
	private JTextField fecha;
	private JTextField nombreart;
	private JTextField nombrecli;
	private JComboBox comboCodarti;
	private JComboBox comboCodcli;
	private JLabel lblMensaje;
	private JTextField pvp;
	private JTextField importe;
	private JTextField stock;
	private TextArea textArea;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					VentanaDialogoVentas dialog = new VentanaDialogoVentas();
					dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
					dialog.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the dialog.
	 */
	public VentanaDialogoVentas() {
		setModal(true);
		setTitle("Ejercicio Neodatis. Ventas de articulos");
		setBounds(100, 100, 1138, 381);
		getContentPane().setLayout(null);
		
		JLabel lblEntradaDeVentas = new JLabel("ENTRADA DE VENTAS DE ART\u00CDCULOS");
		lblEntradaDeVentas.setFont(new Font("Tahoma", Font.BOLD, 13));
		lblEntradaDeVentas.setBounds(102, 11, 259, 22);
		getContentPane().add(lblEntradaDeVentas);
		
		JPanel panel = new JPanel();
		panel.setBackground(new Color(255, 218, 185));
		panel.setBounds(33, 55, 502, 186);
		getContentPane().add(panel);
		panel.setLayout(null);
		
		JLabel lblCdigodeVenta = new JLabel("C\u00F3digode venta:");
		lblCdigodeVenta.setForeground(new Color(0, 0, 128));
		lblCdigodeVenta.setBounds(23, 11, 113, 15);
		panel.add(lblCdigodeVenta);
		lblCdigodeVenta.setFont(new Font("Tahoma", Font.BOLD, 12));
		
		JLabel lblSeleccionaEl = new JLabel("Selecciona el art\u00EDculo: ");
		lblSeleccionaEl.setForeground(new Color(0, 0, 128));
		lblSeleccionaEl.setBounds(10, 52, 126, 22);
		panel.add(lblSeleccionaEl);
		lblSeleccionaEl.setFont(new Font("Tahoma", Font.PLAIN, 12));
		
		JLabel lblSeleccionaElCliente = new JLabel("Selecciona el cliente:");
		lblSeleccionaElCliente.setForeground(new Color(0, 0, 128));
		lblSeleccionaElCliente.setBounds(10, 88, 141, 22);
		panel.add(lblSeleccionaElCliente);
		lblSeleccionaElCliente.setFont(new Font("Tahoma", Font.PLAIN, 12));
		
		JLabel lblUnidadesVendidas = new JLabel("Unidades Vendidas:");
		lblUnidadesVendidas.setForeground(new Color(0, 0, 128));
		lblUnidadesVendidas.setBounds(10, 120, 143, 22);
		panel.add(lblUnidadesVendidas);
		lblUnidadesVendidas.setFont(new Font("Tahoma", Font.PLAIN, 12));
		
		JLabel lblFechaddmmyyyy = new JLabel("Fecha (dd/mm/yyyy):");
		lblFechaddmmyyyy.setForeground(new Color(0, 0, 128));
		lblFechaddmmyyyy.setBounds(10, 149, 141, 26);
		panel.add(lblFechaddmmyyyy);
		lblFechaddmmyyyy.setFont(new Font("Tahoma", Font.PLAIN, 12));
		
		codventa = new JTextField();
		codventa.setHorizontalAlignment(SwingConstants.CENTER);
		codventa.setFont(new Font("Tahoma", Font.BOLD, 11));
		codventa.setBounds(157, 9, 86, 20);
		panel.add(codventa);
		codventa.setColumns(10);
		
		comboCodarti = new JComboBox();
		comboCodarti.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				
				visualizarelarticuloelegido();
			}
		});
		comboCodarti.setFont(new Font("Tahoma", Font.BOLD, 11));
		comboCodarti.setBounds(146, 54, 62, 20);
		panel.add(comboCodarti);
		
		comboCodcli = new JComboBox();
		comboCodcli.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				visualizarclienteelegido();
			}

			
		});
		comboCodcli.setFont(new Font("Tahoma", Font.BOLD, 11));
		comboCodcli.setBounds(141, 90, 90, 20);
		panel.add(comboCodcli);
		
		univen = new JTextField();
		univen.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				mostrarimporte();
			}
		});
		univen.setFont(new Font("Tahoma", Font.BOLD, 11));
		univen.setHorizontalAlignment(SwingConstants.CENTER);
		univen.setBounds(151, 122, 86, 20);
		panel.add(univen);
		univen.setColumns(10);
		
		fecha = new JTextField();
		fecha.setFont(new Font("Tahoma", Font.BOLD, 11));
		fecha.setHorizontalAlignment(SwingConstants.CENTER);
		fecha.setBounds(146, 153, 86, 20);
		panel.add(fecha);
		fecha.setColumns(10);
		
		nombreart = new JTextField();
		nombreart.setEditable(false);
		nombreart.setBounds(218, 54, 126, 20);
		panel.add(nombreart);
		nombreart.setColumns(10);
		
		nombrecli = new JTextField();
		nombrecli.setEditable(false);
		nombrecli.setBounds(241, 90, 126, 20);
		panel.add(nombrecli);
		nombrecli.setColumns(10);
		
		JButton btnConsultar = new JButton("Consultar");
		btnConsultar.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {		
				consultarventa();
			}
		});
		btnConsultar.setBounds(272, 8, 112, 23);
		panel.add(btnConsultar);
		
		pvp = new JTextField();
		pvp.setEditable(false);
		pvp.setHorizontalAlignment(SwingConstants.CENTER);
		pvp.setBounds(410, 39, 62, 20);
		panel.add(pvp);
		pvp.setColumns(10);
		
		importe = new JTextField();
		importe.setHorizontalAlignment(SwingConstants.CENTER);
		importe.setFont(new Font("Tahoma", Font.BOLD, 12));
		importe.setEditable(false);
		importe.setBounds(331, 122, 105, 20);
		panel.add(importe);
		importe.setColumns(10);
		
		JLabel lblPvp = new JLabel("PVP");
		lblPvp.setHorizontalAlignment(SwingConstants.CENTER);
		lblPvp.setFont(new Font("Tahoma", Font.BOLD, 12));
		lblPvp.setBounds(354, 41, 46, 14);
		panel.add(lblPvp);
		
		JLabel lblImporte = new JLabel("Importe");
		lblImporte.setHorizontalAlignment(SwingConstants.CENTER);
		lblImporte.setFont(new Font("Tahoma", Font.BOLD, 12));
		lblImporte.setBounds(251, 125, 70, 17);
		panel.add(lblImporte);
		
		JLabel lbstock = new JLabel("STOCK");
		lbstock.setHorizontalAlignment(SwingConstants.CENTER);
		lbstock.setFont(new Font("Tahoma", Font.BOLD, 12));
		lbstock.setBounds(349, 65, 46, 14);
		panel.add(lbstock);
		
		stock = new JTextField();
		stock.setHorizontalAlignment(SwingConstants.CENTER);
		stock.setEditable(false);
		stock.setColumns(10);
		stock.setBounds(410, 63, 62, 20);
		panel.add(stock);
		
		lblMensaje = new JLabel("-------------------------------------------------------");
		lblMensaje.setFont(new Font("Tahoma", Font.BOLD, 11));
		lblMensaje.setForeground(new Color(255, 0, 0));
		lblMensaje.setHorizontalAlignment(SwingConstants.CENTER);
		lblMensaje.setBounds(33, 246, 502, 37);
		getContentPane().add(lblMensaje);
		
		JButton btnInsertarVenta = new JButton("Insertar venta");
		btnInsertarVenta.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				insertarunaventa();
			}
		});
		btnInsertarVenta.setFont(new Font("Tahoma", Font.BOLD, 11));
		btnInsertarVenta.setBounds(46, 290, 137, 23);
		getContentPane().add(btnInsertarVenta);
		
		JButton btnBorrarVenta = new JButton("Borrar Venta");
		btnBorrarVenta.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				borrarventa();
			}
		});
		btnBorrarVenta.setFont(new Font("Tahoma", Font.BOLD, 11));
		btnBorrarVenta.setBounds(208, 290, 137, 23);
		getContentPane().add(btnBorrarVenta);
		
		JButton btnModificarLaVenta = new JButton("Modificar la venta");
		btnModificarLaVenta.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				
				modificarlaventa();
			}
		});
		btnModificarLaVenta.setFont(new Font("Tahoma", Font.BOLD, 11));
		btnModificarLaVenta.setBounds(367, 291, 147, 22);
		getContentPane().add(btnModificarLaVenta);
		
		textArea = new TextArea();
		textArea.setEditable(false);
		textArea.setBackground(new Color(255, 255, 102));
		textArea.setBounds(544, 55, 568, 256);
		textArea.setFont(new java.awt.Font("Courier", 1, 14));
		getContentPane().add(textArea);
		
		JButton btnVisualizarLasVentas = new JButton("Visualizar las ventas");
		btnVisualizarLasVentas.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				visualizarventas();
			}
		});
		btnVisualizarLasVentas.setFont(new Font("Tahoma", Font.BOLD, 12));
		btnVisualizarLasVentas.setBounds(540, 26, 184, 23);
		getContentPane().add(btnVisualizarLasVentas);
		
		JButton btnLimpiarPantalla = new JButton("Limpiar pantalla");
		btnLimpiarPantalla.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				limpiarpantalla();
				
			}
		});
		btnLimpiarPantalla.setFont(new Font("Tahoma", Font.BOLD, 12));
		btnLimpiarPantalla.setBounds(916, 26, 184, 23);
		getContentPane().add(btnLimpiarPantalla);
		
		JButton btnNewButton = new JButton("Ir a ARTT\u00CDCULOS");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				VentanaDialogoArticc dialog = new VentanaDialogoArticc();
				dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
				dialog.setVisible(true);
			}
		});
		btnNewButton.setFont(new Font("Tahoma", Font.BOLD, 12));
		btnNewButton.setBounds(734, 26, 172, 23);
		getContentPane().add(btnNewButton);
		
		llenarcomboCodarti();
		llenarcomboCodcli();

	}
////////////////////////////////////////////////////////
protected void modificarlaventa() {
	int nv=0, error=0;
	Ventas vent ;
	vent=null;
	String Mensa ="";
	try
	  {
	   nv =Integer.parseInt(codventa.getText().toString());
	   vent=comprobar(nv);
	  }catch (NumberFormatException e)
	  {
		//lblMensaje.setText("Num venta ERRÓNEO, TECLEA NUMERiCO.");
		Mensa = Mensa + "Num venta ERRÓNEO, TECLEA NUMERiCO. ";
		nv=0;
	  }
	if (vent!=null && nv!=0){
		int ncli;
		int art;
		int uni;
		String fechita;
		//Validar datos, si no hay cliente o articulo en el combo pongo 0
		//articulo
		try
		  {
		   art =Integer.parseInt(comboCodarti.getSelectedItem().toString());
		   
		  }catch (NumberFormatException e)
		  {
			art=0; Mensa=Mensa + "ERROR, ELIGE ARTICULO. ";
		  }
		//////Cliente
		try
		  {
		   ncli =Integer.parseInt(comboCodcli.getSelectedItem().toString());
		   
		  }catch (NumberFormatException e)
		  {
			 ncli=0; Mensa=Mensa + "ERROR. ELIGE CLIENTE. ";
		  }
		///Unidades vendidas
		try
		  {
		   uni =Integer.parseInt(univen.getText().toString());
		   
		  }catch (NumberFormatException e)
		  {
			uni=0; importe.setText("");
			Mensa=Mensa + "TECLEA NUMÉRICO ENTERO EN UNIDADES.";
		  }
		
		 lblMensaje.setText(Mensa);
		// validar la fecha
		
		System.out.println("datos " + nv + "  " + ncli + "  " +art + "  " +
		uni + "  " );
		if(	uni>0 && art>0 &&  ncli>0 )
		{ // lo grabo
		 ODB odb = ODBFactory.open("ARTICULOS.DAT");
		 Articulos artic=null;
		 if (art>0){
		     IQuery query = new CriteriaQuery(Articulos.class, 
					    Where.equal("codarti", art));
			  Objects<Articulos> objects = odb.getObjects(query);
			   
			 artic = objects.next();
			 }
		 // busco cliente
		 Clientes clien=null;
		 if (ncli>0){
		     IQuery query = new CriteriaQuery(Clientes.class, 
					    Where.equal("numcli", ncli));
			  Objects<Clientes> objects = odb.getObjects(query);
			   
			  clien = objects.next();
			 }
		 
		 fechita = validarfecha(fecha.getText().toString()); 
		 System.out.println("La fecha = *" + fechita + "**" + fecha.getText().toString());
			
		 if (fechita!=" ")
		 {	 // comprobar stock de artic
			 if ((artic.getStock() - uni + vent.getUniven()) >3)
			 {
			 //Extraigo el objeto de la BD
			 IQuery query = new CriteriaQuery(Ventas.class, 
						    Where.equal("codventa", nv));
				  
			 Objects<Ventas> objects = odb.getObjects(query);
				    
			 vent = objects.next();
			 vent.setCodarti(artic);
			 vent.setFecha(fechita);
			 vent.setNumcli(clien);
			 		
		     // actualizar articulo
		     artic.setStock((artic.getStock() - uni + vent.getUniven()));
		     //asigno las nuevas uni
		     vent.setUniven(uni);
			 odb.store(vent);
		     stock.setText(Integer.toString(artic.getStock()));
		     odb.store(artic);
		     mostrarimporte();
		     lblMensaje.setText("Venta ACTUALIZADA.");
			 }
		     else
		     {
		      lblMensaje.setText("NO HAY STOCK SUFICIENTE, VENTA NO ACTUALIZADA.");
		     }
		 }
		 odb.close(); 
		 
		} // fin grabar
			  
	 } // fin if
		
	else
	{   if (nv!=0)  
	     lblMensaje.setText("Num venta NO EXISTE.");
	    else 
	     lblMensaje.setText("Num venta, erróneo, teclea número entero.");
    	
	}
}// fin modificar

//////////////////////////////////////////////
protected void borrarventa() {
	Ventas vent;
	int nv;
	try
	  {
	   nv =Integer.parseInt(codventa.getText().toString());
	   vent=comprobar(nv);
	   if (vent==null )//  venta nexiste
	   {
		   lblMensaje.setText("Num venta NO EXISTE.");
		}
		else  // venta existe
		{ // se borra
			try{
				  ODB odb = ODBFactory.open("ARTICULOS.DAT");
				  IQuery query = new CriteriaQuery(Ventas.class, 
						    Where.equal("codventa", nv));
				  
				  Objects<Ventas> objects = odb.getObjects(query);
				    
				  Ventas depa = objects.next();
				  // actualizar el articulo
				  IQuery query2 = new CriteriaQuery(Articulos.class, 
						    Where.equal("codarti", depa.getCodarti().getCodarti()));
				  Objects<Articulos> objec2 = odb.getObjects(query2);
				   
				  Articulos artic = objec2.next();
				  artic.setStock(artic.getStock()+depa.getUniven());
				  odb.store(artic);
				  //borro venta
				  odb.delete(depa);
				  odb.commit();
				  odb.close();
				  limpiarpantalla();
				  lblMensaje.setText("Venta borrada.");
				  
					
				} catch (ODBRuntimeException  e ) {
					   lblMensaje.setText("ERROR BD, COMPRUEBA QUE ESTÉ CERRADA.");
				    
				}
			 
			 
		}
	   
	  }catch (NumberFormatException e)
	  {
		lblMensaje.setText("Num venta ERRÓNEO, TECLEA NUMERiCO.");
	  }
	
	}
////////////////////////////////////////
private void limpiarpantalla() {
	univen.setText("");
	fecha.setText("");
	nombrecli.setText("");
	nombreart.setText("");
	pvp.setText("");
	stock.setText("");
	comboCodarti.setSelectedIndex(0);
	comboCodcli.setSelectedIndex(0);
	importe.setText("");
	textArea.setText("");
	
}

///////////////////////////////////////////////
protected void insertarunaventa() {
	int nv=0, error=0;
	Ventas vent = new Ventas();
	vent=null;
	String Mensa ="";
	try
	  {
	   nv =Integer.parseInt(codventa.getText().toString());
	   vent=comprobar(nv);
	  }catch (NumberFormatException e)
	  {
		//lblMensaje.setText("Num venta ERRÓNEO, TECLEA NUMERiCO.");
		Mensa = Mensa + "Num venta ERRÓNEO, TECLEA NUMERiCO. ";
		nv=0;
	  }
	if (vent==null && nv!=0){
		int ncli;
		int art;
		int uni;
		String fechita;
		//Validar datos, si no hay cliente o articulo en el combo pongo 0
		//articulo
		try
		  {
		   art =Integer.parseInt(comboCodarti.getSelectedItem().toString());
		   
		  }catch (NumberFormatException e)
		  {
			art=0; Mensa=Mensa + "ERROR, ELIGE ARTICULO. ";
		  }
		//////Cliente
		try
		  {
		   ncli =Integer.parseInt(comboCodcli.getSelectedItem().toString());
		   
		  }catch (NumberFormatException e)
		  {
			 ncli=0; Mensa=Mensa + "ERROR. ELIGE CLIENTE. ";
		  }
		///Unidades vendidas
		try
		  {
		   uni =Integer.parseInt(univen.getText().toString());
		   
		  }catch (NumberFormatException e)
		  {
			uni=0; importe.setText("");
			Mensa=Mensa + "TECLEA NUMÉRICO ENTERO EN UNIDADES.";
		  }
		
		 lblMensaje.setText(Mensa);
		// validar la fecha
		
		System.out.println("datos " + nv + "  " + ncli + "  " +art + "  " +
		uni + "  " );
		if(	uni>0 && art>0 &&  ncli>0 )
		{ // lo grabo
		    mostrarimporte();
		 ODB odb = ODBFactory.open("ARTICULOS.DAT");
		 Articulos artic=null;
		 if (art>0){
		     IQuery query = new CriteriaQuery(Articulos.class, 
					    Where.equal("codarti", art));
			  Objects<Articulos> objects = odb.getObjects(query);
			   
			 artic = objects.next();
			 }
		 // busco cliente
		 Clientes clien=null;
		 if (ncli>0){
		     IQuery query = new CriteriaQuery(Clientes.class, 
					    Where.equal("numcli", ncli));
			  Objects<Clientes> objects = odb.getObjects(query);
			  clien = objects.next();
			 }
		 
		 fechita = validarfecha(fecha.getText().toString()); 
		 System.out.println("La fecha = *" + fechita + "**" + fecha.getText().toString());
			
		 if (fechita!=" ")
		 {	 // comprobar stock de artic
			 if ((artic.getStock() - uni) >3)
			 {
			 Ventas ventita = new Ventas (nv,artic,clien,uni,fechita);
		     odb.store(ventita);
		     // actualizar articulo
		     artic.setStock((artic.getStock() - uni));
		     stock.setText(Integer.toString(artic.getStock()));
		     odb.store(artic);
		     odb.commit();		 
		     lblMensaje.setText("Venta GRABADA.");
			 }
		     else
		     {
		      lblMensaje.setText("NO HAY STOCK SUFICIENTE, VENTA NO REALIZADA.");
		     }
		 }
		 odb.close(); 
		 
		} // fin grabar
			  
	 } // fin if
		
	else
	{   if (nv!=0) { asignardatospantalla(vent);
	                 lblMensaje.setText("Num venta YA EXISTE.");}
	    else 
	    	lblMensaje.setText("Num venta, erróneo, teclea número entero.");
    	
	}
}

//////////////////////////////////////////////////////////////
protected String validarfecha(String fechaa) {
	
	String fec = fechaa.trim();
	String resul=" ";
	
	int dd=0, mm=0, yyyy=0;
	
	// posicion de los /
	int prime = fec.indexOf("/");
	int segu = fechaa.indexOf("/", prime + 1);
	System.out.println("prime = " + prime+", segu=" + segu);
	
	if (prime>0 && segu>0)
	{
		try
		  {
		   dd = Integer.parseInt(fec.substring(0, prime));
		   mm = Integer.parseInt(fec.substring(prime+1, segu));
		   yyyy = Integer.parseInt(fec.substring(segu+1, fec.length()));
		   System.out.println("dd = " + dd + ",mm= " + mm + ", anio = " + yyyy);
           resul=fec;
           int eerr=0;
           String mensa="";
           if (dd<0 || dd>31) mensa = mensa + "Revisa el dia (1 a 31).";
           if (mm<0 || mm>12) mensa = mensa + "Revisa el mes (1 a 12).";
           if (yyyy<1970 || yyyy>3000) mensa = mensa + "Revisa el año.";
           lblMensaje.setText(mensa);
           if (mensa!="") resul=" ";
           
		  }catch (NumberFormatException e)
		  {
			lblMensaje.setText("REVISA FORMATO DE FECHA DEBE SER dd/mm/yyyy.");
		  }
	}
	else
		lblMensaje.setText("REVISA FORMATO DE FECHA DEBE SER dd/mm/yyyy.");
	
	return resul;	 
}
//////////////////////////////////////////
protected void consultarventa() {
	 try
	 {
	  int vent=0; 
	  try
	  {
	  vent =Integer.parseInt(codventa.getText().toString());
	  ODB odb = ODBFactory.open("ARTICULOS.DAT");
	  IQuery query = new CriteriaQuery(Ventas.class, 
			    Where.equal("codventa", vent));
	  
	  Objects<Ventas> objects = odb.getObjects(query);
	    
	  if (objects.size()==0)
	  {
		  lblMensaje.setText("Venta no existe => " + vent);
	  }
	  else
	  {
		  Ventas venta = objects.next();		  
		    asignardatospantalla(venta);		        		     		    
	      lblMensaje.setText("Venta localizada");		  
	  }
	  odb.close(); 
	  }catch (NumberFormatException e)
	  {
		lblMensaje.setText("Venta ERRÓNEO, TECLEA NUMERiCO: " + vent);
	  }
	 } 
	 catch (ODBRuntimeException  e ) {
	   lblMensaje.setText("ERROR BD, COMPRUEBA QUE ESTÉ CERRADA.");;
	 }
	 			
	}
////////////////////////////////////////////////
private void asignardatospantalla(Ventas venta) {
	
		
	univen.setText(Integer.toString(venta.getUniven()));
	fecha.setText(venta.getFecha());
	nombrecli.setText(venta.getNumcli().getNombre());
	nombreart.setText(venta.getCodarti().getDenom());
	pvp.setText(Float.toString(venta.getCodarti().getPvp()));
	stock.setText(Integer.toString(venta.getCodarti().getStock()));
	
	if (venta.getCodarti() != null){
		  int deppp=venta.getCodarti().getCodarti();
		  comboCodarti.setSelectedItem(deppp);
	}
	else
		comboCodarti.setSelectedIndex(0);;
	
	if (venta.getNumcli()!= null){
			  int deppp=venta.getNumcli().getNumcli();
			  comboCodcli.setSelectedItem(deppp);
		}
		else
			comboCodcli.setSelectedIndex(0);;
	
			mostrarimporte();
}
	
////////////////////////////////////////////
protected Ventas comprobar(int de)
{
	try{
	  ODB odb = ODBFactory.open("ARTICULOS.DAT");
	  IQuery query = new CriteriaQuery(Ventas.class, 
			    Where.equal("codventa", de));
	  
	  Objects<Ventas> objects = odb.getObjects(query);
	    
	  if (objects.size()==0)
	  {
		  odb.close(); 
		  return null;
	  }
	  else
	  {   Ventas depa = objects.next();
		  odb.close(); 
		  return depa;
		  
	  }
	} catch (ODBRuntimeException  e ) {
		   lblMensaje.setText("ERROR BD, COMPRUEBA QUE ESTÉ CERRADA.");
	     return null;	
	}
		  
}
/////////////////////////////////////////////
protected void visualizarventas() {
	
	try
	 {
	  ODB odb = ODBFactory.open("ARTICULOS.DAT");
	  IQuery query = new CriteriaQuery(Ventas.class);
	  
	  Objects<Ventas> objects = odb.getObjects(query);
	  	  
	  if (objects.size()==0)
	  {
		  lblMensaje.setText("NO HAY VENTAS." );
	  }
     else
     {  
	 textArea.setText(" "); // limpiamos el textarea
	
	 textArea.append(" CODVENTA  CODARTI  DENOMINACION            NUMCLI       NOMBRE                        FECHA           UNIVEN       IMPORTE\n");
	 textArea.append("=============================================================================================================================== \n");
	 
	 while (objects.hasNext())
	 {   	  
		 Ventas depa = objects.next();
		 float importe =(float) 0.0;
		 if (depa.getCodarti()!=null)
	      importe = depa.getUniven() * depa.getCodarti().getPvp();
	    
	     String patron="%5d \t %5d \t %-15s \t %5d \t %-22s \t %-10s \t %5d \t %8.2f";
	     
	     String datos =String.format(patron,depa.getCodventa(),
	               depa.getCodarti().getCodarti(),
	               depa.getCodarti().getDenom(),
	               depa.getNumcli().getNumcli(),
	               depa.getNumcli().getNombre(),
	               depa.getFecha(),
	               depa.getUniven(), 
                    importe );
	    	  
	     
	    textArea.append(datos+"\n");
	
	 	    		    	        
	  }
		
 }	
 odb.close();

 } 
	 catch (ODBRuntimeException  e ) {
		 lblMensaje.setText("ERROR BD, COMPRUEBA QUE ESTÉ CERRADA.");
	 }
		
}

//////////////////////////////////////////////
protected void mostrarimporte() {
	
	 int unidades=0 ;
	 float precio=0, calculo=0;
	 try{
		  unidades =Integer.parseInt(univen.getText().toString());
		} catch (NumberFormatException e)
	      {
		    lblMensaje.setText("UNIDADES ERRÓNEAS, TECLEA NUMERiCO:");
		   calculo=1;
		   importe.setText("");
	      }
	 
	 try{
	  precio = Float.parseFloat(pvp.getText().toString());
		} catch (NumberFormatException e)
	      {
		    lblMensaje.setText("No hay datos en el campo PVP.");
		    calculo=1;
		    importe.setText("");
		   }
	 if (calculo == 0)
	 {   lblMensaje.setText("---------------------------------");
		 calculo = unidades * precio;
		 importe.setText(Float.toString(calculo));
		 }

}

///////////////////////////////////////////////////////
protected void visualizarclienteelegido() {
	{
		if (comboCodcli.getSelectedIndex()==0)
			{
			nombrecli.setText("");	
			}
		else
		{
			int codcli=Integer.parseInt(comboCodcli.getSelectedItem().toString());
			try
			 {
			  ODB odb = ODBFactory.open("ARTICULOS.DAT");
			  IQuery query = new CriteriaQuery(Clientes.class, 
					    Where.equal("numcli", codcli));
			  
			  Objects<Clientes> objects = odb.getObjects(query);
			    
			  Clientes depa = objects.next();
			  nombrecli.setText(depa.getNombre()); 
			
			  odb.close(); 
			 
			 } 
			 catch (ODBRuntimeException  e ) {
				 lblMensaje.setText("ERROR BD, COMPRUEBA QUE ESTÉ CERRADA.");;
			 }
		}
		
		}		
		
	}

////////////////////////////////////////////////////////
protected void visualizarelarticuloelegido()
 {
	if (comboCodarti.getSelectedIndex()==0)
		{
		nombreart.setText("");	
		pvp.setText(""); stock.setText("");importe.setText("");
		}
	else
	{
		int codarti=Integer.parseInt(comboCodarti.getSelectedItem().toString());
		try
		 {
		  ODB odb = ODBFactory.open("ARTICULOS.DAT");
		  IQuery query = new CriteriaQuery(Articulos.class, 
				    Where.equal("codarti", codarti));
		  
		  Objects<Articulos> objects = odb.getObjects(query);
		    
		  Articulos depa = objects.next();
		  nombreart.setText(depa.getDenom()); 
		  pvp.setText(Float.toString(depa.getPvp()));
		  stock.setText(Integer.toString(depa.getStock()));
		  mostrarimporte();
		  odb.close(); 
		 
		 } 
		 catch (ODBRuntimeException  e ) {
			 lblMensaje.setText("ERROR BD, COMPRUEBA QUE ESTÉ CERRADA.");;
		 }
	}
	
	}

/////////////////////////////////////////////////////////
public void llenarcomboCodarti()
	{    comboCodarti.removeAllItems();
	     comboCodarti.addItem("   ");
		 try
		 {
		  ODB odb = ODBFactory.open("ARTICULOS.DAT");
		  IQuery query = new CriteriaQuery(Articulos.class);
		  
		  Objects<Articulos> objects = odb.getObjects(query);
		    
		  if (objects.size()==0)
		  {
			  lblMensaje.setText("NO HAY Articulos...." );
		  }
		  else
		  {
			while (objects.hasNext()){
				Articulos depa = objects.next();
				comboCodarti.addItem(depa.getCodarti());
				  			
			}
						  
		  }
		  odb.close(); 
		 
		 } 
		 catch (ODBRuntimeException  e ) {
			 lblMensaje.setText("ERROR BD, COMPRUEBA QUE ESTÉ CERRADA.");;
		 }
	}
////////////////////////////////////////////////////////
public void llenarcomboCodcli()
{    comboCodcli.removeAllItems();
     comboCodcli.addItem("   ");
	 try
	 {
	  ODB odb = ODBFactory.open("ARTICULOS.DAT");
	  IQuery query = new CriteriaQuery(Clientes.class);
	  
	  Objects<Clientes> objects = odb.getObjects(query);
	    
	  if (objects.size()==0)
	  {
		  lblMensaje.setText("NO HAY Clientes...." );
	  }
	  else
	  {
		while (objects.hasNext()){
			Clientes depa = objects.next();
			comboCodcli.addItem(depa.getNumcli());
			  			
		}
					  
	  }
	  odb.close(); 
	 
	 } 
	 catch (ODBRuntimeException  e ) {
		 lblMensaje.setText("ERROR BD, COMPRUEBA QUE ESTÉ CERRADA.");;
	 }
}
}
