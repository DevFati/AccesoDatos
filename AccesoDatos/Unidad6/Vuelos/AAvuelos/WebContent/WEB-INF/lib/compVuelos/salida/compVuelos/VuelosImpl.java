/*
 * Decompiled with CFR 0.152.
 */
package compVuelos;

import compVuelos.Pasaje;
import compVuelos.Pasajero;
import compVuelos.Vuelos;
import compVuelos.VuelosDAO;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class VuelosImpl
implements VuelosDAO {
    static Connection conexion;

    public VuelosImpl() {
        conexion = VuelosImpl.conexionOrcl("VUELOS", "vuelos");
    }

    public static Connection conexionOrcl(String usu, String clave) {
        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            Connection conexion = DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:XE", usu, clave);
            return conexion;
        }
        catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Connection conexionMysql(String basedatos, String usu, String clave) {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            Connection conexion = DriverManager.getConnection("jdbc:mysql://localhost:3307/" + basedatos, usu, clave);
            return conexion;
        }
        catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String insertarpasaje(Pasaje pa) {
        String sql = "INSERT INTO pasaje (pasajerocod,identificador,numasiento, clase, pvp ) VALUES (?, ?, ?, ?, ?)";
        try {
            String mensaje = "";
            boolean error = false;
            if (this.comprobarpasajerovuelo(pa.getPasajerocod(), pa.getIdentificador())) {
                error = true;
                mensaje = "ERROR AL INSERTAR. EL PASAJERO " + pa.getPasajerocod() + " YA EST\u00c1 EN EL VUELO " + pa.getIdentificador() + ". ";
            }
            if (this.comprobarasientovuelo(pa.getNumasiento(), pa.getIdentificador())) {
                error = true;
                mensaje = String.valueOf(mensaje) + "ERROR AL INSERTAR. EL N\u00daMERO DE ASIENTO " + pa.getNumasiento() + " YA EST\u00c1 OCUPADO EN EL VUELO " + pa.getIdentificador() + ". ";
            }
            if (!error) {
                int id = this.calculomaximo();
                PreparedStatement sentencia = conexion.prepareStatement(sql);
                sentencia.setInt(1, pa.getPasajerocod());
                sentencia.setString(2, pa.getIdentificador());
                sentencia.setInt(3, pa.getNumasiento());
                sentencia.setString(4, pa.getClase());
                sentencia.setFloat(5, pa.getPvp());
                int filas = sentencia.executeUpdate();
                mensaje = "REGISTRO INSERTADO CORRECTAMENTE";
                sentencia.close();
            }
            return mensaje;
        }
        catch (SQLException e) {
            e.printStackTrace();
            return "C\u00f3digo de error: " + e.getErrorCode() + "\nMensaje de error: " + e.getMessage();
        }
    }

    private int calculomaximo() {
        int idd = 0;
        String sql = "select coalesce(max(idpasaje),0) + 1 from pasaje ";
        try {
            PreparedStatement sentencia = conexion.prepareStatement(sql);
            ResultSet filas = sentencia.executeQuery();
            if (filas.next()) {
                idd = filas.getInt(1);
            }
            filas.close();
            sentencia.close();
        }
        catch (SQLException e) {
            System.out.println("C\u00f3digo de error: " + e.getErrorCode() + "\nMensaje de error: " + e.getMessage());
            e.printStackTrace();
        }
        return idd;
    }

    private boolean comprobarasientovuelo(int numasiento, String identificador) {
        boolean paso = false;
        String sql = "select * from pasaje where numasiento = ? and identificador = ?";
        try {
            PreparedStatement sentencia = conexion.prepareStatement(sql);
            sentencia.setInt(1, numasiento);
            sentencia.setString(2, identificador);
            ResultSet filas = sentencia.executeQuery();
            if (filas.next()) {
                paso = true;
            }
            filas.close();
            sentencia.close();
        }
        catch (SQLException e) {
            System.out.println("C\u00f3digo de error: " + e.getErrorCode() + "\nMensaje de error: " + e.getMessage());
            e.printStackTrace();
        }
        return paso;
    }

    private boolean comprobarpasajerovuelo(int pasajerocod, String identificador) {
        boolean paso = false;
        String sql = "select * from pasaje where pasajerocod = ? and identificador = ?";
        try {
            PreparedStatement sentencia = conexion.prepareStatement(sql);
            sentencia.setInt(1, pasajerocod);
            sentencia.setString(2, identificador);
            ResultSet filas = sentencia.executeQuery();
            if (filas.next()) {
                paso = true;
            }
            filas.close();
            sentencia.close();
        }
        catch (SQLException e) {
            System.out.println("C\u00f3digo de error: " + e.getErrorCode() + "\nMensaje de error: " + e.getMessage());
            e.printStackTrace();
        }
        return paso;
    }

    @Override
    public String actualizarpasaje(Pasaje pa) {
        String sql = "UPDATE pasaje set pasajerocod= ? ,identificador=?,numasiento=?, clase=?, pvp=?  where idpasaje = ?";
        try {
            String mensaje = "";
            boolean error = false;
            if (this.comprobarpasajerovuelo(pa.getPasajerocod(), pa.getIdentificador())) {
                error = true;
                mensaje = "ERROR AL ACTUALIZAR. EL PASAJERO " + pa.getPasajerocod() + " YA EST\u00c1 EN EL VUELO " + pa.getIdentificador() + ". ";
            }
            if (this.comprobarasientovuelo(pa.getNumasiento(), pa.getIdentificador())) {
                error = true;
                mensaje = String.valueOf(mensaje) + "ERROR AL ACTUALIZAR. EL N\u00daMERO DE ASIENTO " + pa.getNumasiento() + " YA EST\u00c1 OCUPADO EN EL VUELO " + pa.getIdentificador() + ". ";
            }
            if (!error) {
                PreparedStatement sentencia = conexion.prepareStatement(sql);
                sentencia.setInt(1, pa.getPasajerocod());
                sentencia.setString(2, pa.getIdentificador());
                sentencia.setInt(3, pa.getNumasiento());
                sentencia.setString(4, pa.getClase());
                sentencia.setFloat(5, pa.getPvp());
                sentencia.setInt(6, pa.getIdpasaje());
                int filas = sentencia.executeUpdate();
                mensaje = "REGISTRO ACTUALIZADO CORRECTAMENTE";
                sentencia.close();
            }
            return mensaje;
        }
        catch (SQLException e) {
            e.printStackTrace();
            return "C\u00f3digo de error: " + e.getErrorCode() + "\nMensaje de error: " + e.getMessage();
        }
    }

    @Override
    public String borrarpasaje(int id) {
        String mensaje = "";
        String sql = "delete from pasaje where idpasaje = ?";
        try {
            PreparedStatement sentencia = conexion.prepareStatement(sql);
            sentencia.setInt(1, id);
            int filas = sentencia.executeUpdate();
            mensaje = filas > 0 ? "PASAJE CON ID: " + id + " BORRADO CORRECTAMENTE." : "PASAJE CON ID " + id + " NO LOCALIZADO. NO SE HA BORRADO";
            sentencia.close();
        }
        catch (SQLException e) {
            System.out.println("C\u00f3digo de error: " + e.getErrorCode() + "\nMensaje de error: " + e.getMessage());
            mensaje = "C\u00f3digo de error: " + e.getErrorCode() + "\nMensaje de error: " + e.getMessage();
            e.printStackTrace();
        }
        return mensaje;
    }

    @Override
    public ArrayList<Vuelos> getVuelos() {
        ArrayList<Vuelos> lista = new ArrayList<Vuelos>();
        String sql = "select * from vuelo ";
        try {
            PreparedStatement sentencia = conexion.prepareStatement(sql);
            ResultSet filas = sentencia.executeQuery();
            while (filas.next()) {
                String paisorigen = this.paisaeropuerto(filas.getString(2));
                String paisdestino = this.paisaeropuerto(filas.getString(3));
                String nombreorigen = this.nombreeropuerto(filas.getString(2));
                String nombredestino = this.nombreeropuerto(filas.getString(3));
                int num = this.numviajeros(filas.getString(1));
                Vuelos dd = new Vuelos(filas.getString(1), filas.getString(2), nombreorigen, paisorigen, filas.getString(3), nombredestino, paisdestino, filas.getString(4), filas.getString(5), filas.getInt(6), num);
                lista.add(dd);
            }
            filas.close();
            sentencia.close();
        }
        catch (SQLException e) {
            System.out.println("C\u00f3digo de error: " + e.getErrorCode() + "\nMensaje de error: " + e.getMessage());
            e.printStackTrace();
        }
        return lista;
    }

    private int numviajeros(String cod) {
        int num = 0;
        String sql = "select count(*) from pasaje where identificador = ?";
        try {
            PreparedStatement sentencia = conexion.prepareStatement(sql);
            sentencia.setString(1, cod);
            ResultSet filas = sentencia.executeQuery();
            if (filas.next()) {
                num = filas.getInt(1);
            }
            filas.close();
            sentencia.close();
        }
        catch (SQLException e) {
            System.out.println("C\u00f3digo de error: " + e.getErrorCode() + "\nMensaje de error: " + e.getMessage());
            e.printStackTrace();
        }
        return num;
    }

    private String nombreeropuerto(String cod) {
        String nom = "";
        String sql = "select nombre from aeropuerto where codaeropuerto = ?";
        try {
            PreparedStatement sentencia = conexion.prepareStatement(sql);
            sentencia.setString(1, cod);
            ResultSet filas = sentencia.executeQuery();
            if (filas.next()) {
                nom = filas.getString(1);
            }
            filas.close();
            sentencia.close();
        }
        catch (SQLException e) {
            System.out.println("C\u00f3digo de error: " + e.getErrorCode() + "\nMensaje de error: " + e.getMessage());
            e.printStackTrace();
        }
        return nom;
    }

    private String paisaeropuerto(String cod) {
        String pais = "";
        String sql = "select pais from aeropuerto where codaeropuerto = ?";
        try {
            PreparedStatement sentencia = conexion.prepareStatement(sql);
            sentencia.setString(1, cod);
            ResultSet filas = sentencia.executeQuery();
            if (filas.next()) {
                pais = filas.getString(1);
            }
            filas.close();
            sentencia.close();
        }
        catch (SQLException e) {
            System.out.println("C\u00f3digo de error: " + e.getErrorCode() + "\nMensaje de error: " + e.getMessage());
            e.printStackTrace();
        }
        return pais;
    }

    @Override
    public Vuelos getVuelo(String cod) {
        Vuelos vue = null;
        String sql = "select * from vuelo where identificador= ?";
        try {
            PreparedStatement sentencia = conexion.prepareStatement(sql);
            sentencia.setString(1, cod);
            ResultSet filas = sentencia.executeQuery();
            if (filas.next()) {
                String paisorigen = this.paisaeropuerto(filas.getString(2));
                String paisdestino = this.paisaeropuerto(filas.getString(3));
                String nombreorigen = this.nombreeropuerto(filas.getString(2));
                String nombredestino = this.nombreeropuerto(filas.getString(3));
                int num = this.numviajeros(filas.getString(1));
                vue = new Vuelos(filas.getString(1), filas.getString(2), nombreorigen, paisorigen, filas.getString(3), nombredestino, paisdestino, filas.getString(4), filas.getString(5), filas.getInt(6), num);
            }
            filas.close();
            sentencia.close();
        }
        catch (SQLException e) {
            System.out.println("C\u00f3digo de error: " + e.getErrorCode() + "\nMensaje de error: " + e.getMessage());
            e.printStackTrace();
        }
        return vue;
    }

    @Override
    public ArrayList<Pasaje> getPasajeVuelo(String id) {
        ArrayList<Pasaje> lista = new ArrayList<Pasaje>();
        String sql = "select * from pasaje where identificador = ?";
        try {
            PreparedStatement sentencia = conexion.prepareStatement(sql);
            sentencia.setString(1, id);
            ResultSet filas = sentencia.executeQuery();
            while (filas.next()) {
                String nombrepasajero = "";
                String paispasajero = "";
                String sql2 = "select nombre, pais from pasajero where pasajerocod = ?";
                PreparedStatement sentencia2 = conexion.prepareStatement(sql2);
                sentencia2.setInt(1, filas.getInt(2));
                ResultSet pasajero = sentencia2.executeQuery();
                if (pasajero.next()) {
                    nombrepasajero = pasajero.getString(1);
                    paispasajero = pasajero.getString(2);
                }
                pasajero.close();
                sentencia2.close();
                Pasaje pas = new Pasaje(filas.getInt(1), filas.getInt(2), filas.getString(3), filas.getInt(4), filas.getString(5), filas.getFloat(6), nombrepasajero, paispasajero);
                lista.add(pas);
            }
            filas.close();
            sentencia.close();
        }
        catch (SQLException e) {
            System.out.println("C\u00f3digo de error: " + e.getErrorCode() + "\nMensaje de error: " + e.getMessage());
            e.printStackTrace();
        }
        return lista;
    }

    @Override
    public ArrayList<Pasajero> getPasajeros() {
        ArrayList<Pasajero> lista = new ArrayList<Pasajero>();
        String sql = "select * from pasajero";
        try {
            PreparedStatement sentencia = conexion.prepareStatement(sql);
            ResultSet filas = sentencia.executeQuery();
            while (filas.next()) {
                Pasajero dd = new Pasajero(filas.getInt(1), filas.getString(2), filas.getString(3), filas.getString(4), filas.getString(5));
                lista.add(dd);
            }
            filas.close();
            sentencia.close();
        }
        catch (SQLException e) {
            System.out.println("C\u00f3digo de error: " + e.getErrorCode() + "\nMensaje de error: " + e.getMessage());
            e.printStackTrace();
        }
        return lista;
    }

    @Override
    public ArrayList<Pasaje> getPasajes() {
        ArrayList<Pasaje> lista = new ArrayList<Pasaje>();
        String sql = "select * from pasaje ";
        try {
            PreparedStatement sentencia = conexion.prepareStatement(sql);
            ResultSet filas = sentencia.executeQuery();
            while (filas.next()) {
                String nombrepasajero = "";
                String paispasajero = "";
                String sql2 = "select nombre, pais from pasajero where pasajerocod = ?";
                PreparedStatement sentencia2 = conexion.prepareStatement(sql2);
                sentencia2.setInt(1, filas.getInt(2));
                ResultSet pasajero = sentencia2.executeQuery();
                if (pasajero.next()) {
                    nombrepasajero = pasajero.getString(1);
                    paispasajero = pasajero.getString(2);
                }
                pasajero.close();
                sentencia2.close();
                Pasaje pas = new Pasaje(filas.getInt(1), filas.getInt(2), filas.getString(3), filas.getInt(4), filas.getString(5), filas.getFloat(6), nombrepasajero, paispasajero);
                lista.add(pas);
            }
            filas.close();
            sentencia.close();
        }
        catch (SQLException e) {
            System.out.println("C\u00f3digo de error: " + e.getErrorCode() + "\nMensaje de error: " + e.getMessage());
            e.printStackTrace();
        }
        return lista;
    }

    @Override
    public Pasaje getPasaje(int id) {
        Pasaje pas = null;
        String sql = "select * from pasaje where idpasaje = ?";
        try {
            PreparedStatement sentencia = conexion.prepareStatement(sql);
            sentencia.setInt(1, id);
            ResultSet filas = sentencia.executeQuery();
            if (filas.next()) {
                String nombrepasajero = "";
                String paispasajero = "";
                String sql2 = "select nombre, pais from pasajero where pasajerocod = ?";
                PreparedStatement sentencia2 = conexion.prepareStatement(sql2);
                sentencia2.setInt(1, filas.getInt(2));
                ResultSet pasajero = sentencia2.executeQuery();
                if (pasajero.next()) {
                    nombrepasajero = pasajero.getString(1);
                    paispasajero = pasajero.getString(2);
                }
                pasajero.close();
                sentencia2.close();
                pas = new Pasaje(filas.getInt(1), filas.getInt(2), filas.getString(3), filas.getInt(4), filas.getString(5), filas.getFloat(6), nombrepasajero, paispasajero);
            }
            filas.close();
            sentencia.close();
        }
        catch (SQLException e) {
            e.printStackTrace();
            System.out.println("C\u00f3digo de error: " + e.getErrorCode() + "\nMensaje de error: " + e.getMessage());
        }
        return pas;
    }

    @Override
    public String borrarvuelo(String id) {
        String mensaje = "";
        String sql = "delete from vuelo where identificador = ?";
        try {
            PreparedStatement sentencia = conexion.prepareStatement(sql);
            sentencia.setString(1, id);
            int filas = sentencia.executeUpdate();
            mensaje = filas > 0 ? "VUELO CON ID: " + id + ", BORRADO CORRECTAMENTE." : "VUELO CON ID NO LOCALIZADO: " + id + ". NO SE HA BORRADO";
            sentencia.close();
        }
        catch (SQLException e) {
            System.out.println("C\u00f3digo de error: " + e.getErrorCode() + "\nMensaje de error: " + e.getMessage());
            mensaje = "C\u00f3digo de error: " + e.getErrorCode() + "\nMensaje de error: " + e.getMessage();
            e.printStackTrace();
        }
        return mensaje;
    }
}
