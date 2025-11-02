package model;

public class BuzonEntrega extends Buzon {

    public BuzonEntrega(int capacidad) {
        super(Tipo.ENTREGA, capacidad);
    }

    @Override
    public synchronized void depositar(Mensaje m) throws InterruptedException {
        // Espera semiactiva si lleno
        boolean depositado = false;
        while (!depositado) {
            try {
                super.depositarSinWait(m);
                depositado = true;
            } catch (IllegalStateException e) {
                Thread.yield(); // ESPERA SEMIACTIVA
            }
        }
    }

    @Override
    public synchronized Mensaje retirar() throws InterruptedException {
        // ESPERA ACTIVA - no bloquea, solo consulta
        if (estaVacio() && !isCerrado()) {
            return null; // En espera activa, el consumidor maneja el retry
        }
        return super.retirar();
    }
}
