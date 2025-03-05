<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
 <%@ page import="compVuelos.Pasaje, java.util.* " %>
<!DOCTYPE html>
<html>
<head>
<meta charset="ISO-8859-1">
<title>Insert title here</title>
</head>
<body>
<%
ArrayList<Pasaje> listapasaje = (ArrayList) request.getAttribute("lista");
String mensaje= (String) request.getAttribute("mensaje");
%>
	<div align="center">
	  <h2>LISTADO DE PASAJE</h2>
 <%if (mensaje!=null) 
	   out.println("<h2>"+mensaje+"</h2>");
	   %>
 
	  <table>
			<tr>
				<th>Id de pasaje</th>
				<th>Código pasajero</th>
				<th>nombre pasajero</th>
				<th>País pasajero</th>
				<th>Número de asiento</th>
				<th>clase</th>
				<th>PVP</th>
				<th>BORRAR/MODIFICAR</th>
				
			</tr>
			<%
				for (int i = 0; i < listapasaje.size(); i++) {
				Pasaje pas = listapasaje.get(i);
				out.println("<tr>");
				out.println("<td>" + pas.getIdpasaje() + "</td>");
				out.println("<td>" + pas.getPasajerocod() + "</td>");
				out.println("<td>" + pas.getNombre() + "</td>");
				out.println("<td>" + pas.getPais() + "</td>");
				out.println("<td>" + pas.getNumasiento() + "</td>");
				out.println("<td>" + pas.getClase() + "</td>");
				out.println("<td>" + pas.getPvp() + "</td>");
				%>
				<td>
				<a href="/AAvuelos/Controlador?accion=borrar&id=<%=pas.getIdpasaje() %>"> Borrar	</a>
				<a href="/AAvuelos/Controlador?accion=modificar&id=<%=pas.getIdpasaje() %>">| Modificar	</a>		
				</td>
				<%
				out.println("</tr>");

			}
			%>
		</table>
	  
	   <p align='center'>
            <a href = "/AAvuelos/index.jsp">Volver al inicio</a>
        </p>
	  
	</div>



</body>
</html>