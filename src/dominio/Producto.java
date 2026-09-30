package dominio;

import java.io.Serializable;

/** Producto minimarket: costo proveedor vs precio venta + stock mínimo + barras. */
public class Producto implements Serializable {
    private static final long serialVersionUID = 1L; // se mantiene para migrar .dat viejos

    private String id;
    private String nombre;
    private String categoria;
    private double precio; // precioVenta (se conserva nombre por compatibilidad)
    private int stock;
    // --- F1 minimarket (nuevos, con defaults para datos viejos) ---
    private double costoProveedor;
    private int stockMin = 5;
    private String codigoBarras = "";
    private String proveedorId = "";
    private String fechaVence = ""; // yyyy-MM-dd o ""

    public Producto(String id, String nombre, String categoria, double precio, int stock) {
        this(id, nombre, categoria, precio * 0.7, precio, stock, 5, "", "", "");
    }

    public Producto(String id, String nombre, String categoria, double costoProveedor,
                    double precioVenta, int stock, int stockMin, String codigoBarras,
                    String proveedorId, String fechaVence) {
        this.id = id;
        this.nombre = nombre;
        this.categoria = categoria;
        this.costoProveedor = costoProveedor;
        this.precio = precioVenta;
        this.stock = stock;
        this.stockMin = stockMin;
        this.codigoBarras = codigoBarras == null ? "" : codigoBarras;
        this.proveedorId = proveedorId == null ? "" : proveedorId;
        this.fechaVence = fechaVence == null ? "" : fechaVence;
    }

    /** Normaliza datos viejos (null/0) tras deserializar. */
    public void migrarSiFalta() {
        if (codigoBarras == null) codigoBarras = "";
        if (proveedorId == null) proveedorId = "";
        if (fechaVence == null) fechaVence = "";
        if (stockMin <= 0) stockMin = 5;
        if (costoProveedor <= 0 && precio > 0) costoProveedor = Math.round(precio * 0.7 * 100.0) / 100.0;
    }

    public double getValorTotal() { return precio * stock; }
    public double getValorCosto() { return costoProveedor * stock; }
    public double getMargen() { return precio - costoProveedor; }
    public double getMargenPct() { return precio > 0 ? (getMargen() / precio) * 100.0 : 0; }
    public boolean stockBajo() { return stock <= stockMin; }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String n) { this.nombre = n; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String c) { this.categoria = c; }
    public double getPrecio() { return precio; }
    public void setPrecio(double p) { this.precio = p; }
    public double getPrecioVenta() { return precio; }
    public void setPrecioVenta(double p) { this.precio = p; }
    public double getCostoProveedor() { return costoProveedor; }
    public void setCostoProveedor(double c) { this.costoProveedor = c; }
    public int getStockMin() { return stockMin; }
    public void setStockMin(int m) { this.stockMin = m; }
    public String getCodigoBarras() { return codigoBarras; }
    public void setCodigoBarras(String c) { this.codigoBarras = c == null ? "" : c; }
    public String getProveedorId() { return proveedorId; }
    public void setProveedorId(String p) { this.proveedorId = p == null ? "" : p; }
    public String getFechaVence() { return fechaVence; }
    public void setFechaVence(String f) { this.fechaVence = f == null ? "" : f; }
    public int getStock() { return stock; }
    public void setStock(int s) { this.stock = s; }

    @Override
    public String toString() { return nombre + " - $" + precio + " (stock " + stock + ")"; }
}
