package vista;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Tema oscuro centralizado — Design System nivel enterprise.
 * Tokens + estados (hover/pressed/disabled/focus) + accesibilidad.
 * Mantiene la API anterior para no romper paneles existentes.
 */
public class Tema {
    // ---- Tokens de color (contraste AA sobre fondo oscuro) ----
    public static final Color FONDO = new Color(15, 17, 23);
    public static final Color PANEL = new Color(24, 27, 35);
    public static final Color TARJETA = new Color(30, 34, 46);
    public static final Color TARJETA_HOVER = new Color(37, 42, 57);
    public static final Color BORDE = new Color(52, 58, 74);
    public static final Color BORDE_FOCO = new Color(82, 141, 255);
    public static final Color TEXTO = new Color(235, 237, 245);
    public static final Color TEXTO_SEC = new Color(160, 168, 186);
    public static final Color ACENTO = new Color(59, 130, 255);
    public static final Color ACENTO_HOVER = new Color(42, 110, 235);
    public static final Color ACENTO_PRESSED = new Color(30, 92, 210);
    public static final Color VERDE = new Color(52, 211, 132);
    public static final Color VERDE_BG = new Color(52, 211, 132, 22);
    public static final Color ROJO = new Color(255, 107, 107);
    public static final Color ROJO_BG = new Color(255, 107, 107, 20);
    public static final Color AMARILLO = new Color(251, 191, 36);
    public static final Color AMARILLO_BG = new Color(251, 191, 36, 18);
    public static final Color INFO_BG = new Color(59, 130, 255, 18);

    // ---- Espaciado / radio ----
    public static final int R_SM = 8, R_MD = 12, R_LG = 16;
    public static final int S_XS = 6, S_SM = 10, S_MD = 16, S_LG = 24;

    public static final Font TITULO = new Font("Segoe UI", Font.BOLD, 24);
    public static final Font SUB = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font ETIQ = new Font("Segoe UI", Font.BOLD, 12);
    public static final Font CAMPO = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font BOTON = new Font("Segoe UI", Font.BOLD, 13);

    public static void botonPrimario(JButton b) {
        if (b.getClientProperty("tema-primario") != null) return;
        b.putClientProperty("tema-primario", true);
        b.setFont(BOTON);
        b.setForeground(Color.WHITE);
        b.setBackground(ACENTO);
        b.setFocusPainted(false);
        b.setFocusable(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new RoundedBorder(ACENTO, R_MD, 0));
        b.setPreferredSize(new Dimension(0, 42));
        // Estado deshabilitado legible
        b.setDisabledIcon(null);
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                if (b.isEnabled()) { b.setBackground(ACENTO_HOVER); b.setBorder(new RoundedBorder(ACENTO_HOVER, R_MD, 0)); }
            }
            @Override public void mouseExited(MouseEvent e) {
                if (b.isEnabled()) { b.setBackground(ACENTO); b.setBorder(new RoundedBorder(ACENTO, R_MD, 0)); }
            }
            @Override public void mousePressed(MouseEvent e) {
                if (b.isEnabled()) { b.setBackground(ACENTO_PRESSED); b.setBorder(new RoundedBorder(ACENTO_PRESSED, R_MD, 0)); }
            }
            @Override public void mouseReleased(MouseEvent e) {
                if (b.isEnabled()) { b.setBackground(ACENTO_HOVER); b.setBorder(new RoundedBorder(ACENTO_HOVER, R_MD, 0)); }
            }
        });
        b.addPropertyChangeListener("enabled", e -> {
            if (!b.isEnabled()) { b.setBackground(new Color(55, 60, 76)); b.setForeground(TEXTO_SEC); }
            else { b.setBackground(ACENTO); b.setForeground(Color.WHITE); }
        });
    }

    public static void botonSecundario(JButton b) {
        if (b.getClientProperty("tema-sec") != null) return;
        b.putClientProperty("tema-sec", true);
        b.setFont(BOTON);
        b.setForeground(TEXTO);
        b.setBackground(new Color(45, 50, 65));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new RoundedBorder(new Color(45, 50, 65), R_MD, 0));
        b.setPreferredSize(new Dimension(0, 38));
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { if (b.isEnabled()) b.setBackground(TARJETA_HOVER); }
            @Override public void mouseExited(MouseEvent e) { if (b.isEnabled()) b.setBackground(new Color(45, 50, 65)); }
            @Override public void mousePressed(MouseEvent e) { if (b.isEnabled()) b.setBackground(new Color(52, 58, 74)); }
        });
    }

    /** Botón de peligro (eliminar) con affordance clara. */
    public static void botonPeligro(JButton b) {
        botonSecundario(b);
        b.setForeground(ROJO);
    }

    public static void botonSidebar(JButton b, boolean activo) {
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setFocusPainted(true); // accesible por teclado
        b.setFocusable(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        b.setOpaque(true);
        if (activo) {
            b.setBackground(ACENTO);
            b.setForeground(Color.WHITE);
        } else {
            b.setBackground(PANEL);
            b.setForeground(TEXTO_SEC);
        }
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
        t.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        t.setSelectionBackground(new Color(59, 130, 255, 70));
        t.setSelectionForeground(Color.WHITE);
        t.setFillsViewportHeight(true);
        t.setFocusable(true);
        JTableHeader h = t.getTableHeader();
        h.setBackground(new Color(21, 24, 32));
        h.setForeground(TEXTO_SEC);
        h.setFont(new Font("Segoe UI", Font.BOLD, 11));
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
        l.setFont(new Font("Segoe UI", Font.BOLD, 11));
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
        l.setFont(new Font("Segoe UI", Font.BOLD, 13));
        l.setForeground(Color.WHITE);
        l.setBackground(ACENTO);
        l.setOpaque(true);
        l.setPreferredSize(new Dimension(38, 38));
        l.setBorder(new RoundedBorder(ACENTO, 19, 0));
        return l;
    }

    /** Aplica look oscuro a dialogs/tooltips para coherencia. Llamar una vez al arrancar. */
    public static void initGlobales() {
        UIManager.put("ToolTip.background", new Color(30, 34, 46));
        UIManager.put("ToolTip.foreground", TEXTO);
        UIManager.put("ToolTip.border", BorderFactory.createLineBorder(BORDE));
        UIManager.put("OptionPane.background", PANEL);
        UIManager.put("OptionPane.messageForeground", TEXTO);
        UIManager.put("Panel.background", PANEL);
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
        ic.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        ic.setForeground(acento);
        JLabel t = new JLabel("  " + titulo);
        t.setFont(new Font("Segoe UI", Font.BOLD, 11));
        t.setForeground(TEXTO_SEC);
        top.add(ic, BorderLayout.WEST);
        top.add(t, BorderLayout.CENTER);
        JLabel v = new JLabel(valor);
        v.setFont(new Font("Segoe UI", Font.BOLD, 26));
        v.setForeground(Color.WHITE);
        JLabel d = new JLabel(detalle);
        d.setFont(new Font("Segoe UI", Font.PLAIN, 12));
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
