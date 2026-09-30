package vista;

import javax.swing.*;
import java.awt.*;

/**
 * Estado vacío enterprise: ilustración + título + subtítulo + acción opcional.
 * Evita tablas vacías frías; guía al usuario al siguiente paso.
 */
public class EmptyState extends JPanel {
    public EmptyState(Ilustracion.Tipo tipo, String titulo, String subtitulo, String textoBoton, Runnable accion) {
        setOpaque(false);
        setLayout(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.anchor = GridBagConstraints.CENTER;
        g.insets = new Insets(4, 0, 4, 0);

        g.gridy = 0; add(new Ilustracion(tipo, 96), g);
        JLabel t = new JLabel(titulo, SwingConstants.CENTER);
        t.setFont(new Font("Segoe UI", Font.BOLD, 15));
        t.setForeground(Tema.TEXTO);
        g.gridy = 1; g.insets = new Insets(10, 0, 2, 0); add(t, g);
        JLabel s = new JLabel("<html><div style='text-align:center'>" + subtitulo + "</div></html>", SwingConstants.CENTER);
        s.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        s.setForeground(Tema.TEXTO_SEC);
        g.gridy = 2; g.insets = new Insets(0, 0, 10, 0); add(s, g);
        if (textoBoton != null && accion != null) {
            JButton b = new JButton(textoBoton);
            Tema.botonPrimario(b);
            b.setPreferredSize(new Dimension(200, 38));
            b.addActionListener(e -> accion.run());
            g.gridy = 3; add(b, g);
        }
    }
}
