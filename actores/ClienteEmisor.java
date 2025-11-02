package actores;
import model.BuzonEntrada;
import model.Mensaje;

public class ClienteEmisor extends Thread {
    private BuzonEntrada buzonEntrada;
    private int idCliente;
    private int totalMensajes;
    private int mensajesGenerados;

    public ClienteEmisor(int idCliente, BuzonEntrada buzonEntrada, int totalMensajes) {
        this.idCliente = idCliente;
        this.buzonEntrada = buzonEntrada;
        this.totalMensajes = totalMensajes;
        this.mensajesGenerados = 0;
        this.setName("Cliente-" + idCliente);
    }



    @Override
    public void run() {
        try {

            int nextId = 0;

            // 1. Enviar mensaje de INICIO
            Mensaje inicio = new Mensaje(Mensaje.Tipo.INICIO, "Cliente" + idCliente, nextId++);
            buzonEntrada.depositar(inicio);
            System.out.println(getName() + ": Mensaje INICIO enviado");
            

            // 2. Generar mensajes normales
            for (int i = 0; i < totalMensajes; i++) {
                // Generar aleatoriamente si es spam (30% de probabilidad)
                boolean esSpam = Math.random() < 0.3;
                
                Mensaje mensaje = new Mensaje(
                    Mensaje.Tipo.CORREO, 
                    "Cliente" + idCliente, 
                    nextId++, 
                    esSpam
                );
                
                buzonEntrada.depositar(mensaje);
                mensajesGenerados++;
                System.out.println(getName() + ": Mensaje " + i + " enviado (spam: " + esSpam + ")");
                
                // Pequeña pausa entre mensajes para simular producción
                Thread.sleep((long) (Math.random() * 100));
            }

            // 3. Enviar mensaje de FIN
            Mensaje fin = new Mensaje(Mensaje.Tipo.FIN, "Cliente" + idCliente, nextId++);
            buzonEntrada.depositar(fin);
            System.out.println(getName() + ": Mensaje FIN enviado. Total mensajes: " + mensajesGenerados);

        } catch (InterruptedException e) {
            System.out.println(getName() + " interrumpido");
            Thread.currentThread().interrupt();
        }
    }

    public int getMensajesGenerados() {
        return mensajesGenerados;
    }

    public int getIdCliente() {
        return idCliente;
    }
}