package model;
import java.util.LinkedList;
import java.util.Queue;

public abstract class Buzon {
    protected Queue<Mensaje> cola = new LinkedList<>();
    protected int capacidad; //-1 ilimitado
    public enum Tipo { ENTRADA, CUARENTENA, ENTREGA }
    protected Tipo tipo;
    protected volatile boolean cerrado = false; 


    public Buzon(Tipo tipo, int capacidad) {
        this.capacidad = capacidad;
        this.tipo = tipo;
    }

    public synchronized void depositar(Mensaje m) throws InterruptedException {
        if (cerrado) {
            throw new IllegalStateException("Buzon cerrado");
        }
        while (capacidad > 0 && cola.size() >= capacidad) {
            wait();
        }
        cola.add(m);
        System.out.println("[Buzon " + tipo.toString() + "]: Mensaje depositado -> " + 
                          m.getTipo() + " from " + m.getIdCliente() + 
                          " (ID: " + m.getIdMensaje() + ")");
        notifyAll();
    }

    public Mensaje retirar() throws InterruptedException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'retirar'");
    }

    public synchronized void cerrar() {
        this.cerrado = true;
        notifyAll();
        System.out.println("[Buzon " + tipo.toString() + "]: CERRADO - No acepta mas mensajes");
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
