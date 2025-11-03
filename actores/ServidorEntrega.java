package actores;

import model.BuzonEntrega;
import model.Mensaje;
import java.util.Random;

public class ServidorEntrega extends Thread {
    private BuzonEntrega buzonEntrega;
    private volatile boolean terminado;
    private int mensajesProcesados;
    private static volatile boolean finGlobalRecibido = false;

    public ServidorEntrega(int idServidor, BuzonEntrega buzonEntrega) {
        this.buzonEntrega = buzonEntrega;
        this.terminado = false;
        this.mensajesProcesados = 0;
        this.setName("ServidorEntrega-" + idServidor);
    }

    @Override
    public void run() {
        try {
            while (!terminado && !finGlobalRecibido) {
                Mensaje mensaje = buzonEntrega.retirar();
    
                if (mensaje == null) {
                    if (buzonEntrega.isCerrado()) {
                        terminado = true;
                        break;
                    }
                    Thread.sleep(50);
                    continue;
                }
    
                if (mensaje.getTipo() == Mensaje.Tipo.FIN) {
                    finGlobalRecibido = true;
                    terminado = true;
                    break;
                } else {
                    procesarMensaje(mensaje);
                }
            }
    
            System.out.println(getName() + " terminado. Proceso " + mensajesProcesados + " mensaje(s)");
    
        } catch (InterruptedException e) {
            System.out.println(getName() + " interrumpido");
        }
    }

    private void procesarMensaje(Mensaje mensaje) throws InterruptedException {
        mensajesProcesados++;
        Random rand = new Random();
        int tiempoProcesamiento = 200 + rand.nextInt(300);
        Thread.sleep(tiempoProcesamiento);
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