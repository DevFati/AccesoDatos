package clases;

import org.neodatis.odb.ODB;
import org.neodatis.odb.ODBFactory;
import org.neodatis.odb.Objects;

public class VisualizarDatos {
	public static void main(String[] args) {
		ODB odb = ODBFactory.open("equipos.db");// Abrir BD
		Objects<Jugadores> jugadores = odb.getObjects(Jugadores.class);
		System.out.println(jugadores.size() + " Jugadores:");
		int i = 1;
		for (Jugadores jug : jugadores) {
			System.out.println((i++) + ": " + jug.getNombre() + "*" + jug.getDeporte() + "*" + jug.getCiudad() + "*"+ jug.getEdad()+ "*"+jug.getPais());
		}
		Objects<Paises> paises = odb.getObjects(Paises.class);
		System.out.println();
		System.out.println(paises.size() + " Paises:");
		for (Paises p : paises) {
			System.out.println(p.getId()+"*"+p.getNombrePais());
		}
		odb.close();
	}
}
