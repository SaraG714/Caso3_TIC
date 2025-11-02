package actores;
import model.BuzonEntrada;
import model.BuzonEntrega;
import model.BuzonCuarentena;
import model.Mensaje;
import java.util.Random;
import java.util.HashSet;
import java.util.Set;

public class FiltroSpam extends Thread {
    private BuzonEntrada buzonEntrada;
    private BuzonEntrega buzonEntrega;
    private BuzonCuarentena buzonCuarentena;
    private int totalClientes;
    
    // Variables COMPARTIDAS entre todos los filtros
    private static volatile boolean sistemaTerminado = false; // volatile para visibilidad entre threads
    private static boolean finEnviado = false;
    private static Set<String> clientesTerminados = new HashSet<>(); // Set compartido para evitar duplicados

    public FiltroSpam(int idFiltro, BuzonEntrada buzonEntrada, BuzonEntrega buzonEntrega, 
                     BuzonCuarentena buzonCuarentena, int totalClientes) {
        this.buzonEntrada = buzonEntrada;
        this.buzonEntrega = buzonEntrega;
        this.buzonCuarentena = buzonCuarentena;
        this.totalClientes = totalClientes;
        this.setName("FiltroSpam-" + idFiltro);
    }

    @Override
    public void run() {
        try {
            while (!sistemaTerminado) {
                Mensaje mensaje = buzonEntrada.retirar();

                if (mensaje == null) {
                    System.out.println(getName() + ": Buzón cerrado");
                    break;
                }

                procesarMensaje(mensaje);
            }
            
            System.out.println(getName() + " ha terminado");
        } catch (InterruptedException e) {
            System.out.println(getName() + " interrumpido");
        }
    }

    private void procesarMensaje(Mensaje mensaje) throws InterruptedException {
        switch (mensaje.getTipo()) {
            case INICIO:
                System.out.println(getName() + ": " + mensaje.getIdCliente() + " inició");
                break;
                
            case FIN:
                int sizeActual;
                synchronized (FiltroSpam.class) {
                    clientesTerminados.add(mensaje.getIdCliente());
                    sizeActual = clientesTerminados.size();
                }
                System.out.println(getName() + ": " + mensaje.getIdCliente() + 
                                 " terminó (" + sizeActual + "/" + totalClientes + ")");
                
                // Verificar SIEMPRE después de procesar un FIN
                verificarYEnviarFin();
                break;
                
            case CORREO:
                procesarCorreo(mensaje);
                break;
        }
    }

    private void procesarCorreo(Mensaje mensaje) throws InterruptedException {
        System.out.println(getName() + ": Analizando mensaje " + mensaje.getIdMensaje() + 
                         " from " + mensaje.getIdCliente());
        
        if (mensaje.isEsSpam()) {
            // SPAM -> a cuarentena
            Random rand = new Random();
            int tiempoCuarentena = 3 + rand.nextInt(3);
            mensaje.setTiempoCuarentena(tiempoCuarentena);
            
            buzonCuarentena.depositar(mensaje);
            System.out.println(getName() + ": SPAM a cuarentena - Mensaje " + 
                             mensaje.getIdMensaje() + " (" + tiempoCuarentena + "s)");
        } else {
            // VÁLIDO -> a entrega
            buzonEntrega.depositar(mensaje);
            System.out.println(getName() + ": VÁLIDO a entrega - Mensaje " + mensaje.getIdMensaje());
        }
        
        // Verificar también después de procesar correos (por si fue el último mensaje)
        verificarYEnviarFin();
    }

    private void verificarYEnviarFin() {
        synchronized (FiltroSpam.class) {
            // Verificar condiciones: todos los clientes terminaron Y buzón de entrada vacío
            // No requerimos que cuarentena esté vacía porque el ManejadorCuarentena 
            // seguirá procesando mensajes y eventualmente los moverá a entrega o los descartará
            if (clientesTerminados.size() >= totalClientes && 
                buzonEntrada.estaVacio() && 
                !finEnviado) {
                
                // CERRAR el buzón de entrada PRIMERO para liberar a los filtros
                buzonEntrada.cerrar();
                
                // Luego enviar los FIN
                try {
                    Mensaje finSistema = new Mensaje(Mensaje.Tipo.FIN, "SISTEMA", -1);
                    buzonEntrega.depositar(finSistema);
                    buzonCuarentena.depositar(finSistema);
                    finEnviado = true;
                    sistemaTerminado = true;
                    System.out.println("FIN: " + Thread.currentThread().getName() + " envió FIN a buzón de entrega y cuarentena");
                } catch (InterruptedException e) {
                    System.out.println("Error enviando FIN: " + e.getMessage());
                }
            }
        }
    }

    public static boolean isSistemaCompletamenteTerminado() {
        return sistemaTerminado && finEnviado;
    }
}