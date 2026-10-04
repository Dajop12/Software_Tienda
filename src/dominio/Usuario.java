package dominio;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Usuario. Rol: ADMIN (todo/JEFE), VENDEDOR (ventas+productos), CONSULTA (solo ver).
 * PBKDF2 con salt e iteraciones versionadas; migra hashes anteriores al validar.
 */
public class Usuario implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String nombre;
    private String username;
    private String passHash; // v3$iteraciones$saltHex$hashHex, v2 legacy o SHA-256 legado
    private String rol;
    private boolean activo;

    public Usuario(String id, String nombre, String username, String passPlano, String rol, boolean activo) {
        this.id = id;
        this.nombre = nombre;
        this.username = username;
        this.passHash = hashPassword(passPlano);
        this.rol = rol;
        this.activo = activo;
    }

    private static final int ITERACIONES_ACTUALES = 310_000;
    private static final int ITERACIONES_V2 = 21_000;

    /** Nuevo hash PBKDF2-HMAC-SHA256 con salt aleatorio. */
    public static String hashPassword(String plano) {
        if (plano == null) throw new IllegalArgumentException("La contraseña no puede ser null.");
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        try {
            byte[] hash = derivar(plano.toCharArray(), salt, ITERACIONES_ACTUALES);
            return "v3$" + ITERACIONES_ACTUALES + "$" + hex(salt) + "$" + hex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo proteger la contraseña con PBKDF2.", e);
        }
    }

    public boolean verificaPass(String plano) {
        if (plano == null || passHash == null) return false;
        if (passHash.startsWith("v3$") || passHash.startsWith("v2$")) {
            try {
                String[] p = passHash.split("\\$");
                int iteraciones;
                String saltHex;
                String hashHex;
                if (p.length == 4 && "v3".equals(p[0])) {
                    iteraciones = Integer.parseInt(p[1]);
                    if (iteraciones < ITERACIONES_V2 || iteraciones > 2_000_000) return false;
                    saltHex = p[2];
                    hashHex = p[3];
                } else if (p.length == 3 && "v2".equals(p[0])) {
                    iteraciones = ITERACIONES_V2;
                    saltHex = p[1];
                    hashHex = p[2];
                } else {
                    return false;
                }
                byte[] actual = derivar(plano.toCharArray(), unhex(saltHex), iteraciones);
                boolean ok = MessageDigest.isEqual(actual, unhex(hashHex));
                if (ok && p[0].equals("v2")) passHash = hashPassword(plano);
                return ok;
            } catch (Exception e) {
                return false;
            }
        }
        // Legacy SHA-256: si valida, migra al formato actual en memoria.
        boolean ok = MessageDigest.isEqual(passHash.getBytes(StandardCharsets.US_ASCII),
                sha256(plano).getBytes(StandardCharsets.US_ASCII));
        if (ok) passHash = hashPassword(plano);
        return ok;
    }

    private static byte[] derivar(char[] plano, byte[] salt, int iteraciones) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(plano, salt, iteraciones, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
            java.util.Arrays.fill(plano, '\0');
        }
    }

    public void cambiarPass(String nuevaPlana) {
        this.passHash = hashPassword(nuevaPlana);
    }

    private static String sha256(String plano) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(plano.getBytes(StandardCharsets.UTF_8));
            return hex(bytes);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo verificar la contraseña SHA-256 heredada.", e);
        }
    }

    private static String hex(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.toString();
    }

    private static byte[] unhex(String s) {
        byte[] r = new byte[s.length() / 2];
        for (int i = 0; i < r.length; i++) r[i] = (byte) Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16);
        return r;
    }

    public boolean esAdmin() { return "ADMIN".equals(rol); }
    public boolean puedeVender() { return esAdmin() || "VENDEDOR".equals(rol); }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String n) { this.nombre = n; }
    public String getUsername() { return username; }
    public String getRol() { return rol; }
    public void setRol(String r) { this.rol = r; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean a) { this.activo = a; }

    @Override
    public String toString() { return nombre + " (" + username + ")"; }
}
