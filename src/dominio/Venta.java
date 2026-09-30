package dominio;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** Venta con sus items. */
public class Venta implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Item dentro de una venta (guarda costo para contabilidad). */
    public static class Item implements Serializable {
        private static final long serialVersionUID = 1L;
        public String productoId;
        public String nombre;
        public int cantidad;
        public double precioUnit;
        public double costoUnit; // costo proveedor al momento de vender (F1)

        public Item(String productoId, String nombre, int cantidad, double precioUnit) {
            this(productoId, nombre, cantidad, precioUnit, 0);
        }

        public Item(String productoId, String nombre, int cantidad, double precioUnit, double costoUnit) {
            this.productoId = productoId;
            this.nombre = nombre;
            this.cantidad = cantidad;
            this.precioUnit = precioUnit;
            this.costoUnit = costoUnit;
        }

        public double subtotal() { return cantidad * precioUnit; }
        public double ganancia() { return (precioUnit - costoUnit) * cantidad; }
    }

    private String id;
    private String fecha; // "yyyy-MM-dd HH:mm"
    private String vendedor;
    private List<Item> items = new ArrayList<>();
    private double total;
    private String tipoPago = "CONTADO"; // CONTADO | CREDITO (F3 fiado)
    private String clienteId = "";
    private String folio = ""; // F8: T-YYYYMMDD-####
    private String estado = "ACTIVA"; // ACTIVA | CANCELADA (F9)
    private String motivoCancel = "";

    public Venta(String id, String fecha, String vendedor) {
        this.id = id;
        this.fecha = fecha;
        this.vendedor = vendedor;
    }

    public String getTipoPago() { return tipoPago; }
    public void setTipoPago(String t) { this.tipoPago = "CREDITO".equals(t) ? "CREDITO" : "CONTADO"; }
    public String getClienteId() { return clienteId == null ? "" : clienteId; }
    public void setClienteId(String c) { this.clienteId = c == null ? "" : c; }
    public boolean esCredito() { return "CREDITO".equals(tipoPago); }
    public String getFolio() { return folio == null || folio.isEmpty() ? id : folio; }
    public void setFolio(String f) { this.folio = f == null ? "" : f; }
    public boolean isCancelada() { return "CANCELADA".equals(estado); }
    public String getEstado() { return estado == null ? "ACTIVA" : estado; }
    public String getMotivoCancel() { return motivoCancel == null ? "" : motivoCancel; }
    public void anular(String motivo) {
        this.estado = "CANCELADA";
        this.motivoCancel = motivo == null ? "" : motivo;
    }

    public void agregarItem(Item it) {
        items.add(it);
        calcularTotal();
    }

    public void calcularTotal() {
        total = 0;
        for (Item it : items) total += it.subtotal();
    }

    public double getGanancia() {
        double g = 0;
        for (Item it : items) g += it.ganancia();
        return g;
    }

    public String getId() { return id; }
    public String getFecha() { return fecha; }
    public String getVendedor() { return vendedor; }
    public List<Item> getItems() { return items; }
    public double getTotal() { return total; }

    @Override
    public String toString() { return id + " | " + fecha + " | $" + total; }
}
