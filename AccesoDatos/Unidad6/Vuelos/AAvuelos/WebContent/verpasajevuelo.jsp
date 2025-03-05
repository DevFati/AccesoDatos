<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%@ page import="compVuelos.Vuelos, compVuelos.Pasaje, java.util.* "%>
<!DOCTYPE html>
<html>
<head>
<meta charset="ISO-8859-1">
<title>Insert title here</title>
</head>
<body>
	<%
	ArrayList<Vuelos> lista = (ArrayList) request.getAttribute("lista");
	String ident = (String) request.getAttribute("ident");
	if (ident == null) ident = "";
	Vuelos vu = (Vuelos) request.getAttribute("vu");
	ArrayList<Pasaje> listapasaje = (ArrayList) request.getAttribute("listapasaje");
	%>

	<div align="center">
		<h1 align="center">OPERACIONES CON VUELOS</h1>
		<h2 align="center">EXAMEN REALIZADO POR: Nombre alumno</h2>
		<hr>

		<h2>Ver el detalle del pasaje del vuelo</h2>
		<br>
		<form action="/AAvuelos/Controlador?accion=verpasv" method="post">

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
			</select> <input type="submit" name="verpasv" value="Ver pasaje del vuelo" />
		</form>
		<br> 
		<%
			if (vu != null) {
			out.println("<h3>DATOS DEL VUELO: " + ident + "</h3>");
			out.println("Aeropuerto de origen: " + vu.getAeropuertoorigen());
			out.println("<br>Aeropuerto de destino: " + vu.getAeropuertodestino());
			out.println("<br>Tipo de vuelo: " + vu.getTipovuelo());
			out.println("<br>Fecha de vuelo: " + vu.getFechavuelo());
			out.println("<br>Porcentaje de descuento: " + vu.getDescuento());
		}
		%>
		<hr>
		
		<%
		if (listapasaje != null) {
		%>
		<h2>LISTADO DEL PASAJE DEL VUELO: <%=ident%></h2>
		<%
		if (listapasaje.size() == 0) {
			out.println("<h3>VUELO SIN PASAJE</h3>");

		} else {
		%>

		<table border=1>
			<tr>
				<th>Id de <br>pasaje</th>
				<th>Código <br>pasajero</th>
				<th>Nombre pasajero</th>
				<th>País pasajero</th><th>Número de<br> asiento</th>
				<th>Clase</th><th>PVP pasaje</th><th>Descuento</th><th>Importe<br>Total</th>
			</tr>
			<%
			 float suma=0;
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
				float descuento = pas.getPvp() * vu.getDescuento()/100;
				out.println("<td>" + descuento + "</td>");
				float pvp = pas.getPvp() - descuento;
				out.println("<td>" + pvp + "</td>");
				out.println("</tr>");
				suma= suma + pvp;

			}
		
			%>
		</table>

		<%
		out.println("<h3>SUMA DE IMPORTE TOTAL: " + suma + "</h3>");
		}
		%>

		<%
		}
		%>
		<hr>

		<p align='center'>
			<a href="/AAvuelos/index.jsp">Volver al inicio</a> |
		    <a href="/AAvuelos/Controlador?accion=listarvuelos"> Listar los vuelos</a>
	    </p>
	
	
	
		<br>
	</div>
</body>
</html>