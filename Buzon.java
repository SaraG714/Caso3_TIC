import java.util.LinkedList;
import java.util.Queue;

public class Buzon {
    private Queue<Mensaje> cola = new LinkedList<>();
    private int capacidad; // 0 = ilimitado

    public Buzon(int capacidad) {
        this.capacidad = capacidad;
    }

    public synchronized void depositar(Mensaje m) throws InterruptedException {
        while (capacidad > 0 && cola.size() == capacidad) {
            wait(); // Espera pasiva si está lleno
        }
        cola.add(m);
        System.out.println("[Buzon]: Mensaje depositado -> " + m.getTipo() + " from " + m.getIdCliente() + " (ID: " + m.getIdMensaje() + ")");
        notifyAll(); // Despierta a los consumidores
    }

    public synchronized Mensaje retirar() throws InterruptedException {
        while (cola.isEmpty()) {
            wait(); // Espera pasiva si está vacío
        }
        Mensaje m = cola.poll();
        notifyAll(); // Despierta a los productores
        return m;
    }

    public synchronized boolean estaVacio() {
        return cola.isEmpty();
    }
}
