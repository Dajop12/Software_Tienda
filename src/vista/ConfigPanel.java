package vista;

import datos.GestorDatos;
import dominio.Producto;
import dominio.Venta;
import infraestructura.MiniApiServer;
import servicio.Bitacora;
import servicio.PushService;
import javax.swing.*;
import java.awt.*;
import java.io.FileWriter;
import java.io.PrintWriter;

/** Configuración: exportar CSV, restaurar demo, ver bitácora e info. */
public class ConfigPanel extends JPanel {
    private final MainFrame frame;
    private final JTextArea txtLog = new JTextArea();
    private final JLabel lblInfo = new JLabel();

    public ConfigPanel(MainFrame frame) {
        this.frame = frame;
        setBackground(Tema.FONDO);
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JPanel grid = new JPanel(new GridLayout(1, 2, 12, 12));
        grid.setOpaque(false);

        JPanel izq = new JPanel();
        izq.setLayout(new BoxLayout(izq, BoxLayout.Y_AXIS));
        izq.setBackground(Tema.TARJETA);
        izq.setBorder(BorderFactory.createCompoundBorder(
                new Tema.RoundedBorder(Tema.BORDE, 12, 1),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));
        JLabel t1 = new JLabel("Datos y respaldo");
        t1.setFont(Tema.ETIQ); t1.setForeground(Tema.TEXTO);
        t1.setAlignmentX(Component.LEFT_ALIGNMENT);
        izq.add(t1);
        izq.add(Box.createVerticalStrut(8));
        lblInfo.setForeground(Tema.TEXTO_SEC);
        lblInfo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblInfo.setAlignmentX(Component.LEFT_ALIGNMENT);
        izq.add(lblInfo);
        izq.add(Box.createVerticalStrut(12));

        JButton bExpProd = new JButton("⬇ Exportar productos CSV");
        JButton bExpVent = new JButton("⬇ Exportar ventas CSV");
        JButton bExpGan = new JButton("⬇ Ganancia por producto CSV");
        JButton bExpAud = new JButton("⬇ Auditoría CSV");
        JButton bReset = new JButton("⟲ Restaurar datos demo");
        for (JButton b : new JButton[]{bExpProd, bExpVent, bExpGan, bExpAud, bReset}) {
            Tema.botonSecundario(b);
            b.setAlignmentX(Component.LEFT_ALIGNMENT);
            b.setMaximumSize(new Dimension(Short.MAX_VALUE, 40));
            izq.add(b);
            izq.add(Box.createVerticalStrut(8));
        }
        bExpProd.addActionListener(e -> exportarProductos());
        bExpVent.addActionListener(e -> exportarVentas());
        bExpGan.addActionListener(e -> exportarGanancias());
        bExpGan.setEnabled(servicio.Sesion.esJefe());
        bExpGan.setToolTipText(servicio.Sesion.esJefe() ? "Reporte de rentabilidad" : "Solo JEFE");
        bExpAud.addActionListener(e -> {
            try (PrintWriter pw = new PrintWriter(new FileWriter("data/auditoria-export.csv"))) {
                pw.println("fecha,usuario,modulo,accion,detalle");
                for (String[] r : servicio.Auditoria.ultimas(10000)) {
                    pw.printf("%s,%s,%s,%s,\"%s\"%n", r[0], r[1], r[2], r[3], r[4].replace("\"", "'"));
                }
                Toast.exito(this, "Auditoría en data/auditoria-export.csv");
            } catch (Exception ex) { Toast.error(this, "Falló: " + ex.getMessage()); }
        });
        bReset.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(this, "¿Borrar todo y volver a los datos demo?",
                    "Confirmar", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
            GestorDatos.getInstancia().restaurarDemo();
            frame.mostrar("DASH");
            JOptionPane.showMessageDialog(this, "Datos restaurados.", "OK", JOptionPane.INFORMATION_MESSAGE);
        });
        JLabel tNube = new JLabel("Nube y push jefe (F4)");
        tNube.setFont(Tema.ETIQ); tNube.setForeground(Tema.TEXTO);
        tNube.setAlignmentX(Component.LEFT_ALIGNMENT);
        izq.add(tNube);
        izq.add(Box.createVerticalStrut(6));
        JButton bApi = new JButton("▶ Iniciar API :8080");
        JButton bPush = new JButton("Ver cola push");
        JButton bBack = new JButton("🔒 Backup cifrado");
        JButton bRest = new JButton("↩ Restaurar backup");
        JButton bSyncE = new JButton("⇄ Sync exportar (USB)");
        JButton bSyncI = new JButton("⇄ Sync importar (USB)");
        JButton bRep = new JButton("📊 Reporte día (imprimir)");
        for (JButton b : new JButton[]{bApi, bPush, bBack, bRest, bSyncE, bSyncI, bRep}) {
            Tema.botonSecundario(b);
            b.setAlignmentX(Component.LEFT_ALIGNMENT);
            b.setMaximumSize(new Dimension(Short.MAX_VALUE, 40));
            izq.add(b);
            izq.add(Box.createVerticalStrut(8));
        }
        bApi.addActionListener(e -> {
            try {
                if (MiniApiServer.corriendo()) { MiniApiServer.detener(); Toast.info(this, "API detenida."); }
                else { String url = MiniApiServer.iniciar(8080); Toast.exito(this, "API en " + url + " (APK jefe)"); }
            } catch (Exception ex) { Toast.error(this, "No se pudo iniciar API: " + ex.getMessage()); }
            refrescar();
        });
        bPush.addActionListener(e -> {
            java.util.List<String> pen = PushService.pendientes(20);
            JOptionPane.showMessageDialog(this, pen.isEmpty() ? "Sin alertas en cola." : String.join("\n", pen),
                    "Cola push (outbox FCM)", JOptionPane.PLAIN_MESSAGE);
        });
        bBack.addActionListener(e -> {
            try { Toast.exito(this, "Backup: " + servicio.BackupService.crearBackup()); }
            catch (Exception ex) { Toast.error(this, "Backup falló: " + ex.getMessage()); }
        });
        bRest.addActionListener(e -> {
            java.util.List<String> l = servicio.BackupService.listar();
            if (l.isEmpty()) { Toast.info(this, "Sin backups en backup/."); return; }
            String sel = (String) JOptionPane.showInputDialog(this, "Elige:", "Restaurar",
                    JOptionPane.PLAIN_MESSAGE, null, l.toArray(), l.get(l.size() - 1));
            if (sel == null) return;
            if (JOptionPane.showConfirmDialog(this, "Sobrescribe data/ actual. ¿Seguro?",
                    "Confirmar", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
            try { servicio.BackupService.restaurar(sel); Toast.exito(this, "Restaurado. Reinicia la app."); }
            catch (Exception ex) { Toast.error(this, "Falló: " + ex.getMessage()); }
        });
        bSyncE.addActionListener(e -> {
            try { Toast.exito(this, "Sync: " + servicio.SyncService.exportar() + " (pásalo por USB)"); }
            catch (Exception ex) { Toast.error(this, "Falló: " + ex.getMessage()); }
        });
        bSyncI.addActionListener(e -> {
            JFileChooser fc = new JFileChooser("sync");
            if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
            try { Toast.exito(this, "Importado: " + servicio.SyncService.importar(fc.getSelectedFile().getPath())); frame.mostrar("DASH"); }
            catch (Exception ex) { Toast.error(this, "Falló: " + ex.getMessage()); }
        });
        bRep.addActionListener(e -> {
            TicketPrinter.mostrarTicket(this, "Reporte del día",
                    GestorDatos.getInstancia().reporteDiaTicket());
        });
        grid.add(izq);

        JPanel der = new JPanel(new BorderLayout(0, 8));
        der.setBackground(Tema.TARJETA);
        der.setBorder(BorderFactory.createCompoundBorder(
                new Tema.RoundedBorder(Tema.BORDE, 12, 1),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));
        JLabel t2 = new JLabel("Bitácora de actividad");
        t2.setFont(Tema.ETIQ); t2.setForeground(Tema.TEXTO);
        txtLog.setEditable(false);
        txtLog.setBackground(Tema.PANEL);
        txtLog.setForeground(Tema.TEXTO_SEC);
        txtLog.setFont(new Font("Consolas", Font.PLAIN, 11));
        der.add(t2, BorderLayout.NORTH);
        der.add(new JScrollPane(txtLog), BorderLayout.CENTER);
        grid.add(der);

        add(grid, BorderLayout.CENTER);

        JLabel about = new JLabel("Super App v2.0 • Java Swing • Guarda en data/*.dat • Hecho para aprender POO y MVC",
                SwingConstants.CENTER);
        about.setForeground(Tema.TEXTO_SEC);
        about.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        add(about, BorderLayout.SOUTH);
    }

    public void refrescar() {
        GestorDatos g = GestorDatos.getInstancia();
        lblInfo.setText("<html>Usuarios: " + g.getUsuarios().size()
                + " &nbsp;•&nbsp; Productos: " + g.getProductos().size()
                + " &nbsp;•&nbsp; Ventas: " + g.getVentas().size()
                + "<br>API: " + (MiniApiServer.corriendo() ? "🟢 :8080 para APK" : "🔴 apagada")
                + " &nbsp;•&nbsp; Push en cola: " + PushService.pendientes(1000).size()
                + "<br>API key APK: <b>" + MiniApiServer.apiKey().substring(0, 8) + "…</b> (usa ?key=COMPLETA)"
                + " &nbsp;•&nbsp; Ganancia hoy: <b>$" + String.format("%.2f", g.gananciaHoy()) + "</b>"
                + "<br>Archivos en carpeta <b>data/</b><br>Java: " + System.getProperty("java.version") + "</html>");
        StringBuilder sb = new StringBuilder();
        for (String s : Bitacora.todas()) sb.append(s).append("\n");
        txtLog.setText(sb.toString());
        txtLog.setCaretPosition(0);
    }

    private void exportarProductos() {
        String ruta = "data/productos.csv";
        try (PrintWriter pw = new PrintWriter(new FileWriter(ruta))) {
            pw.println("id,nombre,categoria,precio,stock,total");
            for (Producto p : GestorDatos.getInstancia().getProductos()) {
                pw.printf("%s,\"%s\",\"%s\",%.2f,%d,%.2f%n",
                        p.getId(), p.getNombre(), p.getCategoria(), p.getPrecio(), p.getStock(), p.getValorTotal());
            }
            Bitacora.registrar("Exportó productos a " + ruta);
            JOptionPane.showMessageDialog(this, "Guardado en " + ruta, "OK", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportarVentas() {
        String ruta = "data/ventas.csv";
        try (PrintWriter pw = new PrintWriter(new FileWriter(ruta))) {
            pw.println("folio,fecha,vendedor,tipo,cliente,total,ganancia,items");
            for (Venta v : GestorDatos.getInstancia().getVentas()) {
                pw.printf("%s,%s,%s,%s,%s,%.2f,%.2f,%d%n", v.getId(), v.getFecha(), v.getVendedor(),
                        v.getTipoPago(), v.getClienteId(), v.getTotal(), v.getGanancia(), v.getItems().size());
            }
            Bitacora.registrar("Exportó ventas a " + ruta);
            JOptionPane.showMessageDialog(this, "Guardado en " + ruta, "OK", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportarGanancias() {
        String ruta = "data/ganancia-producto.csv";
        try (PrintWriter pw = new PrintWriter(new FileWriter(ruta))) {
            pw.println("id,nombre,unidades,venta,ganancia");
            for (String[] r : GestorDatos.getInstancia().gananciaPorProducto()) {
                pw.printf("%s,\"%s\",%s,%s,%s%n", r[0], r[1], r[2], r[3], r[4]);
            }
            Bitacora.registrar("Exportó ganancias a " + ruta);
            JOptionPane.showMessageDialog(this, "Guardado en " + ruta + "\nHoy ganancia: $"
                    + String.format("%.2f", GestorDatos.getInstancia().gananciaHoy()),
                    "OK", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
