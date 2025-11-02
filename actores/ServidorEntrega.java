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
            System.out.println(getName() + " INICIADO - esperando mensajes...");
            
            while (!terminado) {
                Mensaje mensaje = buzonEntrega.retirar();
    
                if (mensaje == null) {
                    // Si el buzón está cerrado y vacío, terminar
                    if (buzonEntrega.isCerrado() && buzonEntrega.estaVacio()) {
                        System.out.println(getName() + ": Buzón cerrado y vacío - Terminando");
                        terminado = true;
                        break;
                    }
                    // Espera activa corta
                    Thread.sleep(50);
                    continue;
                }
    
                if (mensaje.getTipo() == Mensaje.Tipo.FIN) {
                    System.out.println("🎯 " + getName() + ": Recibió FIN - Terminando");
                    terminado = true;
                    
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