package dominio;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** Factura de compra a proveedor con vencimiento (F3). Al recibirla sube stock. */
public class CompraProveedor implements Serializable {
    private static final long serialVersionUID = 1L;

    public static class ItemC implements Serializable {
        private static final long serialVersionUID = 1L;
        public String productoId; public int cantidad; public double costoUnit;
        public ItemC(String p, int c, double co) { productoId = p; cantidad = c; costoUnit = co; }
    }

    private String id;
    private String proveedorId;
    private String fecha;       // yyyy-MM-dd HH:mm
    private String vencimiento; // yyyy-MM-dd
    private double total;
    private boolean pagada;
    private List<ItemC> items = new ArrayList<>();

    public CompraProveedor(String id, String proveedorId, String fecha, String vencimiento) {
        this.id = id; this.proveedorId = proveedorId; this.fecha = fecha;
        this.vencimiento = vencimiento == null ? "" : vencimiento;
    }

    public void agregar(ItemC it) { items.add(it); total += it.cantidad * it.costoUnit; }
    public String getId() { return id; }
    public String getProveedorId() { return proveedorId; }
    public String getFecha() { return fecha; }
    public String getVencimiento() { return vencimiento; }
    public double getTotal() { return total; }
    public boolean isPagada() { return pagada; }
    public void setPagada(boolean p) { this.pagada = p; }
    public List<ItemC> getItems() { return items; }
}
