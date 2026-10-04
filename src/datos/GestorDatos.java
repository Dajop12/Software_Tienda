package datos;

import dominio.*;
import servicio.Auditoria;
import servicio.Bitacora;
import servicio.PushService;
import java.io.*;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * GestorDatos: singleton que guarda/carga listas en data/*.dat
 * Si no hay archivos, crea datos demo.
 */
public class GestorDatos {
    private static GestorDatos instancia;

    private List<Usuario> usuarios = new ArrayList<>();
    private List<Producto> productos = new ArrayList<>();
    private List<Venta> ventas = new ArrayList<>();
    private List<Proveedor> proveedores = new ArrayList<>();
    private List<MovimientoInventario> movimientos = new ArrayList<>();
    private List<Cliente> clientes = new ArrayList<>();
    private List<TurnoCaja> turnos = new ArrayList<>();
    private List<CompraProveedor> compras = new ArrayList<>();

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private GestorDatos() {
        new File("data").mkdirs();
        cargarTodo();
    }

    public static synchronized GestorDatos getInstancia() {
        if (instancia == null) instancia = new GestorDatos();
        return instancia;
    }

    // ===== getters (listas vivas, modificar + guardarTodo) =====
    public List<Usuario> getUsuarios() { return usuarios; }
    public List<Producto> getProductos() { return productos; }
    public List<Venta> getVentas() { return ventas; }
    public List<Proveedor> getProveedores() { return proveedores; }
    public List<MovimientoInventario> getMovimientos() { return movimientos; }
    public List<Cliente> getClientes() { return clientes; }
    public List<TurnoCaja> getTurnos() { return turnos; }
    public List<CompraProveedor> getCompras() { return compras; }

