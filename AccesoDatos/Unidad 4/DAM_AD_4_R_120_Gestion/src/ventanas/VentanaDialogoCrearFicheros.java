package ventanas;

import java.awt.BorderLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import java.awt.Font;

import javax.swing.JLabel;

import java.awt.Color;

import javax.swing.SwingConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.neodatis.odb.ODB;
import org.neodatis.odb.ODBFactory;
import org.neodatis.odb.ODBRuntimeException;
import org.neodatis.odb.Objects;
import org.neodatis.odb.core.query.IQuery;
import org.neodatis.odb.impl.core.query.criteria.CriteriaQuery;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import datos.Ventas;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.awt.TextArea;

public class VentanaDialogoCrearFicheros extends JDialog {

	private final JPanel contentPanel = new JPanel();
	private JLabel lblMensaje;
	private TextArea textArea;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			VentanaDialogoCrearFicheros dialog = new VentanaDialogoCrearFicheros();
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create the dialog.
	 */
	public VentanaDialogoCrearFicheros() {
		setModal(true);
		setTitle("Ejercicio Neodatis. Creaci\u00F3n de Datos.");
		setBounds(100, 100, 911, 349);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBackground(new Color(220, 220, 220));
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(null);
		JButton btnCrearFichero = new JButton("Crear Fichero Aleatorio");
		btnCrearFichero.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				  lblMensaje.setText("");
				  crearaleatorio();
			}
		});
		btnCrearFichero.setFont(new Font("Tahoma", Font.BOLD, 13));
		btnCrearFichero.setBounds(78, 72, 202, 30);
		contentPanel.add(btnCrearFichero);
	
		JLabel lblCreacinDeFichero = new JLabel("CREACI\u00D3N DE FICHEROS");
		lblCreacinDeFichero.setHorizontalAlignment(SwingConstants.CENTER);
		lblCreacinDeFichero.setForeground(new Color(72, 61, 139));
		lblCreacinDeFichero.setFont(new Font("Mangal", Font.BOLD, 14));
		lblCreacinDeFichero.setBounds(25, 28, 309, 25);
		contentPanel.add(lblCreacinDeFichero);
	
		lblMensaje = new JLabel("-------------------------------------------------------------");
		lblMensaje.setForeground(new Color(128, 0, 0));
		lblMensaje.setBackground(new Color(255, 255, 0));
		lblMensaje.setFont(new Font("Tahoma", Font.PLAIN, 13));
		lblMensaje.setHorizontalAlignment(SwingConstants.CENTER);
		lblMensaje.setBounds(10, 270, 410, 30);
		contentPanel.add(lblMensaje);
		
		JButton btnNewButton = new JButton("Ver Fichero Aleatorio");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				  lblMensaje.setText("");visualizaraleatorio();
			}
		});
		btnNewButton.setFont(new Font("Tahoma", Font.BOLD, 13));
		btnNewButton.setBounds(78, 122, 202, 30);
		contentPanel.add(btnNewButton);
		
		JButton btnCrearFicheroXml = new JButton("Crear Fichero XML");
		btnCrearFicheroXml.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				  lblMensaje.setText("");crearficheroxml();
			}
		});
		btnCrearFicheroXml.setFont(new Font("Tahoma", Font.BOLD, 13));
		btnCrearFicheroXml.setBounds(78, 173, 202, 30);
		contentPanel.add(btnCrearFicheroXml);
		
		textArea = new TextArea();
		textArea.setBackground(new Color(102, 205, 170));
		textArea.setBounds(348, 40, 537, 222);
		contentPanel.add(textArea);
		textArea.setFont(new java.awt.Font("Courier", 1, 14));
		
		JButton btnVerFicheroXml = new JButton("Ver Fichero XML");
		btnVerFicheroXml.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				  lblMensaje.setText("");verficheroxml();
			}
		});
		btnVerFicheroXml.setFont(new Font("Tahoma", Font.BOLD, 13));
		btnVerFicheroXml.setBounds(78, 229, 202, 30);
		contentPanel.add(btnVerFicheroXml);

	}
