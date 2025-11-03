package actores;
import model.BuzonEntrada;
import model.Mensaje;

public class ClienteEmisor extends Thread {
    private BuzonEntrada buzonEntrada;
    private int idCliente;
    private int totalMensajes;

    public ClienteEmisor(int idCliente, BuzonEntrada buzonEntrada, int totalMensajes) {
        this.idCliente = idCliente;
        this.buzonEntrada = buzonEntrada;
        this.totalMensajes = totalMensajes;
        this.setName("Cliente-" + idCliente);
    }

    @Override
    public void run() {
        try {

            int nextId = 0;

            Mensaje inicio = new Mensaje(Mensaje.Tipo.INICIO, String.valueOf(idCliente), nextId++);
            buzonEntrada.depositar(inicio);

            for (int i = 0; i < totalMensajes; i++) {
                boolean esSpam = Math.random() < 0.3;
                
                Mensaje mensaje = new Mensaje(
                    Mensaje.Tipo.CORREO, 
                    String.valueOf(idCliente), 
                    nextId++, 
                    esSpam
                );
                
                buzonEntrada.depositar(mensaje);
                Thread.sleep((long) (Math.random() * 100));
            }

            Mensaje fin = new Mensaje(Mensaje.Tipo.FIN, String.valueOf(idCliente), nextId++);
            buzonEntrada.depositar(fin);

        } catch (InterruptedException e) {
            System.out.println(getName() + " interrumpido");
            Thread.currentThread().interrupt();
        }
    }
}