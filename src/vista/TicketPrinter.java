package vista;

import javax.swing.*;
import java.awt.*;
import java.awt.print.Printable;
import java.awt.print.PrinterJob;

/**
 * F10: imprime el ticket en impresora térmica o PDF (solo JDK).
 */
public class TicketPrinter {
    public static void imprimir(Component padre, String ticket) {
        try {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setJobName("Ticket venta");
            job.setPrintable((g, pf, idx) -> {
                if (idx > 0) return Printable.NO_SUCH_PAGE;
                Graphics2D g2 = (Graphics2D) g;
                g2.translate((int) pf.getImageableX(), (int) pf.getImageableY());
                g2.setFont(new Font("Monospaced", Font.PLAIN, 9));
                FontMetrics fm = g2.getFontMetrics();
                int y = fm.getAscent();
                for (String linea : ticket.split("\n")) {
                    // corta a 42 columnas (térmica 80mm)
                    while (linea.length() > 42) {
                        g2.drawString(linea.substring(0, 42), 0, y);
                        y += fm.getHeight();
                        linea = linea.substring(42);
                    }
                    g2.drawString(linea, 0, y);
                    y += fm.getHeight();
                }
                return Printable.PAGE_EXISTS;
            });
            if (job.printDialog()) job.print();
        } catch (Exception ex) {
            Toast.error(padre, "No se pudo imprimir: " + ex.getMessage());
        }
    }

    /** Diálogo ticket con Imprimir/Cerrar (reemplaza JOptionPane plano). */
    public static void mostrarTicket(Component padre, String titulo, String ticket) {
        JTextArea area = new JTextArea(ticket);
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane sp = new JScrollPane(area);
        sp.setPreferredSize(new Dimension(340, 300));
        Object[] ops = {"🖨 Imprimir", "Cerrar"};
        int op = JOptionPane.showOptionDialog(padre, sp, titulo,
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, ops, ops[1]);
        if (op == 0) imprimir(padre, ticket);
    }
}
