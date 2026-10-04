package vista;

import datos.GestorDatos;
import dominio.Cliente;
import servicio.Bitacora;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestión de Clientes y Control de Fiados.
 */
public class ClientesPanel extends JPanel {
    private final DefaultTableModel modelo = new DefaultTableModel(
            new String[]{"ID", "Nombre", "Teléfono", "Deuda Actual"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tabla = new JTable(modelo);
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(modelo);
    private final JTextField txtBuscar = new JTextField();
    private final JPanel centro = new JPanel(new BorderLayout());

    public ClientesPanel() {
        setBackground(Tema.FONDO);
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        // Barra Superior
        JPanel bar = new JPanel(new BorderLayout(10, 10));
        bar.setOpaque(false);

        JPanel izq = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        izq.setOpaque(false);
        JLabel icon = new JLabel("👥");
        icon.setForeground(Tema.TEXTO);
        txtBuscar.setPreferredSize(new Dimension(200, 38));
        Tema.campo(txtBuscar);
        txtBuscar.putClientProperty("JTextField.placeholderText", "Buscar cliente...");
        txtBuscar.addActionListener(e -> filtrar());
        izq.add(icon);
        izq.add(txtBuscar);
        bar.add(izq, BorderLayout.WEST);

        JPanel der = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        der.setOpaque(false);
        JButton bAdd = new JButton("＋ Nuevo Cliente");
        JButton bFiado = new JButton("💰 Registrar Fiado");
        JButton bPago = new JButton("💳 Registrar Pago");
        JButton bDel = new JButton("🗑 Eliminar");

        Tema.botonPrimario(bAdd);
        Tema.botonSecundario(bFiado);
        Tema.botonSecundario(bPago);
        Tema.botonPeligro(bDel);

        bAdd.setPreferredSize(new Dimension(140, 40));
        bFiado.setPreferredSize(new Dimension(140, 40));
        bPago.setPreferredSize(new Dimension(140, 40));
        bDel.setPreferredSize(new Dimension(120, 40));

        bAdd.addActionListener(e -> dialogoCliente(null));
        bFiado.addActionListener(e -> dialogoAjusteDeuda(true));
        bPago.addActionListener(e -> dialogoAjusteDeuda(false));
        bDel.addActionListener(e -> eliminarSeleccionado());

        der.add(bAdd); der.add(bFiado); der.add(bPago); der.add(bDel);
        bar.add(der, BorderLayout.EAST);
        add(bar, BorderLayout.NORTH);

        // Tabla
        Tema.tabla(tabla);
        tabla.setRowSorter(sorter);

        // Renderizador de Deuda (Rojo si debe dinero)
        tabla.getColumnModel().getColumn(3).setCellRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                try {
                    double deuda = Double.parseDouble(v.toString().replace("$", ""));
                    comp.setForeground(deuda > 0 ? Tema.ROJO : Tema.VERDE);
                } catch (Exception e) {
                    comp.setForeground(Tema.TEXTO);
                }
                comp.setHorizontalAlignment(CENTER);
                return comp;
            }
        });

        JScrollPane scroll = new JScrollPane(tabla);
        Tema.scroll(scroll);
        add(scroll, BorderLayout.CENTER);
    }

    private void filtrar() {
        String texto = "(?i)" + txtBuscar.getText().trim();
        sorter.setRowFilter(texto.isEmpty() ? null : RowFilter.regexFilter(texto, 1));
    }

    public void refrescar() {
        modelo.setRowCount(0);
        for (Cliente c : GestorDatos.getInstancia().getClientes()) {
            modelo.addRow(new Object[]{c.getId(), c.getNombre(), c.getTelefono(), String.format("$%.2f", c.getDeuda())});
        }
    }

    private void dialogoCliente(Cliente edit) {
        JTextField nombre = new JTextField(edit == null ? "" : edit.getNombre());
        JTextField tel = new JTextField(edit == null ? "" : edit.getTelefono());
        Tema.campo(nombre); Tema.campo(tel);

        JPanel f = new JPanel(new GridLayout(4, 1, 5, 5));
        f.setBackground(Tema.PANEL);
        f.add(lbl("Nombre Completo:")); f.add(nombre);
        f.add(lbl("Teléfono:")); f.add(tel);

        if (JOptionPane.showConfirmDialog(this, f, edit == null ? "Nuevo Cliente" : "Editar Cliente",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            String n = nombre.getText().trim();
            String t = tel.getText().trim();
            if (n.isEmpty()) {
                Toast.error(this, "El nombre es obligatorio.");
                return;
            }
            if (edit == null) {
                Cliente nuevo = new Cliente(GestorDatos.nuevoId("C"), n, t);
                GestorDatos.getInstancia().getClientes().add(nuevo);
                Bitacora.registrar("Cliente creado: " + n);
            } else {
                edit.setNombre(n);
            }
            GestorDatos.getInstancia().guardarTodo();
            refrescar();
            Toast.exito(this, "Cliente guardado.");
        }
    }

    private void dialogoAjusteDeuda(boolean esCarga) {
        int row = tabla.getSelectedRow();
        if (row < 0) {
            Toast.info(this, "Selecciona un cliente de la lista.");
            return;
        }
        String id = modelo.getValueAt(tabla.convertRowIndexToModel(row), 0).toString();
        Cliente c = GestorDatos.getInstancia().buscarCliente(id);
        if (c == null) return;

        JTextField monto = new JTextField();
        Tema.campo(monto);

        JPanel f = new JPanel(new GridLayout(2, 1, 5, 5));
        f.setBackground(Tema.PANEL);
        f.add(lbl("Monto a " + (esCarga ? "Anotar (Fiado):" : "Abonar (Pago):")));
        f.add(monto);

        if (JOptionPane.showConfirmDialog(this, f, esCarga ? "Anotar Deuda" : "Registrar Pago",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                double valor = Double.parseDouble(monto.getText().trim());
                if (valor <= 0) throw new NumberFormatException();

                double nuevaDeuda = esCarga ? c.getDeuda() + valor : c.getDeuda() - valor;
                c.setDeuda(nuevaDeuda);

                GestorDatos.getInstancia().guardarTodo();
                refrescar();
                Bitacora.registrar((esCarga ? "Deuda agregada" : "Pago recibido") + " de " + c.getNombre() + ": $" + valor);
                Toast.exito(this, "Operación realizada con éxito.");
            } catch (Exception ex) {
                Toast.error(this, "Ingresa un monto válido.");
            }
        }
    }

    private void eliminarSeleccionado() {
        int row = tabla.getSelectedRow();
        if (row < 0) {
            Toast.info(this, "Selecciona un cliente.");
            return;
        }
        String id = modelo.getValueAt(tabla.convertRowIndexToModel(row), 0).toString();
        if (JOptionPane.showConfirmDialog(this, "¿Eliminar cliente " + id + "?", "Confirmar", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_NO_OPTION) {
            GestorDatos.getInstancia().getClientes().removeIf(x -> x.getId().equals(id));
            GestorDatos.getInstancia().guardarTodo();
            refrescar();
            Toast.exito(this, "Cliente eliminado.");
        }
    }

    private JLabel lbl(String t) {
        JLabel l = new JLabel(t);
        l.setForeground(Tema.TEXTO);
        return l;
    }
}
