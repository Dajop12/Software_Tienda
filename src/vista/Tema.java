package vista;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Tema oscuro centralizado — paleta suave y controles accesibles.
 * Tokens + estados (hover/pressed/disabled/focus) + accesibilidad.
 * Mantiene la API anterior para no romper paneles existentes.
 */
public class Tema {
    private static final String FAMILIA_UI = resolverFamilia(
            "Arial Black", "Inter", "Segoe UI Variable", "Segoe UI", "Aptos", "Noto Sans", "Arial", "Dialog");
    private static final String FAMILIA_MONO = resolverFamilia(
            "Cascadia Code", "Cascadia Mono", "Consolas", "DejaVu Sans Mono", "Monospaced");

    // ---- Tokens de color (Paleta Moderna: Deep Night & Electric Blue) ----
    public static final Color FONDO = new Color(18, 22, 30);
    public static final Color PANEL = new Color(28, 35, 48);
    public static final Color TARJETA = new Color(38, 46, 62);
    public static final Color TARJETA_HOVER = new Color(48, 58, 78);
    public static final Color BORDE = new Color(55, 68, 88);
    public static final Color BORDE_FOCO = new Color(0, 163, 255);
    public static final Color TEXTO = new Color(245, 247, 250);
    public static final Color TEXTO_SEC = new Color(155, 170, 190);
    public static final Color ACENTO = new Color(0, 140, 255);
    public static final Color ACENTO_HOVER = new Color(0, 163, 255);
    public static final Color ACENTO_PRESSED = new Color(0, 110, 210);
    public static final Color VERDE = new Color(46, 204, 113);
    public static final Color VERDE_BG = new Color(46, 204, 113, 30);
    public static final Color ROJO = new Color(231, 76, 60);
    public static final Color ROJO_BG = new Color(231, 76, 60, 30);
    public static final Color AMARILLO = new Color(241, 196, 15);
    public static final Color AMARILLO_BG = new Color(241, 196, 15, 30);
    public static final Color INFO_BG = new Color(0, 140, 255, 30);

    // ---- Botones: colores suaves con estados visibles de interacción ----
    public static final Color PRIMARIO_BG = new Color(37, 111, 151);
    public static final Color PRIMARIO_HOVER = new Color(42, 132, 170);
    public static final Color PRIMARIO_PRESSED = new Color(32, 94, 127);
    public static final Color SEC_BG = new Color(52, 63, 81);
    public static final Color SEC_HOVER = new Color(66, 81, 103);
    public static final Color SEC_PRESSED = new Color(44, 55, 72);
    public static final Color PELIGRO_BG = new Color(116, 54, 63);
    public static final Color PELIGRO_HOVER = new Color(143, 64, 74);
    public static final Color PELIGRO_PRESSED = new Color(96, 44, 53);
    public static final Color BTN_TEXTO = new Color(255, 255, 255);
    public static final Color SEC_TEXTO = new Color(244, 246, 252);
    public static final Color NAV_TEXTO = new Color(208, 218, 231);
    public static final Color PELIGRO_FG = new Color(255, 232, 232);
    public static final Color DISABLED_BG = new Color(61, 69, 84);
    public static final Color DISABLED_FG = new Color(194, 204, 218);

    // ---- Espaciado / radio ----
    public static final int R_SM = 8, R_MD = 12, R_LG = 16;
    public static final int S_XS = 6, S_SM = 10, S_MD = 16, S_LG = 24;

    public static final Font TITULO = fuente(Font.BOLD, 24);
    public static final Font SUB = fuente(Font.PLAIN, 13);
    public static final Font ETIQ = fuente(Font.BOLD, 12);
    public static final Font CAMPO = fuente(Font.PLAIN, 14);
    public static final Font BOTON = fuente(Font.BOLD, 13);

    public static Font fuente(int estilo, int tamano) {
        return new Font(FAMILIA_UI, estilo, tamano);
    }

    public static Font fuenteMono(int estilo, int tamano) {
        return new Font(FAMILIA_MONO, estilo, tamano);
    }

    private static String resolverFamilia(String... candidatas) {
        java.util.Set<String> instaladas = new java.util.HashSet<>(
                java.util.Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String candidata : candidatas) {
            if (instaladas.contains(candidata)) return candidata;
        }
        return "Dialog";
    }

    public static void botonPrimario(JButton b) {
        configurarBoton(b, "primario");
    }

