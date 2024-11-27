package principal;

import java.util.List;
import java.util.Set;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import clases.C1Asignaturas;
import clases.C1Centros;
import clases.C1Profesores;

public class Principal {

	private static SessionFactory factori;

	public static void main(String[] args) {

	LogManager.getLogManager().reset();

	Logger globalLogger = Logger.getLogger(java.util.logging.Logger.GLOBAL_LOGGER_NAME);

	globalLogger.setLevel(java.util.logging.Level.OFF);

	

	factori=Conexion.getSession();

	

	verDatosCentro(1000);

	verDatosCentro(1111);

	verDatosCentro(1050);

	

	mostrarDatosProfesor(1011); //Tiene subordinados

	System.out.println("--------------------------");

	mostrarDatosProfesor(2002); //No es jefe

	System.out.println("--------------------------");

	mostrarDatosProfesor(8888); //No existe;

	

	actualizarCentro(1500, "Centro 1500",2000,"C/Las Palmeras 2"); //Centro nuevo, se crea

	actualizarCentro(1050, "Centro 1050",2000,"C/Las Palmeras 2"); //Centro existe, se actualiza

	

	asignarAsignaturaAProfesor(1000,"Nueva2"); //Nueva asignatura

	asignarAsignaturaAProfesor(1000,"IF0006"); //No tiene la asignatura, se le añade

	asignarAsignaturaAProfesor(1000,"IF0001"); //Ya tiene la asignatura

	

	borrarAsignatura("IF0002"); //Existe y tiene registros

	borrarAsignatura("IF000666"); //No existe

	

	listarCentros();

	}

	

	public static void verDatosCentro(int cod) {

	Session session=factori.openSession();

	C1Centros cen=session.get(C1Centros.class, cod);

	if(cen==null) {

	System.out.println("Cod Centro no existe: "+cod);

	}

	else {

	System.out.println("Cod Centro: "+cen.getCodCentro()+" Nombre: "+cen.getNomCentro());

	Set<C1Profesores>lista=cen.getC1Profesoreses();

	if(lista.size()>0) {

	mostrarDatosProfesoresCentro(lista);

	}

	else System.out.println("Centro sin profesores");

	}

	session.close();

	}

	

	public static void mostrarDatosProfesoresCentro(Set<C1Profesores>lista) {

	System.out.printf("%5s %-30s %-30s %-30s %-20s %n","Cod","NombreProfesor","NombreEspecialidad","Nombre Jefe","NumAsig que imparte");

	System.out.printf("%5s %-30s %-30s %-30s %-20s %n","-----","------------------------------","------------------------------","------------------------------","--------------------");

	int maxAsignaturas=0;

	String nombreMaxAsignaturas="";

	for(C1Profesores p:lista) {

	String jefe="NO TIENE";

	String espe="NO TIENE";

	if(p.getC1Especialidad()!=null) espe=p.getC1Especialidad().getNombreEspe();

	if(p.getC1Profesores()!=null) jefe=p.getC1Profesores().getNombreApe();

	System.out.printf("%5s %-30s %-30s %-30s %-20s %n",p.getCodProf(),p.getNombreApe(),espe,jefe,p.getC1Asignaturases().size());

	if(p.getC1Asignaturases().size()>maxAsignaturas) {

	maxAsignaturas=p.getC1Asignaturases().size();

	nombreMaxAsignaturas=p.getNombreApe();

	}

	else if (p.getC1Asignaturases().size()==maxAsignaturas)nombreMaxAsignaturas+=" "+p.getNombreApe();

	}

	System.out.printf("%5s %-30s %-30s %-30s %-20s %n","-----","------------------------------","------------------------------","------------------------------","--------------------");

	System.out.println("Nombre de profesor que imparte mas asignaturas: "+nombreMaxAsignaturas);

	}

	public static void mostrarDatosProfesor(int id) {

	Session session=factori.openSession();

	C1Profesores pf=(C1Profesores)session.get(C1Profesores.class, id);

	if(pf==null) System.out.println("Cod profesor no existe: "+id);

	else{

	System.out.println("Nombre profesor: "+pf.getNombreApe());

	System.out.println("Nombre especialidad: "+pf.getC1Especialidad().getNombreEspe());

	String jefe="SIN JEFE";

	String codigo="SIN COD";

	if(pf.getC1Profesores()!=null) {

	jefe=pf.getC1Profesores().getNombreApe();

	Short codi=pf.getC1Profesores().getCodProf();

	codigo=codi.toString();

	}

	System.out.println("Nombre jefe: "+jefe+" Codigo: "+codigo);

	System.out.println("Nombre del centro: "+pf.getC1Centros().getNomCentro());

	System.out.println("Imparte: "+pf.getC1Asignaturases().size());

	if(pf.getC1Asignaturases().size()>0) {

	System.out.println("\tCOD ASIG NOMBREASIG");

	System.out.println("\t-------- ----------");

	Set<C1Asignaturas>lista=pf.getC1Asignaturases();

	for(C1Asignaturas a:lista) {

	System.out.println("\t"+a.getCodAsig()+" "+a.getNombreAsi());

	}

	System.out.println("\t-------- ----------");

	}

	if(pf.getC1Profesoreses().size()>0) {

	System.out.println("\tCOD PROF NOMBRE PROF");

	System.out.println("\t-------- ----------");

	Set<C1Profesores>lista=pf.getC1Profesoreses();

	for(C1Profesores p:lista) {

	System.out.println("\t"+p.getCodProf()+" "+p.getNombreApe());

	}

	System.out.println("\t-------- ----------");

	}

	System.out.println("Jefe de profesores: "+pf.getC1Profesoreses().size());

	}

	session.close();

	}

	

