import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import compVuelos.Pasaje;
import compVuelos.Pasajero;
import compVuelos.Vuelos;
import compVuelos.VuelosDAO;
import compVuelos.VuelosImpl;

/**
 * Servlet implementation class Controlador
 */
@WebServlet("/Controlador")
public class Controlador extends HttpServlet {
	private static final long serialVersionUID = 1L;

	VuelosDAO vuelos = new VuelosImpl();

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public Controlador() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		// response.getWriter().append("Served at: ").append(request.getContextPath());

		String accion = request.getParameter("accion");


		if (accion!=null && accion.equals("borrarvuelo")) {
			// sólo se borrarán los vuelos con 0 pasajeros.
			String idvuelo = request.getParameter("id");
			System.out.println("borarr " + idvuelo);
			String mensaje = vuelos.borrarvuelo(idvuelo);
			System.out.println("mensaje " + mensaje);
			ArrayList<Vuelos> lista = vuelos.getVuelos();

			request.setAttribute("mensajeborrar", mensaje);
			request.setAttribute("lista", lista);
			RequestDispatcher rd = request.getRequestDispatcher("/listadovuelos.jsp");
			rd.forward(request, response);


		}



		if (accion!=null && accion.equals("verpasajevuelo")) {
			ArrayList<Vuelos> lista = vuelos.getVuelos();
			request.setAttribute("lista", lista);
			RequestDispatcher rd = request.getRequestDispatcher("/verpasajevuelo.jsp");
			rd.forward(request, response);

		}

		if (accion!=null && accion.equals("listarvuelos")) {
			// Cargamos los datos de los vuelos

			System.out.println("entro");
			ArrayList<Vuelos> lista = vuelos.getVuelos();

			request.setAttribute("lista", lista);
			RequestDispatcher rd = request.getRequestDispatcher("/listadovuelos.jsp");
			rd.forward(request, response);

		}

		if (accion!=null && accion.equals("iradetalle")) {

			ArrayList<Vuelos> lista = vuelos.getVuelos();
			request.setAttribute("lista", lista);
			RequestDispatcher rd = request.getRequestDispatcher("/verdetallesvuelo.jsp");
			rd.forward(request, response);

		}

		if ( accion!=null && accion.equals("modificarborrar")) {

			ArrayList<Pasaje> lista = vuelos.getPasajes();
			request.setAttribute("lista", lista);
			RequestDispatcher rd = request.getRequestDispatcher("/modificarborrarpasaje.jsp");
			rd.forward(request, response);

		}

		if (accion!=null && accion.equals("borrar")) {

			String id = request.getParameter("id");
			System.out.println("borarr " + id);
			String mensaje = vuelos.borrarpasaje(Integer.parseInt(id));

			ArrayList<Pasaje> lista = vuelos.getPasajes();
			request.setAttribute("lista", lista);
			request.setAttribute("mensaje", mensaje);
			RequestDispatcher rd = request.getRequestDispatcher("/modificarborrarpasaje.jsp");
			rd.forward(request, response);

		}

		if (accion!=null && accion.equals("modificar")) {

			String id = request.getParameter("id");
			System.out.println("modificar " + id);

			Pasaje pasaje = vuelos.getPasaje(Integer.parseInt(id));

			ArrayList<Vuelos> lista = vuelos.getVuelos();
			ArrayList<Pasajero> listapas = vuelos.getPasajeros();

			request.setAttribute("titulo", "MODIFICAR PASAJE: " + id);
			request.setAttribute("listavuelos", lista);
			request.setAttribute("listapasajeros", listapas);
			request.setAttribute("pasaje", pasaje);

			RequestDispatcher rd = request.getRequestDispatcher("/insertarmodificar.jsp");
			rd.forward(request, response);

		}

		if (accion!=null && accion.equals("insertarpasaje")) {

			ArrayList<Vuelos> lista = vuelos.getVuelos();
			ArrayList<Pasajero> listapas = vuelos.getPasajeros();

			request.setAttribute("titulo", "INSERTAR PASAJE");
			request.setAttribute("listavuelos", lista);
			request.setAttribute("listapasajeros", listapas);
			RequestDispatcher rd = request.getRequestDispatcher("/insertarmodificar.jsp");
			rd.forward(request, response);

		}

	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		// doGet(request, response);

		// name="verdetallevuelo"

		String accion = request.getParameter("accion");

		if (accion != null && accion.equals("verpasv")) {

			String identificador = request.getParameter("identificador");

			ArrayList<Pasaje> listapasaje = vuelos.getPasajeVuelo(identificador);
			Vuelos vuelo = vuelos.getVuelo(identificador);
			ArrayList<Vuelos> lista = vuelos.getVuelos();

			request.setAttribute("vu", vuelo);
			request.setAttribute("ident", identificador);
			request.setAttribute("listapasaje", listapasaje);
			request.setAttribute("lista", lista);
			RequestDispatcher rd = request.getRequestDispatcher("/verpasajevuelo.jsp");
			rd.forward(request, response);
		}

		String verdetallevuelo = request.getParameter("verdetallevuelo");

		if (verdetallevuelo != null) {

			System.out.println("entro  verdetallevuelo");

			String identificador = request.getParameter("identificador");

			Vuelos vuelo = vuelos.getVuelo(identificador);

			ArrayList<Vuelos> lista = vuelos.getVuelos();

			request.setAttribute("ident", identificador);
			request.setAttribute("vuelo", vuelo);
			request.setAttribute("lista", lista);
			RequestDispatcher rd = request.getRequestDispatcher("/verdetallesvuelo.jsp");
			rd.forward(request, response);

		}

		String verpasajevuelo = request.getParameter("verpasajevuelo");

		if (verpasajevuelo != null) {

			System.out.println("entro  verpasajevuelo");
			String identificador = request.getParameter("identificador");

			ArrayList<Pasaje> listapasaje = vuelos.getPasajeVuelo(identificador);

			ArrayList<Vuelos> lista = vuelos.getVuelos();

			request.setAttribute("ident", identificador);
			request.setAttribute("listapasaje", listapasaje);
			request.setAttribute("lista", lista);
			RequestDispatcher rd = request.getRequestDispatcher("/verdetallesvuelo.jsp");
			rd.forward(request, response);

		}

	}

}
