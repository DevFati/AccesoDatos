package clases;

import org.neodatis.odb.ODB;
import org.neodatis.odb.ODBFactory;
import org.neodatis.odb.Objects;
import org.neodatis.odb.core.query.IQuery;
import org.neodatis.odb.core.query.criteria.Where;
import org.neodatis.odb.impl.core.query.criteria.CriteriaQuery;

public class Principal {
	private static ODB odb;
	public static void main(String[] args) {
		odb = ODBFactory.open("neodatis.test");// Abrir BD
		visualizarConPais();
		filtrarPorPais("Alemania");
		aumentarEdadA(15);
	}
	
	private static void visualizarConPais() {
		System.out.println("Metodo visualizar con pais");
		IQuery query = new CriteriaQuery(Jugadores.class, Where.equal("deporte", "tenis"));
		query.orderByAsc("nombre").orderByAsc("edad"); // Ordena ascendentemente por nombre y edad

		// Obtiene todos los jugadores DE LA CONSULTA
		Objects<Jugadores> objects2 = odb.getObjects(query);
		for (Jugadores jug : objects2) {
			System.out.println("\t: " + jug.getNombre() + "*" + jug.getDeporte() + "*" + jug.getCiudad() + "*"+ jug.getEdad()+"*"+jug.getPais());
		}
	}
	private static void filtrarPorPais(String pais) {
		System.out.println("Metodo filtrar por pais");
		IQuery query = new CriteriaQuery(Jugadores.class,Where.equal("pais.nombrePais",pais));
		query.orderByAsc("nombre").orderByAsc("edad"); // Ordena ascendentemente por nombre y edad
		// Obtiene todos los jugadores DE LA CONSULTA
		Objects<Jugadores> objects = odb.getObjects(query);
		for (Jugadores jug : objects) {
			System.out.println("\t: " + jug.getNombre() + "*" + jug.getDeporte() + "*" + jug.getCiudad() + "*"+ jug.getEdad()+"*"+jug.getPais());
		}
	}
	private static void aumentarEdadA(int edad) {
		System.out.println("Metodo aumentar edad a los de "+edad);
		IQuery query = new CriteriaQuery(Jugadores.class,Where.equal("edad",edad));
		query.orderByAsc("nombre").orderByAsc("edad"); // Ordena ascendentemente por nombre y edad
		// Obtiene todos los jugadores DE LA CONSULTA
		Objects<Jugadores> objects = odb.getObjects(query);
		System.out.println("Jugadores modificados:");
		for (Jugadores jug : objects) {
			jug.setEdad(edad+1);
			odb.store(jug);
			System.out.println("\t: " + jug.getNombre() + "*" + jug.getDeporte() + "*" + jug.getCiudad() + "*"+ jug.getEdad()+"*"+jug.getPais());
		}
		odb.commit();
	}
}
