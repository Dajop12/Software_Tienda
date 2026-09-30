package servicio;

import datos.GestorDatos;
import dominio.MovimientoInventario;
import dominio.Venta;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * F13: sync offline multi-caja por archivo (USB). Append-only por id: no duplica.
 * Exporta ventas+movimientos+clientes a sync/sync-*.json. Importa lo nuevo.
 * Regla: todas las cajas parten del mismo backup; se pasan el archivo en orden.
 */
public class SyncService {
    public static String exportar() throws Exception {
        Files.createDirectories(Paths.get("sync"));
        String nombre = "sync/sync-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".json";
        GestorDatos g = GestorDatos.getInstancia();
        StringBuilder sb = new StringBuilder("{\"ventas\":[");
        List<Venta> vs = g.getVentas();
        for (int i = 0; i < vs.size(); i++) {
            Venta v = vs.get(i);
            if (i > 0) sb.append(",");
            sb.append("{\"id\":\"").append(e(v.getId())).append("\",\"folio\":\"").append(e(v.getFolio()))
                    .append("\",\"fecha\":\"").append(e(v.getFecha())).append("\",\"vendedor\":\"").append(e(v.getVendedor()))
                    .append("\",\"tipo\":\"").append(v.getTipoPago()).append("\",\"cliente\":\"").append(e(v.getClienteId()))
                    .append("\",\"estado\":\"").append(v.getEstado()).append("\",\"total\":").append(v.getTotal()).append(",\"items\":[");
            List<Venta.Item> its = v.getItems();
            for (int j = 0; j < its.size(); j++) {
                Venta.Item it = its.get(j);
                if (j > 0) sb.append(",");
                sb.append("{\"pid\":\"").append(e(it.productoId)).append("\",\"cant\":").append(it.cantidad)
                        .append(",\"precio\":").append(it.precioUnit).append(",\"costo\":").append(it.costoUnit).append("}");
            }
            sb.append("]}");
        }
        sb.append("],\"movimientos\":[");
        List<MovimientoInventario> ms = g.getMovimientos();
        for (int i = 0; i < ms.size(); i++) {
            MovimientoInventario m = ms.get(i);
            if (i > 0) sb.append(",");
            sb.append("{\"id\":\"").append(e(m.getId())).append("\",\"fecha\":\"").append(e(m.getFecha()))
                    .append("\",\"pid\":\"").append(e(m.getProductoId())).append("\",\"tipo\":\"").append(m.getTipo())
                    .append("\",\"cant\":").append(m.getCantidad()).append(",\"antes\":").append(m.getStockAntes())
                    .append(",\"despues\":").append(m.getStockDespues()).append("}");
        }
        sb.append("]}");
        Files.writeString(Paths.get(nombre), sb.toString());
        Bitacora.registrar("Sync exportado: " + nombre);
        return nombre;
    }