/////////////////////////////////////////////////	
protected void verficheroxml() {
		
	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	
	System.out.println("----------LEO XML------------------");
	try {
	 DocumentBuilder builder = factory.newDocumentBuilder();
	 Document document = builder.parse(new File("Ventas.xml"));
	 NodeList listaNodos = document.getElementsByTagName("venta"); 
	
	/*Node raiz =document.getDocumentElement();
	textArea.append("\nRAIZ:" + raiz.getTextContent()); // sale el contenido de cada venta
	textArea.append("\nRAIZ:" + raiz.getOwnerDocument()); // sale el contenido de cada venta
	textArea.append("\nRAIZ:" + document.getTextContent()); 
	*/
	/*<venta>
	<codventa>. . . </codventa>
	<codarti>. . . </codarti>
	<nombrearticulo>. . . </nombrearticulo>
	<numcli>. . . </numcli>
	<nombrecliente>. . . </nombrecliente>
	<univen>. . . </univen> 
	<importe>. . . </importe>
	<fecha>. . . </fecha>
	</venta>
	*/
	
	/* POR consola
	Transformer transformer = TransformerFactory.newInstance().newTransformer();
	Source source = new DOMSource(document);
	String salida = new String();
	Result console= new StreamResult(System.out);
	transformer.transform(source, console);
	textArea.append(salida);
	*/
	 textArea.setText("");
	 textArea.append("\nCODVENTA CODARTI DENOMINACION            NUMCLI  NOMBRE                     FECHA      UNIVEN   IMPORTE");
	 textArea.append("\n======== ======= ======================= ======= ========================= ========== ========== ==========");

	for(int i=0;i<listaNodos.getLength();i++)
		{  
			Node nodo = listaNodos.item(i);  
		    String codventa= ((Element) nodo).getElementsByTagName("codventa").item(0).getTextContent();	 	    
			String codarti= ((Element) nodo).getElementsByTagName("codarti").item(0).getTextContent();	 
			String nombrearticulo= ((Element) nodo).getElementsByTagName("nombrearticulo").item(0).getTextContent();	 	    
			String numcli= ((Element) nodo).getElementsByTagName("numcli").item(0).getTextContent();	 
			String nombrecliente= ((Element) nodo).getElementsByTagName("nombrecliente").item(0).getTextContent();	 	    
			String univen= ((Element) nodo).getElementsByTagName("univen").item(0).getTextContent();	 
			String importe= ((Element) nodo).getElementsByTagName("importe").item(0).getTextContent();	 	    
			String fecha= ((Element) nodo).getElementsByTagName("fecha").item(0).getTextContent();	 
			
			//textArea.append("\n" + nodo.getTextContent()); // sale el contenido de cada venta	
			String cadena = String.format("%n%8s %7s %-23s %7s %-25s %10s %10s %10s ", codventa, codarti, nombrearticulo, numcli, nombrecliente, fecha, univen, importe);
			 textArea.append(cadena);
		}
				
	} catch (FileNotFoundException e) {
		 textArea.append("\n <<<<<FICHERO XML NO EXISTE, NO SE PUEDE LEER. CRÉALO PRIMERO >>>>>\n");   
		 //e.printStackTrace();
	} catch (Exception e) {
		textArea.append("\n <<<<<FICHERO XML ERROR DE ENTRADA SALIDA, al leer la venta.>>>>>\n");   
		lblMensaje.setText(" ERROR DE ENTRADA SALIDA, al leer la venta.");
		 // e.printStackTrace();
		}
}

///////////////////////////////////////////
protected void crearficheroxml() {
  DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	try
	 {
	   DocumentBuilder builder = factory.newDocumentBuilder();
	   DOMImplementation implementation = builder.getDOMImplementation();
	   Document document = implementation.createDocument(null, "Ventas", null);
	   document.setXmlVersion("1.0"); 
	  
	   ODB odb = ODBFactory.open("ARTICULOS.DAT");
	   IQuery query = new CriteriaQuery(Ventas.class);
			  
	   Objects<Ventas> objects = odb.getObjects(query);
			  	  
	   if (objects.size()==0)
			  {
				  lblMensaje.setText("NO HAY VENTAS." );
			  }
	   else
		   {  
			 textArea.setText("");
			 while (objects.hasNext())
			 {   	  
				 Ventas depa = objects.next();
				 float importe =(float) 0.0;
				 if (depa.getCodarti()!=null)
			      importe = depa.getUniven() * depa.getCodarti().getPvp();
				
			     crearnodo(document,depa.getCodventa(),
			               depa.getCodarti().getCodarti(),
			               depa.getCodarti().getDenom(),
			               depa.getNumcli().getNumcli(),
			               depa.getNumcli().getNombre(),
			               depa.getFecha(),
			               depa.getUniven(), importe);	 
			     textArea.append("\nNodo añadido, venta: " + depa.getCodventa());
			     
			  }		
		}	
		odb.close();
	    
		System.out.println("Creo el XML");
		Source source = new DOMSource(document);
		Result result = new StreamResult(new java.io.File("Ventas.xml")); //fich XML 
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.transform(source, result);
		
		textArea.append("\n\nFichero XML Ventas.xml creado");	
		lblMensaje.setText("Fichero XML Ventas.xml creado." );
		// Volcado del documento de memoria a consola
		//Result console= new StreamResult(System.out);
		//transformer.transform(source, console);
						
	}catch (ODBRuntimeException  e ) {
		 lblMensaje.setText("ERROR BD, COMPRUEBA QUE ESTÉ CERRADA.");
	}catch (Exception e)
			{ System.out.println("ERROOR, sale la exception: " + e);}	
		
	}

