package dominio;

import java.io.Serializable;

/** Turno de caja por cajero (F3): apertura/cierre con totales. */
public class TurnoCaja implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private String vendedor;
    private String apertura; // yyyy-MM-dd HH:mm
    private String cierre;   // "" = abierto
    private double totalVentas;
    private double totalGanancia;

    public TurnoCaja(String id, String vendedor, String apertura) {
        this.id = id; this.vendedor = vendedor; this.apertura = apertura; this.cierre = "";
    }

    public boolean abierto() { return cierre == null || cierre.isEmpty(); }
    public String getId() { return id; }
    public String getVendedor() { return vendedor; }
    public String getApertura() { return apertura; }
    public String getCierre() { return cierre; }
    public void setCierre(String c) { this.cierre = c; }
    public double getTotalVentas() { return totalVentas; }
    public void setTotalVentas(double t) { this.totalVentas = t; }
    public double getTotalGanancia() { return totalGanancia; }
    public void setTotalGanancia(double g) { this.totalGanancia = g; }
}
