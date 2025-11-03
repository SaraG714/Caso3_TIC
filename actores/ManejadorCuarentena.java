package actores;
import model.BuzonCuarentena;
import model.BuzonEntrega;
import model.Mensaje;
import java.util.Random;

public class ManejadorCuarentena extends Thread {
    private BuzonCuarentena buzonCuarentena;
    private BuzonEntrega buzonEntrega;
    private volatile boolean terminado;
    private static int mensajesDescartados = 0;

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
                Mensaje mensaje = buzonCuarentena.retirar();
                
                if (mensaje == null) {
                    if (buzonCuarentena.isCerrado()) {
                        terminado = true;
                        break;
                    }
                    Thread.sleep(1000);
                    continue;
                }
                
                if (mensaje.getTipo() == Mensaje.Tipo.FIN) {
                    terminado = true;
                    break;
                }
                
                procesarMensaje(mensaje);
                Thread.sleep(100);
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void procesarMensaje(Mensaje mensaje) throws InterruptedException {
        int tiempoRestante = mensaje.getTiempoCuarentena() - 1;
        mensaje.setTiempoCuarentena(tiempoRestante);
        
        Random rand = new Random();
        int numeroAleatorio = 1 + rand.nextInt(21);
        boolean esMalicioso = (numeroAleatorio % 7 == 0);
        
        if (esMalicioso) {
            synchronized (ManejadorCuarentena.class) {
                mensajesDescartados++;
            }
        } 
        else if (tiempoRestante <= 0) {
            buzonEntrega.depositar(mensaje);
        } 
        else {
            buzonCuarentena.depositar(mensaje);
        }
    }

    public void solicitarTerminacion() {
        this.terminado = true;
        this.interrupt();
    }
    
    public static int getMensajesDescartados() {
        return mensajesDescartados;
    }
}