package model;
import java.util.LinkedList;
import java.util.Queue;

public abstract class Buzon {
    private Queue<Mensaje> cola = new LinkedList<>();
    private int capacidad; // -1 = ilimitado
    public enum Tipo { ENTRADA, CUARENTENA, ENTREGA }
    private Tipo tipo;
    private volatile boolean cerrado = false; 


    public Buzon(Tipo tipo, int capacidad) {
        this.capacidad = capacidad;
        this.tipo = tipo;
    }

    public synchronized void depositar(Mensaje m) throws InterruptedException {
        if (cerrado) {
            throw new IllegalStateException("Buzon cerrado");
        }
        // Para capacidad ilimitada (capacidad = -1), no hacemos wait
        while (capacidad > 0 && cola.size() >= capacidad) {
            wait(); // Espera pasiva si está lleno
        }
        cola.add(m);
        System.out.println("[Buzon " + tipo.toString() + "]: Mensaje depositado -> " + 
                          m.getTipo() + " from " + m.getIdCliente() + 
                          " (ID: " + m.getIdMensaje() + ")");
        notifyAll(); // Despierta a los consumidores
    }

    public synchronized Mensaje retirar() throws InterruptedException {
        while (cola.isEmpty() && !cerrado) {
            wait(); // Espera pasiva si está vacío
        }
        if (cola.isEmpty() && cerrado) {
            return null;
        }
        Mensaje m = cola.poll();
        notifyAll(); // Despierta a los productores
        System.out.println("[Buzon " + tipo.toString() + "]: Mensaje retirado -> " + 
                          m.getTipo() + " from " + m.getIdCliente());
        return m;
    }


    // Método base sin wait() para que lo usen las subclases
    protected synchronized void depositarSinWait(Mensaje m) {
        if (cerrado) {
            throw new IllegalStateException("Buzón cerrado");
        }
        if (capacidad > 0 && cola.size() >= capacidad) {
            throw new IllegalStateException("Buzón lleno");
        }
        cola.add(m);
        System.out.println("[Buzon " + tipo.toString() + "]: Mensaje depositado -> " + 
                          m.getTipo() + " from " + m.getIdCliente() + 
                          " (ID: " + m.getIdMensaje() + ")");
        notifyAll();
    }

    public synchronized void cerrar() {
        this.cerrado = true;
        notifyAll(); // Despertar a TODOS los threads que estén esperando en wait()
        System.out.println("[Buzon " + tipo.toString() + "]: CERRADO - No acepta más mensajes");
    }

    public synchronized boolean estaVacio() {
        return cola.isEmpty();
    }

    public synchronized int getSize() {
        return cola.size();
    }

    public String getTipo() {
        return tipo.toString();
    }

    public boolean isCerrado() {
        return cerrado;
    }
}
