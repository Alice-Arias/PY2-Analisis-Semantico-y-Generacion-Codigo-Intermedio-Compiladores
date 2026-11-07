import java.util.ArrayList;
import java.util.List;

/**
 * Descripcion: Representa un símbolo de la tabla.
 * Entrada: Datos de una variable, arreglo, función o procedimiento.
 * Salida: Objeto con la información del símbolo.
 * Restricciones: Guarda los datos necesarios para identificar cada símbolo.
 */
public class Simbolo {

    // Categorías de los símbolos.
    public static final String VARIABLE      = "variable";
    public static final String ARREGLO       = "arreglo";
    public static final String FUNCION       = "función";
    public static final String PROCEDIMIENTO = "procedimiento";
    public static final String PARAMETRO     = "parámetro";

    // Información básica.
    public String nombre;
    public String categoria;

    // Tipo y tamaño del símbolo.
    public String tipo;
    public int bytes;
    public int elementos = 0;
    public List<Simbolo> parametros = new ArrayList<>();

    // Ubicación del símbolo.
    public Ambito ambito;
    public String segmento = "—";
    public int desplazamiento = 0;
    public int linea;
    public int columna;

    // Información adicional.
    public String valorInicial;
    public String acceso;
    public Ambito tablaHija;

    /**
     * Descripcion: Crea un símbolo con sus datos básicos.
     * Entrada: Nombre, tipo, categoría, línea y columna.
     * Salida: Objeto de tipo Simbolo.
     * Restricciones: Las funciones y procedimientos inician con 0 bytes.
     */
    public Simbolo(String nombre, String tipo, String categoria, int linea, int columna) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.categoria = categoria;
        this.linea = linea;
        this.columna = columna;
        this.bytes = esFuncion() ? 0 : TablaSimbolos.bytesDelTipo(tipo);
    }

    /**
     * Descripcion: Verifica si el símbolo es una función o procedimiento.
     * Entrada: Categoría del símbolo.
     * Salida: true si es función o procedimiento, false en otro caso.
     * Restricciones: Compara con las categorías definidas.
     */
    public boolean esFuncion() {
        return FUNCION.equals(categoria) || PROCEDIMIENTO.equals(categoria);
    }

    /**
     * Descripcion: Verifica si el símbolo es un arreglo.
     * Entrada: Categoría del símbolo.
     * Salida: true si es un arreglo, false en otro caso.
     * Restricciones: Compara con la categoría ARREGLO.
     */
    public boolean esArreglo() {
        return ARREGLO.equals(categoria);
    }

    /**
     * Descripcion: Obtiene la dirección del símbolo.
     * Entrada: Segmento y desplazamiento.
     * Salida: Dirección del símbolo en texto.
     * Restricciones: Las funciones y procedimientos muestran "código".
     */
    public String direccion() {
        return esFuncion() ? "código" : segmento + "+" + desplazamiento;
    }

    /**
     * Descripcion: Obtiene la dimensión del arreglo.
     * Entrada: Cantidad de elementos.
     * Salida: Rango del arreglo en texto.
     * Restricciones: Si no es arreglo muestra "—" y si se desconoce el tamaño muestra "[0..?]".
     */
    public String textoDimensionCorta() {

        if (!esArreglo()) {
            return "—";
        }

        return elementos < 0 ? "[0..?]" : "[0.." + (elementos - 1) + "]";
    }

    /**
     * Descripcion: Crea una lista con los parámetros del símbolo.
     * Entrada: Lista de parámetros.
     * Salida: Parámetros en formato tipo y nombre.
     * Restricciones: Separa los parámetros con comas.
     */
    public String listaParametros() {

        List<String> textos = new ArrayList<>();

        for (Simbolo parametro : parametros) {
            textos.add(parametro.tipo + " " + parametro.nombre);
        }

        return String.join(", ", textos);
    }
}