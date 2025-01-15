package ventanas;

import java.awt.EventQueue;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JLabel;

import java.awt.Font;

import javax.swing.SwingConstants;

import java.awt.Color;

import javax.swing.JButton;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Toolkit;

public class Inicio extends JFrame {

	private JPanel contentPane;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Inicio frame = new Inicio();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	public Inicio() {
		setIconImage(Toolkit.getDefaultToolkit().getImage("dibujoZorro.png"));
		setTitle("Ejercicio Neodatis");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 488, 344);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		JLabel lblMantenimientoDeDatos = new JLabel("Mantenimiento de datos");
		lblMantenimientoDeDatos.setBackground(new Color(51, 255, 255));
		lblMantenimientoDeDatos.setForeground(new Color(0, 0, 128));
		lblMantenimientoDeDatos.setHorizontalAlignment(SwingConstants.CENTER);
		lblMantenimientoDeDatos.setFont(new Font("Tahoma", Font.BOLD, 14));
		lblMantenimientoDeDatos.setBounds(84, 11, 253, 37);
		contentPane.add(lblMantenimientoDeDatos);
		
		JPanel panel = new JPanel();
		panel.setBackground(new Color(175, 238, 238));
		panel.setBounds(40, 44, 394, 245);
		contentPane.add(panel);
		panel.setLayout(null);
		
		JButton btnMantenimientoDeVentas = new JButton("Mantenimiento de ventas.");
		btnMantenimientoDeVentas.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				
				VentanaDialogoVentas dialog = new VentanaDialogoVentas();
				dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
				dialog.setVisible(true);
				
			}
		});
		btnMantenimientoDeVentas.setBounds(71, 79, 264, 37);
		panel.add(btnMantenimientoDeVentas);
		btnMantenimientoDeVentas.setFont(new Font("Arial Black", Font.BOLD, 12));
		
		JButton visualizacion = new JButton("Visualización de datos.");
		visualizacion.setBounds(71, 127, 264, 37);
		panel.add(visualizacion);
		visualizacion.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				VentanaDialogoVisualizacionDatos dialog = new VentanaDialogoVisualizacionDatos();
				dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
				dialog.setVisible(true);
			}
		});
		visualizacion.setFont(new Font("Arial Black", Font.BOLD, 12));
		
		JButton creacion = new JButton("Creación de datos.");
		creacion.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				
				VentanaDialogoCrearFicheros dialog = new VentanaDialogoCrearFicheros();
				dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
				dialog.setVisible(true);
			}
		});
		creacion.setBounds(71, 175, 264, 37);
		panel.add(creacion);
		creacion.setFont(new Font("Arial Black", Font.BOLD, 12));
		
		JButton btnMantenimientoDeArtculos = new JButton("Mantenimiento de Art\u00EDculos.");
		btnMantenimientoDeArtculos.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				VentanaDialogoArticc dialog = new VentanaDialogoArticc();
				dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
				dialog.setVisible(true);
			}
		});
		btnMantenimientoDeArtculos.setFont(new Font("Arial Black", Font.BOLD, 12));
		btnMantenimientoDeArtculos.setBounds(71, 31, 264, 37);
		panel.add(btnMantenimientoDeArtculos);
	}
}
