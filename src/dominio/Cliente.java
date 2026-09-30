package dominio;

import java.io.Serializable;

/** Cliente para fiado (F3). Deuda acumulada de ventas a crédito. */
public class Cliente implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private String nombre;
    private String telefono;
    private double deuda;

    public Cliente(String id, String nombre, String telefono) {
        this.id = id;
        this.nombre = nombre;
        this.telefono = telefono == null ? "" : telefono;
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String n) { this.nombre = n; }
    public String getTelefono() { return telefono; }
    public double getDeuda() { return deuda; }
    public void setDeuda(double d) { this.deuda = Math.max(0, d); }

    @Override
    public String toString() { return nombre + (deuda > 0 ? " (debe $" + String.format("%.2f", deuda) + ")" : ""); }
}
