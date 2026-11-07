/*
 * Descripcion:
 * Guarda la informacion de una expresion.
 *
 * Entrada: Tipo de dato y lugar del valor.
 *
 * Salida: Un objeto con los datos de la expresion.
 *
 * Restricciones:
 * Si el tipo es "error", indica que la expresion tiene un error.
 * Esto evita mostrar errores repetidos.
 */
public class Atrib {

    public String tipo;   // Tipo de dato
    public String lugar;  // Lugar del valor
    public Integer valor; // Valor entero, si existe
    public String codigo;

    /*
     * Descripcion: Crea una expresion.
     * Entrada: Tipo de dato y lugar del valor.
     * Salida: Un objeto con esos datos.
     * Restricciones: El valor inicia en null y el codigo vacio.
     */
    public Atrib(String tipo, String lugar) {
        this.tipo = tipo;
        this.lugar = lugar;
        this.codigo = "";
        this.valor = null;
    }
}