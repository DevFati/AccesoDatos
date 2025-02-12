package control;
import componenteDept.*;
import java.io.IOException;
import java.util.ArrayList;


import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class Controlador
*/
@WebServlet("/Controlador")
public class Controlador extends HttpServlet {

	  //Se usa el component depDAO
	   DepartamentoDAO depDAO = new DepartamentoImpl();

	  public void service(HttpServletRequest request, 
	                      HttpServletResponse response) 
	         throws ServletException, IOException {

	     //Se obtiene la acción a realizar, parámetro accion
	     String op = request.getParameter("accion");

	     //Si es alta se muestra la pantalla de alta de departamento
	     if (op.equals("alta")) {
	         response.sendRedirect("alta.jsp");
	     }

	     //Si es listado, se obtienen los datos de los departamentos
	     //después se envían a listado.jsp
	     if (op.equals("listado")) {
	         ArrayList lista = depDAO.Listar();

	         request.setAttribute("departamentos", lista);
	        
                RequestDispatcher rd =request.getRequestDispatcher("/listado.jsp");
	         rd.forward(request, response);
	     }

	     //Se obtienen los datos de la página JSP
	     //y luego se inserta el departamento en la BD
	     if (op.equals("insertar")) {
	         pantalla.Departamentos dep =  (pantalla.Departamentos)
	              request.getAttribute("depart"); 
	         Departamento departamento = new Departamento
	                (dep.getDeptno(), dep.getDnombre(), dep.getLoc());

	         boolean insertar = depDAO.InsertarDep(departamento);
	         String mensaje = "";

	         if (insertar) {
	             mensaje = "Departamento " + dep.getDeptno() +
	                       " insertado";
	         } else {
	             mensaje = "Error al insertar Departamento " +
	                       dep.getDeptno();
	         }
	         System.out.println(mensaje);

	         response.sendRedirect("index.html");          
	     }
	  }
	}
//
