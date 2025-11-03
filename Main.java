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
                cliente.join();
            }

            System.out.println("✅ Todos los clientes terminaron. Cerrando buzón de entrada...");
            buzonEntrada.cerrar();

            // === 6. ESPERAR A QUE FILTROS TERMINEN NATURALMENTE ===
            System.out.println("\n⏳ ESPERANDO A QUE FILTROS TERMINEN...");
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
                        System.out.println("Estado filtros: " + FiltroSpam.getEstado() + 
                                         " - Intento " + intentosEspera + "/" + maxIntentos);
                        break;
                    }
                }
            }

            if (!todosFiltrosTerminados) {
                System.out.println("🚨 Algunos filtros no terminaron automáticamente");
            } else {
                System.out.println("✅ Todos los filtros terminaron automáticamente");
            }

            // === 7. ESTRATEGIA DE TERMINACIÓN MEJORADA ===
            System.out.println("\n🔁 INICIANDO ESTRATEGIA DE TERMINACIÓN MEJORADA...");
            
            // Dar tiempo para que el FIN se procese
            Thread.sleep(2000);
            
            // Si los servidores no han recibido FIN todavía, depositar FIN adicional
            if (!ServidorEntrega.isFinGlobalRecibido()) {
                System.out.println("🔄 Depositando FIN adicional para servidores...");
                try {
                    Mensaje finAdicional = new Mensaje(Mensaje.Tipo.FIN, "SISTEMA", -2);
                    buzonEntrega.depositar(finAdicional);
                } catch (InterruptedException e) {
                    System.out.println("Error depositando FIN adicional: " + e.getMessage());
                }
            }

            // Esperar un poco más
            Thread.sleep(2000);

            // === 8. CERRAR BUZONES ===
            System.out.println("🔒 Cerrando buzón de entrega...");
            buzonEntrega.cerrar();
            System.out.println("🔒 Cerrando buzón de cuarentena...");
            buzonCuarentena.cerrar();

            // === 9. ESPERAR TERMINACIÓN NATURAL ===
            System.out.println("\n🟢 ESPERANDO A QUE SERVIDORES TERMINEN...");
            boolean todosServidoresTerminados = true;
            for (ServidorEntrega servidor : servidores) {
                servidor.join(3000);
                if (servidor.isAlive()) {
                    todosServidoresTerminados = false;
                    System.out.println("⚠️ " + servidor.getName() + " no terminó en tiempo");
                    servidor.solicitarTerminacion();
                    servidor.join(1000);
                }
            }

            if (todosServidoresTerminados) {
                System.out.println("✅ Todos los servidores terminaron correctamente");
            }

            System.out.println("\n🟣 ESPERANDO A QUE MANEJADOR TERMINE...");
            manejadorCuarentena.join(2000);
            if (manejadorCuarentena.isAlive()) {
                System.out.println("⚠️ ManejadorCuarentena no terminó en tiempo");
                manejadorCuarentena.solicitarTerminacion();
            }

            // === 10. ESTADÍSTICAS FINALES ===
            System.out.println("\n========== ESTADÍSTICAS FINALES ==========");
            System.out.println("📦 Buzón entrada vacío: " + buzonEntrada.estaVacio());
            System.out.println("📦 Buzón cuarentena vacío: " + buzonCuarentena.estaVacio());
            System.out.println("📦 Buzón entrega vacío: " + buzonEntrega.estaVacio());
            System.out.println("📦 Buzón entrega (pendientes): " + buzonEntrega.getSize());

            int totalMensajesServidores = 0;
            for (ServidorEntrega servidor : servidores) {
                totalMensajesServidores += servidor.getMensajesProcesados();
            }
            System.out.println("✉️ Total mensajes procesados por servidores: " + totalMensajesServidores);
            System.out.println("📊 Total mensajes esperados: " + (numClientes * mensajesPorCliente));
            System.out.println("🗑️ Mensajes spam descartados: " + ManejadorCuarentena.getMensajesDescartados());
           

            // Verificar terminación completa
            boolean sistemaCompletamenteTerminado = true;
            for (ServidorEntrega servidor : servidores) {
                if (servidor.isAlive()) {
                    sistemaCompletamenteTerminado = false;
                    System.out.println("❌ " + servidor.getName() + " aún está activo");
                }
            }
            if (manejadorCuarentena.isAlive()) {
                sistemaCompletamenteTerminado = false;
                System.out.println("❌ ManejadorCuarentena aún está activo");
            }
            for (FiltroSpam filtro : filtros) {
                if (filtro.isAlive()) {
                    sistemaCompletamenteTerminado = false;
                    System.out.println("❌ " + filtro.getName() + " aún está activo");
                }
            }

            if (sistemaCompletamenteTerminado) {
                System.out.println("\n✅✅ SISTEMA COMPLETAMENTE TERMINADO ✅✅");
            } else {
                System.out.println("\n⚠️⚠️ SISTEMA PARCIALMENTE TERMINADO ⚠️⚠️");
                System.out.println("(Esto puede ser aceptable si todos los buzones están vacíos)");
            }

        } catch (InterruptedException e) {
            System.out.println("Error de interrupción en el flujo principal: " + e.getMessage());
        }
    }
}