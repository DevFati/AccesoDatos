package Ejercicio;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
	@XmlRootElement(name="productos")
	public class Productos2 {
		private ArrayList<Producto2> lista;

		public Productos2(ArrayList<Producto2> lista) {
			super();
			this.lista = lista;
		}

		public Productos2() {
			super();
		}
		
		@XmlElement(name = "producto")
		public ArrayList<Producto2> getLista() {
			return lista;
		}

		public void setLista(ArrayList<Producto2> lista) {
			this.lista = lista;
		}
}
