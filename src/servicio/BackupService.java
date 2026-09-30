package servicio;

import java.nio.file.*;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.*;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * F7: backup cifrado AES-GCM (solo JDK). Llave en data/backup.key (no subir a git).
 * crearBackup() -> backup/backup-yyyyMMdd-HHmm.zip.enc ; restaurar() lo revierte.
 */
public class BackupService {
    private static byte[] clave() throws Exception {
        Path p = Paths.get("data/backup.key");
        if (!Files.exists(p)) {
            byte[] k = new byte[32];
            new SecureRandom().nextBytes(k);
            Files.createDirectories(p.getParent());
            Files.write(p, k);
            return k;
        }
        return Files.readAllBytes(p);
    }

    public static String crearBackup() throws Exception {
        Files.createDirectories(Paths.get("backup"));
        String nombre = "backup-" + java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")) + ".zip.enc";
        // 1) zip data/*.dat en memoria
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bos)) {
            for (String f : new String[]{"usuarios.dat", "productos.dat", "alumnos.dat", "ventas.dat",
                    "proveedores.dat", "movimientos.dat", "clientes.dat", "turnos.dat", "compras.dat"}) {
                Path p = Paths.get("data/" + f);
                if (!Files.exists(p)) continue;
                zip.putNextEntry(new ZipEntry(f));
                zip.write(Files.readAllBytes(p));
                zip.closeEntry();
            }
        }
        // 2) AES-GCM: [12 iv][cipher]
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(clave(), "AES"), new GCMParameterSpec(128, iv));
        byte[] enc = c.doFinal(bos.toByteArray());
        byte[] out = new byte[12 + enc.length];
        System.arraycopy(iv, 0, out, 0, 12);
        System.arraycopy(enc, 0, out, 12, enc.length);
        Files.write(Paths.get("backup/" + nombre), out);
        Bitacora.registrar("Backup cifrado: " + nombre);
        return "backup/" + nombre;
    }

    public static List<String> listar() {
        try {
            if (!Files.exists(Paths.get("backup"))) return new ArrayList<>();
            List<String> r = new ArrayList<>();
            try (var s = Files.list(Paths.get("backup"))) {
                s.filter(p -> p.toString().endsWith(".enc")).sorted()
                        .forEach(p -> r.add(p.toString().replace("\\", "/")));
            }
            return r;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public static void restaurar(String archivo) throws Exception {
        byte[] all = Files.readAllBytes(Paths.get(archivo));
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(clave(), "AES"),
                new GCMParameterSpec(128, all, 0, 12));
        byte[] zip = c.doFinal(all, 12, all.length - 12);
        try (ZipInputStream zin = new ZipInputStream(new java.io.ByteArrayInputStream(zip))) {
            ZipEntry e;
            while ((e = zin.getNextEntry()) != null) {
                Files.write(Paths.get("data/" + e.getName()), zin.readAllBytes());
            }
        }
        Bitacora.registrar("Backup restaurado: " + archivo + " (reinicia la app)");
    }
}
