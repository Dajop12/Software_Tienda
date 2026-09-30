package servicio;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * F4/F5: cola push al jefe + FCM real si hay FCM_SERVER_KEY.
 * - Venta llama a encolarAlertaStock() (ya integrado en GestorDatos).
 * - LOCAL: solo data/push-cola.log. FCM: además POST a FCM para app cerrada.
 */
public class PushService {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static synchronized void encolar(String titulo, String detalle) {
        String linea = LocalDateTime.now().format(FMT) + " | " + titulo + " | " + detalle;
        try (PrintWriter pw = new PrintWriter(new FileWriter("data/push-cola.log", true), true)) {
            pw.println(linea);
        } catch (Exception ignored) {}
        Bitacora.registrar("Push: " + titulo + " — " + detalle);
        enviarFCMBestEffort(titulo, detalle); // F5: si hay key, llega con app cerrada
    }

    public static void encolarAlertaStock(String nombre, int stock, int stockMin) {
        encolar("STOCK_BAJO", nombre + " queda " + stock + " (mín " + stockMin + ")");
    }

    /** F12: push caducidad 1 vez/día (evita spam). Llamar al abrir app. */
    public static synchronized void alertarVencimientos(java.util.List<dominio.Producto> lista) {
        if (lista == null || lista.isEmpty()) return;
        try {
            java.nio.file.Path p = java.nio.file.Paths.get("data/push-vence-fecha.txt");
            String hoy = java.time.LocalDate.now().toString();
            if (java.nio.file.Files.exists(p)
                    && java.nio.file.Files.readString(p).trim().equals(hoy)) return;
            java.nio.file.Files.createDirectories(p.getParent());
            java.nio.file.Files.writeString(p, hoy);
        } catch (Exception ignored) {}
        for (dominio.Producto pr : lista) {
            encolar("VENCE", pr.getNombre() + " vence " + pr.getFechaVence() + " (stock " + pr.getStock() + ")");
        }
    }

    public static synchronized void registrarToken(String token) {
        if (token == null || token.isBlank()) return;
        try {
            Path p = Paths.get("data/push-tokens.txt");
            Files.createDirectories(p.getParent());
            List<String> cur = Files.exists(p) ? Files.readAllLines(p) : new ArrayList<>();
            if (!cur.contains(token.trim())) {
                cur.add(token.trim());
                Files.write(p, cur);
            }
        } catch (Exception ignored) {}
        Bitacora.registrar("Push token registrado");
    }

    public static synchronized List<String> pendientes(int n) {
        try {
            Path p = Paths.get("data/push-cola.log");
            if (!Files.exists(p)) return new ArrayList<>();
            List<String> todas = Files.readAllLines(p);
            int desde = Math.max(0, todas.size() - n);
            return new ArrayList<>(todas.subList(desde, todas.size()));
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /** Stub FCM: cuando tengas server-key, implementa POST aquí. Firma ya lista. */
    public static String payloadFCM(String titulo, String detalle) {
        return "{\"to\":\"<token-jefe>\",\"notification\":{\"title\":\"" + esc(titulo)
                + "\",\"body\":\"" + esc(detalle) + "\"}}";
    }

    /** F5: envía a todos los tokens registrados. Best-effort, nunca rompe la venta. */
    private static void enviarFCMBestEffort(String titulo, String detalle) {
        String key = System.getenv("FCM_SERVER_KEY");
        if (key == null || key.isBlank()) return;
        try {
            Path p = Paths.get("data/push-tokens.txt");
            if (!Files.exists(p)) return;
            java.net.http.HttpClient http = java.net.http.HttpClient.newHttpClient();
            for (String token : Files.readAllLines(p)) {
                if (token.isBlank()) continue;
                String json = "{\"to\":\"" + esc(token.trim()) + "\",\"notification\":{\"title\":\""
                        + esc(titulo) + "\",\"body\":\"" + esc(detalle) + "\"}}";
                java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create("https://fcm.googleapis.com/fcm/send"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "key=" + key)
                        .POST(java.net.http.HttpRequest.BodyPublishers.ofString(json))
                        .timeout(java.time.Duration.ofSeconds(8))
                        .build();
                http.sendAsync(req, java.net.http.HttpResponse.BodyHandlers.discarding());
            }
        } catch (Exception ignored) {}
    }

    private static String esc(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
