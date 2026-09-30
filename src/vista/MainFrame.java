package vista;

import datos.GestorDatos;
import dominio.Usuario;
import servicio.Bitacora;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ventana principal con sidebar + CardLayout.
 */
public class MainFrame extends JFrame {
    private final Usuario usuario;
    private final CardLayout cards = new CardLayout();
    private final JPanel centro = new JPanel(cards);
    private final JLabel lblTitulo = new JLabel();
    private final JLabel lblSub = new JLabel();
    private final JLabel lblReloj = new JLabel();
    private final JLabel lblStatus = new JLabel();
    private final Map<String, JButton> nav = new LinkedHashMap<>();
    private final Map<String, String> titulos = Map.of(
            "DASH", "Dashboard", "PROD", "Productos", "VENT", "Punto de venta",
            "USER", "Usuarios", "CONF", "Configuración");
    private final Map<String, String> subtitulos = Map.of(
            "DASH", "Resumen del negocio en tiempo real",
            "PROD", "Inventario, precios y stock",
            "VENT", "Cobra rápido y descuenta stock solo",
            "USER", "Roles y acceso (solo ADMIN)",
            "CONF", "Respaldo, exportación y bitácora");

    private DashboardPanel dashboard;
    private ProductosPanel productos;
    private VentasPanel ventas;
    private UsuariosPanel usuarios;
    private ConfigPanel config;

    public MainFrame(Usuario usuario) {
        super("Super App — " + usuario.getNombre());
        this.usuario = usuario;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1150, 700);
        setMinimumSize(new Dimension(1000, 620));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(sidebar(), BorderLayout.WEST);
        add(topbar(), BorderLayout.NORTH);

        centro.setBackground(Tema.FONDO);
        dashboard = new DashboardPanel();
        productos = new ProductosPanel();
        ventas = new VentasPanel(usuario);
        usuarios = new UsuariosPanel(usuario);
        config = new ConfigPanel(this);

        centro.add(dashboard, "DASH");
        centro.add(productos, "PROD");
        centro.add(ventas, "VENT");
        centro.add(usuarios, "USER");
        centro.add(config, "CONF");
        add(centro, BorderLayout.CENTER);
        add(statusbar(), BorderLayout.SOUTH);

        instalarAtajos();
        mostrar("DASH");
        Bitacora.registrar("Entró al sistema: " + usuario.getUsername());
        // F2: alerta inmediata al jefe/local si hay quiebres pendientes
        int bajos = GestorDatos.getInstancia().productosStockBajo().size();
        if (bajos > 0) {
            Toast.error(this, "⚠ " + bajos + " producto(s) con stock bajo. Revisa Productos.");
        }
        // F11: vencen en 7 días o ya vencidos
        int venc = GestorDatos.getInstancia().proximosAVencer(7).size();
        if (venc > 0) {
            Toast.error(this, "⏳ " + venc + " producto(s) vencen ≤7d o vencidos. Revisa Productos.");
        }
        // F12: push caducidad 1 vez/día (llega a FCM si hay key)
        servicio.PushService.alertarVencimientos(GestorDatos.getInstancia().proximosAVencer(7));

