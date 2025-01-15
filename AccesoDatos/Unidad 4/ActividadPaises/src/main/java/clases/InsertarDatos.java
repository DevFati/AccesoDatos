package clases;

import org.neodatis.odb.ODB;
import org.neodatis.odb.ODBFactory;

public class InsertarDatos {
	public static void main(String[] args) {
		ODB odb = ODBFactory.open("equipos.db");// Abrir BD
		// Crear instancias para almacenar en BD
		Paises p1=new Paises(1,"España");
		Paises p2=new Paises(2,"Estados Unidos");
		Paises p3=new Paises(3,"Alemania");
		odb.store(p1);
		odb.store(p2);
		odb.store(p3);
		
		Jugadores j1 = new Jugadores("Maria", "voleibol", "Madrid", 14,p1);
		Jugadores j2 = new Jugadores("Miguel", "tenis", "Madrid", 15,p3);
		Jugadores j3 = new Jugadores("Mario", "baloncesto", "Guadalajara", 15,p1);
		Jugadores j4 = new Jugadores("Alicia", "tenis", "Madrid", 14,p2);
		odb.store(j1);
		odb.store(j2);
		odb.store(j3);
		odb.store(j4);
		System.out.println("Datos insertados");
		odb.close();
	}
}
