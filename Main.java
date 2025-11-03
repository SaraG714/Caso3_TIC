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
        if (args.length < 1) {
            System.out.println("Uso: java Main <archivo_configuracion>");
            System.out.println("Ejemplo: java Main config.txt");
            System.exit(1);
        }

        String archivoConfig = args[0];

        try {
            Configuracion config = new Configuracion(archivoConfig);
            config.mostrarConfiguracion();

            int numClientes = config.getNumClientes();
            int mensajesPorCliente = config.getMensajesPorCliente();
            int numFiltros = config.getNumFiltros();
            int numServidores = config.getNumServidores();
            int capacidadEntrada = config.getCapacidadEntrada();
            int capacidadEntrega = config.getCapacidadEntrega();

            ejecutarSistema(numClientes, mensajesPorCliente, numFiltros, numServidores, 
                           capacidadEntrada, capacidadEntrega);

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void ejecutarSistema(int numClientes, int mensajesPorCliente, int numFiltros,
                                     int numServidores, int capacidadEntrada, int capacidadEntrega) {
        System.out.println("\n========== INICIANDO SISTEMA DE MENSAJERIA ==========\n");

        BuzonEntrada buzonEntrada = new BuzonEntrada(capacidadEntrada);
        BuzonEntrega buzonEntrega = new BuzonEntrega(capacidadEntrega);
        BuzonCuarentena buzonCuarentena = new BuzonCuarentena();

        System.out.println("- Iniciando " + numClientes + " clientes...");
        Thread[] clientes = new Thread[numClientes];
        for (int i = 0; i < numClientes; i++) {
            clientes[i] = new ClienteEmisor(i + 1, buzonEntrada, mensajesPorCliente);
            clientes[i].start();
        }

        System.out.println("- Iniciando manejador de cuarentena...");
        ManejadorCuarentena manejadorCuarentena = new ManejadorCuarentena(buzonCuarentena, buzonEntrega);
        manejadorCuarentena.start();

        System.out.println("- Iniciando " + numFiltros + " filtros spam...");
        FiltroSpam[] filtros = new FiltroSpam[numFiltros];
        for (int i = 0; i < numFiltros; i++) {
            filtros[i] = new FiltroSpam(i, buzonEntrada, buzonEntrega, buzonCuarentena, numClientes);
            filtros[i].start();
        }

        System.out.println("- Iniciando " + numServidores + " servidores de entrega...");
        ServidorEntrega[] servidores = new ServidorEntrega[numServidores];
        for (int i = 0; i < numServidores; i++) {
            servidores[i] = new ServidorEntrega(i, buzonEntrega);
            servidores[i].start();
        }

        try {
            System.out.println("\n- Esperando que clientes terminen...");
            for (Thread cliente : clientes) {
                cliente.join();
            }

            System.out.println("• Clientes terminaron. Cerrando buzón de entrada...");
            buzonEntrada.cerrar();

            System.out.println("\n- Esperando que filtros terminen...");
            boolean todosFiltrosTerminados = false;
            int intentosEspera = 0;
            int maxIntentos = 20;
            
            while (!todosFiltrosTerminados && intentosEspera < maxIntentos) {
                Thread.sleep(1000);
                intentosEspera++;
                
                todosFiltrosTerminados = true;
                for (FiltroSpam filtro : filtros) {
                    if (filtro.isAlive()) {
                        todosFiltrosTerminados = false;
                        break;
                    }
                }
            }

            Thread.sleep(2000);
            
            if (!ServidorEntrega.isFinGlobalRecibido()) {
                try {
                    Mensaje finAdicional = new Mensaje(Mensaje.Tipo.FIN, "SISTEMA", -2);
                    buzonEntrega.depositar(finAdicional);
                } catch (InterruptedException e) {
                    System.out.println("Error depositando FIN adicional: " + e.getMessage());
                }
            }

            Thread.sleep(2000);

            System.out.println("- Cerrando buzones...");
            buzonEntrega.cerrar();
            buzonCuarentena.cerrar();

            System.out.println("\n- Esperando que servidores terminen...");
            boolean todosServidoresTerminados = true;
            for (ServidorEntrega servidor : servidores) {
                servidor.join(3000);
                if (servidor.isAlive()) {
                    todosServidoresTerminados = false;
                    servidor.solicitarTerminacion();
                    servidor.join(1000);
                }
            }

            System.out.println("- Esperando que manejador termine...");
            manejadorCuarentena.join(2000);
            if (manejadorCuarentena.isAlive()) {
                manejadorCuarentena.solicitarTerminacion();
            }

            generarEstadisticasFinales(buzonEntrada, buzonCuarentena, buzonEntrega, 
                                     servidores, numClientes, mensajesPorCliente);

        } catch (InterruptedException e) {
            System.out.println("Error de interrupción en el flujo principal: " + e.getMessage());
        }
    }

    private static void generarEstadisticasFinales(BuzonEntrada buzonEntrada, BuzonCuarentena buzonCuarentena,
                                                  BuzonEntrega buzonEntrega, ServidorEntrega[] servidores,
                                                  int numClientes, int mensajesPorCliente) {
        System.out.println("\n========== ESTADISTICAS FINALES ==========");
        System.out.println("- Buzon entrada vacio: " + buzonEntrada.estaVacio());
        System.out.println("- Buzon cuarentena vacio: " + buzonCuarentena.estaVacio());
        System.out.println("- Buzon entrega vacio: " + buzonEntrega.estaVacio());
        System.out.println("- Buzon entrega (pendientes): " + buzonEntrega.getSize());

        int totalMensajesServidores = 0;
        for (ServidorEntrega servidor : servidores) {
            totalMensajesServidores += servidor.getMensajesProcesados();
        }
        System.out.println("- Total mensajes procesados: " + totalMensajesServidores);
        System.out.println("- Total mensajes esperados: " + (numClientes * mensajesPorCliente));
        System.out.println("- Mensajes spam descartados: " + ManejadorCuarentena.getMensajesDescartados());
        
        boolean sistemaCompletamenteTerminado = true;
        for (ServidorEntrega servidor : servidores) {
            if (servidor.isAlive()) {
                sistemaCompletamenteTerminado = false;
                System.out.println("- " + servidor.getName() + " aun esta activo");
            }
        }

        if (sistemaCompletamenteTerminado && buzonEntrada.estaVacio() && 
            buzonCuarentena.estaVacio() && buzonEntrega.estaVacio()) {
            System.out.println("\n• Sistema completamente terminado");
        } else {
            System.out.println("\n- Sistema parcialmente terminado");
            if (!buzonEntrada.estaVacio()) System.out.println("  * Buzon entrada no vacio");
            if (!buzonCuarentena.estaVacio()) System.out.println("  * Buzon cuarentena no vacio");
            if (!buzonEntrega.estaVacio()) System.out.println("  * Buzon entrega no vacio");
        }
    }
}