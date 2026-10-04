package vista;

import datos.GestorDatos;
import dominio.Producto;
import servicio.Bitacora;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;

/** CRUD de productos con buscador y filtro. */
public class ProductosPanel extends JPanel {
    private final DefaultTableModel modelo = new DefaultTableModel(
            new String[]{"ID", "Nombre", "Categoría", "Precio", "Stock", "Total", "Vence"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tabla = new JTable(modelo);
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(modelo);
    private final JTextField txtBuscar = new JTextField();
    private final JComboBox<String> cmbCat = new JComboBox<>(new String[]{"Todas", "Electrónica", "Oficina", "Papelería", "Abarrotes"});
    private final CardLayout centroCards = new CardLayout();
    private final JPanel centro = new JPanel(centroCards);
    private final JScrollPane scrollTabla;
    private final JLabel lblConteo = new JLabel();
    private final Timer debounce = new Timer(180, null);

    public ProductosPanel() {
        setBackground(Tema.FONDO);
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        // barra superior
        JPanel bar = new JPanel(new BorderLayout(10, 10));
        bar.setOpaque(false);
        Tema.campo(txtBuscar);
        txtBuscar.setPreferredSize(new Dimension(220, 38));
        txtBuscar.setToolTipText("Filtra por nombre. Se aplica solo al escribir.");
        txtBuscar.putClientProperty("JTextField.placeholderText", "Buscar…");
        // Debounce: filtra 180ms después de dejar de escribir (rinde con 10k filas)
        debounce.setRepeats(false);
        debounce.addActionListener(e -> filtrar());
        txtBuscar.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { debounce.restart(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { debounce.restart(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { debounce.restart(); }
        });
        cmbCat.setBackground(Tema.TARJETA);
        cmbCat.setForeground(Tema.TEXTO);
        cmbCat.setToolTipText("Filtrar por categoría");
        cmbCat.addActionListener(e -> filtrar());

        JPanel izq = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        izq.setOpaque(false);
        JLabel iconoBuscar = new JLabel("🔍");
        iconoBuscar.setForeground(Tema.TEXTO);
        izq.add(iconoBuscar);
        izq.add(txtBuscar);
        izq.add(Box.createHorizontalStrut(10));
        izq.add(cmbCat);
        bar.add(izq, BorderLayout.WEST);

        JPanel der = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        der.setOpaque(false);
        JButton bAdd = new JButton("＋ Agregar");
        JButton bEdit = new JButton("✎ Editar");
        JButton bDel = new JButton("🗑 Eliminar");
        JButton bEnt = new JButton("📥 Entrada");
        JButton bKar = new JButton("📜 Kardex");
        Tema.botonPrimario(bAdd); Tema.botonSecundario(bEdit); Tema.botonPeligro(bDel); Tema.botonSecundario(bEnt); Tema.botonSecundario(bKar);
        bAdd.setToolTipText("Agregar producto (valida precio y stock)");
        bEdit.setToolTipText("Editar el producto seleccionado");
        bDel.setToolTipText("Eliminar el producto seleccionado");
        bEnt.setToolTipText("Factura proveedor: entra stock + vencimiento");
        bKar.setToolTipText("Ver entradas/salidas del producto seleccionado");
        bAdd.setMnemonic('A'); bEdit.setMnemonic('E');
        bAdd.setPreferredSize(new Dimension(130, 40));
        bEdit.setPreferredSize(new Dimension(120, 40));
        bDel.setPreferredSize(new Dimension(130, 40));
        bEnt.setPreferredSize(new Dimension(120, 40));
        bKar.setPreferredSize(new Dimension(120, 40));
        bAdd.addActionListener(e -> dialogo(null));
        bEdit.addActionListener(e -> editarSeleccionado());
        bDel.addActionListener(e -> eliminarSeleccionado());
        bEnt.addActionListener(e -> dialogoEntrada());
        bKar.addActionListener(e -> dialogoKardex());
        der.add(bAdd); der.add(bEdit); der.add(bDel); der.add(bEnt); der.add(bKar);
        bar.add(der, BorderLayout.EAST);
        add(bar, BorderLayout.NORTH);

        // tabla + estado vacío ilustrado (CardLayout: evita tabla fría sin datos)
        Tema.tabla(tabla);
        tabla.setRowSorter(sorter);
        // pinta stock bajo en rojo + vencido/próximo en columna Vence
        tabla.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                int modelRow = t.convertRowIndexToModel(r);
                int stock = Integer.parseInt(modelo.getValueAt(modelRow, 4).toString());
                comp.setForeground(stock < 5 ? Tema.ROJO : Tema.TEXTO);
                comp.setBackground(sel ? new Color(59, 130, 255, 70) : Tema.TARJETA);
                setHorizontalAlignment(CENTER);
                return comp;
            }
        });
        tabla.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                String f = v == null ? "" : v.toString();
                Color fg = Tema.TEXTO_SEC;
                if (!f.isBlank()) {
                    try {
                        long d = java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(),
                                java.time.LocalDate.parse(f.trim()));
                        if (d < 0) fg = Tema.ROJO;
                        else if (d <= 30) fg = Tema.AMARILLO;
                    } catch (Exception ignored) {}
                }
                comp.setForeground(fg);
                comp.setBackground(sel ? new Color(59, 130, 255, 70) : Tema.TARJETA);
                setHorizontalAlignment(CENTER);
                return comp;
            }
        });
        scrollTabla = new JScrollPane(tabla);
        Tema.scroll(scrollTabla);
        EmptyState vacio = new EmptyState(Ilustracion.Tipo.EMPTY_PRODUCTOS,
                "Sin productos aquí",
                "Agrega tu primer producto o ajusta la búsqueda.",
                "＋ Agregar producto", () -> dialogo(null));
        vacio.setBorder(BorderFactory.createCompoundBorder(
                new Tema.RoundedBorder(Tema.BORDE, 12, 1),
                BorderFactory.createEmptyBorder(24, 24, 24, 24)));
        centro.setOpaque(false);
        centro.add(scrollTabla, "TABLA");
        centro.add(vacio, "VACIO");
        add(centro, BorderLayout.CENTER);

        lblConteo.setFont(Tema.fuente(Font.PLAIN, 11));
        lblConteo.setForeground(Tema.TEXTO_SEC);
        add(lblConteo, BorderLayout.SOUTH);
    }

    private void filtrar() {
        String texto = "(?i)" + txtBuscar.getText().trim().replaceAll("([\\\\.*+\\[\\](){}|^$])", "\\\\$1");
        String cat = (String) cmbCat.getSelectedItem();
        RowFilter<DefaultTableModel, Object> rfTexto = texto.isEmpty() || texto.equals("(?i)")
                ? null : RowFilter.regexFilter(texto, 1);
        RowFilter<DefaultTableModel, Object> rfCat = "Todas".equals(cat)
                ? null : RowFilter.regexFilter("^" + cat + "$", 2);
        if (rfTexto == null && rfCat == null) sorter.setRowFilter(null);
        else if (rfTexto == null) sorter.setRowFilter(rfCat);
        else if (rfCat == null) sorter.setRowFilter(rfTexto);
        else sorter.setRowFilter(RowFilter.andFilter(java.util.List.of(rfTexto, rfCat)));
        actualizarCentro();
    }

    private void actualizarCentro() {
        boolean hay = sorter.getViewRowCount() > 0;
        centroCards.show(centro, hay ? "TABLA" : "VACIO");
        long venc = GestorDatos.getInstancia().proximosAVencer(30).size();
        lblConteo.setText(sorter.getViewRowCount() + " de " + modelo.getRowCount() + " productos"
                + (GestorDatos.getInstancia().stockBajoCount() > 0
                        ? "  •  ⚠ " + GestorDatos.getInstancia().stockBajoCount() + " con stock bajo" : "")
                + (venc > 0 ? "  •  ⏳ " + venc + " vencen ≤30d" : ""));
    }

    public void refrescar() {
        modelo.setRowCount(0);
        for (Producto p : GestorDatos.getInstancia().getProductos()) {
            modelo.addRow(new Object[]{p.getId(), p.getNombre(), p.getCategoria(),
                    String.format("%.2f", p.getPrecio()), p.getStock(),
                    String.format("%.2f", p.getValorTotal()), p.getFechaVence()});
        }
        filtrar(); // reaplica filtro + empty state + conteo
    }

    /** F11: kardex del producto seleccionado. */
    private void dialogoKardex() {
        int view = tabla.getSelectedRow();
        if (view < 0) { Toast.info(this, "Selecciona un producto para ver su kardex."); return; }
        String id = modelo.getValueAt(tabla.convertRowIndexToModel(view), 0).toString();
        var movs = GestorDatos.getInstancia().movimientosDe(id);
        if (movs.isEmpty()) { Toast.info(this, "Sin movimientos para " + id + "."); return; }
        String[] cols = {"Fecha", "Tipo", "Cant", "Antes", "Después", "Motivo"};
        Object[][] data = new Object[movs.size()][6];
        for (int i = 0; i < movs.size(); i++) {
            var m = movs.get(i);
            data[i] = new Object[]{m.getFecha(), m.getTipo(), m.getCantidad(), m.getStockAntes(), m.getStockDespues(), m.getMotivo()};
        }
        JTable t = new JTable(data, cols);
        Tema.tabla(t);
        JScrollPane sp = new JScrollPane(t);
        Tema.scroll(sp);
        sp.setPreferredSize(new Dimension(560, 280));
        JOptionPane.showMessageDialog(this, sp, "📜 Kardex " + id, JOptionPane.PLAIN_MESSAGE);
    }

    private void editarSeleccionado() {
        int view = tabla.getSelectedRow();
        if (view < 0) {
            Toast.info(this, "Selecciona un producto para editar.");
            return;
        }
        String id = modelo.getValueAt(tabla.convertRowIndexToModel(view), 0).toString();
        Producto p = GestorDatos.getInstancia().buscarProducto(id);
        dialogo(p);
    }

    private void eliminarSeleccionado() {
        int view = tabla.getSelectedRow();
        if (view < 0) {
            Toast.info(this, "Selecciona un producto para eliminar.");
            return;
        }
        String id = modelo.getValueAt(tabla.convertRowIndexToModel(view), 0).toString();
        int ok = JOptionPane.showConfirmDialog(this, "¿Eliminar " + id + "?", "Confirmar", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;
        GestorDatos.getInstancia().getProductos().removeIf(x -> x.getId().equals(id));
        GestorDatos.getInstancia().guardarTodo();
        Bitacora.registrar("Producto eliminado: " + id);
        refrescar();
        Toast.exito(this, "Producto eliminado.");
    }

    private void dialogo(Producto edit) {
        boolean jefe = servicio.Sesion.esJefe();
        JTextField nombre = new JTextField(edit == null ? "" : edit.getNombre());
        JTextField categoria = new JTextField(edit == null ? "Electrónica" : edit.getCategoria());
        JTextField precio = new JTextField(edit == null ? "" : String.valueOf(edit.getPrecio()));
        JTextField stock = new JTextField(edit == null ? "" : String.valueOf(edit.getStock()));
        JTextField costo = new JTextField(edit == null ? "" : String.valueOf(edit.getCostoProveedor()));
        JTextField stockMin = new JTextField(edit == null ? "5" : String.valueOf(edit.getStockMin()));
        JTextField vence = new JTextField(edit == null ? "" : edit.getFechaVence());
        for (JTextField f : new JTextField[]{nombre, categoria, precio, stock, costo, stockMin, vence}) Tema.campo(f);
        JPanel f = new JPanel(new GridLayout(jefe ? 14 : 10, 1, 4, 4));
        f.setBackground(Tema.PANEL);
        f.add(lbl("Nombre:")); f.add(nombre);
        f.add(lbl("Categoría:")); f.add(categoria);
        f.add(lbl("Precio venta:")); f.add(precio);
        f.add(lbl("Stock:")); f.add(stock);
        f.add(lbl("Vence yyyy-MM-dd (vacío = no perecedero):")); f.add(vence);
        if (jefe) {
            f.add(lbl("Costo proveedor (solo JEFE):")); f.add(costo);
            f.add(lbl("Stock mínimo alerta:")); f.add(stockMin);
        }
        int ok = JOptionPane.showConfirmDialog(this, f, edit == null ? "Agregar producto" : "Editar producto",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;
        try {
            String n = nombre.getText().trim();
            String c = categoria.getText().trim();
            double pr = Double.parseDouble(precio.getText().trim());
            int st = Integer.parseInt(stock.getText().trim());
            double co = jefe && !costo.getText().trim().isEmpty() ? Double.parseDouble(costo.getText().trim()) : -1;
            int sm = jefe && !stockMin.getText().trim().isEmpty() ? Integer.parseInt(stockMin.getText().trim()) : -1;
            String fv = vence.getText().trim();
            if (!fv.isEmpty()) java.time.LocalDate.parse(fv); // valida formato
            if (n.isEmpty() || c.isEmpty() || pr <= 0 || st < 0) throw new NumberFormatException();
            if (edit == null) {
                double costoF = co < 0 ? pr * 0.7 : co;
                int minF = sm < 0 ? 5 : sm;
                GestorDatos.getInstancia().getProductos().add(
                        new Producto(GestorDatos.nuevoId("P"), n, c, costoF, pr, st, minF, "", "", fv));
                Bitacora.registrar("Producto agregado: " + n);
                Toast.exito(this, "Producto agregado: " + n);
            } else {
                edit.setNombre(n); edit.setCategoria(c); edit.setPrecio(pr); edit.setStock(st);
                edit.setFechaVence(fv);
                if (jefe) {
                    if (co >= 0) edit.setCostoProveedor(co);
                    if (sm >= 0) edit.setStockMin(sm);
                }
                Bitacora.registrar("Producto editado: " + edit.getId());
                Toast.exito(this, "Producto actualizado.");
            }
            GestorDatos.getInstancia().guardarTodo();
            refrescar();
        } catch (Exception ex) {
            Toast.error(this, "Revisa: precio > 0, stock >= 0, vence yyyy-MM-dd o vacío.");
        }
    }

    /** F3: factura proveedor con vencimiento (entra stock + actualiza costo + kardex). Solo JEFE. */
    private void dialogoEntrada() {
        if (!servicio.Sesion.esJefe()) { Toast.error(this, "Solo JEFE registra entradas/facturas."); return; }
        GestorDatos g = GestorDatos.getInstancia();
        JComboBox<dominio.Producto> cmb = new JComboBox<>(g.getProductos().toArray(new dominio.Producto[0]));
        cmb.setBackground(Tema.TARJETA); cmb.setForeground(Tema.TEXTO);
        JTextField cant = new JTextField("10"); Tema.campo(cant);
        JTextField costo = new JTextField(); Tema.campo(costo);
        JComboBox<dominio.Proveedor> prv = new JComboBox<>(g.getProveedores().toArray(new dominio.Proveedor[0]));
        prv.setBackground(Tema.TARJETA); prv.setForeground(Tema.TEXTO);
        JTextField venc = new JTextField("2026-12-31"); Tema.campo(venc);
        JPanel f = new JPanel(new GridLayout(10, 1, 4, 4));
        f.setBackground(Tema.PANEL);
        f.add(lbl("Producto:")); f.add(cmb);
        f.add(lbl("Cantidad que entra:")); f.add(cant);
        f.add(lbl("Costo unitario (vacío = mantener):")); f.add(costo);
        f.add(lbl("Proveedor:")); f.add(prv);
        f.add(lbl("Vencimiento factura (yyyy-MM-dd):")); f.add(venc);
        if (JOptionPane.showConfirmDialog(this, f, "Entrada / factura proveedor",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        try {
            dominio.Producto p = (dominio.Producto) cmb.getSelectedItem();
            int c = Integer.parseInt(cant.getText().trim());
            double co = costo.getText().trim().isEmpty() ? p.getCostoProveedor() : Double.parseDouble(costo.getText().trim());
            if (p == null || c <= 0 || co < 0) throw new NumberFormatException();
            dominio.Proveedor pv = (dominio.Proveedor) prv.getSelectedItem();
            java.util.List<dominio.CompraProveedor.ItemC> items = java.util.List.of(
                    new dominio.CompraProveedor.ItemC(p.getId(), c, co));
            g.registrarCompra(pv == null ? "" : pv.getId(), items, venc.getText().trim());
            refrescar();
            Toast.exito(this, "Entrada registrada: +" + c + " " + p.getNombre());
        } catch (Exception ex) {
            Toast.error(this, "Revisa cantidad/costo y fecha yyyy-MM-dd.");
        }
    }

    private JLabel lbl(String t) {
        JLabel l = new JLabel(t);
        l.setForeground(Tema.TEXTO);
        return l;
    }
}
