package principal;

import java.math.BigInteger;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import clases.*;

public class Principal {
	private static SessionFactory sesion;

	public static void main(String[] args) {
		Logger.getLogger("org.hibernate").setLevel(Level.OFF);

		sesion = Conexion.getSession();

		Scanner teclado = new Scanner(System.in);
		int opcion = 0;
		do {
			menu();
			opcion = teclado.nextInt();

			switch (opcion) {

			case 1:
				listarCursos();
				break;
			case 2:
				alumnosConMayorNotaMedia();
				asignaturasConMasAlumnos();
				
				break;
			case 3:
				nuevosAlumnos();
				break;
			case 0:
				System.out.println("Adios!");
				break;
			default:
				System.out.println("Opcion invalida!");
				break;
			}
		} while (opcion != 0);

		sesion.close();
	}


		
		
	private static void nuevosAlumnos() {
	    Session session = sesion.openSession();
	    String hqlNuevosAlumnos = "FROM Nuevosalumnos";
	    Query<Nuevosalumnos> query = session.createQuery(hqlNuevosAlumnos, Nuevosalumnos.class);
	    List<Nuevosalumnos> lista = query.list();

	    Transaction tx = session.beginTransaction();

	    for (Nuevosalumnos nuevo : lista) {
	        if (!tx.isActive()) {
	            tx = session.beginTransaction();
	        }

	        String hqlExisteAlumno = "FROM Alumnos WHERE codalum = :codalum";
	        Query<Alumnos> queryExiste = session.createQuery(hqlExisteAlumno, Alumnos.class);
	        queryExiste.setParameter("codalum", nuevo.getCodalum());
	        Alumnos alumnoExistente = queryExiste.uniqueResult();
	        // Comprobar si el alumno ya existe por nombre
	        String hqlExisteNombre = "FROM Alumnos WHERE nombre = :nombre";
	        Query<Alumnos> queryExisteNombre = session.createQuery(hqlExisteNombre, Alumnos.class);
	        queryExisteNombre.setParameter("nombre", nuevo.getNombre());
	        Alumnos alumnoConMismoNombre = queryExisteNombre.uniqueResult();

	        if (alumnoConMismoNombre != null) {
	            System.out.println("El alumno ya existe con el mismo nombre: " + nuevo.getNombre() + " (No se vuelve a insertar).");
	            continue; //paso
	        }

	        // Si el código ya existe, asignar un nuevo código
	        String hqlMaxCod = "SELECT MAX(codalum) FROM Alumnos";
	        BigInteger maxCod = (BigInteger) session.createQuery( hqlMaxCod).uniqueResult();
	        BigInteger nuevoCodigo;

	        if (maxCod != null) {
	            nuevoCodigo =BigInteger.valueOf(maxCod.intValue()+1);   
	        } else {
	            nuevoCodigo = nuevo.getCodalum();
	        }
	        Alumnos nuevoAlumno = new Alumnos();
	        nuevoAlumno.setCodalum(nuevoCodigo);
	        nuevoAlumno.setNombre(nuevo.getNombre());
	        nuevoAlumno.setDireccion(nuevo.getDireccion());
	        nuevoAlumno.setPoblacion(nuevo.getPoblacion());
	        nuevoAlumno.setTelef(nuevo.getTelef());

	        // Asignar curso y representante según población
	        if ("Talavera".equalsIgnoreCase(nuevo.getPoblacion())) {
	            Alumnos representante = session.get(Alumnos.class, 1);
	            Cursos curso = session.get(Cursos.class, 1);
	            nuevoAlumno.setAlumnos(representante);
	            nuevoAlumno.setCursos(curso);
	        } else if ("Toledo".equalsIgnoreCase(nuevo.getPoblacion())) {
	            Alumnos representante = session.get(Alumnos.class, 8);
	            Cursos curso = session.get(Cursos.class, 4);
	            nuevoAlumno.setAlumnos(representante);
	            nuevoAlumno.setCursos(curso);
	        } else {
	            Alumnos representante = session.get(Alumnos.class, 11);
	            Cursos curso = session.get(Cursos.class, 5);
	            nuevoAlumno.setAlumnos(representante);
	            nuevoAlumno.setCursos(curso);
	        }

	        // Insertar nuevo alumno
	        session.persist(nuevoAlumno);
	        tx.commit();

	        System.out.println("Se ha insertado un nuevo alumno: " + nuevoAlumno.getNombre() + " con código " + nuevoCodigo);
	    }

	    session.close();
	}




	

		private static void asignaturasConMasAlumnos() {
		    Session session = sesion.openSession();

		    String hqlAsignaturas = """
		        SELECT a.codasig, a.nombreasig, COUNT(m.alumnos.codalum) AS numAlumnos
		        FROM Matriculas m
		        JOIN m.asignaturas a
		        GROUP BY a.codasig, a.nombreasig
		        HAVING COUNT(m.alumnos.codalum) = (
		            SELECT MAX(COUNT(m2.alumnos.codalum))
		            FROM Matriculas m2
		            GROUP BY m2.asignaturas.codasig
		        )
		    """;

		    Query<Object[]> query = session.createQuery(hqlAsignaturas, Object[].class);
		    List<Object[]> resultados = query.list();

		    System.out.println("Asignaturas con más alumnos:");
		    for (Object[] resultado : resultados) {
		        BigInteger codigo = (BigInteger) resultado[0];
		        String nombre = (String) resultado[1];
		        Long numAlumnos = (Long) resultado[2];
		        System.out.printf("Código: %d | Nombre: %s | Número de alumnos: %d%n", codigo, nombre, numAlumnos);
		    }

		    session.close();
		}


