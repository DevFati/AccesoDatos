<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@ page import="compVuelos.Vuelos, java.util.* " %>
<!DOCTYPE html>
<html>
<head>
<meta charset="ISO-8859-1">
<title>listado vuelos</title>
</head>
<body>
<%
ArrayList<Vuelos> lista = (ArrayList) request.getAttribute("lista");
String mensaje= (String) request.getAttribute("mensajeborrar");
%>

	<div align="center">
	<h1 align="center">OPERACIONES CON VUELOS</h1>
	<h2 align="center">EXAMEN REALIZADO POR: Nombre alumno</h2>
	<hr>
	  <h2>LISTADO DE LOS VUELOS</h2>
	  
	  	  <%
	
	  	    if (mensaje!=null) {out.println("<h3>"+mensaje+"</h3>");}
	  	  %>
      
	  <table border=1><tr><th>Identificador</th><th>Aeropuerto <br>de origen</th>
	  <th>Nombre<br>Aeropuerto</th><th>País de<br>origen</th><th>Aeropuerto <br>de destino</th>
	  <th>Nombre<br>Aeropuerto</th><th>País de<br>destino</th><th>Tipo Vuelo</th>
	  <th>Número <br>Pasajeros</th>
	  <th>Borrar<br>vuelo</th>
	  </tr>
	  <%
	 
	   for (int i=0; i<lista.size();i++){
		   Vuelos vu=lista.get(i);
		   out.println("<tr>");
		   out.println("<td>"+vu.getIdentificador()+"</td>");
		   out.println("<td>"+vu.getAeropuertoorigen()+"</td>");
		   out.println("<td>"+vu.getNombreorigen()+"</td>");
		   out.println("<td>"+vu.getPaisorigen()+"</td>");
		   out.println("<td>"+vu.getAeropuertodestino()+"</td>");
		   out.println("<td>"+vu.getNombredestino()+"</td>");
		   out.println("<td>"+vu.getPaisdestino()+"</td>");
		   out.println("<td>"+vu.getTipovuelo()+"</td>");
		   out.println("<td>"+vu.getNumero()+"</td>");
		   if (vu.getNumero() == 0 )
		   out.println("<td><a href='/AAvuelos/Controlador?accion=borrarvuelo&id="+ vu.getIdentificador()+"'>Borrar</a></td>");
		   else
			  out.println("<td>Borrar</td>");
		    
		   out.println("</tr>");
		   
	   }
	  
	  %>
	  	  
	  </table>
	  
	   <p align='center'>
            <a href = "/AAvuelos/index.jsp">Volver al inicio</a> |
		  <a href="/AAvuelos/Controlador?accion=verpasajevuelo"> Ver el pasaje de un vuelo</a>
	</p>
	  
	</div>
</body>
</html>