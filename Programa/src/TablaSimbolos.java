import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * ============================================================
 *                    TABLA DE SÍMBOLOS
 * ============================================================
 *
 * Descripcion:
 * Implemento la tabla de símbolos con una tabla por ámbito, encadenada
 * al ámbito que la contiene (Aho et al., sección 2.7).
 * La tabla de símbolos está formada por cinco clases, una por archivo:
 *   TablaSimbolos.java         -> administra la cadena de ámbitos (este archivo)
 *   Simbolo.java               -> una entrada de la tabla        (clase Symbol de la lectura)
 *   Ambito.java                -> una tabla por ámbito           (clase Env de la lectura)
 *   ReporteTablaSimbolos.java  -> arma el reporte .txt
 *   CuadroTexto.java           -> dibuja tablas con bordes para el reporte
 *
 * Cada símbolo guarda:
 *   1. Información básica : nombre y categoría.
 *   2. Atributos de tipo  : tipo, tamaño en bytes, dimensión de arreglos
 *                           y parámetros (cantidad, tipos, retorno y paso).
 *   3. Ubicación          : dirección (desplazamiento) y ámbito.
 *   4. Adicionales        : valor inicial, acceso y tabla hija (funciones).
 *
 * Entrada:
 * Las acciones de la gramática (Parser.cup) llaman a estos métodos mientras
 * el parser reconoce el programa:
 *   ¿:  -> abrirAmbito()      :?  -> cerrarAmbito()
 *   val tipo id               -> declararVariable / declararArreglo
 *   tipo id є: params :э      -> declararFuncion
 *   uso de id                 -> buscar
 *
 * Salida:
 * Los ámbitos con sus símbolos y la lista de errores semánticos.
 *
 * Restricciones:
 * Esta clase SOLO GUARDA Y BUSCA: no decide qué es un error. Todas las reglas
 * semánticas están en Parser.cup, que usa errorSemantico() para dejar sus mensajes.
 *
 * Objetivo:
 * Pasar la información de las declaraciones a los usos, y que todas las
 * reglas queden en un solo lugar.
 */
public class TablaSimbolos {

    /* ============================================================
       TAMAÑOS DE LOS TIPOS
       ============================================================ */

    /* Bytes de cada tipo. string se guarda como una referencia a la cadena. */
    private static final Map<String, Integer> BYTES_POR_TIPO = new HashMap<>();

    static {
        BYTES_POR_TIPO.put("int", 4);
        BYTES_POR_TIPO.put("float", 4);
        BYTES_POR_TIPO.put("bool", 1);
        BYTES_POR_TIPO.put("char", 1);
        BYTES_POR_TIPO.put("string", 4);
        BYTES_POR_TIPO.put("void", 0);
    }

    public static int bytesDelTipo(String tipo) {
        return BYTES_POR_TIPO.getOrDefault(tipo, 0);
    }


    /* ============================================================
       DATOS DE LA TABLA
       ============================================================ */

    private Ambito ambitoActual;
    private final List<Ambito> ambitos = new ArrayList<>();      /* todos, en el orden en que se crearon */
    private final List<String> errores = new ArrayList<>();

    /*
     * Datos del próximo ámbito. Hacen falta porque el nombre de un bloque (if, while...)
     * y los parámetros de una función se leen ANTES del ¿: que abre el ámbito.
     */
    private String nombreProximoAmbito = "bloque";
    private Simbolo funcionPendiente = null;
    private final List<Simbolo> parametrosPendientes = new ArrayList<>();

    public TablaSimbolos() {
        ambitoActual = new Ambito(null, "global", true);
        ambitos.add(ambitoActual);
    }


    /* ============================================================
       ÁMBITOS
       ============================================================ */

    /* Indico el nombre del próximo ámbito (if, while, else...) */
    public void siguienteAmbito(String nombre) {
        nombreProximoAmbito = nombre;
    }

    /* Abro un ámbito nuevo dentro del actual (al encontrar ¿:) */
    public void abrirAmbito() {
        abrirAmbito(nombreProximoAmbito);
    }

    public void abrirAmbito(String nombre) {

        boolean esCuerpoDeFuncion = funcionPendiente != null;

        ambitoActual = new Ambito(ambitoActual, nombre, esCuerpoDeFuncion);
        ambitos.add(ambitoActual);

        if (esCuerpoDeFuncion) {
            enlazarConSuFuncion();
        }

        nombreProximoAmbito = "bloque";
        declararParametrosPendientes();
    }

