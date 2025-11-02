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
    private static volatile boolean finEnviado = false; // volatile para double-checked locking
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
                    // Si retirar() devuelve null, puede ser porque:
                    // 1. El buzón está cerrado y vacío -> verificar y terminar
                    // 2. El buzón está vacío pero NO cerrado -> verificar si todos terminaron
                    if (buzonEntrada.isCerrado()) {
                        // Buzón cerrado: verificar condiciones finales
                        verificarYEnviarFin();
                        break;
                    } else {
                        // Buzón vacío pero abierto: verificar si todos los clientes terminaron
                        // (puede que todos los mensajes FIN ya se hayan procesado)
                        verificarYEnviarFin();
                        // Si no se cumplieron las condiciones, continuar esperando
                        // (el retirar() con timeout permitirá reintentar)
                        if (sistemaTerminado) {
                            break; // Las condiciones se cumplieron, terminar
                        }
                        // Continuar el loop para reintentar retirar()
                    }
                } else {
                    // Hay un mensaje, procesarlo
                    procesarMensaje(mensaje);
                }
            }
            System.out.println(getName() + " TERMINADO correctamente");
        } catch (InterruptedException e) {
            System.out.println(getName() + " ❌ INTERRUMPIDO mientras esperaba");
        }
    }

    private void procesarMensaje(Mensaje mensaje) throws InterruptedException {
        switch (mensaje.getTipo()) {
            case INICIO:
                System.out.println(getName() + ": Cliente" + mensaje.getIdCliente() + " inició");
                // Verificar después de procesar INICIO también (por si todos los clientes ya terminaron)
                verificarYEnviarFin();
                break;
                
            case FIN:
                String clienteId = mensaje.getIdCliente();

                synchronized (FiltroSpam.class) {
                    clientesTerminados.add(clienteId);
                    int sizeActual = clientesTerminados.size();
                
                    System.out.println(getName() + ": Cliente" + mensaje.getIdCliente() + 
                                 " terminó (" + sizeActual + "/" + totalClientes + ")");
                }
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
                         " de Cliente" + mensaje.getIdCliente());
        
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
        if (finEnviado || sistemaTerminado) {
            return;
        }
        
        synchronized (FiltroSpam.class) {
            if (finEnviado || sistemaTerminado) {
                return;
            }
            
            boolean todosFinRecibidos = clientesTerminados.size() >= totalClientes;
            boolean entradaVacia = buzonEntrada.estaVacio();
            boolean cuarentenaVacia = buzonCuarentena.isCerrado();
            
            if (todosFinRecibidos && entradaVacia && cuarentenaVacia) {
                finEnviado = true;
                sistemaTerminado = true;
                                
                try {
                    Mensaje finSistema = new Mensaje(Mensaje.Tipo.FIN, "SISTEMA", -1);
                    buzonEntrega.depositar(finSistema);
                    buzonCuarentena.depositar(finSistema);
                    System.out.println("✅ FIN: " + getName() + " envió FIN a buzón de entrega y cuarentena");
                } catch (InterruptedException e) {
                    System.out.println("❌ Error enviando FIN: " + e.getMessage());
                }
            }
        }
    }

    public static boolean isSistemaCompletamenteTerminado() {
        return sistemaTerminado && finEnviado;
    }
}