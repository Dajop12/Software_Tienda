package dominio;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Usuario. Rol: ADMIN (todo/JEFE), VENDEDOR (ventas+productos), CONSULTA (solo ver).
 * F6: PBKDF2+salt v2 (v2$salt$hash). Verifica legacy SHA-256 y migra solo.
 */
public class Usuario implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String nombre;
    private String username;
    private String passHash; // v2$saltHex$hashHex o legacy hex
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

    /** Nuevo hash v2 PBKDF2 21k iteraciones. */
    public static String hashPassword(String plano) {
        try {
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            PBEKeySpec spec = new PBEKeySpec(plano.toCharArray(), salt, 21000, 256);
            byte[] hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            spec.clearPassword();
            return "v2$" + hex(salt) + "$" + hex(hash);
        } catch (Exception e) {
            return "v2$00$00";
        }
    }

    public boolean verificaPass(String plano) {
        if (passHash != null && passHash.startsWith("v2$")) {
            try {
                String[] p = passHash.split("\\$");
                byte[] salt = unhex(p[1]);
                PBEKeySpec spec = new PBEKeySpec(plano.toCharArray(), salt, 21000, 256);
                byte[] hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
                spec.clearPassword();
                return hex(hash).equals(p[2]);
            } catch (Exception e) {
                return false;
            }
        }
        // legacy SHA-256: si ok, migra a v2 en memoria (se persiste en próximo guardarTodo)
        boolean ok = passHash != null && passHash.equals(sha256(plano));
        if (ok) passHash = hashPassword(plano);
        return ok;
    }

    public void cambiarPass(String nuevaPlana) {
        this.passHash = hashPassword(nuevaPlana);
    }

    private static String sha256(String plano) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(plano.getBytes("UTF-8"));
            return hex(bytes);
        } catch (Exception e) {
            return plano;
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
