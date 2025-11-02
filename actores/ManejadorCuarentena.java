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
            System.out.println("🟣 " + getName() + " INICIADO");
            
            while (!terminado) {
                // Intenta retirar un mensaje (espera semiactiva en el buzón)
                Mensaje mensaje = buzonCuarentena.retirar();
                
                if (mensaje == null) {
                    // Si el buzón está cerrado y vacío, terminar
                    if (buzonCuarentena.isCerrado()) {
                        System.out.println("🟣 " + getName() + ": Buzón cerrado y vacío - Terminando");
                        terminado = true;
                        break;
                    }
                    // Espera semiactiva: pausa de 1 segundo como especifica el requerimiento
                    Thread.sleep(1000);
                    continue;
                }
                
                // Verificar si es mensaje FIN
                if (mensaje.getTipo() == Mensaje.Tipo.FIN) {
                    System.out.println("🎯 " + getName() + ": Recibió FIN - Terminando");
                    terminado = true;
                    
                    break;
                }
                
                // Procesar mensaje normal de cuarentena
                procesarMensaje(mensaje);
                
                // Pequeña pausa entre mensajes para no saturar
                Thread.sleep(100);
            }
            
            System.out.println("🟣 " + getName() + " TERMINADO");
            
        } catch (InterruptedException e) {
            System.out.println("🟣 " + getName() + " interrumpido");
            Thread.currentThread().interrupt();
        }
    }

    private void procesarMensaje(Mensaje mensaje) throws InterruptedException {
        // Decrementar tiempo de cuarentena
        int tiempoRestante = mensaje.getTiempoCuarentena() - 1;
        mensaje.setTiempoCuarentena(tiempoRestante);
        
        System.out.println("🟣 " + getName() + ": Mensaje " + mensaje.getIdMensaje() + 
                         " - tiempo restante: " + tiempoRestante + "s");
        
        // Verificar si es malicioso (múltiplo de 7)
        Random rand = new Random();
        int numeroAleatorio = 1 + rand.nextInt(21); // 1-21
        boolean esMalicioso = (numeroAleatorio % 7 == 0);
        
        if (esMalicioso) {
            System.out.println("🟣 " + getName() + ": Mensaje " + mensaje.getIdMensaje() + 
                             " DESCARTADO (malicioso)");
            // No se re-deposita - se descarta permanentemente
        } 
        else if (tiempoRestante <= 0) {
            // Tiempo cumplido - mover a entrega
            buzonEntrega.depositar(mensaje);
            System.out.println("🟣 " + getName() + ": Mensaje " + mensaje.getIdMensaje() + 
                             " movido a entrega");
        } 
        else {
            // Todavía en cuarentena - volver a depositar
            buzonCuarentena.depositar(mensaje);
        }
    }

    public void solicitarTerminacion() {
        this.terminado = true;
        this.interrupt();
    }
}