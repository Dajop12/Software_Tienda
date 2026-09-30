package vista;

import datos.GestorDatos;
import dominio.Alumno;
import servicio.Bitacora;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;

/** CRUD de alumnos con estado Aprobado/Reprobado. */
public class AlumnosPanel extends JPanel {
    private final DefaultTableModel modelo = new DefaultTableModel(
            new String[]{"ID", "Matrícula", "Nombre", "Carrera", "Promedio", "Estado"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tabla = new JTable(modelo);
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(modelo);
    private final JTextField txtBuscar = new JTextField();

    public AlumnosPanel() {
        setBackground(Tema.FONDO);
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JPanel bar = new JPanel(new BorderLayout(10, 10));
        bar.setOpaque(false);
        Tema.campo(txtBuscar);
        txtBuscar.setPreferredSize(new Dimension(240, 38));
        txtBuscar.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filtrar(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filtrar(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filtrar(); }
        });
        JPanel izq = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        izq.setOpaque(false);
        JLabel lupa = new JLabel("🔍"); lupa.setForeground(Tema.TEXTO);
        izq.add(lupa); izq.add(txtBuscar);
        bar.add(izq, BorderLayout.WEST);

        JPanel der = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        der.setOpaque(false);
        JButton bAdd = new JButton("＋ Agregar");
        JButton bEdit = new JButton("✎ Editar");
        JButton bDel = new JButton("🗑 Eliminar");
        Tema.botonPrimario(bAdd); Tema.botonSecundario(bEdit); Tema.botonSecundario(bDel);
        bAdd.addActionListener(e -> dialogo(null));
        bEdit.addActionListener(e -> editar());
        bDel.addActionListener(e -> eliminar());
        der.add(bAdd); der.add(bEdit); der.add(bDel);
        bar.add(der, BorderLayout.EAST);
        add(bar, BorderLayout.NORTH);

        Tema.tabla(tabla);
        tabla.setRowSorter(sorter);
        tabla.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, s, f, r, c);
                setHorizontalAlignment(CENTER);
                comp.setForeground("Aprobado".equals(v) ? Tema.VERDE : Tema.ROJO);
                comp.setBackground(s ? new Color(41, 121, 255, 80) : Tema.TARJETA);
                setFont(getFont().deriveFont(Font.BOLD));
                return comp;
            }
        });
        JScrollPane sp = new JScrollPane(tabla);
        sp.getViewport().setBackground(Tema.TARJETA);
        sp.setBorder(new Tema.RoundedBorder(Tema.BORDE, 12, 1));
        add(sp, BorderLayout.CENTER);
    }

    private void filtrar() {
        String t = txtBuscar.getText().trim();
        sorter.setRowFilter(t.isEmpty() ? null : RowFilter.regexFilter("(?i)" + t));
    }

    public void refrescar() {
        modelo.setRowCount(0);
        for (Alumno a : GestorDatos.getInstancia().getAlumnos()) {
            modelo.addRow(new Object[]{a.getId(), a.getMatricula(), a.getNombre(),
                    a.getCarrera(), String.format("%.1f", a.getPromedio()), a.getEstado()});
        }
    }

    private void editar() {
        int v = tabla.getSelectedRow();
        if (v < 0) { aviso(); return; }
        String id = modelo.getValueAt(tabla.convertRowIndexToModel(v), 0).toString();
        for (Alumno a : GestorDatos.getInstancia().getAlumnos()) {
            if (a.getId().equals(id)) { dialogo(a); return; }
        }
    }

    private void eliminar() {
        int v = tabla.getSelectedRow();
        if (v < 0) { aviso(); return; }
        String id = modelo.getValueAt(tabla.convertRowIndexToModel(v), 0).toString();
        if (JOptionPane.showConfirmDialog(this, "¿Eliminar " + id + "?", "Confirmar",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        GestorDatos.getInstancia().getAlumnos().removeIf(x -> x.getId().equals(id));
        GestorDatos.getInstancia().guardarTodo();
        Bitacora.registrar("Alumno eliminado: " + id);
        refrescar();
    }

    private void aviso() {
        JOptionPane.showMessageDialog(this, "Selecciona un alumno.", "Aviso", JOptionPane.WARNING_MESSAGE);
    }

    private void dialogo(Alumno edit) {
        JTextField nombre = new JTextField(edit == null ? "" : edit.getNombre());
        JTextField carrera = new JTextField(edit == null ? "Sistemas" : edit.getCarrera());
        JTextField prom = new JTextField(edit == null ? "" : String.valueOf(edit.getPromedio()));
        for (JTextField f : new JTextField[]{nombre, carrera, prom}) Tema.campo(f);
        JPanel f = new JPanel(new GridLayout(6, 1, 4, 4));
        f.setBackground(Tema.PANEL);
        f.add(lbl("Nombre:")); f.add(nombre);
        f.add(lbl("Carrera:")); f.add(carrera);
        f.add(lbl("Promedio (0-10):")); f.add(prom);
        int ok = JOptionPane.showConfirmDialog(this, f, edit == null ? "Agregar alumno" : "Editar alumno",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;
        try {
            String n = nombre.getText().trim(), c = carrera.getText().trim();
            double p = Double.parseDouble(prom.getText().trim());
            if (n.isEmpty() || c.isEmpty() || p < 0 || p > 10) throw new NumberFormatException();
            if (edit == null) {
                int nro = GestorDatos.getInstancia().getAlumnos().size() + 1001;
                GestorDatos.getInstancia().getAlumnos().add(new Alumno(
                        GestorDatos.nuevoId("A"), n, "MAT-" + nro, c, p));
                Bitacora.registrar("Alumno agregado: " + n);
            } else {
                edit.setNombre(n); edit.setCarrera(c); edit.setPromedio(p);
                Bitacora.registrar("Alumno editado: " + edit.getId());
            }
            GestorDatos.getInstancia().guardarTodo();
            refrescar();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Promedio debe ser número entre 0 y 10.",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JLabel lbl(String t) {
        JLabel l = new JLabel(t); l.setForeground(Tema.TEXTO); return l;
    }
}
