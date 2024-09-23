
package pruebalibros;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;



@XmlRootElement(name="milibrito")
@XmlType(propOrder = {"isbn", "autor","nombre", "editorial" } )

public class Libro {
	
private String nombre;
private String autor;
private String editorial;
private String isbn;
public Libro(String nombre, String autor, String editorial,
         String isbn) {
	super();
	this.nombre = nombre;
	this.autor = autor;
	this.editorial = editorial;
	this.isbn = isbn;
}   
public Libro() {}
@XmlElement(name="nombrecito")
public String getNombre() { return nombre; }
@XmlElement(name="autorcito")
public String getAutor() { return autor;  }
@XmlElement(name="mieditorial")
public String getEditorial() {return editorial; }
@XmlElement(name="miisbn")
public String getIsbn() { return isbn;}
public void setNombre(String nombre) 
    { this.nombre = nombre; }
public void setAutor(String autor) 
    { this.autor = autor; }
public void setEditorial(String editorial) 
    { this.editorial = editorial;  }
public void setIsbn(String isbn) 
    {  this.isbn = isbn;  }
}
