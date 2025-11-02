import actores.ClienteEmisor;
import actores.FiltroSpam;
import actores.ManejadorCuarentena;
import actores.ServidorEntrega;
import model.BuzonCuarentena;
import model.BuzonEntrada;
import model.BuzonEntrega;
import model.Mensaje;

public class Main {
    public static void main(String[] args) {
        // Configuración
        int numClientes = 2;
        int mensajesPorCliente = 2;
        int numFiltros = 2;
        int numServidores = 2;
        int capacidadEntrada = 5;
        int capacidadEntrega = 3;

        System.out.println("INICIANDO SISTEMA DE MENSAJERÍA");

        // Crear buzones
        BuzonEntrada buzonEntrada = new BuzonEntrada(capacidadEntrada);
        BuzonEntrega buzonEntrega = new BuzonEntrega(capacidadEntrega);
        BuzonCuarentena buzonCuarentena = new BuzonCuarentena();

        // === 1. CREAR E INICIAR CLIENTES ===
        System.out.println("INICIANDO CLIENTES...");
        for (int i = 1; i <= numClientes; i++) {
            ClienteEmisor cliente = new ClienteEmisor(i, buzonEntrada, mensajesPorCliente);
            cliente.start();
        }

        // === 2. CREAR E INICIAR MANEJADOR CUARENTENA ===
        System.out.println("INICIANDO MANEJADOR CUARENTENA...");
        ManejadorCuarentena manejadorCuarentena = new ManejadorCuarentena(buzonCuarentena, buzonEntrega);
        manejadorCuarentena.start();

        // === 3. CREAR E INICIAR FILTROS SPAM ===
        System.out.println("INICIANDO FILTROS SPAM...");
        FiltroSpam[] filtros = new FiltroSpam[numFiltros];
        for (int i = 0; i < numFiltros; i++) {
            filtros[i] = new FiltroSpam(i, buzonEntrada, buzonEntrega, buzonCuarentena, numClientes);
            filtros[i].start();
        }

        // === 4. CREAR E INICIAR SERVIDORES ENTREGA ===
        System.out.println("INICIANDO SERVIDORES ENTREGA...");
        ServidorEntrega[] servidores = new ServidorEntrega[numServidores];
        for (int i = 0; i < numServidores; i++) {
            servidores[i] = new ServidorEntrega(i, buzonEntrega);
            servidores[i].start();
        }

        try {
            // === ESPERAR A QUE TERMINEN LOS COMPONENTES ===

            // 1. Esperar que todos los clientes terminen
            System.out.println("\n ESPERANDO A QUE CLIENTES TERMINEN...");
            Thread.sleep(5000); // Dar tiempo a que procesen

            // 2. Esperar que filtros terminen (con timeout)
            System.out.println("ESPERANDO A QUE FILTROS TERMINEN...");
            boolean filtrosTerminaron = true;
            for (FiltroSpam filtro : filtros) {
                filtro.join(8000); // Timeout de 5 segundos
                if (filtro.isAlive()) {
                    System.out.println("ERROR: " + filtro.getName() + " no terminó en tiempo, interrumpiendo...");
                    filtro.interrupt();
                    filtrosTerminaron=false;
                }
            }

            // 3. Forzar terminación si los filtros no enviaron FIN
            if (!FiltroSpam.isSistemaCompletamenteTerminado()) {
                System.out.println("Forzando terminación del sistema...");
                Mensaje finSistema = new Mensaje(Mensaje.Tipo.FIN, "SISTEMA", -1);
                buzonEntrega.depositar(finSistema);
                buzonCuarentena.depositar(finSistema);
                buzonEntrada.cerrar();
            } else {
                System.out.println("Los filtros terminaron automáticamente");
            }

            // 4. Terminar manejador de cuarentena
            System.out.println("TERMINANDO MANEJADOR CUARENTENA...");
            manejadorCuarentena.solicitarTerminacion();
            manejadorCuarentena.join(3000);

            // 5. Esperar a que servidores terminen
            System.out.println("ESPERANDO A QUE SERVIDORES TERMINEN...");
            for (ServidorEntrega servidor : servidores) {
                servidor.join(5000);
                if (servidor.isAlive()) {
                    System.out.println("ERROR: " + servidor.getName() + " no terminó, forzando...");
                    servidor.solicitarTerminacion();
                }
            }

            // === ESTADÍSTICAS FINALES ===
            System.out.println("\n ===== FINAL =====");
            System.out.println("Buzón entrada vacío: " + buzonEntrada.estaVacio());
            System.out.println("Buzón cuarentena vacío: " + buzonCuarentena.estaVacio());
            System.out.println("Buzón entrega size: " + buzonEntrega.getSize());
            
            int totalMensajesServidores = 0;
            for (ServidorEntrega servidor : servidores) {
                totalMensajesServidores += servidor.getMensajesProcesados();
            }
            System.out.println("Total mensajes procesados por servidores: " + totalMensajesServidores);
            
            System.out.println("¡¡SISTEMA COMPLETAMENTE TERMINADO!!");

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}