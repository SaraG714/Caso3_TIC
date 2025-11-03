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
                    // Buzón vacío - verificar si podemos terminar
                    if (buzonEntrada.isCerrado()) {
                        // Buzón cerrado y vacío - verificar condiciones finales
                        verificarYEnviarFin();
                        if (sistemaTerminado) {
                            break;
                        } else {
                            // Esperar un poco antes de verificar nuevamente
                            Thread.sleep(500);
                        }
                    } else {
                        // Buzón vacío pero abierto - verificar y continuar
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
            System.out.println(getName() + " TERMINADO correctamente");
        } catch (InterruptedException e) {
            System.out.println(getName() + " ❌ INTERRUMPIDO mientras esperaba");
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
        
        // Verificar después de procesar CUALQUIER mensaje
        verificarYEnviarFin();
    }

    private void procesarCorreo(Mensaje mensaje) throws InterruptedException {
        System.out.println(getName() + ": Analizando mensaje " + mensaje.getIdMensaje() + 
                         " de Cliente" + mensaje.getIdCliente());
        
        if (mensaje.isEsSpam()) {
            // SPAM -> a cuarentena
            Random rand = new Random();
            int tiempoCuarentena = 3 + rand.nextInt(3); // 3-5 segundos
            mensaje.setTiempoCuarentena(tiempoCuarentena);
            
            buzonCuarentena.depositar(mensaje);
            System.out.println(getName() + ": SPAM a cuarentena - Mensaje " + 
                             mensaje.getIdMensaje() + " (" + tiempoCuarentena + "s)");
        } else {
            // VÁLIDO -> a entrega
            buzonEntrega.depositar(mensaje);
            System.out.println(getName() + ": VÁLIDO a entrega - Mensaje " + mensaje.getIdMensaje());
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
            
            System.out.println(getName() + " verificando terminación: " +
                             "FINs=" + clientesTerminados.size() + "/" + totalClientes +
                             ", entradaVacia=" + entradaVacia +
                             ", cuarentenaVacia=" + cuarentenaVacia);
            
            if (todosFinRecibidos && entradaVacia && cuarentenaVacia) {
                finEnviado = true;
                sistemaTerminado = true;
                                
                try {
                    Mensaje finSistema = new Mensaje(Mensaje.Tipo.FIN, "SISTEMA", -1);
                    buzonEntrega.depositar(finSistema);
                    buzonCuarentena.depositar(finSistema);
                    System.out.println("✅ FIN: " + getName() + " envió FIN a buzón de entrega y cuarentena");
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    System.out.println("❌ Error enviando FIN: " + e.getMessage());
                }
            }
        }
    }

    public static boolean isSistemaCompletamenteTerminado() {
        return sistemaTerminado && finEnviado;
    }
    
    // Método para ver el estado actual (para debugging)
    public static String getEstado() {
        return "FINs: " + clientesTerminados.size() + ", sistemaTerminado: " + sistemaTerminado + ", finEnviado: " + finEnviado;
    }
}