package vista;

import datos.GestorDatos;
import dominio.Cliente;
import dominio.Producto;
import dominio.TurnoCaja;
import dominio.Usuario;
import dominio.Venta;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Punto de venta F3: turno + contado/fi sílabas + historial. */
public class VentasPanel extends JPanel {
    private final Usuario vendedor;
    private final JComboBox<Producto> cmbProd = new JComboBox<>();
    private final JSpinner spCant = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
    private final JComboBox<String> cmbPago = new JComboBox<>(new String[]{"CONTADO", "CREDITO (fiado)"});
    private final JComboBox<Cliente> cmbCliente = new JComboBox<>();
    private final JLabel lblTurno = new JLabel();
    private final DefaultTableModel modeloCarrito = new DefaultTableModel(
            new String[]{"Producto", "Cant.", "Precio", "Subtotal"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final DefaultTableModel modeloHist = new DefaultTableModel(
            new String[]{"Folio", "Fecha", "Vendedor", "Total", "Estado"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private JTable tHist;
    private final JLabel lblTotal = new JLabel("$0.00");
    private final List<Venta.Item> carrito = new ArrayList<>();

    public VentasPanel(Usuario vendedor) {
        this.vendedor = vendedor;
        setBackground(Tema.FONDO);
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        // arriba: selector
        JPanel sel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        sel.setBackground(Tema.TARJETA);
        sel.setBorder(new Tema.RoundedBorder(Tema.BORDE, 12, 1));
        cmbProd.setBackground(Tema.PANEL);
        cmbProd.setForeground(Tema.TEXTO);
        cmbProd.setPreferredSize(new Dimension(300, 36));
        spCant.setPreferredSize(new Dimension(70, 36));
        JButton bAdd = new JButton("＋ Agregar");
        Tema.botonPrimario(bAdd);
        bAdd.setPreferredSize(new Dimension(130, 38));
        bAdd.addActionListener(e -> agregarAlCarrito());
        sel.add(new JLabel("Producto:") {{ setForeground(Tema.TEXTO); }});
        sel.add(cmbProd);
        sel.add(new JLabel("Cant:") {{ setForeground(Tema.TEXTO); }});
        sel.add(spCant);
        sel.add(bAdd);
        // F3: pago + cliente fiado + turno en segunda fila
        JPanel norte = new JPanel(new GridLayout(2, 1, 0, 8));
        norte.setOpaque(false);
        norte.add(sel);
        JPanel turnoBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        turnoBar.setBackground(Tema.TARJETA);
        turnoBar.setBorder(new Tema.RoundedBorder(Tema.BORDE, 12, 1));
        lblTurno.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTurno.setForeground(Tema.TEXTO_SEC);
        cmbPago.setBackground(Tema.PANEL); cmbPago.setForeground(Tema.TEXTO);
        cmbPago.setToolTipText("CONTADO o fiado a cliente");
        cmbPago.addActionListener(e -> cmbCliente.setEnabled(cmbPago.getSelectedIndex() == 1));
        cmbCliente.setBackground(Tema.PANEL); cmbCliente.setForeground(Tema.TEXTO);
        cmbCliente.setEnabled(false);
        JButton bTurno = new JButton("Abrir/Cerrar turno");
        JButton bFiado = new JButton("Clientes/Fiado");
        Tema.botonSecundario(bTurno); Tema.botonSecundario(bFiado);
        bTurno.addActionListener(e -> toggleTurno());
        bFiado.addActionListener(e -> dialogoFiado());
        turnoBar.add(lblTurno);
        turnoBar.add(new JLabel("Pago:") {{ setForeground(Tema.TEXTO_SEC); }});
        turnoBar.add(cmbPago); turnoBar.add(cmbCliente);
        turnoBar.add(bTurno); turnoBar.add(bFiado);
        norte.add(turnoBar);
        add(norte, BorderLayout.NORTH);

        // centro dividido
        JPanel centro = new JPanel(new GridLayout(1, 2, 12, 12));
        centro.setOpaque(false);

        JPanel cCard = new JPanel(new BorderLayout(0, 8));
        cCard.setBackground(Tema.TARJETA);
        cCard.setBorder(BorderFactory.createCompoundBorder(
                new Tema.RoundedBorder(Tema.BORDE, 12, 1),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        JLabel t1 = new JLabel("Carrito");
        t1.setFont(Tema.ETIQ); t1.setForeground(Tema.TEXTO);
        cCard.add(t1, BorderLayout.NORTH);
        JTable tCarrito = new JTable(modeloCarrito);
        Tema.tabla(tCarrito);
        JScrollPane sp1 = new JScrollPane(tCarrito);
        sp1.getViewport().setBackground(Tema.TARJETA);
        sp1.setBorder(BorderFactory.createEmptyBorder());
        cCard.add(sp1, BorderLayout.CENTER);

        JPanel totalBar = new JPanel(new BorderLayout());
        totalBar.setOpaque(false);
        lblTotal.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTotal.setForeground(Tema.VERDE);
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setOpaque(false);
        JButton bQuitar = new JButton("Quitar");
        JButton bLimpiar = new JButton("Limpiar");
        JButton bCobrar = new JButton("💰 Cobrar");
        Tema.botonSecundario(bQuitar); Tema.botonSecundario(bLimpiar); Tema.botonPrimario(bCobrar);
        bQuitar.addActionListener(e -> {
            int r = tCarrito.getSelectedRow();
            if (r >= 0) { carrito.remove(r); pintarCarrito(); }
        });
        bLimpiar.addActionListener(e -> { carrito.clear(); pintarCarrito(); });
        bCobrar.addActionListener(e -> cobrar());
        btns.add(bQuitar); btns.add(bLimpiar); btns.add(bCobrar);
        totalBar.add(new JLabel("Total:") {{ setForeground(Tema.TEXTO_SEC); setFont(Tema.ETIQ); }}, BorderLayout.WEST);
        totalBar.add(lblTotal, BorderLayout.CENTER);
        totalBar.add(btns, BorderLayout.SOUTH);
        cCard.add(totalBar, BorderLayout.SOUTH);
        centro.add(cCard);

        JPanel hCard = new JPanel(new BorderLayout(0, 8));
        hCard.setBackground(Tema.TARJETA);
        hCard.setBorder(BorderFactory.createCompoundBorder(
                new Tema.RoundedBorder(Tema.BORDE, 12, 1),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        JLabel t2 = new JLabel("Historial de ventas");
        t2.setFont(Tema.ETIQ); t2.setForeground(Tema.TEXTO);
        hCard.add(t2, BorderLayout.NORTH);
        tHist = new JTable(modeloHist);
        Tema.tabla(tHist);
        JScrollPane sp2 = new JScrollPane(tHist);
        sp2.getViewport().setBackground(Tema.TARJETA);
        sp2.setBorder(BorderFactory.createEmptyBorder());
        hCard.add(sp2, BorderLayout.CENTER);
        JButton bAnular = new JButton("✖ Anular folio");
        Tema.botonPeligro(bAnular);
        bAnular.setToolTipText("Anula el folio seleccionado: devuelve stock y revierte fiado");
        bAnular.addActionListener(e -> anularSeleccionada());
        JPanel hSur = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        hSur.setOpaque(false);
        hSur.add(bAnular);
        hCard.add(hSur, BorderLayout.SOUTH);
        centro.add(hCard);

        add(centro, BorderLayout.CENTER);
    }

    private void agregarAlCarrito() {
        Producto p = (Producto) cmbProd.getSelectedItem();
        if (p == null) return;
        int cant = (Integer) spCant.getValue();
        if (cant > p.getStock()) {
            Toast.error(this, "Stock insuficiente. Disponible: " + p.getStock());
            return;
        }
        // si ya está en carrito, suma cantidad
        for (Venta.Item it : carrito) {
            if (it.productoId.equals(p.getId())) {
                if (it.cantidad + cant > p.getStock()) {
                    Toast.error(this, "Supera el stock disponible (" + p.getStock() + ").");
                    return;
                }
                it.cantidad += cant;
                pintarCarrito();
                Toast.info(this, "Cantidad actualizada: " + it.nombre);
                return;
            }
        }
        carrito.add(new Venta.Item(p.getId(), p.getNombre(), cant, p.getPrecioVenta(), p.getCostoProveedor()));
        pintarCarrito();
        Toast.exito(this, "Agregado: " + p.getNombre() + " x" + cant);
    }

    private void pintarCarrito() {
        modeloCarrito.setRowCount(0);
        double total = 0;
        for (Venta.Item it : carrito) {
            modeloCarrito.addRow(new Object[]{it.nombre, it.cantidad,
                    String.format("%.2f", it.precioUnit), String.format("%.2f", it.subtotal())});
            total += it.subtotal();
        }
        lblTotal.setText(String.format("$%.2f", total));
    }

    private void cobrar() {
        if (carrito.isEmpty()) {
            Toast.info(this, "El carrito está vacío. Agrega productos primero.");
            return;
        }
        // F3: exige turno abierto (caja por cajero)
        if (GestorDatos.getInstancia().turnoAbierto(vendedor.getUsername()) == null) {
            Toast.error(this, "Abre tu turno primero (botón Abrir/Cerrar turno).");
            return;
        }
        boolean credito = cmbPago.getSelectedIndex() == 1;
        Cliente cli = (Cliente) cmbCliente.getSelectedItem();
        if (credito && cli == null) {
            Toast.error(this, "Elige un cliente para fiado o crea uno en Clientes/Fiado.");
            return;
        }
        // F2+F3: una sola llamada atómica (valida + descuenta + kardex + fiado + alertas)
        GestorDatos.ResultadoVenta r = GestorDatos.getInstancia().vender(
                vendedor.getUsername(), new ArrayList<>(carrito),
                credito ? "CREDITO" : "CONTADO", credito ? cli.getId() : "");
        if (!r.ok) {
            Toast.error(this, r.mensaje);
            refrescar(); // stock pudo cambiar en otra caja
            return;
        }
        Venta v = r.venta;
        double iva = v.getTotal() - v.getTotal() / 1.16;
        StringBuilder ticket = new StringBuilder("🧾 FOLIO " + v.getFolio() + "\nFecha: " + v.getFecha()
                + "\nVendedor: " + v.getVendedor() + "\n--------------------------\n");
        for (Venta.Item it : v.getItems()) {
            ticket.append(String.format("%s x%d  $%.2f\n", it.nombre, it.cantidad, it.subtotal()));
        }
        ticket.append("--------------------------\nSUBTOTAL: $").append(String.format("%.2f", v.getTotal() - iva));
        ticket.append("\nIVA (16% incl.): $").append(String.format("%.2f", iva));
        ticket.append("\nTOTAL: $").append(String.format("%.2f", v.getTotal()));
        if (servicio.Sesion.esJefe()) ticket.append("\nGanancia: $").append(String.format("%.2f", v.getGanancia()));
        if (v.esCredito()) ticket.append("\nFIADO a: ").append(v.getClienteId());
        TicketPrinter.mostrarTicket(this, "Venta exitosa", ticket.toString());
        Toast.exito(this, "Venta " + v.getId() + " cobrada: $" + String.format("%.2f", v.getTotal()));
        if (r.alertas != null && !r.alertas.isEmpty()) {
            StringBuilder n = new StringBuilder("⚠ Stock bajo: ");
            for (int i = 0; i < Math.min(3, r.alertas.size()); i++) {
                if (i > 0) n.append(", ");
                n.append(r.alertas.get(i).getNombre()).append(" (").append(r.alertas.get(i).getStock()).append(")");
            }
            if (r.alertas.size() > 3) n.append(" +").append(r.alertas.size() - 3).append(" más");
            Toast.error(this, n.toString());
        }
        carrito.clear();
        pintarCarrito();
        refrescar();
    }

    private void anularSeleccionada() {
        int r = tHist.getSelectedRow();
        if (r < 0) { Toast.info(this, "Selecciona un folio del historial."); return; }
        String folio = modeloHist.getValueAt(tHist.convertRowIndexToModel(r), 0).toString();
        String motivo = JOptionPane.showInputDialog(this, "Motivo de anulación de " + folio + ":");
        if (motivo == null) return;
        GestorDatos.ResultadoVenta res = GestorDatos.getInstancia().anularVenta(vendedor.getUsername(), folio, motivo);
        if (!res.ok) { Toast.error(this, res.mensaje); return; }
        JOptionPane.showMessageDialog(this, "🧾 " + res.venta.getFolio() + " CANCELADO\nMotivo: " + motivo
                + "\nStock devuelto. Fiado revertido si aplicaba.", "Anulación", JOptionPane.WARNING_MESSAGE);
        Toast.exito(this, res.mensaje);
        refrescar();
    }

    private void toggleTurno() {
        GestorDatos g = GestorDatos.getInstancia();
        TurnoCaja abierto = g.turnoAbierto(vendedor.getUsername());
        if (abierto == null) {
            g.abrirTurno(vendedor.getUsername());
            Toast.exito(this, "Turno abierto. A vender.");
        } else {
            TurnoCaja c = g.cerrarTurno(vendedor.getUsername());
            JOptionPane.showMessageDialog(this,
                    "Turno " + c.getId() + " cerrado.\nVentas: $" + String.format("%.2f", c.getTotalVentas())
                    + "\nGanancia: $" + String.format("%.2f", c.getTotalGanancia()),
                    "Cierre de caja", JOptionPane.INFORMATION_MESSAGE);
            Toast.exito(this, "Turno cerrado.");
        }
        refrescar();
    }

    private void dialogoFiado() {
        GestorDatos g = GestorDatos.getInstancia();
        StringBuilder sb = new StringBuilder("Deudas:\n");
        for (Cliente c : g.getClientes()) sb.append("- ").append(c.getNombre()).append(": $").append(String.format("%.2f", c.getDeuda())).append("\n");
        sb.append("\nDeuda total: $").append(String.format("%.2f", g.deudasTotales()));
        String[] ops = {"Nuevo cliente", "Abonar", "Cerrar"};
        int op = JOptionPane.showOptionDialog(this, sb.toString(), "Fiado",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, ops, ops[2]);
        if (op == 0) {
            JTextField n = new JTextField(); Tema.campo(n);
            if (JOptionPane.showConfirmDialog(this, n, "Nombre cliente", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
            if (n.getText().trim().isEmpty()) return;
            g.getClientes().add(new dominio.Cliente(GestorDatos.nuevoId("C"), n.getText().trim(), ""));
            g.guardarTodo();
            refrescar();
        } else if (op == 1) {
            Cliente cli = (Cliente) JOptionPane.showInputDialog(this, "Cliente:", "Abonar",
                    JOptionPane.PLAIN_MESSAGE, null, g.getClientes().toArray(), null);
            if (cli == null || cli.getDeuda() <= 0) return;
            String m = JOptionPane.showInputDialog(this, "Monto (debe $" + String.format("%.2f", cli.getDeuda()) + "):");
            try {
                if (g.abonarDeuda(cli.getId(), Double.parseDouble(m))) { Toast.exito(this, "Abono registrado."); refrescar(); }
                else Toast.error(this, "Monto inválido.");
            } catch (Exception ex) { Toast.error(this, "Monto inválido."); }
        }
    }

    public void refrescar() {
        cmbProd.removeAllItems();
        for (Producto p : GestorDatos.getInstancia().getProductos()) {
            if (p.getStock() > 0) cmbProd.addItem(p);
        }
        cmbCliente.removeAllItems();
        for (Cliente c : GestorDatos.getInstancia().getClientes()) cmbCliente.addItem(c);
        TurnoCaja t = GestorDatos.getInstancia().turnoAbierto(vendedor.getUsername());
        lblTurno.setText(t == null ? "🔴 Turno cerrado — ábrelo para vender"
                : "🟢 Turno " + t.getId() + " desde " + t.getApertura());
        lblTurno.setForeground(t == null ? Tema.ROJO : Tema.VERDE);
        modeloHist.setRowCount(0);
        List<Venta> vs = GestorDatos.getInstancia().getVentas();
        for (int i = vs.size() - 1; i >= 0; i--) {
            Venta v = vs.get(i);
            modeloHist.addRow(new Object[]{v.getFolio(), v.getFecha(), v.getVendedor(),
                    String.format("$%.2f", v.getTotal()), v.isCancelada() ? "CANCELADA" : v.getTipoPago()});
        }
    }
}
