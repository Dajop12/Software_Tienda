package servicio;

import datos.GestorDatos;
import dominio.Usuario;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * F7: auth por token para la API. Roles: ADMIN/JEFE todo, VENDEDOR vende+stock, CONSULTA nada.
 * POST /api/login body user=..&pass=.. -> {"token","rol"}. Token 12h.
 * api.key legada = ADMIN (compatibilidad F6).
 */
public class ApiAuth {
    private static final Map<String, Sesion> SESIONES = new ConcurrentHashMap<>();
    private record Sesion(String username, String rol, long expira) {}

    public static String login(String user, String pass) {
        for (Usuario x : GestorDatos.getInstancia().getUsuarios()) {
            if (x.getUsername().equalsIgnoreCase(user == null ? "" : user.trim())
                    && x.isActivo() && x.verificaPass(pass == null ? "" : pass)) {
                String t = UUID.randomUUID().toString().replace("-", "");
                SESIONES.put(t, new Sesion(x.getUsername(), x.getRol(), System.currentTimeMillis() + 12 * 3600_000L));
                Bitacora.registrar("API login: " + x.getUsername());
                return t + "|" + x.getRol();
            }
        }
        return null;
    }

    /** Rol del token o api.key (ADMIN) o null. */
    public static String rolDe(String token, String apiKey, String apiKeyReal) {
        if (token != null && !token.isBlank()) {
            Sesion s = SESIONES.get(token.trim());
            if (s != null && System.currentTimeMillis() < s.expira()) return s.rol();
            SESIONES.remove(token.trim());
        }
        if (apiKeyReal != null && apiKeyReal.equals(apiKey)) return "ADMIN";
        return null;
    }

    public static boolean puedeVender(String rol) {
        return "ADMIN".equals(rol) || "VENDEDOR".equals(rol);
    }

    public static boolean esJefe(String rol) {
        return "ADMIN".equals(rol);
    }
}