    /** Importa lo nuevo por id. Retorna "ventas nuevas + movimientos nuevos". */
    public static String importar(String archivo) throws Exception {
        String json = Files.readString(Paths.get(archivo));
        GestorDatos g = GestorDatos.getInstancia();
        Set<String> idsV = new HashSet<>();
        for (Venta v : g.getVentas()) idsV.add(v.getId());
        Set<String> idsM = new HashSet<>();
        for (MovimientoInventario m : g.getMovimientos()) idsM.add(m.getId());
        int nv = 0, nm = 0;
        // ventas: extrae bloques por "id"... parseo mínimo por marcadores
        for (String bloque : partir(json, "\"ventas\":[", "]")) {
            for (String o : objetos(bloque)) {
                String id = campo(o, "id");
                if (id.isEmpty() || idsV.contains(id)) continue;
                Venta v = new Venta(id, campo(o, "fecha"), campo(o, "vendedor"));
                v.setFolio(campo(o, "folio"));
                v.setTipoPago(campo(o, "tipo"));
                v.setClienteId(campo(o, "cliente"));
                String items = subarreglo(o, "items");
                for (String it : objetos(items)) {
                    v.agregarItem(new Venta.Item(campo(it, "pid"), "", num(it, "cant"),
                            numd(it, "precio"), numd(it, "costo")));
                }
                // nombres reales si el producto existe aquí
                for (Venta.Item it : v.getItems()) {
                    dominio.Producto p = g.buscarProducto(it.productoId);
                    if (p != null) { it.nombre = p.getNombre(); p.setStock(p.getStock() - 0); }
                }
                String est = campo(o, "estado");
                if ("CANCELADA".equals(est)) v.anular("sync");
                g.getVentas().add(v);
                idsV.add(id);
                nv++;
            }
        }
        for (String bloque : partir(json, "\"movimientos\":[", "]")) {
            for (String o : objetos(bloque)) {
                String id = campo(o, "id");
                if (id.isEmpty() || idsM.contains(id)) continue;
                g.getMovimientos().add(new MovimientoInventario(id, campo(o, "fecha"), campo(o, "pid"),
                        "VENTA".equals(campo(o, "tipo")) ? MovimientoInventario.Tipo.VENTA
                                : "ENTRADA".equals(campo(o, "tipo")) ? MovimientoInventario.Tipo.ENTRADA
                                        : MovimientoInventario.Tipo.AJUSTE,
                        num(o, "cant"), num(o, "antes"), num(o, "despues"), "sync"));
                idsM.add(id);
                nm++;
            }
        }
        g.guardarTodo();
        Bitacora.registrar("Sync importado " + archivo + ": +" + nv + " ventas, +" + nm + " movs");
        Auditoria.registrar("caja", "SYNC", "IMPORTAR", archivo + " +" + nv + "v +" + nm + "m");
        return "+" + nv + " ventas, +" + nm + " movimientos. Stock: verifica con kardex (no se pisa).";
    }

    // ---- mini parser JSON (solo este formato) ----
    private static List<String> partir(String json, String ini, String fin) {
        List<String> r = new ArrayList<>();
        int a = json.indexOf(ini);
        if (a < 0) return r;
        a += ini.length();
        int b = json.indexOf(fin, a);
        if (b < 0) return r;
        r.add(json.substring(a, b));
        return r;
    }

    private static List<String> objetos(String arr) {
        List<String> r = new ArrayList<>();
        int d = 0, ini = -1;
        for (int i = 0; i < arr.length(); i++) {
            char c = arr.charAt(i);
            if (c == '{') { if (d == 0) ini = i; d++; }
            else if (c == '}') { d--; if (d == 0 && ini >= 0) { r.add(arr.substring(ini, i + 1)); ini = -1; } }
        }
        return r;
    }

    private static String subarreglo(String o, String clave) {
        String k = "\"" + clave + "\":[";
        int a = o.indexOf(k);
        if (a < 0) return "";
        a += k.length();
        int d = 1;
        for (int i = a; i < o.length(); i++) {
            if (o.charAt(i) == '[') d++;
            else if (o.charAt(i) == ']') { d--; if (d == 0) return o.substring(a, i); }
        }
        return "";
    }

    private static String campo(String o, String clave) {
        String k = "\"" + clave + "\":\"";
        int a = o.indexOf(k);
        if (a >= 0) {
            a += k.length();
            int b = o.indexOf("\"", a);
            return b > a ? o.substring(a, b) : "";
        }
        String kn = "\"" + clave + "\":";
        a = o.indexOf(kn);
        if (a < 0) return "";
        a += kn.length();
        int b = a;
        while (b < o.length() && "-+.0123456789".indexOf(o.charAt(b)) >= 0) b++;
        return o.substring(a, b);
    }

    private static int num(String o, String clave) {
        try { return (int) Double.parseDouble(campo(o, clave)); } catch (Exception e) { return 0; }
    }

    private static double numd(String o, String clave) {
        try { return Double.parseDouble(campo(o, clave)); } catch (Exception e) { return 0; }
    }

    private static String e(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
