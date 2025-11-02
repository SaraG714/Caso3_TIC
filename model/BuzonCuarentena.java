package model;

public class BuzonCuarentena extends Buzon {

    public BuzonCuarentena() {
        super(Tipo.CUARENTENA, -1);
    }

    @Override
    public void depositar(Mensaje m) throws InterruptedException {
        // ESPERA SEMIACTIVA - capacidad ilimitada, solo verificar si está cerrado
        synchronized (this) {
            if (cerrado) {
                throw new IllegalStateException("Buzón cerrado");
            }
            cola.add(m);
            System.out.println("[Buzon " + tipo.toString() + "]: Mensaje depositado -> " + 
                              m.getTipo() + " de Cliente " + m.getIdCliente() + 
                              " (ID: " + m.getIdMensaje() + ")");
            notifyAll();
        }
    }

    @Override
    public Mensaje retirar() throws InterruptedException {
        // ESPERA SEMIACTIVA
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
            Thread.yield(); // Libera CPU fuera del synchronized
        }
    }
}