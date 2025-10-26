public class Mensaje {
    public enum Tipo { INICIO, CORREO, FIN }

    private Tipo tipo;
    private String idCliente;
    private int idMensaje;
    private boolean esSpam;
    private int tiempoCuarentena;

    public Mensaje(Tipo tipo, String idCliente, int idMensaje, boolean esSpam) {
        this.tipo = tipo;
        this.idCliente = idCliente;
        this.idMensaje = idMensaje;
        this.esSpam = esSpam;
        this.tiempoCuarentena = 0;
    }

}
