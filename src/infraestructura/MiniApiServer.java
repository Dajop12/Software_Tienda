package infraestructura;

import com.sun.net.httpserver.HttpServer;
import datos.GestorDatos;
import dominio.Cliente;
import dominio.Producto;
import servicio.ApiAuth;
import servicio.PushService;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * F4-F7: mini API con solo JDK para APK jefe y PC web. F7 con roles.
 *  POST /api/login body user=..&pass=.. -> {"token","rol"} (12h)
 *  GET  /api/stock-bajo (VENDEDOR+)  GET /api/ventas-hoy (VENDEDOR+)
 *  GET  /api/deudas (solo JEFE/ADMIN)  POST /api/push-token (JEFE)
 * Auth: header X-API-TOKEN o ?token=... ; api.key legada = ADMIN.
 */
public class MiniApiServer {
    private static HttpServer server;
    private static int puerto = 8080;

    public static synchronized boolean corriendo() {
        return server != null;
    }

    public static synchronized int getPuerto() { return puerto; }

    public static synchronized String apiKey() {
        try {
            java.nio.file.Path p = java.nio.file.Paths.get("data/api.key");
            if (!java.nio.file.Files.exists(p)) {
                String k = java.util.UUID.randomUUID().toString().replace("-", "");
                java.nio.file.Files.createDirectories(p.getParent());
                java.nio.file.Files.writeString(p, k);
                return k;
            }
            return java.nio.file.Files.readString(p).trim();
        } catch (Exception e) {
            return "demo-key";
        }
    }

    private static boolean autorizado(com.sun.net.httpserver.HttpExchange ex) throws IOException {
        return rol(ex) != null;
    }

    /** Rol del llamante o null + 401. */
    private static String rol(com.sun.net.httpserver.HttpExchange ex) throws IOException {
        String q = ex.getRequestURI().getQuery();
        String token = ex.getRequestHeaders().getFirst("X-API-TOKEN");
        if (token == null && q != null) {
            for (String kv : q.split("&")) {
                if (kv.startsWith("token=")) token = kv.substring(6);
            }
        }
        String key = ex.getRequestHeaders().getFirst("X-API-KEY");
        if (key == null && q != null) {
            for (String kv : q.split("&")) {
                if (kv.startsWith("key=")) key = kv.substring(4);
            }
        }
        String rol = ApiAuth.rolDe(token, key, apiKey());
        if (rol == null) responder(ex, 401, "{\"error\":\"auth. POST /api/login o ?token=...\"}");
        return rol;
    }

