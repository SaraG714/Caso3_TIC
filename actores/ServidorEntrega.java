package actores;

import model.BuzonEntrega;
import model.Mensaje;
import java.util.Random;

public class ServidorEntrega extends Thread {
    private BuzonEntrega buzonEntrega;
    private int idServidor;
    private volatile boolean terminado;
    private int mensajesProcesados;
    private static volatile boolean finGlobalRecibido = false;

    public ServidorEntrega(int idServidor, BuzonEntrega buzonEntrega) {
        this.idServidor = idServidor;
        this.buzonEntrega = buzonEntrega;
        this.terminado = false;
        this.mensajesProcesados = 0;
        this.setName("ServidorEntrega-" + idServidor);
    }

    @Override
    public void run() {
        try {
            System.out.println(getName() + " INICIADO - esperando mensajes...");
            
            while (!terminado && !finGlobalRecibido) {
                Mensaje mensaje = buzonEntrega.retirar();
    
                if (mensaje == null) {
                    if (buzonEntrega.isCerrado()) {
                        System.out.println(getName() + ": Buzón cerrado y vacío - Terminando");
                        terminado = true;
                        break;
                    }
                    Thread.sleep(50);
                    continue;
                }
    
                if (mensaje.getTipo() == Mensaje.Tipo.FIN) {
                    System.out.println("🎯 " + getName() + ": Recibió FIN - Terminando INMEDIATAMENTE");
                    finGlobalRecibido = true;
                    terminado = true;
                    // NO re-depositar el FIN
                    break;
                } else {
                    procesarMensaje(mensaje);
                }
            }
    
            System.out.println(getName() + " ha terminado. Procesó " + mensajesProcesados + " mensajes");
    
        } catch (InterruptedException e) {
            System.out.println(getName() + " interrumpido");
        }
    }

    private void procesarMensaje(Mensaje mensaje) throws InterruptedException {
        mensajesProcesados++;
        System.out.println(getName() + ": Procesando mensaje " + mensaje.getIdMensaje() + 
                         " de Cliente " + mensaje.getIdCliente());
        
        // Simular tiempo de procesamiento aleatorio (más corto para pruebas)
        Random rand = new Random();
        int tiempoProcesamiento = 200 + rand.nextInt(300); // 0.2 - 0.5 segundos para pruebas
        Thread.sleep(tiempoProcesamiento);
        
        System.out.println(getName() + ": Mensaje " + mensaje.getIdMensaje() + 
                         " procesado (" + tiempoProcesamiento + "ms)");
    }

    public void solicitarTerminacion() {
        this.terminado = true;
        this.interrupt();
    }

    public int getMensajesProcesados() {
        return mensajesProcesados;
    }
    
    public static boolean isFinGlobalRecibido() {
        return finGlobalRecibido;
    }
}