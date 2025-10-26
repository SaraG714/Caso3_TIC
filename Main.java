public class Main {
    public static void main(String[] args) {
        Buzon buzonEntrada = new Buzon(5); // Capacidad limitada
        ClienteEmisor c1 = new ClienteEmisor("C1", 3, buzonEntrada);
        ClienteEmisor c2 = new ClienteEmisor("C2", 3, buzonEntrada);

        c1.start();
        c2.start();
    }
}
