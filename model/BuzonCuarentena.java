package model;

public class BuzonCuarentena extends Buzon {

    public BuzonCuarentena() {
        super(Tipo.CUARENTENA, -1);
    }


    @Override
    public synchronized void depositar(Mensaje m) throws InterruptedException {
        // Para capacidad ilimitada, no hay wait() por lleno
        // Pero implementamos espera semiactiva con yield
        boolean depositado = false;
        while (!depositado) {
            try {
                super.depositarSinWait(m); // Método sin wait()
                depositado = true;
            } catch (IllegalStateException e) {
                Thread.yield(); // ESPERA SEMIACTIVA
            }
        }
    }

    @Override
    public synchronized Mensaje retirar() throws InterruptedException {
        // Espera semiactiva si vacío
        while (estaVacio() && !isCerrado()) {
            Thread.yield(); // ESPERA SEMIACTIVA
        }
        return super.retirar();
    }
    
}
