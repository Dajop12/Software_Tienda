package vista;

import datos.GestorDatos;
import dominio.Venta;
import servicio.Bitacora;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/** Dashboard: KPIs + gráfica 7 días + últimas ventas + actividad. */
public class DashboardPanel extends JPanel {
    private final JPanel kpis = new JPanel(new GridLayout(1, 4, 12, 12));
    private final GraficaPanel grafica = new GraficaPanel();
    private final DefaultTableModel modeloVentas = new DefaultTableModel(
            new String[]{"Folio", "Fecha", "Vendedor", "Total"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final DefaultListModel<String> modeloLog = new DefaultListModel<>();

    public DashboardPanel() {
        setBackground(Tema.FONDO);
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        kpis.setOpaque(false);
        add(kpis, BorderLayout.NORTH);

        JPanel medio = new JPanel(new GridLayout(1, 2, 12, 12));
        medio.setOpaque(false);

        JPanel gCard = new JPanel(new BorderLayout());
        gCard.setBackground(Tema.TARJETA);
        gCard.setBorder(BorderFactory.createCompoundBorder(
                new Tema.RoundedBorder(Tema.BORDE, 14, 1),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        JLabel gt = new JLabel("Ventas últimos 7 días");
        gt.setFont(Tema.ETIQ); gt.setForeground(Tema.TEXTO);
        gCard.add(gt, BorderLayout.NORTH);
        grafica.setPreferredSize(new Dimension(0, 220));
        gCard.add(grafica, BorderLayout.CENTER);
        medio.add(gCard);

        JPanel dCard = new JPanel(new BorderLayout(0, 8));
        dCard.setBackground(Tema.TARJETA);
        dCard.setBorder(BorderFactory.createCompoundBorder(
                new Tema.RoundedBorder(Tema.BORDE, 14, 1),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        JLabel dt = new JLabel("Últimas ventas");
        dt.setFont(Tema.ETIQ); dt.setForeground(Tema.TEXTO);
        dCard.add(dt, BorderLayout.NORTH);
        JTable tabla = new JTable(modeloVentas);
        Tema.tabla(tabla);
        JScrollPane sp = new JScrollPane(tabla);
        Tema.scroll(sp);
        dCard.add(sp, BorderLayout.CENTER);
        medio.add(dCard);

        add(medio, BorderLayout.CENTER);

        JPanel logCard = new JPanel(new BorderLayout());
        logCard.setBackground(Tema.TARJETA);
        logCard.setBorder(BorderFactory.createCompoundBorder(
                new Tema.RoundedBorder(Tema.BORDE, 14, 1),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));
        logCard.setPreferredSize(new Dimension(0, 130));
        JLabel lt = new JLabel("Actividad reciente");
        lt.setFont(Tema.ETIQ); lt.setForeground(Tema.TEXTO);
        JList<String> log = new JList<>(modeloLog);
        log.setBackground(Tema.TARJETA);
        log.setForeground(Tema.TEXTO_SEC);
        log.setFont(new Font("Consolas", Font.PLAIN, 11));
        logCard.add(lt, BorderLayout.NORTH);
        logCard.add(new JScrollPane(log), BorderLayout.CENTER);
        add(logCard, BorderLayout.SOUTH);
    }

    public void refrescar() {
        GestorDatos g = GestorDatos.getInstancia();
        boolean jefe = servicio.Sesion.esJefe();
        kpis.removeAll();
        kpis.add(Tema.tarjetaKpi("💰", "VENTAS HOY", String.format("$%.2f", g.ventasHoy()),
                jefe ? "ganancia $" + String.format("%.2f", g.gananciaHoy()) : g.getVentas().size() + " ventas totales", Tema.VERDE));
        kpis.add(Tema.tarjetaKpi("📦", "PRODUCTOS", String.valueOf(g.getProductos().size()),
                jefe ? String.format("Inv. venta $%.0f / costo $%.0f", g.valorInventario(), g.valorInventarioCosto())
                        : String.format("Inventario $%.0f", g.valorInventario()), Tema.ACENTO));
        kpis.add(Tema.tarjetaKpi("⚠", "STOCK BAJO", String.valueOf(g.stockBajoCount()),
                "productos con < 5 piezas", g.stockBajoCount() > 0 ? Tema.AMARILLO : Tema.VERDE));
        kpis.add(Tema.tarjetaKpi("🤝", "FIADO", String.format("$%.0f", g.deudasTotales()),
                g.getClientes().size() + " clientes a crédito", g.deudasTotales() > 0 ? Tema.AMARILLO : Tema.VERDE));
        kpis.revalidate(); kpis.repaint();

        grafica.setDatos(g.ventasUltimos7Dias(), g.etiquetasUltimos7Dias());

        modeloVentas.setRowCount(0);
        List<Venta> vs = g.getVentas();
        for (int i = vs.size() - 1; i >= 0 && modeloVentas.getRowCount() < 6; i--) {
            Venta v = vs.get(i);
            modeloVentas.addRow(new Object[]{v.getFolio(), v.getFecha(), v.getVendedor(),
                    String.format("$%.2f", v.getTotal())});
        }
        modeloLog.clear();
        for (String s : Bitacora.ultimas(8)) modeloLog.addElement(s);
    }

    /** Gráfica de barras dibujada con Java2D. */
    static class GraficaPanel extends JPanel {
        private double[] datos = new double[7];
        private String[] etiquetas = new String[7];
        GraficaPanel() { setBackground(Tema.TARJETA); }
        void setDatos(double[] d, String[] e) { datos = d; etiquetas = e; repaint(); }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            double max = 1;
            for (double v : datos) max = Math.max(max, v);
            int n = datos.length, w = getWidth(), h = getHeight();
            int base = h - 28, top = 10;
            // rejilla horizontal: lectura rápida de magnitudes
            g2.setColor(new Color(52, 58, 74, 90));
            for (int li = 1; li <= 3; li++) {
                int y = top + (base - top) * li / 4;
                g2.drawLine(16, y, w - 16, y);
            }
            // estado vacío ilustrado en texto si no hay ventas
            boolean vacia = true;
            for (double v : datos) if (v > 0) { vacia = false; break; }
            if (vacia) {
                g2.setColor(Tema.TEXTO_SEC);
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                String m = "Sin ventas esta semana — cobra en Punto de venta";
                int tw = g2.getFontMetrics().stringWidth(m);
                g2.drawString(m, (w - tw) / 2, h / 2);
                g2.dispose();
                return;
            }
            int anchoBarra = Math.max(18, (w - 40) / n - 16);
            for (int i = 0; i < n; i++) {
                int x = 20 + i * ((w - 40) / n) + (((w - 40) / n) - anchoBarra) / 2;
                int alto = Math.max(4, (int) ((h - top - 34) * (datos[i] / max)));
                int y = base - alto;
                // degradado por barra: profundidad sin costo
                GradientPaint gp = new GradientPaint(x, y, new Color(96, 165, 250),
                        x, y + alto, new Color(59, 130, 255));
                g2.setPaint(gp);
                g2.fillRoundRect(x, y, anchoBarra, alto, 8, 8);
                g2.setColor(Tema.TEXTO_SEC);
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                String et = etiquetas[i] == null ? "" : etiquetas[i];
                g2.drawString(et, x, h - 12);
                if (datos[i] > 0) {
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                    g2.drawString(String.format("%.0f", datos[i]), x, y - 4);
                }
            }
            g2.dispose();
        }
    }
}
