package vista;

import datos.GestorDatos;
import dominio.Usuario;
import servicio.AuthService;
import servicio.Bitacora;
import javax.swing.*;
import java.awt.*;

/**
 * Login oscuro, reutiliza la idea anterior pero con AuthService + registro.
 * Demo: admin/1234, ana/1234, invitado/1234
 */
public class LoginFrame extends JFrame {
    private final JTextField txtUser = new JTextField();
    private final JPasswordField txtPass = new JPasswordField();
    private final JLabel lblMsg = new JLabel(" ");
    private final JLabel lblIntentos = new JLabel();
    private final AuthService auth = AuthService.get();

    public LoginFrame() {
        super("Super App — Iniciar sesión");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(860, 540);
        setMinimumSize(new Dimension(780, 500));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new GridLayout(1, 2));
        root.add(panelIzquierdo());
        root.add(panelDerecho());
        add(root);
        actualizarIntentos();
        // timer para mostrar desbloqueo
        new Timer(500, e -> {
            if (auth.bloqueado()) {
                lblMsg.setForeground(Tema.ROJO);
                lblMsg.setText("⛔ Bloqueado. Espera " + auth.segundosRestantes() + " s…");
            }
        }).start();
    }

    private JPanel panelIzquierdo() {
        Tema.GradientPanel p = new Tema.GradientPanel(new Color(13, 19, 54), new Color(59, 130, 255));
        p.setLayout(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(32, 40, 32, 40));
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.fill = GridBagConstraints.HORIZONTAL; g.anchor = GridBagConstraints.WEST;

        // Ilustración hero: le da identidad al producto sin assets externos
        Ilustracion hero = new Ilustracion(Ilustracion.Tipo.LOGIN_HERO, 110);
        hero.setAlignmentX(Component.LEFT_ALIGNMENT);
        g.gridy = 0; g.insets = new Insets(0, 0, 10, 0);
        p.add(hero, g);

        JLabel titulo = new JLabel("<html><b style='font-size:26px; color:white'>Super App</b><br>"
                + "<span style='font-size:13px; color:#C5CAE9'>Tienda + Escuela + Admin</span></html>");
        g.gridy = 1; g.insets = new Insets(0, 0, 16, 0);
        p.add(titulo, g);

        JLabel feats = new JLabel("<html><p style='color:#E8EAF6; font-size:13px'>"
                + "✔ Dashboard con gráficas<br>✔ Productos y punto de venta<br>"
                + "✔ Alumnos y calificaciones<br>✔ Usuarios y roles<br>✔ Guarda en archivos locales</p></html>");
        g.gridy = 2; g.insets = new Insets(0, 0, 24, 0);
        p.add(feats, g);

        JPanel card = new JPanel(new GridLayout(3, 1));
        card.setBackground(new Color(255, 255, 255, 30));
        card.setBorder(BorderFactory.createCompoundBorder(
                new Tema.RoundedBorder(new Color(255, 255, 255, 90), 12, 1),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        JLabel a = new JLabel("🔑  Demo: admin / 1234");
        a.setForeground(Color.WHITE); a.setFont(new Font("Segoe UI", Font.BOLD, 13));
        JLabel b = new JLabel("Vendedor: ana / 1234");
        b.setForeground(new Color(232, 234, 246)); b.setFont(new Font("Consolas", Font.PLAIN, 12));
        JLabel c = new JLabel("Consulta: invitado / 1234");
        c.setForeground(new Color(232, 234, 246)); c.setFont(new Font("Consolas", Font.PLAIN, 12));
        card.add(a); card.add(b); card.add(c);
        g.gridy = 3; g.insets = new Insets(0, 0, 0, 0);
        p.add(card, g);
        return p;
    }

    private JPanel panelDerecho() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Tema.FONDO);
        p.setBorder(BorderFactory.createEmptyBorder(36, 42, 36, 42));
        GridBagConstraints gb = new GridBagConstraints();
        gb.fill = GridBagConstraints.HORIZONTAL; gb.gridx = 0; gb.weightx = 1.0;
        int y = 0;

        JLabel t = new JLabel("Bienvenido de nuevo");
        t.setFont(Tema.TITULO); t.setForeground(Color.WHITE);
        gb.gridy = y++; gb.insets = new Insets(0, 0, 4, 0); p.add(t, gb);
        JLabel s = new JLabel("Ingresa para abrir el panel principal");
        s.setFont(Tema.SUB); s.setForeground(Tema.TEXTO_SEC);
        gb.gridy = y++; gb.insets = new Insets(0, 0, 16, 0); p.add(s, gb);

        lblMsg.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblMsg.setForeground(Tema.ROJO);
        gb.gridy = y++; gb.insets = new Insets(0, 0, 8, 0); p.add(lblMsg, gb);

        JLabel lu = new JLabel("👤  USUARIO");
        lu.setFont(Tema.ETIQ); lu.setForeground(Tema.TEXTO);
        lu.setLabelFor(txtUser);
        gb.gridy = y++; gb.insets = new Insets(0, 0, 0, 0); p.add(lu, gb);
        Tema.campo(txtUser); txtUser.setText("admin");
        txtUser.setToolTipText("Tu nombre de usuario (ej. admin). Enter para continuar.");
        txtUser.getAccessibleContext().setAccessibleName("Usuario");
        gb.gridy = y++; gb.insets = new Insets(4, 0, 12, 0); p.add(txtUser, gb);

        JLabel lp = new JLabel("🔒  CONTRASEÑA");
        lp.setFont(Tema.ETIQ); lp.setForeground(Tema.TEXTO);
        lp.setLabelFor(txtPass);
        gb.gridy = y++; gb.insets = new Insets(0, 0, 0, 0); p.add(lp, gb);
        Tema.campo(txtPass); txtPass.setText("1234"); txtPass.setEchoChar('•');
        txtPass.setToolTipText("Tu contraseña. Puedes mostrarla con el checkbox.");
        txtPass.getAccessibleContext().setAccessibleName("Contraseña");
        gb.gridy = y++; gb.insets = new Insets(4, 0, 6, 0); p.add(txtPass, gb);

        JPanel extra = new JPanel(new BorderLayout());
        extra.setOpaque(false);
        JCheckBox ver = new JCheckBox("Mostrar");
        ver.setOpaque(false); ver.setForeground(Tema.TEXTO_SEC);
        ver.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        ver.setToolTipText("Mostrar / ocultar contraseña");
        ver.setMnemonic('M');
        ver.addActionListener(e -> txtPass.setEchoChar(ver.isSelected() ? (char) 0 : '•'));
        lblIntentos.setForeground(Tema.TEXTO_SEC);
        lblIntentos.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        extra.add(ver, BorderLayout.WEST); extra.add(lblIntentos, BorderLayout.EAST);
        gb.gridy = y++; gb.insets = new Insets(0, 0, 14, 0); p.add(extra, gb);

        JButton btn = new JButton("➜   Entrar al sistema");
        Tema.botonPrimario(btn);
        btn.setToolTipText("Entrar (Enter)");
        btn.setMnemonic('E');
        gb.gridy = y++; gb.insets = new Insets(0, 0, 10, 0); gb.ipady = 4; p.add(btn, gb);

        JPanel dos = new JPanel(new GridLayout(1, 2, 10, 0));
        dos.setOpaque(false);
        JButton limpiar = new JButton("Limpiar");
        JButton registro = new JButton("Crear cuenta");
        Tema.botonSecundario(limpiar); Tema.botonSecundario(registro);
        limpiar.setToolTipText("Limpia los campos (Esc)");
        registro.setToolTipText("Crea una cuenta con rol Consulta");
        // Esc limpia: estándar de formularios enterprise
        p.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke("ESCAPE"), "limpiar");
        p.getActionMap().put("limpiar", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { limpiar.doClick(); }
        });
        dos.add(limpiar); dos.add(registro);
        gb.gridy = y++; gb.ipady = 0; gb.insets = new Insets(0, 0, 0, 0); p.add(dos, gb);

        getRootPane().setDefaultButton(btn);

        final JButton btnRef = btn;
        btn.addActionListener(e -> hacerLogin(btnRef));
        limpiar.addActionListener(e -> {
            txtUser.setText(""); txtPass.setText("");
            lblMsg.setText(" "); txtUser.requestFocusInWindow();
        });
        registro.addActionListener(e -> dialogoRegistro());
        // foco inicial: UX correcta, el usuario escribe de inmediato
        SwingUtilities.invokeLater(txtUser::requestFocusInWindow);
        return p;
    }

    private void actualizarIntentos() {
        lblIntentos.setText("Intentos: " + auth.getIntentos() + "/" + AuthService.MAX_INTENTOS);
    }

    private void hacerLogin(JButton btn) {
        if (auth.bloqueado()) {
            lblMsg.setForeground(Tema.ROJO);
            lblMsg.setText("⛔ Bloqueado. Espera " + auth.segundosRestantes() + " s…");
            sacudir();
            return;
        }
        // Estado loading: evita doble click y comunica progreso
        btn.setEnabled(false);
        String original = btn.getText();
        btn.setText("… Verificando");
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        Timer espera = new Timer(350, ev -> {
            AuthService.Resultado r = auth.login(txtUser.getText(), new String(txtPass.getPassword()));
            btn.setEnabled(true);
            btn.setText(original);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            lblMsg.setForeground(r.ok ? Tema.VERDE : Tema.ROJO);
            lblMsg.setText(r.mensaje);
            actualizarIntentos();
            if (!r.ok) {
                sacudir();
                return;
            }
            if (r.debeCambiar) {
                // F7: cambio obligado de 1234 (no se puede omitir)
                while (true) {
                    JPasswordField n1 = new JPasswordField(); Tema.campo(n1);
                    JPasswordField n2 = new JPasswordField(); Tema.campo(n2);
                    JPanel f = new JPanel(new GridLayout(4, 1, 4, 4));
                    f.setBackground(Tema.PANEL);
                    JLabel a = new JLabel("Nueva contraseña (mín 6):"); a.setForeground(Tema.TEXTO);
                    JLabel b = new JLabel("Repite:"); b.setForeground(Tema.TEXTO);
                    f.add(a); f.add(n1); f.add(b); f.add(n2);
                    if (JOptionPane.showConfirmDialog(this, f, "⚠ Cambia tu pass 1234",
                            JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.OK_OPTION) {
                        lblMsg.setText("Debes cambiar 1234 para entrar.");
                        return;
                    }
                    String s1 = new String(n1.getPassword()), s2 = new String(n2.getPassword());
                    if (!s1.equals(s2)) { Toast.error(this, "No coinciden."); continue; }
                    AuthService.Resultado c = auth.cambiarPass(r.usuario, "1234", s1);
                    if (!c.ok) { Toast.error(this, c.mensaje); continue; }
                    Toast.exito(this, "Pass actualizada.");
                    break;
                }
            }
            Toast.exito(this, "Sesión iniciada. Cargando panel…");
            Timer t = new Timer(450, ev2 -> {
                dispose();
                new MainFrame(r.usuario).setVisible(true);
            });
            t.setRepeats(false); t.start();
        });
        espera.setRepeats(false); espera.start();
    }

    /** Sacudida real: feedback kinestésico de error sin modal. */
    private void sacudir() {
        final Point origen = getLocation();
        Timer timer = new Timer(14, null);
        final int[] paso = {0};
        timer.addActionListener(e -> {
            int i = paso[0]++;
            setLocation(origen.x + (i % 2 == 0 ? 9 : -9), origen.y);
            if (i >= 6) { timer.stop(); setLocation(origen); }
        });
        timer.start();
    }

    private void dialogoRegistro() {
        JTextField nombre = new JTextField(); Tema.campo(nombre);
        JTextField user = new JTextField(); Tema.campo(user);
        JPasswordField pass = new JPasswordField(); Tema.campo(pass);
        JPanel f = new JPanel(new GridLayout(6, 1, 4, 4));
        f.setBackground(Tema.PANEL);
        JLabel l1 = new JLabel("Nombre:"); l1.setForeground(Tema.TEXTO);
        JLabel l2 = new JLabel("Usuario:"); l2.setForeground(Tema.TEXTO);
        JLabel l3 = new JLabel("Contraseña:"); l3.setForeground(Tema.TEXTO);
        f.add(l1); f.add(nombre); f.add(l2); f.add(user); f.add(l3); f.add(pass);
        int ok = JOptionPane.showConfirmDialog(this, f, "Crear cuenta (rol CONSULTA)",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;
        String n = nombre.getText().trim(), u = user.getText().trim(), pw = new String(pass.getPassword());
        if (n.isEmpty() || u.isEmpty() || pw.length() < 4) {
            JOptionPane.showMessageDialog(this, "Completa todo. Contraseña mínimo 4 caracteres.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        for (Usuario x : GestorDatos.getInstancia().getUsuarios()) {
            if (x.getUsername().equalsIgnoreCase(u)) {
                JOptionPane.showMessageDialog(this, "Ese usuario ya existe.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        Usuario nuevo = new Usuario(GestorDatos.nuevoId("U"), n, u, pw, "CONSULTA", true);
        GestorDatos.getInstancia().getUsuarios().add(nuevo);
        GestorDatos.getInstancia().guardarTodo();
        Bitacora.registrar("Cuenta creada: " + u);
        JOptionPane.showMessageDialog(this, "Cuenta creada. Ya puedes entrar.", "OK", JOptionPane.INFORMATION_MESSAGE);
        txtUser.setText(u);
    }
}
