import datos.GestorDatos;
import vista.LoginFrame;
import javax.swing.*;

/**
 * Super App v2.0 — Punto de entrada.
 * Arquitectura MVC por capas:
 *  dominio/  -> entidades puras (Usuario, Producto, Venta, Cliente, Turno)
 *  datos/    -> GestorDatos (archivos data/*.dat)
 *  servicio/ -> AuthService (login), Bitacora (log)
 *  vista/    -> Tema + Login + Main + 6 paneles
 *
 * Para correr (incluye lib/ por FlatLaf):
 *   javac -encoding UTF-8 -cp "lib/*" -d bin (Get-ChildItem -Recurse -Filter *.java -Path src).FullName
 *   java -cp "bin;lib/*" App
 */
public class App {
    public static void main(String[] args) {
        // Framework de decoración: FlatLaf moderno (lib/flatlaf-*.jar).
        // Si falta el JAR, usa el look del sistema como respaldo.
        try {
            com.formdev.flatlaf.FlatDarkLaf.setup();
        } catch (Exception | NoClassDefFoundError e) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
        }
        vista.Tema.initGlobales();
        // Inicializa datos (crea demo si es primera vez)
        GestorDatos.getInstancia();
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