	private static void actualizarCentro(int codCentro, String nombre, int codDirector, String direccion) {

	Session sesion=factori.openSession();

	C1Centros centro=(C1Centros)sesion.get(C1Centros.class, codCentro);

	Transaction tx=sesion.beginTransaction();

	if(centro==null) { //Si el centro no existe, lo creamos

	centro=new C1Centros();

	centro.setCodCentro((short)codCentro);

	centro.setNomCentro(nombre);

	centro.setDirector((short)codDirector);

	centro.setDireccion(direccion);

	sesion.persist(centro);

	System.out.println("Centro creado: "+codCentro);

	}

	else { //Si existe, lo actualizamos

	centro.setCodCentro((short)codCentro);

	centro.setNomCentro(nombre);

	centro.setDirector((short)codDirector);

	centro.setDireccion(direccion);

	sesion.merge(centro);

	System.out.println("Centro actualizado: "+codCentro);

	}

	//Añadimos el profesor 1000 al centro. Volverlo a cargar

	C1Profesores pro=(C1Profesores)sesion.get(C1Profesores.class, 1000);

	if(pro!=null) { //Si el profesor existe, se añade

	centro.getC1Profesoreses().add(pro);

	sesion.merge(centro);

	System.out.println("Profesor 1000 añadido al centro "+codCentro);

	}

	tx.commit();

	sesion.close();

	}

	

	private static void asignarAsignaturaAProfesor(int codProfesor, String codAsignatura) {

	Session session = factori.openSession();

	Transaction tx=session.beginTransaction();

	C1Profesores profesor=session.get(C1Profesores.class, codProfesor);

	if (profesor == null) {

	System.out.println("El profesor no existe. "+codProfesor);

	}

	else {

	C1Asignaturas asignatura=session.get(C1Asignaturas.class, codAsignatura);

	if(asignatura==null) { //No existe, se crea

	asignatura=new C1Asignaturas();

	asignatura.setCodAsig(codAsignatura);

	asignatura.setNombreAsi(codAsignatura+" NOMBRE");

	System.out.println(codAsignatura+" es nueva");

	session.persist(asignatura);

	

	System.out.println("Asignatura creada: "+codAsignatura);

	}

	boolean yaTiene=false;

	Set<C1Asignaturas>lista=profesor.getC1Asignaturases();

	for(C1Asignaturas a:lista) {

	if(a.getCodAsig().equals(codAsignatura)) {

	yaTiene=true;

	System.out.println("Profesor "+codProfesor+" ya tiene la asignatura "+codAsignatura);

	}

	}

	if(!yaTiene) {

	profesor.getC1Asignaturases().add(asignatura);

	session.merge(asignatura);

	System.out.println("Asignatura "+codAsignatura+" añadida al profesor "+codProfesor);

	}

	}

	try {

	tx.commit();

	}catch( org.hibernate.exception.ConstraintViolationException e) {

	e.printStackTrace();

	}catch( org.hibernate.exception.GenericJDBCException e2){

	System.out.println(e2.getMessage());

	}

	

	session.close();

	}

	

	private static void borrarAsignatura(String codAsig) {

	Session session=factori.openSession();

	C1Asignaturas asi=(C1Asignaturas)session.get(C1Asignaturas.class, codAsig);

	if(asi==null) {

	System.out.println("LA ASIGNATURA A BORRAR NO EXISTE: "+codAsig);

	}

	else {

	Transaction tx=session.beginTransaction();

	session.remove(asi);

	tx.commit();

	System.out.println("LA ASIGNATURA HA SIDO BORRADA: "+codAsig);

	}

	}

	private static void listarCentros() {

	Session session=factori.openSession();

	Query<C1Centros>q=session.createQuery("from C1Centros",C1Centros.class);

	List<C1Centros> lista=q.getResultList();

	if(lista.size()>0) { //Si el tamaño de la lista es mayor que 0

	for(C1Centros c:lista) {

	System.out.println("Cod centro: "+c.getCodCentro()+" Nombre: "+c.getNomCentro()+" Numero profesores: "+c.getC1Profesoreses().size());

	System.out.println("--------------------------------------------------------------------------------------------------------------------");

	if(c.getC1Profesoreses().size()>0) {

	mostrarDatosProfesoresCentro(c.getC1Profesoreses());

	}

	else System.out.println("(No hay profesores en este centro)");

	System.out.println("");

	}

	

	}

	else System.out.println("No hay centros");

	session.close();

	}

}
