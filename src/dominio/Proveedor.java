package dominio;

import java.io.Serializable;

/** Proveedor del minimarket (para facturas y costo). */
public class Proveedor implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private String nombre;
    private String telefono;

    public Proveedor(String id, String nombre, String telefono) {
        this.id = id;
        this.nombre = nombre;
        this.telefono = telefono == null ? "" : telefono;
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String n) { this.nombre = n; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String t) { this.telefono = t; }

    @Override
    public String toString() { return nombre; }
}
