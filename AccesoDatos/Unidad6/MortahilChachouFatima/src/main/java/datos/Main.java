package datos;

import java.util.Scanner;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import org.bson.Document;

public class Main {
    public static void main(String[] args) {
        // Conectar a MongoDB (asegúrate de usar el mismo nombre de DB que en CrearBDMongo_Tarea5.java)
        MongoClient cliente = new MongoClient("localhost", 27017);
        MongoDatabase db = cliente.getDatabase("MortahilChachouFatima2324");
        
        Scanner sc = new Scanner(System.in);
        int opcion;
        do {
            mostrarMenu();
            opcion = sc.nextInt();
            sc.nextLine();  // Consumir salto de línea
            switch (opcion) {
                case 1:
                    actualizarViajes(db);
                    break;
                case 2:
                    actualizarClientes(db);
                    break;
                case 3:
                    consultaClientes(db, sc);
                    break;
                case 0:
                    System.out.println("Saliendo...");
                    break;
                default:
                    System.out.println("Opción incorrecta, intenta de nuevo.");
            }
        } while (opcion != 0);
        
        sc.close();
        cliente.close();
    }
    
    private static void mostrarMenu() {
        System.out.println("Tarea MongoDB. Realizada por: Fatima Mortahil Chachou");
        System.out.println("............................................................");
        System.out.println(" 1. Actualizar en la colección de viajes.");
        System.out.println(" 2. Actualizar en la colección de clientes.");
        System.out.println(" 3. Consulta de clientes.");
        System.out.println(" 0. SALIR.");
        System.out.println("............................................................");
        System.out.print("TECLEA OPERACIÓN: ");
    }
    
    // Método para actualizar la colección de viajes
    private static void actualizarViajes(MongoDatabase db) {
        MongoCollection<Document> viajesCol = db.getCollection("viajes");
        MongoCollection<Document> reservasCol = db.getCollection("reservas");
        
        // Recorrer cada viaje y sumar las plazas reservadas de la colección reservas
        for (Document viaje : viajesCol.find()) {
            int idViaje = viaje.getInteger("id");
            // Sumar las plazas de las reservas correspondientes
            int totalPlazas = reservasCol.find(new Document("idviaje", idViaje))
                                         .map(doc -> doc.getInteger("plazas"))
                                         .into(new java.util.ArrayList<>())
                                         .stream()
                                         .mapToInt(Integer::intValue)
                                         .sum();
            // Actualizar el campo viajeros
            viajesCol.updateOne(new Document("id", idViaje), new Document("$set", new Document("viajeros", totalPlazas)));
            
            // Mostrar la salida
            String descripcion = viaje.getString("descripcion");
            if(totalPlazas > 0){
                System.out.println("idViaje: " + idViaje + ", descripción: " + descripcion + ", actualizado con " + totalPlazas + " viajeros.");
            } else {
                System.out.println("idViaje: " + idViaje + ", descripción: " + descripcion + ", sin viajeros.");
            }
        }
        System.out.println("Fin actualización viajes.");
    }
    
    // Método para actualizar la colección de clientes
    private static void actualizarClientes(MongoDatabase db) {
        MongoCollection<Document> clientesCol = db.getCollection("clientes");
        MongoCollection<Document> reservasCol = db.getCollection("reservas");
        MongoCollection<Document> viajesCol = db.getCollection("viajes");
        
        for (Document cliente : clientesCol.find()) {
            int idCliente = cliente.getInteger("id");
            // Obtener reservas del cliente
            java.util.List<Document> reservas = reservasCol.find(new Document("idcliente", idCliente))
                                                          .into(new java.util.ArrayList<>());
            int viajesContratados = reservas.size();
            double importeTotal = 0;
            for (Document reserva : reservas) {
                int idViaje = reserva.getInteger("idviaje");
                int plazas = reserva.getInteger("plazas");
                Document viaje = viajesCol.find(new Document("id", idViaje)).first();
                if (viaje != null) {
                    double pvp = viaje.getDouble("pvp");
                    importeTotal += pvp * plazas;
                }
            }
            // Actualizar el cliente
            clientesCol.updateOne(new Document("id", idCliente), new Document("$set", 
                new Document("viajescontratados", viajesContratados).append("importetotal", importeTotal)));
            
            // Mostrar mensaje
            String nombre = cliente.getString("nombre");
            if(viajesContratados > 0) {
                System.out.println("idCliente: " + idCliente + ", nombre: " + nombre + 
                    ", actualizado con " + viajesContratados + " viajes contratados, y con total importe: " + importeTotal);
            } else {
                System.out.println("idCliente: " + idCliente + ", nombre: " + nombre + " no ha contratado viajes");
            }
        }
        System.out.println("Fin proceso de actualización de clientes.");
    }
    
    // Método para consultar los viajes reservados por un cliente
    private static void consultaClientes(MongoDatabase db, Scanner sc) {
        MongoCollection<Document> clientesCol = db.getCollection("clientes");
        MongoCollection<Document> reservasCol = db.getCollection("reservas");
        MongoCollection<Document> viajesCol = db.getCollection("viajes");
        
        while (true) {
            System.out.println("===================================================================");
            System.out.print("Introduce id del cliente: ");
            int idCliente = sc.nextInt();
            sc.nextLine(); // Consumir salto de línea
            if (idCliente == 0) {
                System.out.println("Fin proceso de consulta....");
                break;
            }
            
            Document cliente = clientesCol.find(new Document("id", idCliente)).first();
            if (cliente == null) {
                System.out.println("NO EXISTE EL ID DE CLIENTE");
                continue;
            }
            
            String nombre = cliente.getString("nombre");
            // Obtener reservas del cliente
            java.util.List<Document> reservas = reservasCol.find(new Document("idcliente", idCliente))
                                                          .into(new java.util.ArrayList<>());
            int viajesContratados = reservas.size();
            System.out.println(nombre + ", Viajes contratados: " + viajesContratados);
            System.out.println("===================================================================");
            
            if (viajesContratados == 0) {
                System.out.println(" NO HA CONTRATADO NINGÚN VIAJE");
            } else {
                // Imprimir cabeceras
                System.out.printf("%-3s %-30s %-12s %-12s %-6s\n", "ID", "DESCRIPCION", "FEC SALIDA", "PVP", "PLAZAS");
                System.out.println("=== ============================== ========== =========== ======");
                double importeTotal = 0;
                for (Document reserva : reservas) {
                    int idViaje = reserva.getInteger("idviaje");
                    int plazas = reserva.getInteger("plazas");
                    Document viaje = viajesCol.find(new Document("id", idViaje)).first();
                    if (viaje != null) {
                        String descripcion = viaje.getString("descripcion");
                        String fecSalida = viaje.getString("fechasalida");
                        double pvp = viaje.getDouble("pvp");
                        importeTotal += pvp * plazas;
                        System.out.printf("%-3d %-30s %-12s %11.2f %6d\n", idViaje, descripcion, fecSalida, pvp, plazas);
                    }
                }
                System.out.println("                                      =============================");
                System.out.printf("                                      Importe total: %12.2f\n", importeTotal);
            }
            System.out.println("===================================================================");
        }
    }
}
