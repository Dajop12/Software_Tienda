package servicio;

import dominio.Usuario;

/**
 * F10: usuario en turno para permisos UI. CAJERO no ve costo/ganancia.
 */
public class Sesion {
    private static Usuario actual;

    public static void setActual(Usuario u) { actual = u; }
    public static Usuario getActual() { return actual; }
    public static void limpiar() { actual = null; }
    public static boolean esJefe() { return actual != null && actual.esAdmin(); }
    public static boolean puedeVerCostos() { return esJefe(); }
}