    /* La función y el ámbito de su cuerpo quedan apuntando el uno al otro */
    private void enlazarConSuFuncion() {
        ambitoActual.funcionDuena = funcionPendiente;
        funcionPendiente.tablaHija = ambitoActual;
        funcionPendiente = null;
    }

    /* Los parámetros de una función se declaran dentro del ámbito de su cuerpo */
    private void declararParametrosPendientes() {
        for (Simbolo parametro : parametrosPendientes) {
            declarar(parametro);
        }
        parametrosPendientes.clear();
    }

    /*
     * Regreso al ámbito que contiene al actual (al encontrar :?).
     * Si se cierra el cuerpo de una función, se guarda en ella el tamaño
     * de su registro de activación.
     */
    public void cerrarAmbito() {

        if (ambitoActual.esGlobal()) {
            return;
        }

        Simbolo funcion = ambitoActual.funcionDuena;

        if (funcion != null) {
            funcion.bytes = ambitoActual.marco.siguienteDireccion;
        }

        ambitoActual = ambitoActual.padre;
    }


    /* ============================================================
       DECLARACIONES
       ============================================================ */

    /*
     * Inserto un símbolo en el ámbito actual y le asigno ámbito, acceso y dirección.
     *
     * Salida: null si se insertó. Si el nombre ya existía en ESTE ámbito no se inserta
     *         y se devuelve el símbolo anterior: Parser.cup decide que eso es un error.
     */
    public Simbolo declarar(Simbolo simbolo) {

        if (simbolo == null || simbolo.nombre == null) {
            return null;
        }

        Simbolo existente = ambitoActual.buscarSoloAqui(simbolo.nombre);

        if (existente != null) {
            return existente;
        }

        simbolo.ambito = ambitoActual;
        simbolo.acceso = ambitoActual.esGlobal() ? "público" : "privado";

        /* Las funciones viven en el código: no ocupan espacio de datos */
        if (!simbolo.esFuncion()) {
            reservarDireccion(simbolo);
        }

        ambitoActual.agregar(simbolo);
        return null;
    }

    /* La dirección es el siguiente espacio libre del marco (zona global o registro de la función) */
    private void reservarDireccion(Simbolo simbolo) {

        Ambito marco = ambitoActual.marco;

        simbolo.desplazamiento = marco.siguienteDireccion;
        simbolo.segmento = marco.esGlobal() ? "global" : "pila";

        if (simbolo.bytes > 0) {
            marco.siguienteDireccion += simbolo.bytes;
        }
    }

    public Simbolo declararVariable(String nombre, String tipo, String valorInicial, int linea, int columna) {

        Simbolo variable = new Simbolo(nombre, tipo, Simbolo.VARIABLE, linea, columna);
        variable.valorInicial = unirSignoNegativo(valorInicial);

        return declarar(variable);
    }

    /*
     * tamano          : texto entre ʃ: y :ʅ (null si no se escribió).
     * cantidadValores : cantidad de valores de la lista ¿: ... :? (0 si no hay lista).
     */
    public Simbolo declararArreglo(String nombre, String tipo, String tamano, int cantidadValores,
                                   String valorInicial, int linea, int columna) {

        Simbolo arreglo = new Simbolo(nombre, tipo, Simbolo.ARREGLO, linea, columna);

        arreglo.valorInicial = unirSignoNegativo(valorInicial);
        arreglo.elementos = cantidadDeElementos(tamano, cantidadValores);
        arreglo.bytes = arreglo.elementos > 0 ? arreglo.elementos * bytesDelTipo(tipo) : 0;

        return declarar(arreglo);
    }

