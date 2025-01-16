package ejercicios;

import java.math.BigInteger;

import org.neodatis.odb.ODB;
import org.neodatis.odb.ODBFactory;
import org.neodatis.odb.ObjectValues;
import org.neodatis.odb.Values;
import org.neodatis.odb.core.query.criteria.Where;
import org.neodatis.odb.impl.core.query.values.ValuesCriteriaQuery;

import datos.Ventas;

public class CosultaVentas {
private static ODB odb;
public static void main(String[] args) {
	odb=ODBFactory.open("ARTICULOS.DAT");
	numVentas(odb);
	totalArti(odb);
	odb.close();
}
private static void numVentas(ODB odb2) {
	Values val=odb.getValues(new ValuesCriteriaQuery(Ventas.class).count("*"));
	
	ObjectValues ov2=val.nextValues();
	BigInteger value2=(BigInteger)ov2.getByAlias("*");
	System.out.println("Numero de ventas: "+value2.intValue());
}
private static void totalArti(ODB odb2) {
	Values groupby = odb.getValues(new ValuesCriteriaQuery(Ventas.class, Where.equal("codarti.codarti",1)).field("codarti.codarti").count("*").groupBy("codarti.codarti"));
	
	while(groupby.hasNext()) {
		ObjectValues objetos=(ObjectValues) groupby.next();
		System.out.println("Coad arti: "+objetos.getByAlias("codarti.codarti")+" Num. ventas: "+objetos.getByIndex(1));
	}
	
}

}