    // ===== persistencia =====
    private <T> List<T> leer(String ruta, Class<T> tipo) {
        Path archivo = Paths.get(ruta);
        if (!Files.exists(archivo)) return null;
        try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(archivo))) {
            Object o = ois.readObject();
            if (!(o instanceof List<?> elementos)) {
                throw new IOException("el contenido no es una lista");
            }
            List<T> resultado = new ArrayList<>(elementos.size());
            for (Object elemento : elementos) {
                if (!tipo.isInstance(elemento)) {
                    throw new IOException("contiene un registro incompatible");
                }
                resultado.add(tipo.cast(elemento));
            }
            return resultado;
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("No se pudo leer " + ruta + ": " + e.getMessage()
                    + ". Los datos no se reemplazaron; restaura un backup antes de continuar.", e);
        }
    }

    private void escribir(String ruta, List<?> lista) {
        Path destino = Paths.get(ruta);
        Path temporal = null;
        try {
            Files.createDirectories(destino.getParent());
            temporal = Files.createTempFile(destino.getParent(), destino.getFileName().toString(), ".tmp");
            try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(temporal))) {
                oos.writeObject(lista);
                oos.flush();
            }
            try {
                Files.move(temporal, destino, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporal, destino, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudieron guardar los datos en " + ruta
                    + ": " + e.getMessage(), e);
        } finally {
            if (temporal != null) {
                try {
                    Files.deleteIfExists(temporal);
                } catch (IOException e) {
                    System.err.println("No se pudo limpiar el temporal " + temporal + ": " + e.getMessage());
                }
            }
        }
    }

    private void cargarTodo() {
        String[] archivos = {"usuarios.dat", "productos.dat", "ventas.dat", "proveedores.dat",
                "movimientos.dat", "clientes.dat", "turnos.dat", "compras.dat"};
        boolean instalacionNueva = true;
        for (String archivo : archivos) {
            if (Files.exists(Paths.get("data", archivo))) {
                instalacionNueva = false;
                break;
            }
        }
        if (instalacionNueva) {
            crearDemo();
            guardarTodo();
        } else {
            usuarios = leerRequerido("data/usuarios.dat", Usuario.class);
            productos = leerRequerido("data/productos.dat", Producto.class);
            ventas = leerRequerido("data/ventas.dat", Venta.class);
            proveedores = leerOpcional("data/proveedores.dat", Proveedor.class);
            movimientos = leerOpcional("data/movimientos.dat", MovimientoInventario.class);
            clientes = leerOpcional("data/clientes.dat", Cliente.class);
            turnos = leerOpcional("data/turnos.dat", TurnoCaja.class);
            compras = leerOpcional("data/compras.dat", CompraProveedor.class);
            migrarProductos(); // compatibilidad .dat viejos (F1)
        }
        Bitacora.registrar("Datos cargados: " + usuarios.size() + " usuarios, "
                + productos.size() + " productos, " + ventas.size() + " ventas.");
    }

    private <T> List<T> leerRequerido(String ruta, Class<T> tipo) {
        List<T> lista = leer(ruta, tipo);
        if (lista == null) {
            throw new IllegalStateException("Falta el archivo requerido " + ruta
                    + ". No se crearon datos demo para proteger la información existente; "
                    + "restaura un backup antes de iniciar.");
        }
        return lista;
    }

    private <T> List<T> leerOpcional(String ruta, Class<T> tipo) {
        List<T> lista = leer(ruta, tipo);
        return lista == null ? new ArrayList<>() : lista;
    }

    /** Migra datos viejos: defaults de producto + tablas de referencia + barras. */
    private void migrarProductos() {
        boolean cambio = false;
        java.util.Set<String> barras = new java.util.HashSet<>();
        for (Producto p : productos) {
            if (p.getCodigoBarras() != null && !p.getCodigoBarras().isBlank()) {
                barras.add(p.getCodigoBarras().trim());
            }
        }
        for (Producto p : productos) {
            int antes = p.getStockMin();
            p.migrarSiFalta();
            if (p.getStockMin() != antes) cambio = true;
            // Repara códigos autogenerados por migración previa (patrón 75017526994x)
            String demo = codigoDemo(p.getId());
            if (demo != null && !demo.equals(p.getCodigoBarras())
                    && p.getCodigoBarras() != null && p.getCodigoBarras().startsWith("75017526994")) {
                barras.remove(p.getCodigoBarras());
                if (barras.add(demo)) {
                    p.setCodigoBarras(demo);
                    cambio = true;
                    continue;
                }
            }
            // Backfill de código de barras determinista y único.
            // Los demo P-001..P-008 conservan su código documentado.
            if (p.getCodigoBarras() == null || p.getCodigoBarras().isBlank()) {
                String cod = codigoDemo(p.getId());
                if (cod == null) {
                    String base = "7501" + String.format("%08d", Math.abs(p.getId().hashCode()) % 100000000);
                    cod = base;
                    int suf = 0;
                    while (!barras.add(cod)) cod = base.substring(0, 11) + (suf++ % 10);
                } else if (!barras.add(cod)) {
                    // Ya ocupado por otro producto: genera uno libre
                    String base = "7501" + String.format("%08d", Math.abs((p.getId() + p.getNombre()).hashCode()) % 100000000);
                    cod = base;
                    int suf = 0;
                    while (!barras.add(cod)) cod = base.substring(0, 11) + (suf++ % 10);
                }
                p.setCodigoBarras(cod);
                cambio = true;
            }
            // Proveedor válido o genérico
            if (p.getProveedorId() == null || p.getProveedorId().isBlank()
                    || buscarProveedor(p.getProveedorId()) == null) {
                p.setProveedorId("PR-001");
                cambio = true;
            }
        }
        if (buscarProveedor("PR-001") == null) {
            proveedores.add(new Proveedor("PR-001", "Distribuidora Tech", "555-0101"));
            cambio = true;
        }
        if (buscarProveedor("PR-002") == null) {
            proveedores.add(new Proveedor("PR-002", "Oficina Total", "555-0102"));
            cambio = true;
        }
        if (buscarProveedor("PR-003") == null) {
            proveedores.add(new Proveedor("PR-003", "Abarrotes El Centro", "555-0103"));
            cambio = true;
        }
        if (buscarCliente("C-001") == null) {
            clientes.add(new Cliente("C-001", "Doña Marta (fiado)", "555-0201"));
            cambio = true;
        }
        if (buscarCliente("C-002") == null) {
            clientes.add(new Cliente("C-002", "Tienda La Esquina", "555-0202"));
            cambio = true;
        }
        if (cambio) guardarTodo();
    }

    private Proveedor buscarProveedor(String id) {
        for (Proveedor pr : proveedores) if (pr.getId().equals(id)) return pr;
        return null;
    }

    /** Códigos documentados de los productos demo (coinciden con README/etiquetas). */
    private static String codigoDemo(String id) {
        return switch (id) {
            case "P-001" -> "750100001001";
            case "P-002" -> "750100002002";
            case "P-003" -> "750100003003";
            case "P-004" -> "750100004004";
            case "P-005" -> "750100005005";
            case "P-006" -> "750100006006";
            case "P-007" -> "750100007007";
            case "P-008" -> "750100008008";
            default -> null;
        };
    }

    public synchronized void guardarTodo() {
        escribir("data/usuarios.dat", usuarios);
        escribir("data/productos.dat", productos);
        escribir("data/ventas.dat", ventas);
        escribir("data/proveedores.dat", proveedores);
        escribir("data/movimientos.dat", movimientos);
        escribir("data/clientes.dat", clientes);
        escribir("data/turnos.dat", turnos);
        escribir("data/compras.dat", compras);
    }

    public void restaurarDemo() {
        crearDemo();
        guardarTodo();
        Bitacora.registrar("Datos demo restaurados.");
    }

    // ===== datos demo =====
    private void crearDemo() {
        usuarios = new ArrayList<>();
        usuarios.add(new Usuario("U-001", "Administrador", "admin", "1234", "ADMIN", true));
        usuarios.add(new Usuario("U-002", "Vendedora Ana", "ana", "1234", "VENDEDOR", true));
        usuarios.add(new Usuario("U-003", "Invitado", "invitado", "1234", "CONSULTA", true));

        productos = new ArrayList<>();
        // Demo con costo (70% venta), stockMin y código de barras
        productos.add(new Producto("P-001", "Laptop HP 15\"", "Electrónica", 8750, 12500, 8, 2, "750100001001", "PR-001", ""));
        productos.add(new Producto("P-002", "Mouse inalámbrico", "Electrónica", 245, 350, 25, 10, "750100002002", "PR-001", ""));
        productos.add(new Producto("P-003", "Teclado mecánico", "Electrónica", 630, 900, 12, 5, "750100003003", "PR-001", ""));
        productos.add(new Producto("P-004", "Monitor 24\"", "Electrónica", 2240, 3200, 6, 3, "750100004004", "PR-001", ""));
        productos.add(new Producto("P-005", "Silla oficina", "Oficina", 1470, 2100, 4, 2, "750100005005", "PR-002", ""));
        productos.add(new Producto("P-006", "Cuaderno profesional", "Papelería", 60, 85, 50, 20, "750100006006", "PR-002", ""));
        productos.add(new Producto("P-007", "Café molido 1kg", "Abarrotes", 154, 220, 30, 12, "750100007007", "PR-003", "2026-12-31"));
        productos.add(new Producto("P-008", "Audífonos Bluetooth", "Electrónica", 525, 750, 3, 5, "750100008008", "PR-001", ""));

        proveedores = new ArrayList<>();
        proveedores.add(new Proveedor("PR-001", "Distribuidora Tech", "555-0101"));
        proveedores.add(new Proveedor("PR-002", "Oficina Total", "555-0102"));
        proveedores.add(new Proveedor("PR-003", "Abarrotes El Centro", "555-0103"));
        movimientos = new ArrayList<>();
        clientes = new ArrayList<>();
        clientes.add(new Cliente("C-001", "Doña Marta (fiado)", "555-0201"));
        clientes.add(new Cliente("C-002", "Tienda La Esquina", "555-0202"));
        turnos = new ArrayList<>();
        compras = new ArrayList<>();

        ventas = new ArrayList<>();
        // 5 ventas en los últimos días para que la gráfica se vea viva
        String[] vends = {"admin", "ana", "admin", "ana", "admin"};
        String[][] itemsDemo = {
            {"P-002", "2"}, {"P-006", "5"}, {"P-003", "1"},
            {"P-007", "3"}, {"P-002", "1"}
        };
        for (int i = 0; i < 5; i++) {
            LocalDateTime fecha = LocalDateTime.now().minusDays(4 - i);
            Venta v = new Venta("V-100" + i, fecha.format(FMT), vends[i]);
            String pid = itemsDemo[i][0];
            int cant = Integer.parseInt(itemsDemo[i][1]);
            Producto pr = buscarProducto(pid);
            if (pr != null) v.agregarItem(new Venta.Item(pid, pr.getNombre(), cant, pr.getPrecioVenta(), pr.getCostoProveedor()));
            ventas.add(v);
        }
    }

    public Producto buscarProducto(String id) {
        for (Producto p : productos) if (p.getId().equals(id)) return p;
        return null;
    }

    /** Búsqueda por código de barras (lector pistola / APK). */
    public Producto buscarPorCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) return null;
        String c = codigo.trim();
        for (Producto p : productos) {
            if (c.equalsIgnoreCase(p.getCodigoBarras()) || c.equalsIgnoreCase(p.getId())) return p;
        }
        return null;
    }

    /** Productos en alerta (para push al jefe + badge en PC). */
    public List<Producto> productosStockBajo() {
        List<Producto> r = new ArrayList<>();
        for (Producto p : productos) if (p.stockBajo()) r.add(p);
        return r;
    }

    /** F11: kardex de un producto (más reciente primero). */
    public List<MovimientoInventario> movimientosDe(String productoId) {
        List<MovimientoInventario> r = new ArrayList<>();
        for (int i = movimientos.size() - 1; i >= 0; i--) {
            if (movimientos.get(i).getProductoId().equals(productoId)) r.add(movimientos.get(i));
        }
        return r;
    }

    /** F11: productos que vencen en <= dias (solo con stock). Fecha yyyy-MM-dd. */
    public List<Producto> proximosAVencer(int dias) {
        List<Producto> r = new ArrayList<>();
        java.time.LocalDate hoy = java.time.LocalDate.now();
        for (Producto p : productos) {
            String f = p.getFechaVence();
            if (f == null || f.isBlank() || p.getStock() <= 0) continue;
            try {
                long d = java.time.temporal.ChronoUnit.DAYS.between(hoy, java.time.LocalDate.parse(f.trim()));
                if (d <= dias) r.add(p);
            } catch (Exception ignored) {}
        }
        r.sort((a, b) -> a.getFechaVence().compareTo(b.getFechaVence()));
        return r;
    }

    /** Kardex: registra movimiento y persiste (llamar dentro de la venta). */
    public void registrarMovimiento(String productoId, MovimientoInventario.Tipo tipo,
                                    int cantidad, int antes, int despues, String motivo) {
        movimientos.add(new MovimientoInventario(nuevoId("M"), ahora(), productoId,
                tipo, cantidad, antes, despues, motivo));
    }

    /** Resultado de venta atómica F2. */
    public static class ResultadoVenta {
        public final boolean ok;
        public final String mensaje;
        public final Venta venta;
        public final List<Producto> alertas;
        public ResultadoVenta(boolean ok, String mensaje, Venta v, List<Producto> alertas) {
            this.ok = ok; this.mensaje = mensaje; this.venta = v; this.alertas = alertas;
        }
    }

    /**
     * F2: venta atómica y sincronizada (2 cajas no descuadran).
     * Relee precio/costo de Producto (no confía en el cliente) y registra kardex.
     */
    public synchronized ResultadoVenta vender(String vendedor, List<Venta.Item> solicitud) {
        if (solicitud == null || solicitud.isEmpty())
            return new ResultadoVenta(false, "Carrito vacío.", null, productosStockBajo());
        // 1) validar todo antes de tocar stock
        for (Venta.Item s : solicitud) {
            Producto p = buscarProducto(s.productoId);
            if (p == null) return new ResultadoVenta(false, "Producto no existe: " + s.productoId, null, productosStockBajo());
            if (s.cantidad <= 0) return new ResultadoVenta(false, "Cantidad inválida en " + p.getNombre(), null, productosStockBajo());
            if (s.cantidad > p.getStock())
                return new ResultadoVenta(false, "Stock insuficiente: " + p.getNombre() + " (hay " + p.getStock() + ")", null, productosStockBajo());
        }
        // 2) aplicar
        Venta v = new Venta(nuevoId("V"), ahora(), vendedor);
        v.setFolio(siguienteFolio());
        for (Venta.Item s : solicitud) {
            Producto p = buscarProducto(s.productoId);
            int antes = p.getStock();
            p.setStock(antes - s.cantidad);
            v.agregarItem(new Venta.Item(p.getId(), p.getNombre(), s.cantidad, p.getPrecioVenta(), p.getCostoProveedor()));
            registrarMovimiento(p.getId(), MovimientoInventario.Tipo.VENTA, s.cantidad, antes, p.getStock(), "Venta " + v.getId());
        }
        ventas.add(v);
        guardarTodo();
        Bitacora.registrar("Venta " + v.getId() + " por " + vendedor + " total $" + String.format("%.2f", v.getTotal())
                + " ganancia $" + String.format("%.2f", v.getGanancia()));
        Auditoria.registrar(vendedor, "VENTAS", "VENDER", v.getFolio() + " total $" + String.format("%.2f", v.getTotal()));
        List<Producto> alertas = productosStockBajo();
        for (Producto p : alertas) PushService.encolarAlertaStock(p.getNombre(), p.getStock(), p.getStockMin());
        return new ResultadoVenta(true, "Venta " + v.getId() + " cobrada.", v, alertas);
    }

    /**
     * F9: anula venta: restaura stock + kardex ENTRADA + revierte fiado. No borra (auditable).
     */
    public synchronized ResultadoVenta anularVenta(String vendedor, String folioOId, String motivo) {
        Venta v = null;
        for (Venta x : ventas) {
            if (x.getFolio().equalsIgnoreCase(folioOId) || x.getId().equalsIgnoreCase(folioOId)) { v = x; break; }
        }
        if (v == null) return new ResultadoVenta(false, "Folio no existe.", null, productosStockBajo());
        if (v.isCancelada()) return new ResultadoVenta(false, "Ya estaba cancelada.", v, productosStockBajo());
        if (motivo == null || motivo.trim().length() < 3)
            return new ResultadoVenta(false, "Motivo mínimo 3 letras.", null, productosStockBajo());
        for (Venta.Item it : v.getItems()) {
            Producto p = buscarProducto(it.productoId);
            if (p == null) continue;
            int antes = p.getStock();
            p.setStock(antes + it.cantidad);
            registrarMovimiento(p.getId(), MovimientoInventario.Tipo.ENTRADA, it.cantidad, antes, p.getStock(),
                    "Anulación " + v.getFolio());
        }
        if (v.esCredito() && !v.getClienteId().isEmpty()) {
            Cliente c = buscarCliente(v.getClienteId());
            if (c != null) {
                // Revierte hasta lo adeudado; si ya abonó de más, el excedente queda en caja
                double revierte = Math.min(v.getTotal(), c.getDeuda());
                c.setDeuda(c.getDeuda() - revierte);
                double sobrante = v.getTotal() - revierte;
                if (sobrante > 0.005) {
                    Bitacora.registrar("Anulación " + v.getFolio() + ": abono previo $"
                            + String.format("%.2f", sobrante) + " queda en caja");
                }
            }
        }
        v.anular(motivo.trim());
        guardarTodo();
        Bitacora.registrar("Anulada " + v.getFolio() + " por " + vendedor + " (" + motivo.trim() + ")");
        Auditoria.registrar(vendedor, "VENTAS", "ANULAR", v.getFolio() + " (" + motivo.trim() + ")");
        return new ResultadoVenta(true, "Anulado " + v.getFolio() + ". Stock devuelto.", v, productosStockBajo());
    }

    /** F8: folio consecutivo por día T-YYYYMMDD-#### (SAT-friendly). */
    public synchronized String siguienteFolio() {
        try {
            String hoy = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            java.nio.file.Path p = java.nio.file.Paths.get("data/folio-dia.txt");
            int seq = 1;
            if (java.nio.file.Files.exists(p)) {
                String[] v = String.join("", java.nio.file.Files.readAllLines(p)).split(";");
                if (v.length == 2 && v[0].equals(hoy)) seq = Integer.parseInt(v[1]) + 1;
            }
            java.nio.file.Files.createDirectories(p.getParent());
            java.nio.file.Files.writeString(p, hoy + ";" + seq);
            return "T-" + hoy + "-" + String.format("%04d", seq);
        } catch (Exception e) {
            return nuevoId("T");
        }
    }

    /** F2: entrada de proveedor (sube stock + kardex). */
    public synchronized boolean registrarEntrada(String productoId, int cantidad, String motivo) {
        Producto p = buscarProducto(productoId);
        if (p == null || cantidad <= 0) return false;
        int antes = p.getStock();
        p.setStock(antes + cantidad);
        registrarMovimiento(productoId, MovimientoInventario.Tipo.ENTRADA, cantidad, antes, p.getStock(),
                motivo == null ? "Entrada proveedor" : motivo);
        guardarTodo();
        Bitacora.registrar("Entrada " + cantidad + " x " + p.getNombre() + " (" + motivo + ")");
        Auditoria.registrar("caja", "INVENTARIO", "ENTRADA", cantidad + " x " + p.getNombre());
        return true;
    }

    // ===== F3: fiado + turnos + compras =====
    public synchronized ResultadoVenta vender(String vendedor, List<Venta.Item> solicitud, String tipoPago, String clienteId) {
        if ("CREDITO".equals(tipoPago)) {
            Cliente c = buscarCliente(clienteId);
            if (c == null) return new ResultadoVenta(false, "Elige un cliente para fiado.", null, productosStockBajo());
        }
        ResultadoVenta r = vender(vendedor, solicitud);
        if (r.ok && "CREDITO".equals(tipoPago)) {
            r.venta.setTipoPago("CREDITO");
            r.venta.setClienteId(clienteId);
            Cliente c = buscarCliente(clienteId);
            c.setDeuda(c.getDeuda() + r.venta.getTotal());
            guardarTodo();
            Bitacora.registrar("Fiado " + r.venta.getId() + " a " + c.getNombre() + " $" + String.format("%.2f", r.venta.getTotal()));
        }
        return r;
    }

    public Cliente buscarCliente(String id) {
        for (Cliente c : clientes) if (c.getId().equals(id)) return c;
        return null;
    }

    public synchronized boolean abonarDeuda(String clienteId, double monto) {
        Cliente c = buscarCliente(clienteId);
        if (c == null || monto <= 0 || monto > c.getDeuda()) return false;
        c.setDeuda(c.getDeuda() - monto);
        guardarTodo();
        Bitacora.registrar("Abono $" + String.format("%.2f", monto) + " de " + c.getNombre());
        Auditoria.registrar("caja", "FIADO", "ABONO", c.getNombre() + " $" + String.format("%.2f", monto));
        return true;
    }

    public double deudasTotales() {
        double s = 0;
        for (Cliente c : clientes) s += c.getDeuda();
        return s;
    }

    public synchronized TurnoCaja abrirTurno(String vendedor) {
        TurnoCaja abierto = turnoAbierto(vendedor);
        if (abierto != null) return abierto;
        TurnoCaja t = new TurnoCaja(nuevoId("T"), vendedor, ahora());
        turnos.add(t);
        guardarTodo();
        Bitacora.registrar("Turno abierto " + t.getId() + " por " + vendedor);
        Auditoria.registrar(vendedor, "CAJA", "ABRIR_TURNO", t.getId());
        return t;
    }

    public TurnoCaja turnoAbierto(String vendedor) {
        for (int i = turnos.size() - 1; i >= 0; i--) {
            TurnoCaja t = turnos.get(i);
            if (t.getVendedor().equals(vendedor) && t.abierto()) return t;
        }
        return null;
    }

    public synchronized TurnoCaja cerrarTurno(String vendedor) {
        TurnoCaja t = turnoAbierto(vendedor);
        if (t == null) return null;
        double tv = 0, tg = 0;
        for (Venta v : ventas) {
            if (v.isCancelada()) continue;
            if (v.getVendedor().equals(vendedor) && v.getFecha().compareTo(t.getApertura()) >= 0) {
                tv += v.getTotal(); tg += v.getGanancia();
            }
        }
        t.setTotalVentas(tv);
        t.setTotalGanancia(tg);
        t.setCierre(ahora());
        guardarTodo();
        Bitacora.registrar("Turno cerrado " + t.getId() + " ventas $" + String.format("%.2f", tv));
        Auditoria.registrar(vendedor, "CAJA", "CERRAR_TURNO", t.getId() + " $" + String.format("%.2f", tv));
        return t;
    }

    /** F3: factura proveedor con vencimiento; al recibir sube stock + kardex. */
    public synchronized CompraProveedor registrarCompra(String proveedorId, List<CompraProveedor.ItemC> items, String vencimiento) {
        CompraProveedor c = new CompraProveedor(nuevoId("C"), proveedorId, ahora(), vencimiento);
        for (CompraProveedor.ItemC it : items) {
            Producto p = buscarProducto(it.productoId);
            if (p == null || it.cantidad <= 0) continue;
            int antes = p.getStock();
            p.setStock(antes + it.cantidad);
            if (it.costoUnit > 0) p.setCostoProveedor(it.costoUnit); // actualiza costo real
            c.agregar(new CompraProveedor.ItemC(it.productoId, it.cantidad, it.costoUnit));
            registrarMovimiento(p.getId(), MovimientoInventario.Tipo.ENTRADA, it.cantidad, antes, p.getStock(), "Compra " + c.getId());
        }
        compras.add(c);
        guardarTodo();
        Bitacora.registrar("Compra " + c.getId() + " a " + proveedorId + " $" + String.format("%.2f", c.getTotal()) + " vence " + vencimiento);
        Auditoria.registrar("caja", "COMPRAS", "FACTURA", c.getId() + " $" + String.format("%.2f", c.getTotal()));
        return c;
    }

    // ===== consultas para dashboard (F9 excluye CANCELADA) =====
    public double ventasHoy() {
        String hoy = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        double s = 0;
        for (Venta v : ventas) if (!v.isCancelada() && v.getFecha().startsWith(hoy)) s += v.getTotal();
        return s;
    }

    public double[] ventasUltimos7Dias() {
        double[] arr = new double[7];
        for (int i = 0; i < 7; i++) {
            LocalDateTime d = LocalDateTime.now().minusDays(6 - i);
            String dia = d.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            double s = 0;
            for (Venta v : ventas) if (!v.isCancelada() && v.getFecha().startsWith(dia)) s += v.getTotal();
            arr[i] = s;
        }
        return arr;
    }

    public String[] etiquetasUltimos7Dias() {
        String[] e = new String[7];
        for (int i = 0; i < 7; i++) {
            e[i] = LocalDateTime.now().minusDays(6 - i).format(DateTimeFormatter.ofPattern("dd/MM"));
        }
        return e;
    }

    public long stockBajoCount() {
        return productos.stream().filter(Producto::stockBajo).count();
    }

    public double valorInventario() {
        double s = 0;
        for (Producto p : productos) s += p.getValorTotal();
        return s;
    }

    /** Valor a costo (lo invertido) vs a venta (lo potencial). */
    public double valorInventarioCosto() {
        double s = 0;
        for (Producto p : productos) s += p.getValorCosto();
        return s;
    }

    /** Ganancia real del día (venta - costo, sin canceladas). */
    public double gananciaHoy() {
        String hoy = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        double g = 0;
        for (Venta v : ventas) if (!v.isCancelada() && v.getFecha().startsWith(hoy)) g += v.getGanancia();
        return g;
    }

    /** F6: reporte ganancia por producto (unidades, venta, ganancia). Ordenado por ganancia. */
    public List<String[]> gananciaPorProducto() {
        Map<String, double[]> m = new LinkedHashMap<>(); // id -> [uds, venta, ganancia]
        Map<String, String> nombres = new LinkedHashMap<>();
        for (Venta v : ventas) {
            if (v.isCancelada()) continue;
            for (Venta.Item it : v.getItems()) {
                double[] a = m.computeIfAbsent(it.productoId, k -> new double[3]);
                a[0] += it.cantidad; a[1] += it.subtotal(); a[2] += it.ganancia();
                nombres.putIfAbsent(it.productoId, it.nombre);
            }
        }
        List<String[]> r = new ArrayList<>();
        for (Map.Entry<String, double[]> e : m.entrySet()) {
            double[] a = e.getValue();
            r.add(new String[]{e.getKey(), nombres.get(e.getKey()),
                    String.valueOf((int) a[0]), String.format("%.2f", a[1]), String.format("%.2f", a[2])});
        }
        r.sort((x, y) -> Double.compare(Double.parseDouble(y[4]), Double.parseDouble(x[4])));
        return r;
    }

    /** F14: reporte del día consolidado (multi-caja tras sync). */
    public String reporteDiaTicket() {
        String hoy = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        double tv = 0, tg = 0;
        int n = 0, anul = 0;
        Map<String, double[]> porCaja = new LinkedHashMap<>(); // vendedor -> [ventas, ganancia, n]
        Map<String, int[]> top = new LinkedHashMap<>(); // producto -> [uds]
        Map<String, String> nombres = new LinkedHashMap<>();
        for (Venta v : ventas) {
            if (!v.getFecha().startsWith(hoy)) continue;
            if (v.isCancelada()) { anul++; continue; }
            n++; tv += v.getTotal(); tg += v.getGanancia();
            double[] a = porCaja.computeIfAbsent(v.getVendedor(), k -> new double[3]);
            a[0] += v.getTotal(); a[1] += v.getGanancia(); a[2]++;
            for (Venta.Item it : v.getItems()) {
                int[] u = top.computeIfAbsent(it.productoId, k -> new int[1]);
                u[0] += it.cantidad;
                nombres.putIfAbsent(it.productoId, it.nombre);
            }
        }
        StringBuilder sb = new StringBuilder("📊 REPORTE " + hoy + "\n--------------------------\n");
        sb.append("Ventas: ").append(n).append("  Anuladas: ").append(anul).append("\n");
        sb.append("TOTAL: $").append(String.format("%.2f", tv)).append("\n");
        sb.append("GANANCIA: $").append(String.format("%.2f", tg)).append("\n");
        sb.append("--------------------------\nPOR CAJA:\n");
        if (porCaja.isEmpty()) sb.append("(sin ventas)\n");
        for (Map.Entry<String, double[]> e : porCaja.entrySet()) {
            sb.append(String.format("%s: %d x $%.2f / g $%.2f\n", e.getKey(),
                    (int) e.getValue()[2], e.getValue()[0], e.getValue()[1]));
        }
        sb.append("--------------------------\nTOP (uds):\n");
        top.entrySet().stream().sorted((a, b) -> Integer.compare(b.getValue()[0], a.getValue()[0])).limit(5)
                .forEach(e -> sb.append(String.format("%s x%d\n", nombres.get(e.getKey()), e.getValue()[0])));
        sb.append("--------------------------\nDeuda total: $").append(String.format("%.2f", deudasTotales()));
        return sb.toString();
    }

    /** F15: mismo reporte en JSON para la APK (deudas solo si jefe). */
    public String reporteDiaJSON(boolean jefe) {
        String hoy = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        double tv = 0, tg = 0;
        int n = 0, anul = 0;
        Map<String, double[]> porCaja = new LinkedHashMap<>();
        Map<String, int[]> top = new LinkedHashMap<>();
        Map<String, String> nombres = new LinkedHashMap<>();
        for (Venta v : ventas) {
            if (!v.getFecha().startsWith(hoy)) continue;
            if (v.isCancelada()) { anul++; continue; }
            n++; tv += v.getTotal(); tg += v.getGanancia();
            double[] a = porCaja.computeIfAbsent(v.getVendedor(), k -> new double[3]);
            a[0] += v.getTotal(); a[1] += v.getGanancia(); a[2]++;
            for (Venta.Item it : v.getItems()) {
                int[] u = top.computeIfAbsent(it.productoId, k -> new int[1]);
                u[0] += it.cantidad;
                nombres.putIfAbsent(it.productoId, it.nombre);
            }
        }
        StringBuilder sb = new StringBuilder("{\"fecha\":\"" + hoy + "\",\"ventas\":" + n
                + ",\"anuladas\":" + anul + ",\"total\":" + String.format(Locale.ROOT, "%.2f", tv)
                + ",\"ganancia\":" + String.format(Locale.ROOT, "%.2f", jefe ? tg : 0)
                + ",\"deudas\":" + String.format(Locale.ROOT, "%.2f", jefe ? deudasTotales() : 0)
                + ",\"porCaja\":[");
        boolean p = true;
        for (Map.Entry<String, double[]> e : porCaja.entrySet()) {
            if (!p) sb.append(",");
            p = false;
            sb.append("{\"vendedor\":\"").append(e.getKey()).append("\",\"n\":").append((int) e.getValue()[2])
                    .append(",\"total\":").append(String.format(Locale.ROOT, "%.2f", e.getValue()[0]))
                    .append(",\"ganancia\":").append(String.format(Locale.ROOT, "%.2f", jefe ? e.getValue()[1] : 0)).append("}");
        }
        sb.append("],\"top\":[");
        boolean q = true;
        List<Map.Entry<String, int[]>> tops = new ArrayList<>(top.entrySet());
        tops.sort((a, b) -> Integer.compare(b.getValue()[0], a.getValue()[0]));
        for (int i = 0; i < Math.min(5, tops.size()); i++) {
            if (!q) sb.append(",");
            q = false;
            sb.append("{\"id\":\"").append(tops.get(i).getKey()).append("\",\"nombre\":\"")
                    .append(nombres.get(tops.get(i).getKey()).replace("\"", "'"))
                    .append("\",\"uds\":").append(tops.get(i).getValue()[0]).append("}");
        }
        return sb.append("]}").toString();
    }

    public static String nuevoId(String prefijo) {
        return prefijo + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    public static String ahora() {
        return LocalDateTime.now().format(FMT);
    }
}
