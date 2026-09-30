package servicio;

import datos.GestorDatos;
import dominio.Usuario;

/**
 * AuthService: login con intentos y bloqueo persistente (F6 sobrevive reinicios).
 */
public class AuthService {
    private static final AuthService INST = new AuthService();
    public static AuthService get() { return INST; }

    public static final int MAX_INTENTOS = 5;
    public static final int SEG_BLOQUEO = 60;

    private int intentosRestantes = MAX_INTENTOS;
    private long bloqueoHasta = 0;

    private AuthService() { cargarBloqueo(); }

    private void cargarBloqueo() {
        try {
            java.nio.file.Path p = java.nio.file.Paths.get("data/auth-bloqueo.txt");
            if (!java.nio.file.Files.exists(p)) return;
            String[] v = String.join("", java.nio.file.Files.readAllLines(p)).split(";");
            if (v.length == 2) { intentosRestantes = Integer.parseInt(v[0]); bloqueoHasta = Long.parseLong(v[1]); }
        } catch (Exception ignored) {}
    }

    private void guardarBloqueo() {
        try {
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get("data"));
            java.nio.file.Files.writeString(java.nio.file.Paths.get("data/auth-bloqueo.txt"),
                    intentosRestantes + ";" + bloqueoHasta);
        } catch (Exception ignored) {}
    }

    public static class Resultado {
        public final boolean ok;
        public final String mensaje;
        public final Usuario usuario;
        public final boolean debeCambiar; // F7: sigue con 1234
        public Resultado(boolean ok, String mensaje, Usuario u) {
            this(ok, mensaje, u, false);
        }
        public Resultado(boolean ok, String mensaje, Usuario u, boolean debeCambiar) {
            this.ok = ok; this.mensaje = mensaje; this.usuario = u; this.debeCambiar = debeCambiar;
        }
    }

    public boolean bloqueado() {
        return System.currentTimeMillis() < bloqueoHasta;
    }

    public long segundosRestantes() {
        long ms = bloqueoHasta - System.currentTimeMillis();
        return ms > 0 ? (ms + 999) / 1000 : 0;
    }

    public int getIntentos() { return intentosRestantes; }

    public Resultado login(String username, String pass) {
        if (bloqueado()) {
            return new Resultado(false, "⛔ Bloqueado. Espera " + segundosRestantes() + " s…", null);
        }
        String u = username == null ? "" : username.trim();
        String p = pass == null ? "" : pass;
        if (u.isEmpty() || p.isEmpty()) {
            return new Resultado(false, "⚠ Completa usuario y contraseña.", null);
        }
        Usuario encontrado = null;
        for (Usuario x : GestorDatos.getInstancia().getUsuarios()) {
            if (x.getUsername().equalsIgnoreCase(u)) { encontrado = x; break; }
        }
        if (encontrado == null || !encontrado.verificaPass(p)) {
            intentosRestantes--;
            guardarBloqueo();
            Bitacora.registrar("Login fallido: " + u);
            if (intentosRestantes <= 0) {
                bloqueoHasta = System.currentTimeMillis() + SEG_BLOQUEO * 1000L;
                intentosRestantes = MAX_INTENTOS;
                guardarBloqueo();
                return new Resultado(false, "⛔ Demasiados intentos. Bloqueado " + SEG_BLOQUEO + " s.", null);
            }
            return new Resultado(false, "✖ Datos incorrectos. Te quedan " + intentosRestantes + " intento(s).", null);
        }
        if (!encontrado.isActivo()) {
            return new Resultado(false, "⛔ Usuario desactivado. Contacta al admin.", null);
        }
        intentosRestantes = MAX_INTENTOS;
        guardarBloqueo();
        GestorDatos.getInstancia().guardarTodo(); // persiste migración hash v2 si aplicó
        Bitacora.registrar("Login OK: " + encontrado.getUsername());
        Sesion.setActual(encontrado); // F10: permisos UI
        boolean debe = encontrado.verificaPass("1234"); // F7: pass de fábrica
        return new Resultado(true, debe ? "⚠ Cambia tu contraseña 1234." : "✔ ¡Bienvenido, " + encontrado.getNombre() + "!",
                encontrado, debe);
    }

    /** F7: cambio obligado con mínimo 6. */
    public Resultado cambiarPass(Usuario u, String actual, String nueva) {
        if (u == null) return new Resultado(false, "Sin usuario.", null);
        if (!u.verificaPass(actual)) return new Resultado(false, "Actual incorrecta.", null);
        if (nueva == null || nueva.length() < 6) return new Resultado(false, "Mínimo 6 caracteres.", null);
        u.cambiarPass(nueva);
        GestorDatos.getInstancia().guardarTodo();
        Bitacora.registrar("Pass cambiada: " + u.getUsername());
        return new Resultado(true, "✔ Contraseña actualizada.", u);
    }
}