		private static void alumnosConMayorNotaMedia() {
		    Session session = sesion.openSession();

		    String hqlAlumnos = """
		        SELECT a.codalum, a.nombre, AVG(m.notaasig) AS notaMedia
		        FROM Matriculas m
		        JOIN m.alumnos a
		        GROUP BY a.codalum, a.nombre
		        HAVING AVG(m.notaasig) = (
		            SELECT MAX(AVG(m2.notaasig))
		            FROM Matriculas m2
		            GROUP BY m2.alumnos.codalum
		        )
		    """;

		    Query<Object[]> query = session.createQuery(hqlAlumnos, Object[].class);
		    List<Object[]> resultados = query.list();

		    System.out.println("Alumnos con mayor nota media:");
		    for (Object[] resultado : resultados) {
		        BigInteger codigo = (BigInteger) resultado[0];
		        String nombre = (String) resultado[1];
		        Double notaMedia = (Double) resultado[2];
		        System.out.printf("Código: %d | Nombre: %s | Nota media: %.2f%n", codigo, nombre, notaMedia);
		    }

		    session.close();
		}


		private static void listarCursos() {
		    Session session = sesion.openSession();
		    String con = "from Cursos";
		    Query<Cursos> q = session.createQuery(con, Cursos.class);

		    List<Cursos> lista = q.getResultList();

		    for (Cursos curso : lista) {
		        System.out.printf("%-20s %-30s %-30s %20s%n",
		                          "CODCURSO:", curso.getCodcurso(),
		                          "DENOMINACIÓN:", curso.getDenominacion());
		        System.out.printf("%-20s %-30s %-30s %20s%n",
		                          "PRECIO:", curso.getPrecio(),
		                          "NIVEL:", curso.getNivel());

		        Set<Cursoasig> listaCursoAsigs = curso.getCursoasigs();
		        Set<Alumnos> listaAlumnos = curso.getAlumnoses();

		        System.out.printf("%-20s %-30s %-30s %20s%n",
		                          "Número de alumnos:", listaAlumnos.size(),
		                          "Número de asignaturas:", listaCursoAsigs.size());

		        System.out.printf("%-10s %-30s %-20s %20s %10s %10s %10s%n",
		                          "CODASIG", "NOMBREASIG", "PRECIOASIG",
		                          "TIPOASIG", "%INCREMENTO", "NUM_ALUMNOS", "TOTALASIG");
		        System.out.printf("%-10s %-30s %-20s %20s %10s %10s %10s%n",
		                          "-------", "------------------------------",
		                          "--------------------", "--------------------",
		                          "----------", "----------", "----------");

		        int totalNumAlumnos = 0;
		        double totalAsignaturas = 0;

		        for (Cursoasig cursoasig : listaCursoAsigs) {
		            Asignaturas a = cursoasig.getAsignaturas();

		            
		            double incrementoPorcentaje = 0;
		            switch (a.getTipoasig()) {
		                case 'A':
		                    incrementoPorcentaje = 0.05;
		                    break;
		                case 'B':
		                    incrementoPorcentaje = 0.06;
		                    break;
		                case 'C':
		                    incrementoPorcentaje = 0.08;
		                    break;
		                case 'D':
		                    incrementoPorcentaje = 0.10;
		                    break;
		            }

		            double incremento = a.getPrecioasig().doubleValue() * incrementoPorcentaje;
		            
		          

	
		            String hqlNumAlumnos = """
		            	    SELECT COUNT(DISTINCT m)
		            	    FROM Matriculas m
		            	    WHERE m.asignaturas.codasig = :codAsig
		            	""";
		            Query<Long> queryNumAlumnos = session.createQuery(hqlNumAlumnos, Long.class);
		            queryNumAlumnos.setParameter("codAsig", a.getCodasig());
		            int numAlumnosAsignatura = queryNumAlumnos.uniqueResult().intValue();

		            double totalAsignatura = (a.getPrecioasig().doubleValue() + incremento) * numAlumnosAsignatura;

		            if(totalAsignatura!=0) {
			            System.out.printf("%-10s %-30s %-20s %20s %10s %10d %10s%n",
			                              a.getCodasig(), a.getNombreasig(),
			                              a.getPrecioasig(), a.getTipoasig(),
			                              incremento , numAlumnosAsignatura, totalAsignatura);

			            // Acumular totales
			            totalNumAlumnos += numAlumnosAsignatura;
			            totalAsignaturas += totalAsignatura;
		            }
		          
		        }

		        // Mostrar totales por curso
		        
		        
		        System.out.printf("%-20s %-30d %-30s %20.2f %n",
		                          "TOTAL ALUMNOS:", totalNumAlumnos,
		                          "TOTAL ASIGNATURAS:", totalAsignaturas);
		        System.out.println("--------------------------------------------------------------------------------");
		    }

		    session.close();
		}
		
		
		

		
	


	private static void menu() {
		System.out.println("Elige una opcion");
		System.out.println("1 - Listar cursos");
		System.out.println("2-  Ejercicio 2");
		System.out.println("3-  Ejercicio 3");
		System.out.println("0 - Salir");
	}


}
