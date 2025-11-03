package model;
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

    public Mensaje(Tipo tipo, String idCliente, int idMensaje) {
        this(tipo, idCliente, idMensaje, false);
    }

    public int getIdMensaje() {
        return idMensaje;
    }
    public Tipo getTipo() {
        return tipo;
    }
    public String getIdCliente() {
        return idCliente;
    }

    public boolean isEsSpam() {
        return esSpam;
    }

    public int getTiempoCuarentena() {
        return tiempoCuarentena;
    }

    public void setTiempoCuarentena(int tiempoCuarentena) {
        this.tiempoCuarentena = tiempoCuarentena;
    }
}
