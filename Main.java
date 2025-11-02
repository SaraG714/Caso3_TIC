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
        // === CONFIGURACIÓN GENERAL ===
        int numClientes = 5;
        int mensajesPorCliente = 10;
        int numFiltros = 1;
        int numServidores = 1;
        int capacidadEntrada = 10;
        int capacidadEntrega = 8;

        System.out.println("\n========== INICIANDO SISTEMA DE MENSAJERÍA ==========\n");

        // === CREAR BUZONES ===
        BuzonEntrada buzonEntrada = new BuzonEntrada(capacidadEntrada);
        BuzonEntrega buzonEntrega = new BuzonEntrega(capacidadEntrega);
        BuzonCuarentena buzonCuarentena = new BuzonCuarentena();

        // === 1. CREAR E INICIAR CLIENTES ===
        System.out.println("🔵 INICIANDO CLIENTES...");
        Thread[] clientes = new Thread[numClientes];

        for (int i = 0; i < numClientes; i++) {
            clientes[i] = new ClienteEmisor(i + 1, buzonEntrada, mensajesPorCliente);
            clientes[i].start();
        }

        // === 2. CREAR E INICIAR MANEJADOR DE CUARENTENA ===
        System.out.println("🟣 INICIANDO MANEJADOR DE CUARENTENA...");
        ManejadorCuarentena manejadorCuarentena = new ManejadorCuarentena(buzonCuarentena, buzonEntrega);
        manejadorCuarentena.start();

        // === 3. CREAR E INICIAR FILTROS SPAM ===
        System.out.println("🟠 INICIANDO FILTROS SPAM...");
        FiltroSpam[] filtros = new FiltroSpam[numFiltros];
        for (int i = 0; i < numFiltros; i++) {
            filtros[i] = new FiltroSpam(i, buzonEntrada, buzonEntrega, buzonCuarentena, numClientes);
            filtros[i].start();
        }

        // === 4. CREAR E INICIAR SERVIDORES DE ENTREGA ===
        System.out.println("🟢 INICIANDO SERVIDORES DE ENTREGA...");
        ServidorEntrega[] servidores = new ServidorEntrega[numServidores];
        for (int i = 0; i < numServidores; i++) {
            servidores[i] = new ServidorEntrega(i, buzonEntrega);
            servidores[i].start();
        }

        try {
            // === 5. ESPERAR A QUE LOS CLIENTES TERMINEN ===
            System.out.println("\n⏳ ESPERANDO A QUE CLIENTES TERMINEN...");
            for (Thread cliente : clientes) {
                cliente.join();  // Espera real a que cada cliente finalice
            }

            System.out.println("✅ Todos los clientes terminaron. Cerrando buzón de entrada...");
            buzonEntrada.cerrar();

            // === 6. ESPERAR A QUE FILTROS TERMINEN ===
            System.out.println("\n⏳ ESPERANDO A QUE FILTROS TERMINEN...");

            for (FiltroSpam filtro : filtros) {
                System.out.println("  - " + filtro.getName() + ": " +
                        (filtro.isAlive() ? "ACTIVO" : "TERMINADO"));
                filtro.join(10000); // Espera hasta 8 segundos

                if (filtro.isAlive()) {
                    System.out.println("⚠️ ERROR: " + filtro.getName() +
                            " no terminó en tiempo. Interrumpiendo...");
                    filtro.interrupt();
                }
            }

            // === 7. SI LOS FILTROS NO TERMINAN, FORZAR FIN DEL SISTEMA ===
            if (!FiltroSpam.isSistemaCompletamenteTerminado()) {
                System.out.println("🚨 Forzando terminación del sistema...");
                try {
                    Mensaje finSistema = new Mensaje(Mensaje.Tipo.FIN, "SISTEMA", -1);
                    buzonEntrega.depositar(finSistema);
                    buzonCuarentena.depositar(finSistema);
                } catch (InterruptedException e) {
                    System.out.println("Error forzando terminación: " + e.getMessage());
                }
            } else {
                System.out.println("✅ Los filtros terminaron automáticamente");
            }

            // Dar tiempo para que los FIN se depositen y procesen
            Thread.sleep(2000);
            
            // Esperar a que el buzón de entrega se vacíe (servidores procesan FIN)
            // O cerrarlo si aún tiene mensajes después de un tiempo
            int intentos = 0;
            while (!buzonEntrega.estaVacio() && intentos < 10) {
                Thread.sleep(500);
                intentos++;
            }
            
            // Cerrar buzones para permitir que servidores y manejador terminen
            // (aunque aún tengan mensajes, cerrar permite que retirar() funcione normalmente
            // y solo devuelva null cuando esté vacío)
            System.out.println("🔒 Cerrando buzón de entrega...");
            buzonEntrega.cerrar();
            System.out.println("🔒 Cerrando buzón de cuarentena...");
            buzonCuarentena.cerrar();

            // === 8. FINALIZAR MANEJADOR DE CUARENTENA ===
            System.out.println("\n🟣 TERMINANDO MANEJADOR DE CUARENTENA...");
            manejadorCuarentena.solicitarTerminacion();
            manejadorCuarentena.join(3000);

            // === 9. ESPERAR A QUE SERVIDORES TERMINEN ===
            System.out.println("\n🟢 ESPERANDO A QUE SERVIDORES TERMINEN...");
            for (ServidorEntrega servidor : servidores) {
                servidor.join(5000);
                if (servidor.isAlive()) {
                    System.out.println("⚠️ " + servidor.getName() +
                            " no terminó, forzando terminación...");
                    servidor.solicitarTerminacion();
                }
            }

            // === 10. ESTADÍSTICAS FINALES ===
            System.out.println("\n========== ESTADÍSTICAS FINALES ==========");
            System.out.println("📦 Buzón entrada vacío: " + buzonEntrada.estaVacio());
            System.out.println("📦 Buzón cuarentena vacío: " + buzonCuarentena.estaVacio());
            System.out.println("📦 Buzón entrega (pendientes): " + buzonEntrega.getSize());

            int totalMensajesServidores = 0;
            for (ServidorEntrega servidor : servidores) {
                totalMensajesServidores += servidor.getMensajesProcesados();
            }
            System.out.println("✉️ Total mensajes procesados por servidores: " + totalMensajesServidores);

            System.out.println("\n✅✅ SISTEMA COMPLETAMENTE TERMINADO ✅✅");

        } catch (InterruptedException e) {
            System.out.println("Error de interrupción en el flujo principal: " + e.getMessage());
        }
    }
}
