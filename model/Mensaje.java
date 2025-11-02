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

    // Constructor para mensajes de control (INICIO/FIN)
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

    public void setIdMensaje(int idMensaje) {
        this.idMensaje = idMensaje;
    }

    public void setTipo(Tipo tipo) {
        this.tipo = tipo;
    }

    public void setIdCliente(String idCliente) {
        this.idCliente = idCliente;
    }

    public boolean isEsSpam() {
        return esSpam;
    }

    public boolean getEsSpam() {
        return esSpam;
    }

    public void setEsSpam(boolean esSpam) {
        this.esSpam = esSpam;
    }

    public int getTiempoCuarentena() {
        return tiempoCuarentena;
    }

    public void setTiempoCuarentena(int tiempoCuarentena) {
        this.tiempoCuarentena = tiempoCuarentena;
    }

    @Override
    public String toString() {
        return "Mensaje{" +
                "tipo=" + tipo +
                ", cliente='" + idCliente + '\'' +
                ", id=" + idMensaje +
                ", spam=" + esSpam +
                ", cuarentena=" + tiempoCuarentena +
                '}';
    }
}