////////////////////////////////////////////////////////////////////////////
private void crearnodo(Document document, int codventa, int codarti,
		String denom, int numcli, String nombre, String fecha, int univen,
		float importe) {
		/*<venta>
		<codventa>. . . </codventa>
		<codarti>. . . </codarti>
		<nombrearticulo>. . . </nombrearticulo>
		<numcli>. . . </numcli>
		<nombrecliente>. . . </nombrecliente>
		<univen>. . . </univen> 
		<importe>. . . </importe>
		<fecha>. . . </fecha>
		</venta>
		*/
	  Element venta=document.createElement("venta");
	  document.getDocumentElement().appendChild(venta);
	  // añado la etiqueta codventa
      Element codven=document.createElement("codventa");
	  Text text = document.createTextNode(Integer.toString(codventa));
	  venta.appendChild(codven);
	  codven.appendChild(text);
	 
	  // añado la etiqueta codarti
      Element codart=document.createElement("codarti");
	  text = document.createTextNode(Integer.toString(codarti));
	  venta.appendChild(codart);
	  codart.appendChild(text);
	  
	  // añado la etiqueta nombrearticulo
      Element nomar=document.createElement("nombrearticulo");
	  text = document.createTextNode(denom);
	  venta.appendChild(nomar);
	  nomar.appendChild(text);
	
	  // añado la etiqueta numcli
      Element nucli=document.createElement("numcli");
	  text = document.createTextNode(Integer.toString(numcli));
	  venta.appendChild(nucli);
	  nucli.appendChild(text);
	
	  // añado la etiqueta nombrecliente
      Element nomcli=document.createElement("nombrecliente");
	  text = document.createTextNode(nombre);
	  venta.appendChild(nomcli);
	  nomcli.appendChild(text);
	
	  // añado la etiqueta univen
      Element uni=document.createElement("univen");
	  text = document.createTextNode(Integer.toString(univen));
	  venta.appendChild(uni);
	  uni.appendChild(text);
	  
	  // añado la etiqueta importe
      Element imp=document.createElement("importe");
	  text = document.createTextNode(Float.toString(importe));
	  venta.appendChild(imp);
	  imp.appendChild(text);
	  
	  // añado la etiqueta fecha
      Element fec=document.createElement("fecha");
	  text = document.createTextNode(fecha);
	  venta.appendChild(fec);
	  fec.appendChild(text);
	
}

////////////////////////////////////////////////////////////////////////////
protected void visualizaraleatorio() {
	try {
	 File fichero = new File("AleatorioArtic.dat");
	  //declara el fichero de acceso aleatorio
	 RandomAccessFile file;

	 file = new RandomAccessFile(fichero, "r");
	 
	 int codventa, codarti, numcli, univen;
	 String denom="",	nombre="", fecha="";
	 float importe;
	 
    long posicion=0;  //para situarnos al principio
    textArea.setText(" "); // limpiamos el textarea
	
	 textArea.append(" CODVENTA  CODARTI  DENOMINACION            NUMCLI       NOMBRE                        FECHA           UNIVEN       IMPORTE\n");
	 textArea.append("=============================================================================================================================== \n");
	
	for(;;){ 
		denom="";	nombre=""; fecha="";
		file.seek(posicion); 
		codventa=file.readInt();   
        if (codventa!=0){
        	codarti = file.readInt();   
            for (int i = 0; i < 15; i++) {
	        denom = denom + file.readChar(); }
            numcli = file.readInt(); 
            for (int i = 0; i < 20; i++) {
    	         nombre = nombre + file.readChar(); }
            for (int i = 0; i < 10; i++) {
   	         fecha = fecha + file.readChar(); }
            univen =file.readInt();   
            importe = file.readFloat();
    	  
		System.out.println("codventa: " + codventa + 
				", codarti: "+  codarti + 
	                   ", denom: "+denom +
	                   ", numcli: " + numcli +
	                   ", nombre: "+  nombre + 
	                   ", fecha: "+fecha +
	                   ", univen: " + univen +
	                   ", importe: " + importe 
	                   );   
	   String patron="%5d \t %5d \t %-15s \t %5d \t %-22s \t %-10s \t %5d \t %8.2f";
	     
	   String datos =String.format(patron,codventa,
	               codarti,denom,numcli,nombre,fecha,univen,importe );
	    textArea.append(datos+"\n");
        }
		posicion= posicion + 110; 
        
	    if (file.getFilePointer()==file.length())break;
		   
   }//fin bucle for 
   file.close();  //cerrar fichero 
	} catch (FileNotFoundException e) {
		 textArea.append("\n <<<<<FICHERO NO EXISTE, NO SE PUEDE LEER. CREALO PRIMERO >>>>>\n");   
		 //e.printStackTrace();
	}catch (IOException e2)
	{   textArea.append("\n <<<<<FICHERO ERROR DE ENTRADA SALIDA, al leer la venta.>>>>>\n");   
		lblMensaje.setText(" ERROR DE ENTRADA SALIDA, al leer la venta.");
	    // e2.printStackTrace();
	}	
	
	}
