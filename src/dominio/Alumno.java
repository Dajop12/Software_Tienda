package dominio;

import java.io.Serializable;

/** Alumno de la escuela. Promedio 0-10. */
public class Alumno implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String nombre;
    private String matricula;
    private String carrera;
    private double promedio;

    public Alumno(String id, String nombre, String matricula, String carrera, double promedio) {
        this.id = id;
        this.nombre = nombre;
        this.matricula = matricula;
        this.carrera = carrera;
        this.promedio = promedio;
    }

    public String getEstado() { return promedio >= 7.0 ? "Aprobado" : "Reprobado"; }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String n) { this.nombre = n; }
    public String getMatricula() { return matricula; }
    public String getCarrera() { return carrera; }
    public void setCarrera(String c) { this.carrera = c; }
    public double getPromedio() { return promedio; }
    public void setPromedio(double p) { this.promedio = p; }

    @Override
    public String toString() { return matricula + " - " + nombre; }
}
