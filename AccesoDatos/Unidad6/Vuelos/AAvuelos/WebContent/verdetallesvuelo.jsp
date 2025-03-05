<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%@ page import="compVuelos.Vuelos, compVuelos.Pasaje, java.util.* "%>
<!DOCTYPE html>
<html>
<head>
<meta charset="ISO-8859-1">
<title>verpasaje</title>
</head>
<body>
	<%
		ArrayList<Vuelos> lista = (ArrayList) request.getAttribute("lista");
		String ident = (String) request.getAttribute("ident");
		if (ident == null)
			ident = "";

		Vuelos vu = (Vuelos) request.getAttribute("vuelo");

		ArrayList<Pasaje> listapasaje = (ArrayList) request.getAttribute("listapasaje");
	%>

	<div align="center">
		<h2>Detalle de los vuelos</h2>
		<br>
		<br>
		<form action="/AAvuelos/Controlador" method="post">

			Selecciona un vuelo: <select name='identificador'>
				<%
					String selec = "";
				for (int i = 0; i < lista.size(); i++) {
					Vuelos dep = (Vuelos) lista.get(i);
					selec = "";
					if (ident.equals(dep.getIdentificador())) {
						selec = "selected";
					}
					out.println("<option " + selec + " value='" + dep.getIdentificador() + "'>" + dep.getIdentificador() + "</option>");
				}
				%>
			</select> <input type="submit" name="verdetallevuelo"
				value="Ver detalle del vuelo" /> <input type="submit"
				name="verpasajevuelo" value="Ver pasaje del vuelo" />
		</form>
		<br>
		<br>
		<hr>
		<%
			if (vu != null) {
			out.println("<h2>DETALLE DEL VUELO CON IDENTIFICADOR: " + ident + "</h2>");

			out.println("<br>Aeropuerto de origen: " + vu.getAeropuertoorigen());
			out.println("<br>Nombre aeropuerto de origen: " + vu.getNombreorigen());
			out.println("<br>Pais aeropuerto de origen:" + vu.getPaisorigen());
			out.println("<br>Aeropuerto de destino: " + vu.getAeropuertodestino());
			out.println("<br>Nombre Aeropuerto de destino: " + vu.getNombredestino());
			out.println("<br>Nombre pais de destino: " + vu.getPaisdestino());
			out.println("<br>Tipo de vuelo: " + vu.getTipovuelo());
			out.println("<br>Número de pasajeros: " + vu.getNumero());
		}
		%>


		<%
			if (listapasaje != null) {
		%>
		<h2>LISTADO DEL PASAJE DELVUELO: <%=ident%></h2>
		<%
			if (listapasaje.size() == 0) {
				out.println("<h3>VUELO SIN PASAJE</h3>");
            
		} else {
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
				out.println("</tr>");

			}
			%>
		</table>

		<%
			}
		%>

		<%
			}
		%>
		<hr>
		
        <p align='center'>
            <a href = "/AAvuelos/index.jsp">Volver al inicio</a>
        </p>
       <br> 
	</div>
</body>
</html>