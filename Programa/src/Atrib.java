/*
 * ============================================================
 * Atrib
 * ============================================================
 * Descripcion:
 *   Atributo que viaja por el parser en cada expresion. Lleva el
 *   TIPO de la expresion (para el analisis semantico), el LUGAR
 *   donde queda su valor en el codigo intermedio y, si la expresion
 *   es una constante entera, su VALOR.
 *
 * Entrada:  el tipo y el lugar.
 * Salida:   un objeto que las acciones de CUP pasan de produccion
 *           en produccion.
 * Restricciones:
 *   El tipo "error" significa que la expresion ya reporto un error;
 *   las reglas siguientes lo ignoran para no repetir mensajes.
 * Objetivo:
 *   Que la misma accion de CUP valide el tipo y calcule las constantes
 *   (por ejemplo 1 + 1 = 2) para revisar los indices de los arreglos.
 */
public class Atrib {

    public String tipo;      /* int, float, bool, char, string, void o error */
    public String lugar;     /* literal, identificador o temporal */
    public Integer valor;    /* valor si es una constante entera (3, 1 + 1, -2); null si no se conoce */

    // Nuevo campo para el código de tres direcciones
    public String codigo;

    public Atrib(String tipo, String lugar) {
        this.tipo = tipo;
        this.lugar = lugar;
        this.codigo = "";
        this.valor = null;
    }
}