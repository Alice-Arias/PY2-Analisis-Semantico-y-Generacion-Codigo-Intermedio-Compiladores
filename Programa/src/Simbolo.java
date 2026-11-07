import java.util.ArrayList;
import java.util.List;

/*
 * ============================================================
 *                         SIMBOLO
 * ============================================================
 * Descripcion: una entrada de la tabla con todos sus atributos.
 */
public class Simbolo {

    /* Categorías (el lenguaje no tiene constantes, clases ni tipos de usuario) */
    public static final String VARIABLE      = "variable";
    public static final String ARREGLO       = "arreglo";
    public static final String FUNCION       = "función";
    public static final String PROCEDIMIENTO = "procedimiento";
    public static final String PARAMETRO     = "parámetro";

    /* 1. Información básica */
    public String nombre;
    public String categoria;

    /* 2. Atributos de tipo */
    public String tipo;                     /* tipo del dato, o tipo de retorno en funciones */
    public int bytes;                       /* tamaño en memoria (en funciones: su registro de activación) */
    public int elementos = 0;               /* arreglos: cantidad de elementos (-1 = desconocido) */
    public List<Simbolo> parametros = new ArrayList<>();

    /* 3. Ubicación */
    public Ambito ambito;
    public String segmento = "—";           /* global o pila */
    public int desplazamiento = 0;          /* posición dentro del segmento */
    public int linea;
    public int columna;

    /* 4. Adicionales */
    public String valorInicial;
    public String acceso;                   /* público (global) o privado (local) */
    public Ambito tablaHija;                /* funciones: el ámbito de su cuerpo */

    public Simbolo(String nombre, String tipo, String categoria, int linea, int columna) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.categoria = categoria;
        this.linea = linea;
        this.columna = columna;
        this.bytes = esFuncion() ? 0 : TablaSimbolos.bytesDelTipo(tipo);
    }

    public boolean esFuncion() {
        return FUNCION.equals(categoria) || PROCEDIMIENTO.equals(categoria);
    }

    public boolean esArreglo() {
        return ARREGLO.equals(categoria);
    }

    /* Dirección legible: global+n, pila+n o código */
    public String direccion() {
        return esFuncion() ? "código" : segmento + "+" + desplazamiento;
    }

    /* Dimensión para la tabla: [0..n-1], [0..?] o — si no es arreglo */
    public String textoDimensionCorta() {

        if (!esArreglo()) {
            return "—";
        }

        return elementos < 0 ? "[0..?]" : "[0.." + (elementos - 1) + "]";
    }

    /* Parámetros formales: int a, char b */
    public String listaParametros() {

        List<String> textos = new ArrayList<>();

        for (Simbolo parametro : parametros) {
            textos.add(parametro.tipo + " " + parametro.nombre);
        }

        return String.join(", ", textos);
    }
}