////////////////////////////////////////////////////////////////////////////
protected void crearaleatorio() {
		
	File fichero = new File("AleatorioArtic.dat");
	   //declara el fichero de acceso aleatorio
	RandomAccessFile file;
	try {
	    
	  file = new RandomAccessFile(fichero, "rw");
	  ODB odb = ODBFactory.open("ARTICULOS.DAT");
	  IQuery query = new CriteriaQuery(Ventas.class);
	  
	  Objects<Ventas> objects = odb.getObjects(query);
	  	  
	  if (objects.size()==0)
	  {
		  lblMensaje.setText("NO HAY VENTAS." );
	  }
   else
   {  
	 textArea.setText("");
	 while (objects.hasNext())
	 {   	  
		 Ventas depa = objects.next();
		 float importe =(float) 0.0;
		 if (depa.getCodarti()!=null)
	      importe = depa.getUniven() * depa.getCodarti().getPvp();
		
	     grabarenaleatorio(file,depa.getCodventa(),
	               depa.getCodarti().getCodarti(),
	               depa.getCodarti().getDenom(),
	               depa.getNumcli().getNumcli(),
	               depa.getNumcli().getNombre(),
	               depa.getFecha(),
	               depa.getUniven(),  importe);	 	    		    	        
	 }	
}	
odb.close();
file.close(); 
lblMensaje.setText("<<<<FICHERO ALEATORIO CREADO >>>>>>");
	} catch (ODBRuntimeException  e ) {
		 lblMensaje.setText("ERROR BD, COMPRUEBA QUE ESTÉ CERRADA.");
	} catch (FileNotFoundException e) {
		 lblMensaje.setText(" <<<<<ERROR Fichero aleatorio no encontrado.>>>>>>");
		//e.printStackTrace();
	} catch (IOException e2)
	{
	    lblMensaje.setText(" ERROR DE ENTRADA SALIDA.");
	   // e2.printStackTrace();
	}
}

////////////////////////////////////////////////////////////////////////////
private void grabarenaleatorio(RandomAccessFile file, int codventa, 
		int codarti, String denom,	int numcli, String nombre,
        String fecha, int univen, float imp) 
{
	//Int CODVENTA 	Int CODARTI 	String de 15 caracteres DENOMINACION
	//Int NUMCLI String de 20 caracteres NOMBRE	String de 10 caracteres FECHA
	//Int UNIVEN 	Float IMPORTE
	// 4 + 4+  30 + 4 + 40 + 20 + 4 + 4 =>110
    //relleno a blancos las cadenas
 try {
	int n=denom.length();
	for (int i =n; i<15;i++)
		denom = denom + " ";
	n=nombre.length();	
	for (int i =n; i<20;i++)
		nombre = nombre + " ";
	n=fecha.length();	
	for (int i =n; i<10;i++)
		fecha = fecha + " ";
	
	StringBuffer buffer = null;//buffer para las cadenas
	  
	long posicion;
	posicion = (codventa-1)*110;
    file.seek(posicion);
	    // insertar
    file.writeInt(codventa); 
	    
	    file.writeInt(codarti);
	    buffer = new StringBuffer( denom );      
	    buffer.setLength(15); 
	    file.writeChars(buffer.toString());
	    
	    file.writeInt(numcli);      
	    buffer = new StringBuffer( nombre );      
	    buffer.setLength(20); 
	    file.writeChars(buffer.toString());
	    
	    buffer = new StringBuffer( fecha );      
	    buffer.setLength(10); 
	    file.writeChars(buffer.toString());
	    
	    file.writeInt(univen);    
	    file.writeFloat(imp);
	    System.out.println(" REGISTRO INSERTADO VENTA :" + codventa);
	    textArea.append("\nREGISTRO INSERTADO VENTA:" + codventa);
	    
}catch (IOException e2)
	{
	    textArea.append("\n <<<<<ERROR DE ENTRADA SALIDA EN ALEATORIO, al insertar la venta..>>>>>\n");   
	   lblMensaje.setText(" ERROR DE ENTRADA SALIDA, al insertar la venta.");
	   // e2.printStackTrace();
	}
} // fin metodo
////////////////////////////////////////////////////////////////////////////
}// fin clase
	
