/*
 *
 * Descripción:
 * Guarda la información de cada expresión que se analiza.
 * Incluye su tipo de dato, dónde se encuentra su valor y,
 * si es un número entero, también guarda ese valor.
 *
 * Entrada: el tipo de dato y el lugar donde está el valor.
 *
 * Salida: un objeto que se utiliza en las reglas del parser.
 *
 * Restricciones:
 * Si el tipo es "error", significa que ya se encontró un
 * error en esa expresión. Así se evitan mensajes repetidos.
 *
 */

public class Atrib {
    public String tipo;   // Tipo de dato
    public String lugar;  // Lugar donde está el valor
    public Integer valor; // Guarda el valor si es un número entero; si no se conoce, queda en null.
    public String codigo; 

    public Atrib(String tipo, String lugar) {
        this.tipo = tipo;
        this.lugar = lugar;
        this.codigo = "";
        this.valor = null;
    }
}