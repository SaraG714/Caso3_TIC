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
    
    private static volatile boolean sistemaTerminado = false;
    private static volatile boolean finEnviado = false;
    private static Set<String> clientesTerminados = new HashSet<>();

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
                    if (buzonEntrada.isCerrado()) {
                        verificarYEnviarFin();
                        if (sistemaTerminado) {
                            break;
                        } else {
                            Thread.sleep(500);
                        }
                    } else {
                        verificarYEnviarFin();
                        if (sistemaTerminado) {
                            break;
                        }
                        Thread.sleep(100);
                    }
                } else {
                    procesarMensaje(mensaje);
                }
            }
            System.out.println(getName() + " terminado");
        } catch (InterruptedException e) {
            System.out.println(getName() + " interrumpido");
        }
    }

    private void procesarMensaje(Mensaje mensaje) throws InterruptedException {
        switch (mensaje.getTipo()) {
            case INICIO:
                System.out.println(getName() + ": Cliente" + mensaje.getIdCliente() + " inició");
                break;
                
            case FIN:
                String clienteId = mensaje.getIdCliente();
                synchronized (FiltroSpam.class) {
                    clientesTerminados.add(clienteId);
                    int sizeActual = clientesTerminados.size();
                    System.out.println(getName() + ": Cliente" + mensaje.getIdCliente() + 
                                 " terminó (" + sizeActual + "/" + totalClientes + ")");
                }
                break;
                
            case CORREO:
                procesarCorreo(mensaje);
                break;
        }
        verificarYEnviarFin();
    }

    private void procesarCorreo(Mensaje mensaje) throws InterruptedException {
        if (mensaje.isEsSpam()) {
            Random rand = new Random();
            int tiempoCuarentena = 3 + rand.nextInt(3);
            mensaje.setTiempoCuarentena(tiempoCuarentena);
            buzonCuarentena.depositar(mensaje);
            System.out.println(getName() + ": SPAM a cuarentena - Mensaje " + 
                             mensaje.getIdMensaje() + " (" + tiempoCuarentena + "s)");
        } else {
            buzonEntrega.depositar(mensaje);
            System.out.println(getName() + ": VALIDO a entrega - Mensaje " + mensaje.getIdMensaje());
        }
    }

    public void verificarYEnviarFin() {
        if (finEnviado || sistemaTerminado) {
            return;
        }
        
        synchronized (FiltroSpam.class) {
            if (finEnviado || sistemaTerminado) {
                return;
            }
            
            boolean todosFinRecibidos = (clientesTerminados.size() >= totalClientes);
            boolean entradaVacia = buzonEntrada.estaVacio();
            boolean cuarentenaVacia = buzonCuarentena.estaVacio();
            
            if (todosFinRecibidos && entradaVacia && cuarentenaVacia) {
                finEnviado = true;
                sistemaTerminado = true;
                
                try {
                    Mensaje finSistema = new Mensaje(Mensaje.Tipo.FIN, "SISTEMA", -1);
                    buzonEntrega.depositar(finSistema);
                    buzonCuarentena.depositar(finSistema);
                    System.out.println("• " + getName() + " envio FIN a buzones");
                    Thread.sleep(100);
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