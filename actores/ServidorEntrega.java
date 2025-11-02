package actores;

import model.BuzonEntrega;
import model.Mensaje;
import java.util.Random;

public class ServidorEntrega extends Thread {
    private BuzonEntrega buzonEntrega;
    private int idServidor;
    private volatile boolean terminado;
    private int mensajesProcesados;

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
            while (!terminado) {
                // ESPERA ACTIVA: el buzón retorna null si vacío
                Mensaje mensaje = buzonEntrega.retirar();
                
                if (mensaje == null) {
                    // Buzón vacío - espera activa (consultar de nuevo)
                    Thread.sleep(50); // Pequeña pausa para no saturar CPU
                    continue;
                }
                
                if (mensaje.getTipo() == Mensaje.Tipo.FIN) {
                    System.out.println(getName() + ": Recibió FIN - Terminando");
                    terminado = true;
                    buzonEntrega.depositar(mensaje); // Re-depositar para otros servidores
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
                         " from " + mensaje.getIdCliente());
        
        // Simular tiempo de procesamiento aleatorio
        Random rand = new Random();
        int tiempoProcesamiento = 500 + rand.nextInt(1000); // 0.5 - 1.5 segundos
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
}