        // reloj cada segundo
        Timer t = new Timer(1000, e -> lblReloj.setText(
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))));
        t.start();
    }

    private JPanel sidebar() {
        JPanel s = new JPanel();
        s.setBackground(Tema.PANEL);
        s.setPreferredSize(new Dimension(224, 0));
        s.setLayout(new BoxLayout(s, BoxLayout.Y_AXIS));
        s.setBorder(BorderFactory.createEmptyBorder(18, 12, 18, 12));

        // Marca
        JLabel logo = new JLabel("◉ Super App");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 18));
        logo.setForeground(Color.WHITE);
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);
        s.add(logo);
        JLabel tag = new JLabel("ENTERPRISE • v2.0");
        tag.setFont(new Font("Segoe UI", Font.BOLD, 10));
        tag.setForeground(Tema.ACENTO);
        tag.setAlignmentX(Component.LEFT_ALIGNMENT);
        s.add(tag);
        s.add(Box.createVerticalStrut(14));

        // Tarjeta de usuario: avatar + nombre + badge de rol
        JPanel userCard = new JPanel(new BorderLayout(10, 0));
        userCard.setBackground(Tema.TARJETA);
        userCard.setBorder(BorderFactory.createCompoundBorder(
                new Tema.RoundedBorder(Tema.BORDE, 12, 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        userCard.setMaximumSize(new Dimension(Short.MAX_VALUE, 62));
        userCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        userCard.add(Tema.avatar(usuario.getNombre()), BorderLayout.WEST);
        JPanel utxt = new JPanel(new GridLayout(2, 1, 0, 1));
        utxt.setOpaque(false);
        JLabel un = new JLabel(truncar(usuario.getNombre(), 18));
        un.setFont(new Font("Segoe UI", Font.BOLD, 12));
        un.setForeground(Color.WHITE);
        un.setToolTipText(usuario.getNombre() + " • " + usuario.getUsername());
        utxt.add(un);
        JPanel badgeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        badgeRow.setOpaque(false);
        Color rolFg = usuario.esAdmin() ? Tema.VERDE : ("VENDEDOR".equals(usuario.getRol()) ? Tema.ACENTO : Tema.TEXTO_SEC);
        Color rolBg = usuario.esAdmin() ? Tema.VERDE_BG : ("VENDEDOR".equals(usuario.getRol()) ? Tema.INFO_BG : new Color(160, 168, 186, 28));
        badgeRow.add(Tema.badge(usuario.getRol(), rolFg, rolBg));
        utxt.add(badgeRow);
        userCard.add(utxt, BorderLayout.CENTER);
        s.add(userCard);
        s.add(Box.createVerticalStrut(16));

        JLabel menu = new JLabel("MENÚ");
        menu.setFont(new Font("Segoe UI", Font.BOLD, 10));
        menu.setForeground(Tema.TEXTO_SEC);
        menu.setAlignmentX(Component.LEFT_ALIGNMENT);
        s.add(menu);
        s.add(Box.createVerticalStrut(6));

        agregarNav(s, "DASH", "📊  Dashboard", "Ver resumen (Ctrl+1)");
        agregarNav(s, "PROD", "📦  Productos", "Gestionar inventario (Ctrl+2)");
        agregarNav(s, "VENT", "🛒  Punto de venta", "Cobrar (Ctrl+3)");
        agregarNav(s, "USER", "👥  Usuarios", "Solo ADMIN (Ctrl+4)");
        agregarNav(s, "CONF", "⚙️  Configuración", "Respaldo y bitácora (Ctrl+5)");
        s.add(Box.createVerticalGlue());

        JButton salir = new JButton("⏻  Cerrar sesión");
        Tema.botonSecundario(salir);
        salir.setToolTipText("Volver al login");
        salir.setAlignmentX(Component.LEFT_ALIGNMENT);
        salir.addActionListener(e -> {
            Bitacora.registrar("Salió: " + usuario.getUsername());
            servicio.Sesion.limpiar();
            dispose();
            new LoginFrame().setVisible(true);
        });
        s.add(salir);
        return s;
    }

    private void agregarNav(JPanel s, String clave, String texto, String tip) {
        JButton b = new JButton(texto);
        Tema.botonSidebar(b, false);
        b.setToolTipText(tip);
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Short.MAX_VALUE, 44));
        b.addActionListener(e -> {
            if (clave.equals("USER") && !usuario.esAdmin()) {
                Toast.error(this, "Sin permiso. Requiere rol ADMIN (tienes " + usuario.getRol() + ").");
                return;
            }
            mostrar(clave);
        });
        nav.put(clave, b);
        s.add(Box.createVerticalStrut(6));
        s.add(b);
    }

    private JPanel topbar() {
        JPanel t = new JPanel(new BorderLayout(12, 0));
        t.setBackground(Tema.PANEL);
        t.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.BORDE),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)));
        JPanel tit = new JPanel(new GridLayout(2, 1, 0, 0));
        tit.setOpaque(false);
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitulo.setForeground(Color.WHITE);
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(Tema.TEXTO_SEC);
        tit.add(lblTitulo); tit.add(lblSub);
        lblReloj.setFont(new Font("Consolas", Font.PLAIN, 12));
        lblReloj.setForeground(Tema.TEXTO_SEC);
        lblReloj.setHorizontalAlignment(SwingConstants.RIGHT);
        t.add(tit, BorderLayout.WEST);
        t.add(lblReloj, BorderLayout.EAST);
        return t;
    }

    private JPanel statusbar() {
        JPanel st = new JPanel(new BorderLayout());
        st.setBackground(new Color(21, 24, 32));
        st.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Tema.BORDE),
                BorderFactory.createEmptyBorder(6, 18, 6, 18)));
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblStatus.setForeground(Tema.TEXTO_SEC);
        JLabel local = new JLabel("● Local");
        local.setFont(new Font("Segoe UI", Font.BOLD, 11));
        local.setForeground(Tema.VERDE);
        local.setToolTipText("Datos guardados en carpeta data/");
        st.add(lblStatus, BorderLayout.WEST);
        st.add(local, BorderLayout.EAST);
        return st;
    }

    /** Atajos Ctrl+1..5: navegación sin mouse (estándar enterprise). */
    private void instalarAtajos() {
        String[] claves = {"DASH", "PROD", "VENT", "USER", "CONF"};
        for (int i = 0; i < claves.length; i++) {
            final String c = claves[i];
            getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                    .put(KeyStroke.getKeyStroke("control " + (i + 1)), "nav-" + c);
            getRootPane().getActionMap().put("nav-" + c, new AbstractAction() {
                @Override public void actionPerformed(ActionEvent e) {
                    if (c.equals("USER") && !usuario.esAdmin()) {
                        Toast.error(MainFrame.this, "Solo ADMIN puede gestionar usuarios.");
                        return;
                    }
                    mostrar(c);
                }
            });
        }
    }

    private static String truncar(String s, int n) {
        return s.length() <= n ? s : s.substring(0, n - 1) + "…";
    }

    /** Cambia de sección y refresca datos. */
    public void mostrar(String clave) {
        for (Map.Entry<String, JButton> e : nav.entrySet()) {
            boolean activo = e.getKey().equals(clave);
            Tema.botonSidebar(e.getValue(), activo);
            // Indicador activo: fondo + barra lateral implícita vía borde
            e.getValue().setBackground(activo ? Tema.PRIMARIO_BG : Tema.PANEL);
            e.getValue().setForeground(activo ? Tema.BTN_TEXTO : Tema.NAV_TEXTO);
            e.getValue().setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, activo ? 3 : 0, 0, 0, activo ? Color.WHITE : Tema.PANEL),
                    BorderFactory.createEmptyBorder(10, activo ? 13 : 16, 10, 16)));
        }
        lblTitulo.setText(titulos.getOrDefault(clave, clave));
        lblSub.setText(subtitulos.getOrDefault(clave, ""));
        switch (clave) {
            case "DASH" -> dashboard.refrescar();
            case "PROD" -> productos.refrescar();
            case "VENT" -> ventas.refrescar();
            case "USER" -> usuarios.refrescar();
            case "CONF" -> config.refrescar();
        }
        cards.show(centro, clave);
        actualizarStatus();
    }

    private void actualizarStatus() {
        GestorDatos g = GestorDatos.getInstancia();
        int bajos = g.productosStockBajo().size();
        int venc = g.proximosAVencer(30).size();
        // Badge en sidebar: visible sin entrar al módulo
        JButton bProd = nav.get("PROD");
        if (bProd != null) bProd.setText(bajos > 0 ? "📦  Productos (" + bajos + " ⚠)" : "📦  Productos");
        String alerta = bajos > 0 ? "  •  ⚠ " + bajos + " stock bajo" : "  •  ✔ stock OK";
        if (venc > 0) alerta += "  •  ⏳ " + venc + " vencen ≤30d";
        lblStatus.setText(String.format("%d productos  •  %d ventas  •  %d usuarios%s   —   %s",
                g.getProductos().size(), g.getVentas().size(),
                g.getUsuarios().size(), alerta, lblTitulo.getText()));
        lblStatus.setForeground(bajos > 0 || venc > 0 ? Tema.AMARILLO : Tema.TEXTO_SEC);
    }

    public Usuario getUsuario() { return usuario; }
}
