package dominio;

import java.io.Serializable;

/** Kardex mínimo: auditoría de cada entrada/salida de inventario. */
public class MovimientoInventario implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Tipo { ENTRADA, VENTA, AJUSTE }

    private String id;
    private String fecha; // yyyy-MM-dd HH:mm
    private String productoId;
    private Tipo tipo;
    private int cantidad;
    private int stockAntes;
    private int stockDespues;
    private String motivo;

    public MovimientoInventario(String id, String fecha, String productoId, Tipo tipo,
                                int cantidad, int stockAntes, int stockDespues, String motivo) {
        this.id = id;
        this.fecha = fecha;
        this.productoId = productoId;
        this.tipo = tipo;
        this.cantidad = cantidad;
        this.stockAntes = stockAntes;
        this.stockDespues = stockDespues;
        this.motivo = motivo == null ? "" : motivo;
    }

    public String getId() { return id; }
    public String getFecha() { return fecha; }
    public String getProductoId() { return productoId; }
    public Tipo getTipo() { return tipo; }
    public int getCantidad() { return cantidad; }
    public int getStockAntes() { return stockAntes; }
    public int getStockDespues() { return stockDespues; }
    public String getMotivo() { return motivo; }
}
