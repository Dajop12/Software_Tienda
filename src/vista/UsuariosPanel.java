package vista;

import datos.GestorDatos;
import dominio.Usuario;
import servicio.Bitacora;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/** Gestión de usuarios. Solo ADMIN debería llegar aquí (MainFrame lo filtra). */
public class UsuariosPanel extends JPanel {
    private final Usuario actual;
    private final DefaultTableModel modelo = new DefaultTableModel(
            new String[]{"Nombre", "Usuario", "Rol", "Activo"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tabla = new JTable(modelo);

    public UsuariosPanel(Usuario actual) {
        this.actual = actual;
        setBackground(Tema.FONDO);
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        bar.setOpaque(false);
        JButton bAdd = new JButton("＋ Agregar");
        JButton bRol = new JButton("Cambiar rol");
        JButton bAct = new JButton("Activar/Desactivar");
        JButton bDel = new JButton("Eliminar");
        Tema.botonPrimario(bAdd); Tema.botonSecundario(bRol);
        Tema.botonSecundario(bAct); Tema.botonSecundario(bDel);
        bAdd.addActionListener(e -> agregar());
        bRol.addActionListener(e -> cambiarRol());
        bAct.addActionListener(e -> toggleActivo());
        bDel.addActionListener(e -> eliminar());
        bar.add(bAdd); bar.add(bRol); bar.add(bAct); bar.add(bDel);
        add(bar, BorderLayout.NORTH);

        Tema.tabla(tabla);
        JScrollPane sp = new JScrollPane(tabla);
        sp.getViewport().setBackground(Tema.TARJETA);
        sp.setBorder(new Tema.RoundedBorder(Tema.BORDE, 12, 1));
        add(sp, BorderLayout.CENTER);

        JLabel nota = new JLabel("Roles: ADMIN = todo • VENDEDOR = ventas/productos • CONSULTA = solo ver. No puedes eliminarte a ti mismo.");
        nota.setForeground(Tema.TEXTO_SEC);
        nota.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        add(nota, BorderLayout.SOUTH);
    }

    public void refrescar() {
        modelo.setRowCount(0);
        for (Usuario u : GestorDatos.getInstancia().getUsuarios()) {
            modelo.addRow(new Object[]{u.getNombre(), u.getUsername(), u.getRol(), u.isActivo() ? "Sí" : "No"});
        }
    }

    private Usuario seleccionado() {
        int r = tabla.getSelectedRow();
        if (r < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un usuario.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        String user = modelo.getValueAt(r, 1).toString();
        for (Usuario u : GestorDatos.getInstancia().getUsuarios()) {
            if (u.getUsername().equals(user)) return u;
        }
        return null;
    }

    private void agregar() {
        JTextField nombre = new JTextField(); Tema.campo(nombre);
        JTextField user = new JTextField(); Tema.campo(user);
        JPasswordField pass = new JPasswordField(); Tema.campo(pass);
        JComboBox<String> rol = new JComboBox<>(new String[]{"ADMIN", "VENDEDOR", "CONSULTA"});
        rol.setBackground(Tema.TARJETA); rol.setForeground(Tema.TEXTO);
        JPanel f = new JPanel(new GridLayout(8, 1, 4, 4));
        f.setBackground(Tema.PANEL);
        f.add(lbl("Nombre:")); f.add(nombre);
        f.add(lbl("Usuario:")); f.add(user);
        f.add(lbl("Contraseña (min 4):")); f.add(pass);
        f.add(lbl("Rol:")); f.add(rol);
        if (JOptionPane.showConfirmDialog(this, f, "Agregar usuario",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        String n = nombre.getText().trim(), u = user.getText().trim(), p = new String(pass.getPassword());
        if (n.isEmpty() || u.isEmpty() || p.length() < 4) {
            error("Completa todo. Contraseña mínimo 4.");
            return;
        }
        for (Usuario x : GestorDatos.getInstancia().getUsuarios()) {
            if (x.getUsername().equalsIgnoreCase(u)) { error("Ese usuario ya existe."); return; }
        }
        GestorDatos.getInstancia().getUsuarios().add(
                new Usuario(GestorDatos.nuevoId("U"), n, u, p, (String) rol.getSelectedItem(), true));
        GestorDatos.getInstancia().guardarTodo();
        Bitacora.registrar("Usuario creado: " + u + " por " + actual.getUsername());
        refrescar();
    }

    private void cambiarRol() {
        Usuario u = seleccionado();
        if (u == null) return;
        if (u.getUsername().equals(actual.getUsername())) {
            error("No cambies tu propio rol (te bloquearías).");
            return;
        }
        String[] roles = {"ADMIN", "VENDEDOR", "CONSULTA"};
        String r = (String) JOptionPane.showInputDialog(this, "Rol para " + u.getUsername(),
                "Cambiar rol", JOptionPane.PLAIN_MESSAGE, null, roles, u.getRol());
        if (r == null) return;
        u.setRol(r);
        GestorDatos.getInstancia().guardarTodo();
        Bitacora.registrar("Rol cambiado: " + u.getUsername() + " -> " + r);
        refrescar();
    }

    private void toggleActivo() {
        Usuario u = seleccionado();
        if (u == null) return;
        if (u.getUsername().equals(actual.getUsername())) {
            error("No te desactives a ti mismo.");
            return;
        }
        u.setActivo(!u.isActivo());
        GestorDatos.getInstancia().guardarTodo();
        Bitacora.registrar("Usuario " + u.getUsername() + " activo=" + u.isActivo());
        refrescar();
    }

    private void eliminar() {
        Usuario u = seleccionado();
        if (u == null) return;
        if (u.getUsername().equals(actual.getUsername())) {
            error("No puedes eliminarte a ti mismo.");
            return;
        }
        long admins = GestorDatos.getInstancia().getUsuarios().stream()
                .filter(x -> x.esAdmin() && x.isActivo()).count();
        if (u.esAdmin() && admins <= 1) {
            error("No puedes eliminar al último ADMIN activo.");
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "¿Eliminar " + u.getUsername() + "?",
                "Confirmar", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        GestorDatos.getInstancia().getUsuarios().remove(u);
        GestorDatos.getInstancia().guardarTodo();
        Bitacora.registrar("Usuario eliminado: " + u.getUsername());
        refrescar();
    }

    private JLabel lbl(String t) {
        JLabel l = new JLabel(t); l.setForeground(Tema.TEXTO); return l;
    }

    private void error(String m) {
        JOptionPane.showMessageDialog(this, m, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
