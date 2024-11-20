package principal;


import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class Conexion {

	private static final SessionFactory sessionFactory;
	static {
		try {
			// Inicializa entorno Hibernate. Crea la SessionFactory de hibernate.cfg.xml
			sessionFactory = new Configuration().configure().buildSessionFactory();
		} catch (Throwable ex) {
			// Throwable es la clase base d las excepciones.
			System.err.println("INICIO DE SessionFactory fallado.." + ex);
			throw new ExceptionInInitializerError(ex);
			// esta excepción se lanza cuando hay un error al inicializar una variable
			// estática
		}
	}

	public static SessionFactory getSession() {
		return sessionFactory;
	}
}
