package actores;
import model.BuzonCuarentena;
import model.BuzonEntrega;
import model.Mensaje;
import java.util.Random;

public class ManejadorCuarentena extends Thread {
    private BuzonCuarentena buzonCuarentena;
    private BuzonEntrega buzonEntrega;
    private volatile boolean terminado;

    public ManejadorCuarentena(BuzonCuarentena buzonCuarentena, BuzonEntrega buzonEntrega) {
        this.buzonCuarentena = buzonCuarentena;
        this.buzonEntrega = buzonEntrega;
        this.terminado = false;
        this.setName("ManejadorCuarentena");
    }

    @Override
    public void run() {
        try {
            while (!terminado) {
                // El buzón ya maneja espera semiactiva, solo procesamos si hay mensajes
                if (!buzonCuarentena.estaVacio()) {
                    procesarCuarentena();
                } else {
                    // Pequeña pausa cuando no hay trabajo (no es espera, solo evitar CPU alto)
                    Thread.sleep(100);
                }
            }
            System.out.println("ManejadorCuarentena ha terminado");
        } catch (InterruptedException e) {
            System.out.println("ManejadorCuarentena interrumpido");
        }
    }

    private void procesarCuarentena() throws InterruptedException {
        System.out.println("ManejadorCuarentena revisando mensajes...");
        
        // Procesar todos los mensajes disponibles
        while (!buzonCuarentena.estaVacio()) {
            Mensaje mensaje = buzonCuarentena.retirar(); // Espera semiactiva en el buzón
            
            if (mensaje == null) break;
            
            if (mensaje.getTipo() == Mensaje.Tipo.FIN) {
                System.out.println("ManejadorCuarentena recibió FIN, terminando...");
                terminado = true;
                return;
            }
            
            // Decrementar tiempo de cuarentena
            int tiempoRestante = mensaje.getTiempoCuarentena() - 1;
            mensaje.setTiempoCuarentena(tiempoRestante);
            
            System.out.println("ManejadorCuarentena: Mensaje " + mensaje.getIdMensaje() + 
                             " - tiempo restante: " + tiempoRestante + "s");
            
            // Verificar si es malicioso (múltiplo de 7)
            Random rand = new Random();
            int numeroAleatorio = 1 + rand.nextInt(21); // 1-21
            boolean esMalicioso = (numeroAleatorio % 7 == 0);
            
            if (esMalicioso) {
                System.out.println("ManejadorCuarentena:  Mensaje " + mensaje.getIdMensaje() + 
                                 " DESCARTADO (malicioso)");
                // No se re-deposita - se descarta permanentemente
            } 
            else if (tiempoRestante <= 0) {
                // Tiempo cumplido - mover a entrega (espera semiactiva en el buzón)
                buzonEntrega.depositar(mensaje);
                System.out.println("ManejadorCuarentena: Mensaje " + mensaje.getIdMensaje() + 
                                 " movido a entrega");
            } 
            else {
                // Todavía en cuarentena - volver a depositar (espera semiactiva en el buzón)
                buzonCuarentena.depositar(mensaje);
            }
        }
    }

    public void solicitarTerminacion() {
        this.terminado = true;
        this.interrupt();
    }
}