    public static void botonSecundario(JButton b) {
        configurarBoton(b, "secundario");
    }

    /** Botón de peligro (eliminar) con affordance clara. */
    public static void botonPeligro(JButton b) {
        configurarBoton(b, "peligro");
    }

    private static void configurarBoton(JButton b, String estilo) {
        b.putClientProperty("tema-estilo", estilo);
        b.setFont(BOTON);
        b.setFocusPainted(false);
        b.setFocusable(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setOpaque(true);
        b.setContentAreaFilled(true);
        b.setBorderPainted(true);
        b.setMargin(new Insets(9, 16, 9, 16));
        b.setMinimumSize(new Dimension(0, 40));
        if (!Boolean.TRUE.equals(b.getClientProperty("tema-listeners"))) {
            b.putClientProperty("tema-listeners", true);
            b.getModel().addChangeListener(e -> actualizarBoton(b));
            b.addFocusListener(new java.awt.event.FocusAdapter() {
                @Override public void focusGained(java.awt.event.FocusEvent e) { actualizarBoton(b); }
                @Override public void focusLost(java.awt.event.FocusEvent e) { actualizarBoton(b); }
            });
            b.addPropertyChangeListener("enabled", e -> actualizarBoton(b));
        }
        actualizarBoton(b);
    }

    private static void actualizarBoton(JButton b) {
        String estilo = (String) b.getClientProperty("tema-estilo");
        Color normal, hover, presionado, texto;
        if ("primario".equals(estilo)) {
            normal = PRIMARIO_BG; hover = PRIMARIO_HOVER; presionado = PRIMARIO_PRESSED; texto = BTN_TEXTO;
        } else if ("peligro".equals(estilo)) {
            normal = PELIGRO_BG; hover = PELIGRO_HOVER; presionado = PELIGRO_PRESSED; texto = PELIGRO_FG;
        } else {
            normal = SEC_BG; hover = SEC_HOVER; presionado = SEC_PRESSED; texto = SEC_TEXTO;
        }
        ButtonModel model = b.getModel();
        Color fondo = !b.isEnabled() ? DISABLED_BG
                : model.isPressed() && model.isArmed() ? presionado
                : model.isRollover() ? hover : normal;
        b.setBackground(fondo);
        b.setForeground(b.isEnabled() ? texto : DISABLED_FG);
        b.setBorder(new RoundedBorder(b.hasFocus() ? BORDE_FOCO : fondo, R_MD, b.hasFocus() ? 2 : 1));
    }

    public static void botonSidebar(JButton b, boolean activo) {
        b.putClientProperty("tema-nav-activo", activo);
        b.setFont(fuente(Font.BOLD, 13));
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setFocusPainted(true); // accesible por teclado
        b.setFocusable(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setOpaque(true);
        b.setContentAreaFilled(true);
        if (!Boolean.TRUE.equals(b.getClientProperty("tema-nav-listeners"))) {
            b.putClientProperty("tema-nav-listeners", true);
            b.getModel().addChangeListener(e -> actualizarSidebar(b));
            b.addFocusListener(new java.awt.event.FocusAdapter() {
                @Override public void focusGained(java.awt.event.FocusEvent e) { actualizarSidebar(b); }
                @Override public void focusLost(java.awt.event.FocusEvent e) { actualizarSidebar(b); }
            });
        }
        actualizarSidebar(b);
    }

    private static void actualizarSidebar(JButton b) {
        boolean activo = Boolean.TRUE.equals(b.getClientProperty("tema-nav-activo"));
        ButtonModel model = b.getModel();
        Color fondo = activo ? PRIMARIO_BG : model.isRollover() ? TARJETA_HOVER : PANEL;
        if (model.isPressed() && model.isArmed()) fondo = activo ? PRIMARIO_PRESSED : SEC_PRESSED;
        b.setBackground(fondo);
        b.setForeground(activo ? BTN_TEXTO : NAV_TEXTO);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, activo ? 3 : 0, 0, 0,
                        activo ? BORDE_FOCO : fondo),
                BorderFactory.createEmptyBorder(10, activo ? 13 : 16, 10, 16)));
    }

    public static void campo(JTextField c) {
        if (c.getClientProperty("tema-campo") != null) return;
        c.putClientProperty("tema-campo", true);
        c.setFont(CAMPO);
        c.setForeground(TEXTO);
        c.setBackground(TARJETA);
        c.setCaretColor(BORDE_FOCO);
        c.setBorder(new RoundedBorder(BORDE, R_MD, 1));
        c.setPreferredSize(new Dimension(0, 40));
        // Anillo de foco visible (accesibilidad) + hover sutil
        c.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) {
                c.setBorder(new RoundedBorder(BORDE_FOCO, R_MD, 2));
                c.setBackground(new Color(36, 41, 55));
            }
            @Override public void focusLost(java.awt.event.FocusEvent e) {
                c.setBorder(new RoundedBorder(BORDE, R_MD, 1));
                c.setBackground(TARJETA);
            }
        });
        c.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                if (!c.hasFocus()) c.setBorder(new RoundedBorder(new Color(70, 77, 96), R_MD, 1));
            }
            @Override public void mouseExited(MouseEvent e) {
                if (!c.hasFocus()) c.setBorder(new RoundedBorder(BORDE, R_MD, 1));
            }
        });
    }

    public static void tabla(JTable t) {
        t.setBackground(TARJETA);
        t.setForeground(TEXTO);
        t.setGridColor(new Color(52, 58, 74, 120));
        t.setShowGrid(false);
        t.setShowHorizontalLines(true);
        t.setShowVerticalLines(false);
        t.setIntercellSpacing(new Dimension(0, 1));
        t.setRowHeight(32);
        t.setFont(fuente(Font.PLAIN, 12));
        t.setSelectionBackground(new Color(59, 130, 255, 70));
        t.setSelectionForeground(Color.WHITE);
        t.setFillsViewportHeight(true);
        t.setFocusable(true);
        JTableHeader h = t.getTableHeader();
        h.setBackground(new Color(21, 24, 32));
        h.setForeground(TEXTO_SEC);
        h.setFont(fuente(Font.BOLD, 11));
        h.setPreferredSize(new Dimension(0, 34));
        h.setReorderingAllowed(false);
        ((DefaultTableCellRenderer) h.getDefaultRenderer()).setHorizontalAlignment(SwingConstants.LEFT);
        ((DefaultTableCellRenderer) h.getDefaultRenderer()).setBorder(
                BorderFactory.createEmptyBorder(0, 12, 0, 12));
    }

    /** Scroll oscuro consistente. */
    public static void scroll(JScrollPane sp) {
        sp.getViewport().setBackground(TARJETA);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getVerticalScrollBar().setUnitIncrement(16);
    }

    /** Tarjeta contenedora con elevación sutil. */
    public static JPanel card(JPanel contenido) {
        contenido.setBackground(TARJETA);
        contenido.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(BORDE, R_LG, 1),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));
        return contenido;
    }

    /** Badge de estado (Aprobado, ADMIN, Stock bajo…). */
    public static JLabel badge(String texto, Color fg, Color bg) {
        JLabel l = new JLabel(texto, SwingConstants.CENTER);
        l.setFont(fuente(Font.BOLD, 11));
        l.setForeground(fg);
        l.setBackground(bg);
        l.setOpaque(true);
        l.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(bg, 20, 0),
                BorderFactory.createEmptyBorder(3, 10, 3, 10)));
        return l;
    }

    /** Avatar con iniciales. */
    public static JLabel avatar(String nombre) {
        String ini = "?";
        String[] partes = nombre.trim().split("\\s+");
        if (partes.length == 1) ini = partes[0].substring(0, Math.min(1, partes[0].length())).toUpperCase();
        else ini = (partes[0].substring(0, 1) + partes[1].substring(0, 1)).toUpperCase();
        JLabel l = new JLabel(ini, SwingConstants.CENTER);
        l.setFont(fuente(Font.BOLD, 13));
        l.setForeground(BTN_TEXTO);
        l.setBackground(PRIMARIO_BG);
        l.setOpaque(true);
        l.setPreferredSize(new Dimension(38, 38));
        l.setBorder(new RoundedBorder(PRIMARIO_BG, 19, 0));
        return l;
    }

    /** Aplica look oscuro a dialogs/tooltips para coherencia. Llamar una vez al arrancar. */
    public static void initGlobales() {
        Font base = fuente(Font.PLAIN, 13);
        UIManager.put("defaultFont", base);
        for (String componente : new String[]{"Label", "Button", "ToggleButton", "TextField",
                "PasswordField", "TextArea", "ComboBox", "CheckBox", "RadioButton",
                "Table", "TableHeader", "TabbedPane", "Menu", "MenuItem", "OptionPane"}) {
            UIManager.put(componente + ".font", base);
        }
        UIManager.put("ToolTip.background", TARJETA);
        UIManager.put("ToolTip.foreground", TEXTO);
        UIManager.put("ToolTip.border", BorderFactory.createLineBorder(BORDE));
        UIManager.put("OptionPane.background", PANEL);
        UIManager.put("OptionPane.messageForeground", TEXTO);
        UIManager.put("Panel.background", PANEL);
        // Texto deshabilitado legible (por defecto el LAF lo pone casi invisible)
        UIManager.put("Button.disabledText", DISABLED_FG);
        UIManager.put("Button.disabledBackground", DISABLED_BG);
        UIManager.put("ComboBox.disabledForeground", DISABLED_FG);
        // Framework FlatLaf: esquinas redondeadas y foco visible en TODO
        UIManager.put("Button.arc", 14);
        UIManager.put("Component.arc", 12);
        UIManager.put("TextComponent.arc", 12);
        UIManager.put("ProgressBar.arc", 8);
        UIManager.put("Component.focusWidth", 2);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.showVerticalLines", false);
        UIManager.put("Table.intercellSpacing", new Dimension(0, 1));
        UIManager.put("ScrollBar.thumbArc", 8);
        UIManager.put("ScrollBar.trackArc", 8);
    }

    /** Tarjeta KPI: titulo arriba, valor grande, detalle pequeño. */
    public static JPanel tarjetaKpi(String titulo, String valor, String detalle, Color acento) {
        return tarjetaKpi("◉", titulo, valor, detalle, acento);
    }

    /** Tarjeta KPI con icono (recomendada). */
    public static JPanel tarjetaKpi(String icono, String titulo, String valor, String detalle, Color acento) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setBackground(TARJETA);
        p.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(BORDE, R_LG, 1),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel ic = new JLabel(icono);
        ic.setFont(fuente(Font.PLAIN, 16));
        ic.setForeground(acento);
        JLabel t = new JLabel("  " + titulo);
        t.setFont(fuente(Font.BOLD, 11));
        t.setForeground(TEXTO_SEC);
        top.add(ic, BorderLayout.WEST);
        top.add(t, BorderLayout.CENTER);
        JLabel v = new JLabel(valor);
        v.setFont(fuente(Font.BOLD, 26));
        v.setForeground(Color.WHITE);
        JLabel d = new JLabel(detalle);
        d.setFont(fuente(Font.PLAIN, 12));
        d.setForeground(acento);
        // barrita de color
        JPanel barra = new JPanel();
        barra.setPreferredSize(new Dimension(0, 4));
        barra.setBackground(acento);
        barra.setBorder(BorderFactory.createEmptyBorder());
        p.add(top, BorderLayout.NORTH);
        p.add(v, BorderLayout.CENTER);
        p.add(d, BorderLayout.SOUTH);
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(TARJETA);
        wrap.add(barra, BorderLayout.NORTH);
        wrap.add(p, BorderLayout.CENTER);
        // hover sutil: feedback de interactividad
        wrap.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { p.setBackground(TARJETA_HOVER); top.setBackground(TARJETA_HOVER); }
            @Override public void mouseExited(MouseEvent e) { p.setBackground(TARJETA); top.setBackground(TARJETA); }
        });
        return wrap;
    }

    /** Borde redondeado. */
    public static class RoundedBorder extends AbstractBorder {
        private final Color color; private final int radio; private final int grosor;
        public RoundedBorder(Color c, int r, int g) { color = c; radio = r; grosor = g; }
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(Math.max(grosor, 1)));
            int off = Math.max(grosor, 1);
            g2.drawRoundRect(x + off / 2, y + off / 2, w - off, h - off, radio, radio);
            g2.dispose();
        }
        @Override public Insets getBorderInsets(Component c) { return new Insets(8, 14, 8, 14); }
        @Override public Insets getBorderInsets(Component c, Insets i) { i.set(8, 14, 8, 14); return i; }
    }

    /** Panel con degradado. */
    public static class GradientPanel extends JPanel {
        private final Color c1, c2;
        public GradientPanel(Color a, Color b) { c1 = a; c2 = b; setOpaque(false); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setPaint(new GradientPaint(0, 0, c1, getWidth(), getHeight(), c2));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
