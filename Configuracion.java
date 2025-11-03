
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Configuracion {
    private Map<String, Integer> parametros;
    
    public Configuracion(String archivoConfig) throws IOException {
        this.parametros = new HashMap<>();
        cargarConfiguracion(archivoConfig);
    }
    
    private void cargarConfiguracion(String archivoConfig) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(archivoConfig));
        String linea;
        
        while ((linea = reader.readLine()) != null) {
            linea = linea.trim();
            if (linea.isEmpty() || linea.startsWith("#")) {
                continue; // Saltar líneas vacías o comentarios
            }
            
            String[] partes = linea.split("=");
            if (partes.length == 2) {
                String clave = partes[0].trim();
                int valor = Integer.parseInt(partes[1].trim());
                parametros.put(clave, valor);
            }
        }
        reader.close();
    }
    
    public int getNumClientes() {
        return parametros.getOrDefault("numClientes", 5);
    }
    
    public int getMensajesPorCliente() {
        return parametros.getOrDefault("mensajesPorCliente", 10);
    }
    
    public int getNumFiltros() {
        return parametros.getOrDefault("numFiltros", 2);
    }
    
    public int getNumServidores() {
        return parametros.getOrDefault("numServidores", 2);
    }
    
    public int getCapacidadEntrada() {
        return parametros.getOrDefault("capacidadEntrada", 10);
    }
    
    public int getCapacidadEntrega() {
        return parametros.getOrDefault("capacidadEntrega", 8);
    }
    
    public void mostrarConfiguracion() {
        System.out.println("=== CONFIGURACIÓN CARGADA ===");
        System.out.println("Clientes: " + getNumClientes());
        System.out.println("Mensajes por cliente: " + getMensajesPorCliente());
        System.out.println("Filtros: " + getNumFiltros());
        System.out.println("Servidores: " + getNumServidores());
        System.out.println("Capacidad entrada: " + getCapacidadEntrada());
        System.out.println("Capacidad entrega: " + getCapacidadEntrega());
        System.out.println("=============================");
    }
}