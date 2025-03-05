/*
 * Decompiled with CFR 0.152.
 */
package compVuelos;

import compVuelos.Pasaje;
import compVuelos.Pasajero;
import compVuelos.Vuelos;
import java.util.ArrayList;

public interface VuelosDAO {
    public String insertarpasaje(Pasaje var1);

    public String actualizarpasaje(Pasaje var1);

    public String borrarpasaje(int var1);

    public String borrarvuelo(String var1);

    public ArrayList<Vuelos> getVuelos();

    public Vuelos getVuelo(String var1);

    public ArrayList<Pasaje> getPasajeVuelo(String var1);

    public ArrayList<Pasajero> getPasajeros();

    public ArrayList<Pasaje> getPasajes();

    public Pasaje getPasaje(int var1);
}
