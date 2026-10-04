package vista;

import javax.swing.*;
import java.awt.*;

/**
 * Toast no bloqueante estilo enterprise (éxito / error / info).
 * No interrumpe el flujo como JOptionPane; se autodestruye.
 */
public class Toast {
    public enum Tipo { EXITO, ERROR, INFO }

    public static void show(java.awt.Component padre, String mensaje, Tipo tipo) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> show(padre, mensaje, tipo));
            return;
        }
        java.awt.Window win = padre instanceof java.awt.Window
                ? (java.awt.Window) padre
                : SwingUtilities.getWindowAncestor(padre);
        if (win == null) return;
        Color borde, fondo;
        String icono;
        switch (tipo) {
            case EXITO -> { borde = Tema.VERDE; fondo = new Color(22, 38, 32); icono = "✔ "; }
            case ERROR -> { borde = Tema.ROJO; fondo = new Color(42, 26, 28); icono = "✖ "; }
            default -> { borde = Tema.ACENTO; fondo = new Color(24, 34, 54); icono = "ℹ "; }
        }
        JWindow popup = new JWindow(win);
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setBackground(fondo);
        p.setBorder(BorderFactory.createCompoundBorder(
                new Tema.RoundedBorder(borde, 12, 1),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        JLabel l = new JLabel(icono + mensaje);
        l.setFont(Tema.fuente(Font.BOLD, 12));
        l.setForeground(Color.WHITE);
        p.add(l, BorderLayout.CENTER);
        popup.add(p);
        popup.pack();
        // abajo-centro del frame padre (si aún no es visible, centro de pantalla)
        int x, y;
        try {
            Point loc = win.getLocationOnScreen();
            x = loc.x + (win.getWidth() - popup.getWidth()) / 2;
            y = loc.y + win.getHeight() - popup.getHeight() - 48;
        } catch (IllegalComponentStateException e) {
            Dimension ss = Toolkit.getDefaultToolkit().getScreenSize();
            x = (ss.width - popup.getWidth()) / 2;
            y = ss.height - popup.getHeight() - 80;
        }
        popup.setLocation(Math.max(x, 0), Math.max(y, 0));
        popup.setVisible(true);
        try { popup.setOpacity(0f); } catch (Exception ignored) {}
        // fade-in + espera + fade-out (Timer, sin bloquear EDT)
        Timer in = new Timer(20, null);
        in.addActionListener(e -> {
            float op = popup.getOpacity() + 0.08f;
            if (op >= 1f) { op = 1f; in.stop();
                Timer espera = new Timer(2100, ev -> {
                    Timer out = new Timer(20, null);
                    out.addActionListener(ev2 -> {
                        float o2 = popup.getOpacity() - 0.08f;
                        if (o2 <= 0f) { out.stop(); popup.setVisible(false); popup.dispose(); }
                        else { try { popup.setOpacity(o2); } catch (Exception ignored) {} }
                    });
                    out.start();
                });
                espera.setRepeats(false); espera.start();
            }
            try { popup.setOpacity(op); } catch (Exception ignored) {}
        });
        in.start();
    }

    public static void exito(java.awt.Component p, String m) { show(p, m, Tipo.EXITO); }
    public static void error(java.awt.Component p, String m) { show(p, m, Tipo.ERROR); }
    public static void info(java.awt.Component p, String m) { show(p, m, Tipo.INFO); }
}
