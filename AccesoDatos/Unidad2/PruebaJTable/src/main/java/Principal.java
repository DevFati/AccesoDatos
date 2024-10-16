import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JLabel;
import java.awt.Font;
import java.awt.Color;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.sql.*;
import java.util.List;
import java.util.Vector;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class Principal extends JFrame {

	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JTable table;
	private DefaultTableModel modelo;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Principal frame = new Principal();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public Principal() {
		setTitle("PRUEBA JTABLE");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 450, 300);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));

		setContentPane(contentPane);
		contentPane.setLayout(null);

		JLabel lblNewLabel = new JLabel("PRUEBAS JTABLE-RESULSETMETADATA");
		lblNewLabel.setForeground(Color.BLUE);
		lblNewLabel.setFont(new Font("Tahoma", Font.BOLD, 14));
		lblNewLabel.setBounds(10, 11, 338, 35);
		contentPane.add(lblNewLabel);

		JButton btnVerEmpleados = new JButton("Ver empleados");
		btnVerEmpleados.setFont(new Font("Tahoma", Font.BOLD, 11));
		btnVerEmpleados.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				llenarTablaBD("empleados");
			}
			
		});
		btnVerEmpleados.setBounds(10, 57, 131, 23);
		contentPane.add(btnVerEmpleados);

		JButton btnVerDepartamentos = new JButton("Ver departamentos");
		btnVerDepartamentos.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				llenarTablaBD("departamentos");
			}

			
		});
		btnVerDepartamentos.setFont(new Font("Tahoma", Font.BOLD, 11));
		btnVerDepartamentos.setBounds(151, 57, 148, 23);
		contentPane.add(btnVerDepartamentos);

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(10, 103, 414, 147);
		contentPane.add(scrollPane);

		table = new JTable();
		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				System.out.println("He pulsado en la fila " + table.getSelectedRow());
				Vector datostabla = modelo.getDataVector();
				List datosFila = (List) datostabla.get(table.getSelectedRow());
				System.out.println(datosFila);
				
				//Visualizamos los datos de la fila, si no eta vacia
				
				if (datosFila.size() > 0) {
					 for (int i = 0; i < datosFila.size(); i++) {
					   System.out.println("Dato " + i + " : " + datosFila.get(i));
					}
				}
			}
		});
		table.setModel(new DefaultTableModel(
				new Object[][] { { "ffffsf", null, null, null, null }, { null, "dsfdf", null, null, null },
						{ "fgdfgfd", null, null, null, null }, { null, null, null, null, null },
						{ null, null, null, null, null }, { null, null, null, null, null }, },
				new String[] { "Columna1", "Columna2", "Columna3", "Columna4", "Columna5" }));
		scrollPane.setViewportView(table);

		JButton btnVerOtro = new JButton("Ver otros");
		btnVerOtro.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				llenarJtable();
			}
		});
		btnVerOtro.setFont(new Font("Tahoma", Font.BOLD, 11));
		btnVerOtro.setBounds(322, 57, 89, 23);
		contentPane.add(btnVerOtro);
	}

	private void llenarJtable() {

		String[] etiquetas = { "Nombre", "Apellido", "Tlf" };
		Object[][] datos = { { "Ali", "Ramos", "1233" }, { "Dori", "Gil", "125533" }, { "Juan", "Sánchez", "333" } };

		// Para llenar el JTABLE se necesita un DefaultTableModel
		// Si lo cargo con mis datos lo hago así
		modelo = new DefaultTableModel(datos, etiquetas);
		modelo.setColumnIdentifiers(etiquetas);
		modelo.setDataVector(datos, etiquetas);

		// Asignamos el modelo a la tabla
		table.setModel(modelo);
		Color fg = Color.PINK;
		table.setBackground(fg);
		table.setForeground(Color.BLUE);
	}
	
	private void llenarTablaBD(String nombreTabla) {
		
		try {
			Class.forName("oracle.jdbc.driver.OracleDriver");
			Connection conexion = DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:xe", "ejemplo",
					"dam");
			Statement sentencia=conexion.createStatement();
			String consulta="SELECT * FROM "+nombreTabla;
			ResultSet resul=sentencia.executeQuery(consulta);
			ResultSetMetaData rsmd=resul.getMetaData();
			int nColumnas=rsmd.getColumnCount(); //Número de columnas
			
			
			String consulta2="SELECT * FROM "+nombreTabla; //Consulta para obtener el numero de filas 
			Statement sentencia2=conexion.createStatement();
			ResultSet resul2=sentencia2.executeQuery(consulta2);
			resul2.next(); //si devuelve un solo registro hacemos esto
			int filas=resul2.getInt(1);
			sentencia2.close();
			resul2.close();
			
			//Creamos los Arrays
			String[] etiquetas = new String[nColumnas];
			Object[][] datos = new Object[filas][nColumnas];
			
			//Llenamos los Arrays 
			
			//lleno el array de etiquetas 
			for (int i = 1; i <= nColumnas; i++) {
				rsmd.getColumnName(i);
				System.out.println("Añado la columna " + rsmd.getColumnName(i).toUpperCase());
				etiquetas[i - 1] = rsmd.getColumnName(i).toUpperCase();
			}
			
			//lleno el array de datos 
			int numeroFila = 0;
			
			resul = sentencia.executeQuery(consulta);
			while (resul.next()) {
			   //Bucle para cada fila, añadir las columnas 
		         for (int i = 0; i < nColumnas; i++) {
					datos[numeroFila][i] = resul.getObject(i + 1);
					System.out.println("Añado la columna " + i + ", datos " + 
		                      resul.getString(i + 1));
				}
			   numeroFila++;
			}
			
			
			modelo = new DefaultTableModel(datos, etiquetas);
			modelo.setColumnIdentifiers(etiquetas); //esto puede sobrar
			modelo.setDataVector(datos, etiquetas); //esto puede sobrar
			
			// Asignamos el modelo a la tabla
			table.setModel(modelo);
			Color fg = Color.PINK;
			table.setBackground(fg);
			table.setForeground(Color.BLUE);

			resul.close();
			conexion.close();
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