    /*
     * Cantidad de elementos de un arreglo:
     * el tamaño escrito, o la cantidad de valores si no se escribió tamaño.
     * -1 si no se puede saber (por ejemplo, si el tamaño es una expresión).
     */
    private static int cantidadDeElementos(String tamano, int cantidadValores) {

        if (tamano == null) {
            return cantidadValores > 0 ? cantidadValores : -1;
        }

        try {
            return Integer.parseInt(tamano.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /*
     * Declaro una función (o un procedimiento si es void) en el ámbito actual
     * y dejo sus parámetros listos para el ámbito de su cuerpo.
     */
    public Simbolo declararFuncion(String nombre, String tipo, List<Simbolo> parametros, int linea, int columna) {

        boolean esProcedimiento = "void".equals(tipo);
        String categoria = esProcedimiento ? Simbolo.PROCEDIMIENTO : Simbolo.FUNCION;

        Simbolo funcion = new Simbolo(nombre, tipo, categoria, linea, columna);

        if (parametros != null) {
            funcion.parametros.addAll(parametros);
            parametrosPendientes.addAll(parametros);
        }

        Simbolo existente = declarar(funcion);

        siguienteAmbito((esProcedimiento ? "procedimiento " : "funcion ") + nombre);
        funcionPendiente = funcion;

        return existente;
    }

    /* Une el signo negativo con su número: "- 0.01" -> "-0.01", "x - - 14" -> "x - -14" */
    private static String unirSignoNegativo(String valor) {

        if (valor == null) {
            return null;
        }

        return valor.replaceAll("(^|[-+*/,Ͱ]\\s|mod\\s|pot\\s|є:\\s|ʃ:\\s|¿:\\s)-\\s(?=\\d)", "$1-");
    }


    /* ============================================================
       USOS Y ERRORES
       ============================================================ */

    /*
     * Busco un nombre desde el ámbito actual hacia los que lo contienen.
     * Devuelve null si no existe. No reporta nada: Parser.cup decide qué hacer.
     */
    public Simbolo buscar(String nombre) {
        return nombre == null ? null : ambitoActual.buscar(nombre);
    }

    /*
     * Guardo un error semántico. Lo llama Parser.cup cada vez que una regla se incumple.
     * Usa el mismo formato que los errores léxicos, para que Main los muestre igual.
     */
    public void errorSemantico(int linea, int columna, String mensaje) {
        errores.add("Error semántico en línea " + linea + ", columna " + columna + ": " + mensaje);
    }

    public List<String> getErrores() {
        return errores;
    }


    /* ============================================================
       CONSULTAS (las usan Main y el reporte)
       ============================================================ */

    public List<Ambito> getAmbitos() {
        return ambitos;
    }

    public Ambito ambitoGlobal() {
        return ambitos.get(0);
    }

    public int cantidadSimbolos() {

        int total = 0;

        for (Ambito ambito : ambitos) {
            total += ambito.cantidad();
        }

        return total;
    }

    /* Árbol de ámbitos dibujado con ├── y └── */
    public String arbolDeAmbitos() {
        return ReporteTablaSimbolos.dibujarArbol(ambitoGlobal());
    }


    /* ============================================================
       REPORTE EN ARCHIVO
       ============================================================ */

    /* Carpeta donde se guarda el reporte, dentro de Programa */
    public static final String CARPETA_REPORTE = "salida" + File.separator + "tabla_simbolos";

    /*
     * Descripcion:
     * Ubico la carpeta salida/tabla_simbolos dentro de la carpeta Programa:
     *   ...\Programa\bin                    <- aquí están los .class
     *   ...\Programa\salida\tabla_simbolos  <- aquí se guarda el reporte
     *
     * Restricciones:
     * Se calcula a partir de la ubicación de los .class, así funciona sin
     * importar desde qué carpeta se ejecute el programa.
     */
    public static File carpetaReporte() {
        try {
            File carpetaBin = new File(TablaSimbolos.class
                    .getProtectionDomain().getCodeSource().getLocation().toURI());

            return new File(carpetaBin.getParentFile(), CARPETA_REPORTE);

        } catch (Exception e) {
            /* Si no se puede ubicar, se usa la carpeta actual */
            return new File(CARPETA_REPORTE);
        }
    }

    /*
     * Descripcion:
     * Guardo el reporte completo en la carpeta tabla_simbolos.
     *
     * Entrada: el archivo fuente analizado (de él sale el nombre del reporte).
     * Salida : el archivo creado, por ejemplo tabla_simbolos/prueba_tabla_simbolos.txt
     */
    public File guardarReporte(File fuente) throws IOException {
        return guardarReporte(fuente, new ReporteTablaSimbolos(this).generar(fuente.getName()));
    }

    /* Guardo un texto ya preparado, por ejemplo la tabla tal como se ve en la consola */
    public File guardarReporte(File fuente, String contenido) throws IOException {

        File carpeta = carpetaReporte();

        if (!carpeta.exists()) {
            carpeta.mkdirs();
        }

        File reporte = new File(carpeta, nombreSinExtension(fuente) + "_tabla_simbolos.txt");

        Files.writeString(reporte.toPath(), contenido, StandardCharsets.UTF_8);

        return reporte;
    }

    /* "prueba.cmm" -> "prueba" */
    private static String nombreSinExtension(File archivo) {

        String nombre = archivo.getName();
        int punto = nombre.lastIndexOf('.');

        return punto > 0 ? nombre.substring(0, punto) : nombre;
    }
}
