public class ClienteEmisor extends Thread {
    private String id;
    private int numMensajes;
    private Buzon buzonEntrada;

    public ClienteEmisor(String id, int numMensajes, Buzon buzonEntrada) {
        this.id = id;
        this.numMensajes = numMensajes;
        this.buzonEntrada = buzonEntrada;
    }

    @Override
    public void run() {
        try {
            System.out.println(id + " -> enviando INICIO");            // Mensaje de inicio
            buzonEntrada.depositar(new Mensaje(Mensaje.Tipo.INICIO, id, 0, false));

            // Mensajes de correo
            for (int i = 1; i <= numMensajes; i++) {
                boolean esSpam = Math.random() < 0.3; // 30% de probabilidad de spam
                //prueba
                System.out.println(id + " -> enviando correo #" + i + (esSpam ? " [SPAM]" : ""));
                buzonEntrada.depositar(new Mensaje(Mensaje.Tipo.CORREO, id, i, esSpam));
            }

            // Mensaje de fin
            System.out.println(id + " -> enviando FIN");
            buzonEntrada.depositar(new Mensaje(Mensaje.Tipo.FIN, id, 0, false));

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
