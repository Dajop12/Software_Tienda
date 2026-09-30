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
            new String[]{"ID", "Nombre", "Categoría", "Precio", "Stock", "Total"}, 0) {
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

        JPanel izq = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        izq.setOpaque(false);
        izq.add(new JLabel("🔍") {{ setForeground(Tema.TEXTO); }});
        izq.add(txtBuscar); izq.add(cmbCat);
        bar.add(izq, BorderLayout.WEST);

        JPanel der = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        der.setOpaque(false);
        JButton bAdd = new JButton("＋ Agregar");
        JButton bEdit = new JButton("✎ Editar");
        JButton bDel = new JButton("🗑 Eliminar");
        JButton bEnt = new JButton("📥 Entrada");
        Tema.botonPrimario(bAdd); Tema.botonSecundario(bEdit); Tema.botonPeligro(bDel); Tema.botonSecundario(bEnt);
        bAdd.setToolTipText("Agregar producto (valida precio y stock)");
        bEdit.setToolTipText("Editar el producto seleccionado");
        bDel.setToolTipText("Eliminar el producto seleccionado");
        bEnt.setToolTipText("Factura proveedor: entra stock + vencimiento");
        bAdd.setMnemonic('A'); bEdit.setMnemonic('E');
        bAdd.setPreferredSize(new Dimension(120, 38));
        bEdit.setPreferredSize(new Dimension(110, 38));
        bDel.setPreferredSize(new Dimension(120, 38));
        bEnt.setPreferredSize(new Dimension(110, 38));
        bAdd.addActionListener(e -> dialogo(null));
        bEdit.addActionListener(e -> editarSeleccionado());
        bDel.addActionListener(e -> eliminarSeleccionado());
        bEnt.addActionListener(e -> dialogoEntrada());
        der.add(bAdd); der.add(bEdit); der.add(bDel); der.add(bEnt);
        bar.add(der, BorderLayout.EAST);
        add(bar, BorderLayout.NORTH);

        // tabla + estado vacío ilustrado (CardLayout: evita tabla fría sin datos)
        Tema.tabla(tabla);
        tabla.setRowSorter(sorter);
        // pinta stock bajo en rojo
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

        lblConteo.setFont(new Font("Segoe UI", Font.PLAIN, 11));
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
        lblConteo.setText(sorter.getViewRowCount() + " de " + modelo.getRowCount() + " productos"
                + (GestorDatos.getInstancia().stockBajoCount() > 0
                        ? "  •  ⚠ " + GestorDatos.getInstancia().stockBajoCount() + " con stock bajo" : ""));
    }

    public void refrescar() {
        modelo.setRowCount(0);
        for (Producto p : GestorDatos.getInstancia().getProductos()) {
            modelo.addRow(new Object[]{p.getId(), p.getNombre(), p.getCategoria(),
                    String.format("%.2f", p.getPrecio()), p.getStock(),
                    String.format("%.2f", p.getValorTotal())});
        }
        filtrar(); // reaplica filtro + empty state + conteo
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
        for (JTextField f : new JTextField[]{nombre, categoria, precio, stock, costo, stockMin}) Tema.campo(f);
        JPanel f = new JPanel(new GridLayout(jefe ? 12 : 8, 1, 4, 4));
        f.setBackground(Tema.PANEL);
        f.add(lbl("Nombre:")); f.add(nombre);
        f.add(lbl("Categoría:")); f.add(categoria);
        f.add(lbl("Precio venta:")); f.add(precio);
        f.add(lbl("Stock:")); f.add(stock);
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
            if (n.isEmpty() || c.isEmpty() || pr <= 0 || st < 0) throw new NumberFormatException();
            if (edit == null) {
                double costoF = co < 0 ? pr * 0.7 : co;
                int minF = sm < 0 ? 5 : sm;
                GestorDatos.getInstancia().getProductos().add(
                        new Producto(GestorDatos.nuevoId("P"), n, c, costoF, pr, st, minF, "", "", ""));
                Bitacora.registrar("Producto agregado: " + n);
                Toast.exito(this, "Producto agregado: " + n);
            } else {
                edit.setNombre(n); edit.setCategoria(c); edit.setPrecio(pr); edit.setStock(st);
                if (jefe) {
                    if (co >= 0) edit.setCostoProveedor(co);
                    if (sm >= 0) edit.setStockMin(sm);
                }
                Bitacora.registrar("Producto editado: " + edit.getId());
                Toast.exito(this, "Producto actualizado.");
            }
            GestorDatos.getInstancia().guardarTodo();
            refrescar();
        } catch (NumberFormatException ex) {
            Toast.error(this, "Revisa: nombre/categoría no vacíos, precio > 0, stock >= 0.");
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
