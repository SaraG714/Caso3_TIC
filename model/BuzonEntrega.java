package model;

public class BuzonEntrega extends Buzon {

    public BuzonEntrega(int capacidad) {
        super(Tipo.ENTREGA, capacidad);
    }

    @Override
    public void depositar(Mensaje m) throws InterruptedException {
        // ESPERA SEMIACTIVA
        while (true) {
            synchronized (this) {
                if (cerrado) {
                    throw new IllegalStateException("Buzón cerrado");
                }
                if (capacidad <= 0 || cola.size() < capacidad) {
                    cola.add(m);
                    System.out.println("[Buzon " + tipo.toString() + "]: Mensaje depositado -> " + 
                                      m.getTipo() + " de Cliente " + m.getIdCliente() + 
                                      " (ID: " + m.getIdMensaje() + ")");
                    notifyAll();
                    return;
                }
            }
            Thread.yield(); // Libera CPU fuera del synchronized
        }
    }
    
    @Override
    public Mensaje retirar() throws InterruptedException {
        // ESPERA ACTIVA
        while (true) {
            synchronized (this) {
                if (!cola.isEmpty()) {
                    Mensaje m = cola.poll();
                    System.out.println("[Buzon " + tipo.toString() + "]: Mensaje retirado -> " + 
                                      m.getTipo() + " de Cliente " + m.getIdCliente());
                    notifyAll();
                    return m;
                }
                if (isCerrado()) {
                    return null;
                }
            }
        }
    }
}