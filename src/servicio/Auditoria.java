package servicio;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * F8: auditoría estructurada quién-hizo-qué (aparte de Bitacora).
 * CSV: fecha,usuario,modulo,accion,detalle -> data/auditoria.log
 */
public class Auditoria {
    private static final List<String[]> MEM = new ArrayList<>();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static synchronized void registrar(String usuario, String modulo, String accion, String detalle) {
        String f = LocalDateTime.now().format(FMT);
        MEM.add(new String[]{f, usuario, modulo, accion, detalle});
        try (PrintWriter pw = new PrintWriter(new FileWriter("data/auditoria.log", true), true)) {
            pw.printf("%s,%s,%s,%s,\"%s\"%n", f, csv(usuario), csv(modulo), csv(accion), detalle.replace("\"", "'"));
        } catch (Exception ignored) {}
    }

    public static synchronized List<String[]> ultimas(int n) {
        int d = Math.max(0, MEM.size() - n);
        return new ArrayList<>(MEM.subList(d, MEM.size()));
    }

    private static String csv(String s) {
        return s == null ? "" : s.replace(",", ";");
    }
}
