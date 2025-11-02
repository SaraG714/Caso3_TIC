package model;

public class BuzonEntrada extends Buzon {
    
    public BuzonEntrada(int capacidad) {
        super(Tipo.ENTRADA, capacidad);
    }

    @Override
    public synchronized Mensaje retirar() throws InterruptedException {
        // Espera hasta que haya un mensaje o el buzón esté cerrado y vacío
        while (cola.isEmpty()) {
            if (isCerrado()) {
                // cerrado y vacío -> no hay más mensajes
                return null;
            }
            wait(); // espera pasiva hasta notify/notifyAll (cuando depositan o cuando cerramos)
        }
    
        Mensaje m = cola.poll();
        notifyAll(); // despertar productores si estaban esperando espacio
        System.out.println("[Buzon " + getTipo() + "]: Mensaje retirado -> " +
                           m.getTipo() + " de Cliente " + m.getIdCliente());
        return m;
    }
    
    
}
