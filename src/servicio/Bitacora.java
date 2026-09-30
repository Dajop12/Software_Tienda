package servicio;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Bitácora simple en memoria + archivo data/bitacora.log */
public class Bitacora {
    private static final List<String> entradas = new ArrayList<>();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static synchronized void registrar(String accion) {
        String linea = LocalDateTime.now().format(FMT) + " - " + accion;
        entradas.add(linea);
        try (PrintWriter pw = new PrintWriter(new FileWriter("data/bitacora.log", true), true)) {
            pw.println(linea);
        } catch (Exception ignored) {}
    }

    public static synchronized List<String> ultimas(int n) {
        int desde = Math.max(0, entradas.size() - n);
        return new ArrayList<>(entradas.subList(desde, entradas.size()));
    }

    public static synchronized List<String> todas() {
        return new ArrayList<>(entradas);
    }
}
