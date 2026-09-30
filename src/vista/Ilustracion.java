package vista;

import javax.swing.*;
import java.awt.*;

/**
 * Ilustración vectorial liviana (Java2D, sin assets externos).
 * Patrón enterprise: blob suave + glifo + decoraciones.
 */
public class Ilustracion extends JPanel {
    public enum Tipo {
        LOGIN_HERO("◉", new Color(59, 130, 255), new Color(139, 92, 246)),
        EMPTY_PRODUCTOS("📦", new Color(59, 130, 255), new Color(52, 211, 132)),
        EMPTY_VENTAS("🛒", new Color(251, 191, 36), new Color(59, 130, 255)),
        EMPTY_ALUMNOS("🎓", new Color(139, 92, 246), new Color(59, 130, 255)),
        EMPTY_USUARIOS("👥", new Color(52, 211, 132), new Color(59, 130, 255)),
        SEGURIDAD("🛡", new Color(52, 211, 132), new Color(59, 130, 255));

        final String glifo; final Color c1, c2;
        Tipo(String g, Color a, Color b) { glifo = g; c1 = a; c2 = b; }
    }

    private final Tipo tipo;
    private final int tamano;

    public Ilustracion(Tipo tipo) { this(tipo, 120); }
    public Ilustracion(Tipo tipo, int tamano) {
        this.tipo = tipo; this.tamano = tamano;
        setOpaque(false);
        setPreferredSize(new Dimension(tamano + 40, tamano + 20));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        int cx = w / 2, cy = h / 2 - 4;
        int r = tamano / 2;

        // blob trasero (degradado suave con transparencia)
        g2.setColor(new Color(tipo.c2.getRed(), tipo.c2.getGreen(), tipo.c2.getBlue(), 38));
        g2.fillOval(cx - r - 14, cy - r - 6, (r + 14) * 2, (r + 6) * 2);
        g2.setColor(new Color(tipo.c1.getRed(), tipo.c1.getGreen(), tipo.c1.getBlue(), 46));
        g2.fillOval(cx - r, cy - r + 6, r * 2, r * 2);

        // círculo central sólido
        GradientPaint gp = new GradientPaint(cx - r, cy - r, tipo.c1, cx + r, cy + r, tipo.c2);
        g2.setPaint(gp);
        g2.fillOval(cx - r, cy - r, r * 2, r * 2);

        // brillo superior
        g2.setColor(new Color(255, 255, 255, 46));
        g2.fillOval(cx - r + 12, cy - r + 10, r, r / 2);

        // glifo centrado
        g2.setColor(Color.WHITE);
        int fs = (int) (r * 0.95);
        g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, fs));
        FontMetrics fm = g2.getFontMetrics();
        int tx = cx - fm.stringWidth(tipo.glifo) / 2;
        int ty = cy + fm.getAscent() / 2 - 4;
        g2.drawString(tipo.glifo, tx, ty);

        // decoraciones: puntos + mini barras (estilo dashboard)
        g2.setColor(new Color(255, 255, 255, 90));
        g2.fillOval(cx + r - 6, cy - r + 4, 8, 8);
        g2.fillOval(cx - r - 2, cy + r - 16, 6, 6);
        g2.setColor(new Color(tipo.c1.getRed(), tipo.c1.getGreen(), tipo.c1.getBlue(), 130));
        int bw = 10;
        for (int i = 0; i < 3; i++) {
            int bh = 10 + i * 8;
            g2.fillRoundRect(cx - r - 22 + i * 14, cy + r + 8 - bh, bw, bh, 4, 4);
        }
        g2.dispose();
    }
}