    public static synchronized String iniciar(int p) throws IOException {
        if (server != null) return "http://localhost:" + puerto;
        puerto = p;
        apiKey(); // genera si falta
        server = HttpServer.create(new InetSocketAddress(puerto), 0);
        server.createContext("/api/salud", ex -> responder(ex, "{\"ok\":true}"));
        server.createContext("/api/login", ex -> {
            if (!"POST".equals(ex.getRequestMethod())) { responder(ex, 405, "{\"error\":\"POST user=..&pass=..\"}"); return; }
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String user = "", pass = "";
            for (String kv : body.split("&")) {
                if (kv.startsWith("user=")) user = kv.substring(5);
                if (kv.startsWith("pass=")) pass = kv.substring(5);
            }
            String r = ApiAuth.login(user, pass);
            if (r == null) { responder(ex, 401, "{\"error\":\"credenciales\"}"); return; }
            String[] t = r.split("\\|");
            responder(ex, "{\"token\":\"" + t[0] + "\",\"rol\":\"" + t[1] + "\"}");
        });
        server.createContext("/api/stock-bajo", ex -> {
            if (!"GET".equals(ex.getRequestMethod())) { responder(ex, 405, "{\"error\":\"GET\"}"); return; }
            String rol = rol(ex);
            if (rol == null) return;
            if (!ApiAuth.puedeVender(rol)) { responder(ex, 403, "{\"error\":\"solo VENDEDOR+\"}"); return; }
            List<Producto> bajos = GestorDatos.getInstancia().productosStockBajo();
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < bajos.size(); i++) {
                Producto pr = bajos.get(i);
                if (i > 0) sb.append(",");
                sb.append("{\"id\":\"").append(esc(pr.getId())).append("\",\"nombre\":\"").append(esc(pr.getNombre()))
                        .append("\",\"stock\":").append(pr.getStock()).append(",\"stockMin\":").append(pr.getStockMin()).append("}");
            }
            responder(ex, sb.append("]").toString());
        });
        server.createContext("/api/ventas-hoy", ex -> {
            String rol = rol(ex);
            if (rol == null) return;
            if (!ApiAuth.puedeVender(rol)) { responder(ex, 403, "{\"error\":\"solo VENDEDOR+\"}"); return;
            }
            responder(ex,
                "{\"ventas\":" + GestorDatos.getInstancia().ventasHoy()
                + ",\"ganancia\":" + GestorDatos.getInstancia().gananciaHoy()
                + ",\"deudas\":" + GestorDatos.getInstancia().deudasTotales() + "}"); });
        server.createContext("/api/deudas", ex -> {
            String rol = rol(ex);
            if (rol == null) return;
            if (!ApiAuth.esJefe(rol)) { responder(ex, 403, "{\"error\":\"solo JEFE\"}"); return; }
            StringBuilder sb = new StringBuilder("{\"total\":"
                    + GestorDatos.getInstancia().deudasTotales() + ",\"clientes\":[");
            List<Cliente> cl = GestorDatos.getInstancia().getClientes();
            boolean primero = true;
            for (Cliente c : cl) {
                if (c.getDeuda() <= 0) continue;
                if (!primero) sb.append(",");
                primero = false;
                sb.append("{\"id\":\"").append(esc(c.getId())).append("\",\"nombre\":\"").append(esc(c.getNombre()))
                        .append("\",\"deuda\":").append(c.getDeuda()).append("}");
            }
            responder(ex, sb.append("]}").toString());
        });
        server.createContext("/api/vencimientos", ex -> {
            if (!"GET".equals(ex.getRequestMethod())) { responder(ex, 405, "{\"error\":\"GET ?dias=30\"}"); return; }
            String rol = rol(ex);
            if (rol == null) return;
            if (!ApiAuth.puedeVender(rol)) { responder(ex, 403, "{\"error\":\"solo VENDEDOR+\"}"); return; }
            int dias = 30;
            try {
                String q = ex.getRequestURI().getQuery();
                if (q != null) for (String kv : q.split("&")) {
                    if (kv.startsWith("dias=")) dias = Integer.parseInt(kv.substring(5));
                }
            } catch (Exception ignored) {}
            StringBuilder sb = new StringBuilder("[");
            var lista = GestorDatos.getInstancia().proximosAVencer(dias);
            for (int i = 0; i < lista.size(); i++) {
                Producto pr = lista.get(i);
                if (i > 0) sb.append(",");
                long rest;
                try {
                    rest = java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(),
                            java.time.LocalDate.parse(pr.getFechaVence().trim()));
                } catch (Exception e) { rest = 0; }
                sb.append("{\"id\":\"").append(esc(pr.getId())).append("\",\"nombre\":\"").append(esc(pr.getNombre()))
                        .append("\",\"stock\":").append(pr.getStock())
                        .append(",\"vence\":\"").append(esc(pr.getFechaVence()))
                        .append("\",\"dias\":").append(rest).append("}");
            }
            responder(ex, sb.append("]").toString());
        });
        server.createContext("/api/push-token", ex -> {
            if (!"POST".equals(ex.getRequestMethod())) { responder(ex, 405, "{\"error\":\"POST token=XXX\"}"); return; }
            String rol = rol(ex);
            if (rol == null) return;
            if (!ApiAuth.esJefe(rol)) { responder(ex, 403, "{\"error\":\"solo JEFE\"}"); return; }
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String token = body.startsWith("token=") ? body.substring(6).trim() : body.trim();
            PushService.registrarToken(token);
            responder(ex, "{\"ok\":true}");
        });
        server.createContext("/api/reporte", ex -> {
            if (!"GET".equals(ex.getRequestMethod())) { responder(ex, 405, "{\"error\":\"GET\"}"); return; }
            String rol = rol(ex);
            if (rol == null) return;
            if (!ApiAuth.puedeVender(rol)) { responder(ex, 403, "{\"error\":\"solo VENDEDOR+\"}"); return; }
            responder(ex, GestorDatos.getInstancia().reporteDiaJSON(ApiAuth.esJefe(rol)));
        });
        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(4));
        server.start();
        return "http://localhost:" + puerto;
    }

    public static synchronized void detener() {
        if (server != null) { server.stop(0); server = null; }
    }

    private static void responder(com.sun.net.httpserver.HttpExchange ex, String json) throws IOException {
        responder(ex, 200, json);
    }

    private static void responder(com.sun.net.httpserver.HttpExchange ex, int codigo, String json) throws IOException {
        byte[] b = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        ex.sendResponseHeaders(codigo, b.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(b); }
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
