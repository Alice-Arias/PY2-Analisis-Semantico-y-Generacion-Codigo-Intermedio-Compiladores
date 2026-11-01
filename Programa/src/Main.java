import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java_cup.runtime.Symbol;

/*
 * Descripcion:
 * Coordino el analisis lexico, sintactico y semantico (tabla de simbolos).
 *
 * Entrada:
 * Recibe el archivo .cmm que se desea analizar.
 *
 * Salida:
 * Muestra los resultados y los errores encontrados.
 *
 * Restricciones:
 * El archivo debe seguir las reglas del lenguaje.
 *
 * Objetivo:
 * Ejecutar las tres fases del analizador en un solo programa.
 */

public class Main {

    /*
     * Descripcion:
     * Guardo la configuracion general que utiliza el programa.
     *
     * Entrada:
     * Valores definidos dentro del programa.
     *
     * Salida:
     * Configuracion disponible para las diferentes funciones.
     *
     * Restricciones:
     * Los valores deben coincidir con el formato de salida esperado.
     *
     * Objetivo:
     * Mantener organizada la configuracion del analizador.
     */
    private static final int ANCHO = 76;

    /* Acepta "Error léxico ..." y "Error semántico ..." */
    private static final Pattern PATRON_ERROR = Pattern.compile(  "Error \\S+ en línea (\\d+), columna (\\d+): (.*)");

    /* Nombre de token -> cómo se escribe en el lenguaje. */
    private static final Map<String, String> VISIBLE = new HashMap<>();

    static {
        VISIBLE.put("PLUS", "+");
        VISIBLE.put("MINUS", "-");
        VISIBLE.put("MULTIPLY", "*");
        VISIBLE.put("ENTIREDIV", "//");
        VISIBLE.put("FLOATDIV", "/");
        VISIBLE.put("MOD", "mod");
        VISIBLE.put("POT", "pot");
        VISIBLE.put("INCREMENT", "++");
        VISIBLE.put("DECREMENT", "--");
        VISIBLE.put("LTE", "<=");
        VISIBLE.put("GTE", ">=");
        VISIBLE.put("EQUAL", "==");
        VISIBLE.put("NEQ", "!=");
        VISIBLE.put("LT", "<");
        VISIBLE.put("GT", ">");
        VISIBLE.put("AND", "λ");
        VISIBLE.put("OR", "θ");
        VISIBLE.put("NOT", "Σ");
        VISIBLE.put("OPBLOCK", "¿:");
        VISIBLE.put("CLBLOCK", ":?");
        VISIBLE.put("PARENOP", "є:");
        VISIBLE.put("PARENCL", ":э");
        VISIBLE.put("OPBRACKET", "ʃ:");
        VISIBLE.put("CLBRACKET", ":ʅ");
        VISIBLE.put("TERMINATOR", "»");
        VISIBLE.put("ASIGN", "Ͱ");
        VISIBLE.put("COMA", ",");
        VISIBLE.put("ID", "identificador");
        VISIBLE.put("LITERAL_INT", "número entero");
        VISIBLE.put("LITERAL_FLOAT", "número decimal");
        VISIBLE.put("LITERAL_CHAR", "carácter");
        VISIBLE.put("LITERAL_STRING", "cadena de texto");
        VISIBLE.put("EOF", "fin de archivo");
    }

    private static PrintStream consola;

    private static int totalTokens = 0;

    /*
     * Descripcion:
     * Verifico si la consola permite mostrar colores.
     *
     * Entrada:
     * Configuracion del sistema operativo y la consola.
     *
     * Salida:
     * Retorna verdadero si se pueden usar colores.
     *
     * Restricciones:
     * Depende de las capacidades de la consola.
     *
     * Objetivo:
     * Mejorar la presentacion de los resultados.
     */
    private static final boolean COLOR = detectarColor();

    /*
     * Los colores están activados por defecto.
     * Para desactivarlos: definir la variable de entorno NO_COLOR
     * (por ejemplo, "set NO_COLOR=1" antes de ejecutar).
     */
    private static boolean detectarColor() {

        if (System.getenv("NO_COLOR") != null) {
            return false;
        }

        String so = System.getProperty("os.name", "").toLowerCase();

        if (so.contains("win")) {
            habilitarAnsiWindows();
            return true;
        }

        String term = System.getenv("TERM");

        return term != null && !term.equals("dumb");
    }

    /*
     * Descripcion:
     * Intento habilitar el uso de colores ANSI en Windows.
     *
     * Entrada:
     * La configuracion actual de la consola.
     *
     * Salida:
     * La consola queda preparada para mostrar colores.
     *
     * Restricciones:
     * Solo se aplica cuando el sistema es Windows.
     *
     * Objetivo:
     * Permitir una mejor visualizacion de los mensajes.
     */
    private static void habilitarAnsiWindows() {
        try {
            new ProcessBuilder("cmd", "/c", "rem")
                    .inheritIO()
                    .start()
                    .waitFor();
        } catch (Exception e) {
            /* Si falla, simplemente no se verán colores. */
        }
    }

    /*
     * Descripcion:
     * Aplico un color ANSI a un texto.
     *
     * Entrada:
     * El codigo de color y el texto.
     *
     * Salida:
     * El texto con color, o sin color si la consola no lo permite.
     *
     * Restricciones:
     * Depende de que la consola soporte colores.
     *
     * Objetivo:
     * Resaltar la informacion importante.
     */
    private static String col(String codigo, String texto) {

        return COLOR ? "\u001B[" + codigo + "m" + texto + "\u001B[0m" : texto;
    }

    /*
     * Descripcion:
     * Muestro el titulo principal del programa.
     *
     * Entrada:
     * El texto que se desea mostrar como titulo.
     *
     * Salida:
     * Un titulo centrado en la consola.
     *
     * Restricciones:
     * El texto debe poder ajustarse al ancho de la consola.
     *
     * Objetivo:
     * Presentar de forma clara el inicio del programa.
     */
    private static void banner(String texto) {
        int libre = ANCHO - 2 - texto.length();
        int izq = Math.max(0, libre / 2);
        int der = Math.max(0, libre - izq);

        System.out.println();
        System.out.println(col("1;36", "╔" + "═".repeat(ANCHO - 2) + "╗"));
        System.out.println(col("1;36", "║" + " ".repeat(izq) + texto  + " ".repeat(der) + "║"));
        System.out.println(col("1;36", "╚" + "═".repeat(ANCHO - 2) + "╝"));
    }

    /*
     * Descripcion:
     * Muestro el nombre de una seccion del programa.
     *
     * Entrada:
     * El texto de la seccion.
     *
     * Salida:
     * Una seccion visible en la consola.
     *
     * Restricciones:
     * El texto debe ser corto para mantener la presentacion.
     *
     * Objetivo:
     * Separar visualmente las diferentes fases del analisis.
     */
    private static void seccion(String texto) {
        String cabecera = "━━ " + texto + " ";

        System.out.println();
        System.out.println(col("1;36", cabecera  + "━".repeat(Math.max(3, ANCHO - cabecera.length()))));
        System.out.println();
    }

    /*
     * Descripcion:
     * Muestro un cuadro con el resultado de una operacion.
     *
     * Entrada:
     * El estado de la operacion y los mensajes que se mostraran.
     *
     * Salida:
     * Un cuadro informativo en la consola.
     *
     * Restricciones:
     * Los mensajes deben poder mostrarse dentro del cuadro.
     *
     * Objetivo:
     * Presentar los resultados de forma ordenada.
     */
    private static void caja(boolean correcto, String... lineas) {

        String c = correcto ? "1;32" : "1;31";

        System.out.println(col(c, "┌" + "─".repeat(ANCHO - 2) + "┐"));
        for (String l : lineas) {

            System.out.println(col(c,  String.format("│ %-" + (ANCHO - 4) + "s │", l)));
        }

        System.out.println(col(c, "└" + "─".repeat(ANCHO - 2) + "┘"));
    }

    /*
     * Descripcion:
     * Muestro una etiqueta junto con su valor.
     *
     * Entrada:
     * La etiqueta y el valor que se desea mostrar.
     *
     * Salida:
     * Un dato presentado en la consola.
     *
     * Restricciones:
     * La etiqueta y el valor deben ser datos validos.
     *
     * Objetivo:
     * Facilitar la lectura de la informacion.
     */
    private static void kv(String etiqueta, Object valor) {   System.out.printf("  %-24s : %s%n", etiqueta, valor);
    }

    /*
     * Descripcion:
     * Muestro la cantidad de elementos encontrados.
     *
     * Entrada:
     * La etiqueta y el numero que se desea mostrar.
     *
     * Salida:
     * Un contador visible en la consola.
     *
     * Restricciones:
     * El valor debe ser un numero entero.
     *
     * Objetivo:
     * Mostrar de forma clara la cantidad de tokens o errores.
     */
    private static void contador(String etiqueta, int valor) {

        String numero = String.format("%5d", valor);

        String color = etiqueta.startsWith("Errores")
                ? (valor > 0 ? "1;31" : "1;32")
                : "1;36";

        System.out.println(String.format("  %-24s : ", etiqueta)   + col(color, numero));
    }

    /*
     * Descripcion:
     * Muestro el estado final de una operacion.
     *
     * Entrada:
     * El resultado de la operacion y un mensaje.
     *
     * Salida:
     * Un mensaje indicando si la operacion fue correcta.
     *
     * Restricciones:
     * El estado debe ser verdadero o falso.
     *
     * Objetivo:
     * Informar claramente el resultado de cada fase.
     */
    private static void estado(boolean correcto, String mensaje) {
        System.out.println();
        System.out.println(col(correcto ? "1;32" : "1;31",  "  " + (correcto ? " " : " ") + mensaje));
    }

    /*
     * Descripcion:
     * Organizo un texto para mostrarlo dentro del ancho disponible.
     *
     * Entrada:
     * El texto y los valores usados para ajustar su posicion.
     *
     * Salida:
     * El texto organizado para la consola.
     *
     * Restricciones:
     * El texto debe respetar el ancho definido.
     *
     * Objetivo:
     * Evitar que el contenido se salga del cuadro.
     */
    private static void envolver(String primero, String resto, String texto) {

        int ancho = ANCHO - primero.length();

        StringBuilder linea = new StringBuilder();

        boolean esPrimera = true;

        for (String palabra : texto.split(" ")) {

            if (linea.length() > 0   && linea.length() + 1 + palabra.length() > ancho) {

                System.out.println((esPrimera ? primero : resto) + linea);
                esPrimera = false;
                linea.setLength(0);
            }

            if (linea.length() > 0) {
                linea.append(' ');
            }

            linea.append(palabra);
        }

        System.out.println((esPrimera ? primero : resto) + linea);
    }

    /*
     * Descripcion:
     * Divido un texto en varias lineas segun un ancho.
     *
     * Entrada:
     * El texto y el ancho maximo permitido.
     *
     * Salida:
     * Una lista con el texto dividido en lineas.
     *
     * Restricciones:
     * El ancho debe ser mayor que cero.
     *
     * Objetivo:
     * Mostrar textos largos de forma ordenada.
     */
    private static List<String> ajustar(String texto, int ancho) {

        List<String> lineas = new ArrayList<>();

        StringBuilder actual = new StringBuilder();

        for (String palabra : texto.split(" ")) {

            if (actual.length() > 0  && actual.length() + 1 + palabra.length() > ancho) {

                lineas.add(actual.toString());

                actual.setLength(0);
            }

            if (actual.length() > 0) {

                actual.append(' ');
            }

            actual.append(palabra);
        }

        lineas.add(actual.toString());

        return lineas;
    }

    /*
     * Descripcion:
     * Muestro una nota informativa en la consola.
     *
     * Entrada:
     * El texto de la nota.
     *
     * Salida:
     * Una nota visible para el usuario.
     *
     * Restricciones:
     * El texto debe ser valido.
     *
     * Objetivo:
     * Mostrar informacion adicional durante la ejecucion.
     */
    private static void nota(String texto) {

        List<String> partes = ajustar(texto, ANCHO - 4);

        for (int i = 0; i < partes.size(); i++) {

            System.out.println(   (i == 0 ? col("1;36", "  ℹ ") : "    ") + partes.get(i));
        }
    }

    /*
     * Descripcion:
     * Agrego una fila dentro de un cuadro de informacion.
     *
     * Entrada:
     * El texto normal y el texto con formato.
     *
     * Salida:
     * Una fila preparada para mostrar en la consola.
     *
     * Restricciones:
     * Los textos deben tener el mismo contenido.
     *
     * Objetivo:
     * Mantener ordenada la informacion mostrada.
     */
    private static void fila(String plano, String coloreado) {

        int relleno = Math.max(0, ANCHO - 4 - plano.length());

        String borde = col("31", "│");

        System.out.println(borde + " " + coloreado   + " ".repeat(relleno) + " " + borde);
    }

    /*
     * Descripcion:
     * Muestro un dato dentro de un cuadro de error.
     *
     * Entrada:
     * La etiqueta, el valor y el color utilizado.
     *
     * Salida:
     * El dato mostrado de forma ordenada.
     *
     * Restricciones:
     * La informacion debe poder ajustarse al cuadro.
     *
     * Objetivo:
     * Facilitar la lectura de los detalles del error.
     */
    private static void detalle(String etiqueta, String valor, String color) {

        String prefijo = String.format("%-11s : ", etiqueta);

        List<String> partes = ajustar(valor, ANCHO - 4 - prefijo.length());

        for (int i = 0; i < partes.size(); i++) {

            String p = i == 0 ? prefijo : " ".repeat(prefijo.length());

            fila(p + partes.get(i),   (i == 0 ? col("1", prefijo) : p)    + col(color, partes.get(i)));
        }
    }

    /*
     * Descripcion:
     * Muestro un campo con su respectivo valor.
     *
     * Entrada:
     * La etiqueta y el valor del campo.
     *
     * Salida:
     * El campo mostrado en la consola.
     *
     * Restricciones:
     * La informacion debe ser valida.
     *
     * Objetivo:
     * Organizar los datos de los errores sintacticos.
     */
    private static void campo(String etiqueta, String valor) {
        envolver(
                String.format("     %-11s : ", etiqueta), " ".repeat(19),  valor);
    }

    /*
     * Descripcion:
     * Obtengo una version corta de la ruta de un archivo.
     *
     * Entrada:
     * El archivo cuya ruta se desea mostrar.
     *
     * Salida:
     * La ruta corta del archivo.
     *
     * Restricciones:
     * El archivo debe tener una ruta valida.
     *
     * Objetivo:
     * Evitar mostrar rutas demasiado largas en la consola.
     */
    private static String rutaCorta(File f) {
        try {

            Path base = Paths.get("").toAbsolutePath().normalize();

            Path ruta = f.getCanonicalFile().toPath();

            if (ruta.startsWith(base)) {

                return base.relativize(ruta).toString();
            }

            return ruta.toString();

        } catch (Exception e) {

            return f.getName();
        }
    }

    /*
     * ========================================================
     * MAIN
     * ========================================================
     */

    public static void main(String[] args) throws Exception {

        long inicio = System.nanoTime();

        consola = new PrintStream(

                new FileOutputStream(FileDescriptor.out), true, "UTF-8");

        System.setOut(consola);

        if (args.length < 1) {

            banner("ANALIZADOR · USO");

            System.out.println();

            System.out.println(   "  java Main <archivo_fuente> [archivo_tokens]");

            System.out.println();
            return;
        }

        File archivoFuente = new File(args[0]);

        File archivoTokens = new File(args.length > 1 ? args[1] : "tokens.txt");

        Map<Integer, String> nombres = nombresDeTokens();

        /* ----------------- ENCABEZADO ----------------- */

        banner("ANALIZADOR LÉXICO, SINTÁCTICO Y SEMÁNTICO");

        System.out.println();

        kv("Archivo fuente", rutaCorta(archivoFuente));

        kv("Archivo de tokens", rutaCorta(archivoTokens));

        /* ----------------- FASE 1: LÉXICO ----------------- */

        seccion("FASE 1 · ANÁLISIS LÉXICO");

        List<String> erroresLexicos = analisisLexico(archivoFuente, archivoTokens, nombres);

        contador("Tokens reconocidos", totalTokens);

        contador("Errores léxicos", erroresLexicos.size());

        if (erroresLexicos.isEmpty()) {

            estado(true, "Análisis léxico correcto.");

        } else {

            mostrarTablaLexica(erroresLexicos);
            estado(false, "El archivo contiene errores léxicos.");
        }

        /* ----------------- FASE 2: SINTÁCTICO ----------------- */

        seccion("FASE 2 · ANÁLISIS SINTÁCTICO");

        ParserConErrores analizador = analisisSintactico(archivoFuente);

        List<String> lineasFuente = leerLineas(archivoFuente);

        int numero = 1;

        for (ErrorSintactico e : analizador.errores) {

            mostrarErrorSintactico(numero++, e, lineasFuente);
        }

        if (analizador.errores.isEmpty()) {

            if (analizador.abortado) {

                System.out.println("   El análisis se interrumpió de forma");

                System.out.println("    inesperada antes de terminar.");

            } else {
                estado(true, "Análisis sintáctico correcto.");
            }

        } else {

            mostrarCorreccionGlobal(analizador);

            estado(false, "El archivo contiene errores sintácticos.");
        }

        /* ----------------- FASE 3: SEMÁNTICO ----------------- */

        seccion("FASE 3 · ANÁLISIS SEMÁNTICO");

        /* La tabla la llenó el parser con las acciones semánticas de la gramática. */
        TablaSimbolos tabla = analizador.tabla;

        File reporte = tabla.guardarReporte(archivoFuente);

        contador("Ámbitos", tabla.getAmbitos().size());

        contador("Símbolos declarados", tabla.cantidadSimbolos());

        List<String> erroresSemanticos = tabla.getErrores();

        contador("Errores semánticos", erroresSemanticos.size());

        if (erroresSemanticos.isEmpty()) {

            estado(true, "Análisis semántico correcto.");

        } else {

            mostrarErroresSemanticos(erroresSemanticos);
            estado(false, "El archivo contiene errores semánticos.");
        }

        /* ----------------- RESULTADO FINAL ----------------- */

        seccion("RESULTADO FINAL");

        contador("Tokens reconocidos", totalTokens);

        contador("Errores léxicos", erroresLexicos.size());

        contador("Errores sintácticos", analizador.errores.size());

        contador("Errores semánticos", erroresSemanticos.size());

        int erroresTotales = erroresLexicos.size() + analizador.errores.size() + erroresSemanticos.size();

        System.out.println("  " + "─".repeat(24) + " : " + "─".repeat(5));

        contador("Errores totales", erroresTotales);

        long ms = (System.nanoTime() - inicio) / 1_000_000;

        String tiempo = ms < 1000
                ? ms + " ms"
                : (ms / 1000) + "." + String.format("%03d", ms % 1000) + " s";

        System.out.println(String.format("  %-24s : ", "Tiempo de ejecución") + col("1;36", String.format("%5s", tiempo)));
        System.out.println();

        if (erroresTotales == 0) {

            caja(true, "  ARCHIVO CORRECTO",
                    "El archivo SÍ puede ser generado por la gramática.",
                    "No se encontraron errores léxicos, sintácticos ni semánticos.");

        } else {
            caja(false, "  ARCHIVO CON ERRORES",
                    "El archivo NO puede ser generado por la gramática.",
                    "Corrige los errores indicados y vuelve a ejecutar.");
        }

        /* ----------------- TABLA DE SÍMBOLOS ----------------- */

        System.out.println();

        cuadroReporte(reporte);

        System.out.println();

        if (preguntarSiNo("¿Desea ver la tabla de símbolos? (s/n): ")) {

            seccion("TABLA DE SÍMBOLOS");

            mostrarTablaSimbolosCompleta(tabla);

        } else {

            System.out.println();
            System.out.println(col("90", "  Puede revisarla después en la ruta indicada."));
        }

        System.out.println();
    }

    /*
     * ========================================================
     * TABLA DE SÍMBOLOS EN CONSOLA
     * ========================================================
     */

    /*
     * Descripcion:
     * Pregunto al usuario si o no desde la consola.
     *
     * Entrada:
     * El texto de la pregunta.
     *
     * Salida:
     * Verdadero si responde s/si, falso si responde n/no.
     *
     * Restricciones:
     * Si no hay consola para leer (entrada cerrada), se toma como no.
     *
     * Objetivo:
     * Dejar que el usuario decida si ver la tabla.
     */
    private static boolean preguntarSiNo(String pregunta) {

        java.util.Scanner lector = new java.util.Scanner(System.in, StandardCharsets.UTF_8);

        while (true) {

            System.out.print(col("1;33", "  " + pregunta));
            System.out.flush();

            if (!lector.hasNextLine()) {
                System.out.println();
                return false;
            }

            String r = lector.nextLine().trim().toLowerCase();

            if (r.equals("s") || r.equals("si") || r.equals("sí")) {
                return true;
            }

            if (r.equals("n") || r.equals("no")) {
                return false;
            }

            System.out.println(col("31", "  Respuesta no válida. Escriba s o n."));
        }
    }

    /*
     * Descripcion:
     * Muestro en un cuadro verde dónde quedó guardada la tabla.
     *
     * Entrada:
     * El archivo .txt generado.
     *
     * Salida:
     * Un cuadro con la carpeta y el nombre del archivo.
     *
     * Restricciones:
     * Si la ruta es más ancha que el cuadro, se corta en varias líneas.
     *
     * Objetivo:
     * Que la ubicación del reporte se lea con claridad.
     */
    private static void cuadroReporte(File reporte) {

        String verde = "1;32";
        String titulo = " TABLA DE SÍMBOLOS GENERADA ";

        File carpeta = reporte.getParentFile();
        File proyecto = carpeta == null ? null : carpeta.getParentFile();

        String carpetaCorta = (proyecto == null ? "" : proyecto.getName() + File.separator)
                + (carpeta == null ? "" : carpeta.getName());

        System.out.println(col(verde, "┌─" + titulo
                + "─".repeat(Math.max(1, ANCHO - 3 - titulo.length())) + "┐"));

        filaCuadro("", "", verde);

        filaCuadro("✔ La tabla de símbolos se guardó correctamente.",
                col(verde, "✔ La tabla de símbolos se guardó correctamente."), verde);

        filaCuadro("", "", verde);

        datoCuadro("Archivo", reporte.getName(), "1;37", verde);
        datoCuadro("Carpeta", carpetaCorta, "36", verde);
        datoCuadro("Ruta", reporte.getAbsolutePath(), "90", verde);

        filaCuadro("", "", verde);

        System.out.println(col(verde, "└" + "─".repeat(ANCHO - 2) + "┘"));
    }

    /* Una fila del cuadro: el texto plano se usa para calcular el relleno */
    private static void filaCuadro(String plano, String coloreado, String colorBorde) {

        int relleno = Math.max(0, ANCHO - 4 - plano.length());

        System.out.println(col(colorBorde, "│") + " " + coloreado
                + " ".repeat(relleno) + " " + col(colorBorde, "│"));
    }

    /*
     * Etiqueta : valor dentro del cuadro. Si el valor es una ruta larga,
     * se corta justo después de una barra para que cada carpeta quede entera.
     */
    private static void datoCuadro(String etiqueta, String valor, String color, String colorBorde) {

        String prefijo = String.format("%-8s : ", etiqueta);

        int ancho = ANCHO - 4 - prefijo.length();

        List<String> partes = new ArrayList<>();

        String resto = valor;

        while (resto.length() > ancho) {

            int corte = Math.max(resto.lastIndexOf(File.separatorChar, ancho - 1),
                                 resto.lastIndexOf('/', ancho - 1));

            corte = corte > 0 ? corte + 1 : ancho;   /* sin barra: se corta en el ancho */

            partes.add(resto.substring(0, corte));

            resto = resto.substring(corte);
        }

        partes.add(resto);

        for (int i = 0; i < partes.size(); i++) {

            String p = i == 0 ? prefijo : " ".repeat(prefijo.length());

            filaCuadro(p + partes.get(i), (i == 0 ? col("1", p) : p) + col(color, partes.get(i)), colorBorde);
        }
    }

    /* Anchos de las columnas de la tabla en consola */
    private static final int[] COLUMNAS = {14, 13, 7, 5, 10, 5};

    /*
     * Descripcion:
     * Muestro la tabla de símbolos en consola, con colores:
     * primero el árbol de ámbitos y luego una tabla por ámbito.
     *
     * Entrada:
     * La tabla de símbolos construida por el parser.
     *
     * Salida:
     * La tabla impresa en la consola.
     *
     * Restricciones:
     * Debe caber en el ancho de la consola (76 caracteres).
     *
     * Objetivo:
     * Que la tabla se entienda de un vistazo.
     */
    private static void mostrarTablaSimbolos(TablaSimbolos tabla) {

        /* ---- Árbol de ámbitos ---- */

        System.out.println(col("1", "  Árbol de ámbitos"));
        System.out.println();

        for (String l : tabla.arbolDeAmbitos().split("\n")) {
            System.out.println(col("36", l));
        }

        System.out.println();

        /* ---- Leyenda de colores ---- */

        System.out.println("  " + col("1", "Categorías: ")
                + col("32", "variable") + "  "
                + col("33", "arreglo") + "  "
                + col("35", "parámetro") + "  "
                + col("34;1", "función") + "  "
                + col("36;1", "procedimiento"));

        System.out.println(col("90", "  Dirección: global+n = zona estática · pila+n = registro de activación"));

        System.out.println();

        /* ---- Una tabla por ámbito ---- */

        for (Ambito a : tabla.getAmbitos()) {

            System.out.println(col("1;36", "  ▌ " + a.ruta()));

            System.out.println(col("90", "    nivel " + a.nivel
                    + (a.nivel == 0 ? " (global)" : " (local)")
                    + " · padre: " + (a.anterior == null ? "—" : a.anterior.nombre)
                    + " · tablas hijas: " + a.nombresHijas()));

            if (a.cantidad() == 0) {
                System.out.println(col("90", "    (sin declaraciones)"));
                System.out.println();
                continue;
            }

            System.out.println(col("90", "  " + borde('┌', '┬', '┐')));

            filaTabla(new String[]{"NOMBRE", "CATEGORÍA", "TIPO", "BYTES", "DIRECCIÓN", "LÍNEA"},
                      new String[]{"1", "1", "1", "1", "1", "1"});

            System.out.println(col("90", "  " + borde('├', '┼', '┤')));

            List<String> detalles = new ArrayList<>();

            for (Simbolo s : a.simbolos()) {

                filaTabla(new String[]{
                                corto(s.nombre, COLUMNAS[0]),
                                s.categoria,
                                s.tipo,
                                s.esFuncion() ? "—" : (s.bytes > 0 ? String.valueOf(s.bytes) : "?"),
                                s.direccion(),
                                String.valueOf(s.linea)},
                          new String[]{"1", colorCategoria(s.categoria), "0", "0", "0", "0"});

                /* Información que no cabe en la tabla */
                if (s.esFuncion()) {
                    detalles.add(col("1", s.nombre) + " " + s.firma()
                            + col("90", " → retorna ") + s.tipo
                            + col("90", " · " + s.parametros.size() + " parámetro(s) por valor · registro de " + s.bytes + " B"));
                } else {
                    if ("arreglo".equals(s.categoria)) {
                        detalles.add(col("1", s.nombre) + col("90", " dimensión ") + s.textoDimensionCorta()
                                + col("90", " (" + (s.elementos < 0 ? "?" : String.valueOf(s.elementos)) + " elementos)"));
                    }
                    if (s.valorInicial != null) {
                        detalles.add(col("1", s.nombre) + col("90", " = ") + s.valorInicial);
                    }
                }
            }

            System.out.println(col("90", "  " + borde('└', '┴', '┘')));

            for (String d : detalles) {
                System.out.println(col("90", "    ↳ ") + d);
            }

            System.out.println();
        }
    }

    /* Línea de borde de la tabla: ┌───┬───┐ */
    private static String borde(char izq, char medio, char der) {

        StringBuilder sb = new StringBuilder().append(izq);

        for (int i = 0; i < COLUMNAS.length; i++) {
            sb.append("─".repeat(COLUMNAS[i] + 2));
            sb.append(i < COLUMNAS.length - 1 ? medio : der);
        }
        return sb.toString();
    }

    /* Fila de la tabla: cada celda se rellena y luego se colorea */
    private static void filaTabla(String[] celdas, String[] colores) {

        StringBuilder sb = new StringBuilder("  " + col("90", "│"));

        for (int i = 0; i < COLUMNAS.length; i++) {

            String texto = String.format("%-" + COLUMNAS[i] + "s", celdas[i]);

            sb.append(" ")
              .append("0".equals(colores[i]) ? texto : col(colores[i], texto))
              .append(" ")
              .append(col("90", "│"));
        }

        System.out.println(sb);
    }

    /* ============================================================
     * TABLA DE SÍMBOLOS COMPLETA (una sola tabla con todas las columnas)
     * ============================================================ */

    /* Nº, NOMBRE, CATEGORÍA, TIPO, ÁMBITO, BYTES, DIMENSIÓN, DIRECCIÓN, VALOR INICIAL, ACCESO, LÍNEA, COL */
    private static final String[] TITULOS_TS = {
        "Nº", "NOMBRE", "CATEGORÍA", "TIPO", "ÁMBITO", "BYTES",
        "DIMENSIÓN", "DIRECCIÓN", "VALOR INICIAL", "ACCESO", "LÍNEA", "COL"};

    private static final int[] ANCHOS_TS = {3, 14, 13, 7, 24, 5, 10, 11, 26, 8, 5, 3};

    /*
     * Descripcion:
     * Imprimo la tabla de símbolos con todas sus columnas. Cada ámbito
     * (global, cada función, cada if, while...) es un grupo de filas
     * separado de los demás por una línea.
     *
     * Entrada:  la tabla de símbolos ya llena.
     * Salida:   la tabla en la consola.
     * Restricciones:
     * Es ancha (unos 166 caracteres): conviene una terminal grande.
     * Los textos largos pasan a la línea de abajo dentro de su celda.
     */
    private static void mostrarTablaSimbolosCompleta(TablaSimbolos tabla) {

        /* ---- Árbol de ámbitos: muestra quién es hijo de quién ---- */

        System.out.println(col("1", "  Árbol de ámbitos (cada ámbito es una tabla; los de abajo son sus hijos)"));
        System.out.println();

        for (String l : tabla.arbolDeAmbitos().split("\n")) {
            System.out.println(col("36", l));
        }

        System.out.println();

        /* ---- Leyenda ---- */

        System.out.println("  " + col("1", "Categorías: ")
                + col("32", "variable") + "  "
                + col("33", "arreglo") + "  "
                + col("35", "parámetro") + "  "
                + col("34;1", "función") + "  "
                + col("36;1", "procedimiento"));

        System.out.println(col("90", "  Dirección: global+n = zona estática · pila+n = registro de activación"));
        System.out.println(col("90", "  Acceso:    público = declarado en el ámbito global · privado = dentro de una función o bloque"));
        System.out.println();

        /* ---- La tabla ---- */

        System.out.println(col("90", "  " + bordeTS('┌', '┬', '┐')));

        String[] colTitulos = new String[TITULOS_TS.length];
        java.util.Arrays.fill(colTitulos, "1;36");
        filaTS(TITULOS_TS, colTitulos);

        System.out.println(col("90", "  " + bordeTS('├', '┼', '┤')));

        int numero = 1;
        boolean primero = true;

        for (Ambito a : tabla.getAmbitos()) {

            /* Un ámbito sin símbolos no tiene filas: aparece solo en el árbol */
            if (a.cantidad() == 0) {
                continue;
            }

            /* Línea que separa un ámbito (una tabla) del siguiente */
            if (!primero) {
                System.out.println(col("90", "  " + bordeTS('├', '┼', '┤')));
            }
            primero = false;

            String ambito = nombreAmbito(a);

            for (Simbolo s : a.simbolos()) {

                String[] celdas = {
                    String.valueOf(numero++),
                    s.nombre,
                    s.categoria,
                    s.tipo,
                    ambito,
                    s.esFuncion() ? "—" : (s.bytes > 0 ? String.valueOf(s.bytes) : "?"),
                    s.esFuncion() ? "—" : s.textoDimensionCorta(),
                    s.direccion(),
                    s.esFuncion() || s.valorInicial == null ? "—" : s.valorInicial,
                    s.acceso,
                    String.valueOf(s.linea),
                    String.valueOf(s.columna)
                };

                String[] colores = {
                    "0", "1", colorCategoria(s.categoria), "0", "36", "0",
                    "arreglo".equals(s.categoria) ? "33" : "90", "0", "0",
                    "público".equals(s.acceso) ? "32" : "90", "0", "0"
                };

                filaTS(celdas, colores);
            }
        }

        System.out.println(col("90", "  " + bordeTS('└', '┴', '┘')));
        System.out.println();
    }

    /* "global › funcion func1 › if"  →  "función func1 › if" (el global se queda como "global") */
    private static String nombreAmbito(Ambito a) {
        String ruta = a.ruta();
        if (a.nivel > 0 && ruta.startsWith("global › ")) {
            ruta = ruta.substring("global › ".length());
        }
        return ruta.replace("funcion ", "función ");
    }

    /* Línea de borde de la tabla completa */
    private static String bordeTS(char izq, char medio, char der) {

        StringBuilder sb = new StringBuilder().append(izq);

        for (int i = 0; i < ANCHOS_TS.length; i++) {
            sb.append("─".repeat(ANCHOS_TS[i] + 2));
            sb.append(i < ANCHOS_TS.length - 1 ? medio : der);
        }
        return sb.toString();
    }

    /* Una fila de la tabla completa; si una celda es larga, la fila ocupa varias líneas */
    private static void filaTS(String[] celdas, String[] colores) {

        List<List<String>> partes = new ArrayList<>();
        int alto = 1;

        for (int i = 0; i < ANCHOS_TS.length; i++) {
            List<String> l = partirTexto(celdas[i], ANCHOS_TS[i]);
            partes.add(l);
            alto = Math.max(alto, l.size());
        }

        for (int r = 0; r < alto; r++) {

            StringBuilder sb = new StringBuilder("  " + col("90", "│"));

            for (int i = 0; i < ANCHOS_TS.length; i++) {

                String t = r < partes.get(i).size() ? partes.get(i).get(r) : "";
                String texto = String.format("%-" + ANCHOS_TS[i] + "s", t);

                sb.append(" ")
                  .append("0".equals(colores[i]) ? texto : col(colores[i], texto))
                  .append(" ")
                  .append(col("90", "│"));
            }

            System.out.println(sb);
        }
    }

    /* Corta un texto en líneas de máximo "ancho" caracteres, sin partir palabras si se puede */
    private static List<String> partirTexto(String texto, int ancho) {

        List<String> lineas = new ArrayList<>();
        StringBuilder actual = new StringBuilder();

        for (String palabra : texto.split(" ")) {

            while (palabra.length() > ancho) {
                if (actual.length() > 0) {
                    lineas.add(actual.toString());
                    actual.setLength(0);
                }
                lineas.add(palabra.substring(0, ancho));
                palabra = palabra.substring(ancho);
            }

            if (actual.length() > 0 && actual.length() + 1 + palabra.length() > ancho) {
                lineas.add(actual.toString());
                actual.setLength(0);
            }

            if (actual.length() > 0) {
                actual.append(' ');
            }
            actual.append(palabra);
        }

        if (actual.length() > 0 || lineas.isEmpty()) {
            lineas.add(actual.toString());
        }
        return lineas;
    }

    private static String colorCategoria(String categoria) {
        switch (categoria) {
            case "variable":      return "32";
            case "arreglo":       return "33";
            case "parámetro":     return "35";
            case "función":       return "34;1";
            case "procedimiento": return "36;1";
            default:              return "0";
        }
    }

    private static String corto(String texto, int max) {
        return texto.length() <= max ? texto : texto.substring(0, max - 1) + "…";
    }

    /*
     * ========================================================
     * FASE 1: LÉXICO
     * ========================================================
     */
    /*
     * Descripcion:
     * Realizo el analisis lexico del archivo fuente.
     *
     * Entrada:
     * El archivo fuente, el archivo de tokens y los nombres de los tokens.
     *
     * Salida:
     * Genero los tokens y registro los errores lexicos.
     *
     * Restricciones:
     * El archivo debe seguir las reglas del lenguaje.
     *
     * Objetivo:
     * Identificar los elementos validos del archivo fuente.
     */
    private static List<String> analisisLexico( File fuente,  File tokens,  Map<Integer, String> nombres) throws Exception {

        /* Se silencia lo que el Lexer imprima por su cuenta. */
        PrintStream original = System.out;

        System.setOut(new PrintStream(OutputStream.nullOutputStream()));

        try (
                Reader lector = new InputStreamReader(  new FileInputStream(fuente),  StandardCharsets.UTF_8);

                PrintWriter salida = new PrintWriter(tokens, "UTF-8")) {

            Lexer lexer = new Lexer(lector);

            salida.printf("%-8s %-8s %-18s %s%n",  "LINEA", "COLUMNA", "TOKEN", "LEXEMA");

            salida.println("-".repeat(60));

            totalTokens = 0;

            Symbol token = lexer.next_token();

            while (token.sym != sym.EOF) {

                salida.printf("%-8d %-8d %-18s %s%n",   token.left,   token.right,   nombres.getOrDefault(token.sym, "DESCONOCIDO"), token.value);

                totalTokens++;

                token = lexer.next_token();
            }

            List<String> errores = lexer.getErrores();

            if (!errores.isEmpty()) {
                salida.println();

                salida.println("ERRORES LEXICOS (" + errores.size() + ")");

                salida.println("-".repeat(60));

                for (String error : errores) {
                    salida.println(error);
                }
            }

            return errores;

        } finally {
            System.setOut(original);
        }
    }

    /*
     * Descripcion:
     * Muestro una tabla de errores (léxicos o semánticos).
     *
     * Entrada:
     * La lista de errores con el formato "Error ... en línea L, columna C: detalle".
     *
     * Salida:
     * Una tabla con la informacion de cada error.
     *
     * Restricciones:
     * Los errores deben tener el formato esperado.
     *
     * Objetivo:
     * Facilitar la identificacion de los errores.
     */
    private static void mostrarTablaLexica(List<String> errores) {

        System.out.println();

        System.out.println(col("1;36", String.format("  %-4s %6s %5s   %s",  "Nº", "LÍNEA", "COL", "DESCRIPCIÓN")));

        System.out.println(col("90", "  " + "─".repeat(4) + " "+ "─".repeat(6) + " " + "─".repeat(5) + "   "  + "─".repeat(ANCHO - 24)));

        int numero = 1;

        for (String error : errores) {

            String texto = error == null
                    ? ""
                    : error.trim().replace("\r", " ").replace("\n", " ");

            Matcher m = PATRON_ERROR.matcher(texto);

            String linea = "-";

            String columna = "-";

            String detalle = texto;

            if (m.matches()) {
                linea = m.group(1);

                columna = m.group(2);

                detalle = m.group(3);
            }

            String num = String.format("%02d", numero);

            String planoPrefijo = String.format("  %-4s %6s %5s   ",
                    num, linea, columna);

            String colorPrefijo = "  "
                    + col("90", String.format("%-4s", num)) + " "
                    + col("33", String.format("%6s", linea)) + " "
                    + col("33", String.format("%5s", columna)) + "   ";

            List<String> partes = ajustar(detalle, ANCHO - planoPrefijo.length());

            for (int i = 0; i < partes.size(); i++) {
                System.out.println(
                        (i == 0  ? colorPrefijo  : " ".repeat(planoPrefijo.length()))    + partes.get(i));
            }

            numero++;
        }
    }

    /*
     * ========================================================
     * ERRORES SEMÁNTICOS (una línea por error, con colores)
     * ========================================================
     */

    /* Ancho máximo de esta sección: cada error cabe en una sola línea.
       El ancho que se usa de verdad se ajusta a la descripción más larga. */
    private static final int ANCHO_ERR = 170;

    /* Largo del prefijo de cada línea: Nº, LÍNEA, COL y VALIDACIÓN */
    private static final int PREFIJO_ERR = 37;

    /* El parser entrega cada error como "[ARI-3] mensaje": grupo 1 = ARI, 2 = 3, 3 = mensaje */
    private static final Pattern PATRON_REGLA = Pattern.compile("^\\[([A-Z]+)-(\\d+)\\]\\s*(.*)$");

    /* Familia de reglas -> nombre de la validación */
    private static final Map<String, String> FAMILIAS = new HashMap<>();

    static {
        FAMILIAS.put("TS",  "Nombres");
        FAMILIAS.put("ARI", "Aritmética");
        FAMILIAS.put("REL", "Comparación");
        FAMILIAS.put("LOG", "Lógica");
        FAMILIAS.put("ASG", "Asignación");
        FAMILIAS.put("ARR", "Arreglos");
        FAMILIAS.put("FUN", "Funciones");
        FAMILIAS.put("RET", "Return");
        FAMILIAS.put("CON", "Control");
        FAMILIAS.put("INC", "Incremento");
        FAMILIAS.put("ES",  "Entrada/Salida");
        FAMILIAS.put("PRG", "Programa");
    }

    /* Un solo color para la validación, para que la lista se lea sin ruido */
    private static final String COLOR_VALIDACION = "36";    /* cian: como los encabezados */

    /*
     * Descripcion:
     * Muestro los errores semánticos. Cada error ocupa una línea con el
     * número, la línea y la columna del código, la regla, el tipo de
     * validación (con su color) y la descripción.
     *
     * Entrada:
     * La lista de errores ("Error ... en línea L, columna C: [REGLA] detalle").
     *
     * Salida:
     * Los errores en la consola, y al final un resumen por tipo de validación.
     *
     * Restricciones:
     * Si una descripción no cabe en el ancho, continúa en la línea siguiente.
     *
     * Objetivo:
     * Que cada error se entienda de un vistazo.
     */
    private static void mostrarErroresSemanticos(List<String> errores) {

        /* ---- 1. Se analiza cada error para saber cuánto mide la descripción más larga ---- */

        List<String[]> filas = new ArrayList<>();   /* {línea, columna, validación, detalle} */

        int masLarga = 0;

        for (String error : errores) {

            String texto = error == null ? "" : error.trim().replace("\r", " ").replace("\n", " ");

            String linea = "-";
            String columna = "-";
            String detalle = texto;

            Matcher m = PATRON_ERROR.matcher(texto);

            if (m.matches()) {
                linea = m.group(1);
                columna = m.group(2);
                detalle = m.group(3);
            }

            /* Tipo de validación (sale del código de la regla: [ARI-3] -> Aritmética) */
            String validacion = "Semántico";

            Matcher r = PATRON_REGLA.matcher(detalle);

            if (r.matches()) {
                detalle = r.group(3);
                validacion = FAMILIAS.getOrDefault(r.group(1), validacion);
            }

            filas.add(new String[]{linea, columna, validacion, detalle});

            masLarga = Math.max(masLarga, detalle.length());
        }

        /* Ancho de la sección: lo que pide la descripción más larga, sin pasar de ANCHO_ERR */
        int ancho = Math.min(ANCHO_ERR, Math.max(100, PREFIJO_ERR + masLarga));

        /* ---- 2. Se muestra ---- */

        System.out.println();

        System.out.println(col("1;36", String.format("  %-4s %6s %5s  %-15s %s",
                "Nº", "LÍNEA", "COL", "VALIDACIÓN", "DESCRIPCIÓN")));

        System.out.println(col("90", "  " + "─".repeat(ancho - 2)));

        /* validación -> cantidad (en orden de aparición) */
        Map<String, Integer> conteo = new LinkedHashMap<>();

        int numero = 1;

        for (String[] f : filas) {

            conteo.merge(f[2], 1, Integer::sum);

            String num = String.format("%02d", numero++);

            String prefijoColor = "  "
                    + col("90", String.format("%-4s", num)) + " "
                    + col("33", String.format("%6s", f[0])) + " "
                    + col("33", String.format("%5s", f[1])) + "  "
                    + col(COLOR_VALIDACION, String.format("%-15s", f[2])) + " ";

            List<String> partes = ajustar(f[3], ancho - PREFIJO_ERR);

            for (int i = 0; i < partes.size(); i++) {
                System.out.println((i == 0 ? prefijoColor : " ".repeat(PREFIJO_ERR)) + partes.get(i));
            }
        }

        /* ---- 3. Resumen por tipo de validación ---- */

        System.out.println(col("90", "  " + "─".repeat(ancho - 2)));

        StringBuilder plano = new StringBuilder("  Por validación: ");
        StringBuilder color = new StringBuilder("  " + col("1", "Por validación: "));

        for (Map.Entry<String, Integer> e : conteo.entrySet()) {

            String item = e.getKey() + " " + e.getValue();

            if (plano.length() + item.length() + 5 > ancho) {
                System.out.println(color);
                plano = new StringBuilder("    ");
                color = new StringBuilder("    ");
            }

            plano.append(item).append("   ");
            color.append(col(COLOR_VALIDACION, e.getKey())).append(" ")
                 .append(col("1;33", String.valueOf(e.getValue()))).append("   ");
        }

        System.out.println(color);
    }

    /*
     * ========================================================
     * FASE 2: SINTÁCTICO
     * ========================================================
     */
    /*
     * Descripcion:
     * Realizo el analisis sintactico del archivo fuente.
     *
     * Entrada:
     * El archivo .cmm que se desea analizar.
     *
     * Salida:
     * Obtengo los errores sintacticos encontrados.
     *
     * Restricciones:
     * El codigo debe seguir las reglas definidas en la gramatica.
     *
     * Objetivo:
     * Verificar que la estructura del programa sea correcta.
     */
    private static ParserConErrores analisisSintactico(File fuente)

            throws Exception {

        /* Se silencia lo que el Lexer imprima mientras el parser lee. */
        PrintStream original = System.out;

        System.setOut(new PrintStream(OutputStream.nullOutputStream()));

        ParserConErrores analizador;

        try (Reader lector = new InputStreamReader(

                new FileInputStream(fuente), StandardCharsets.UTF_8)) {

            analizador = new ParserConErrores(new Lexer(lector));

            try {
                analizador.parse();

            } catch (Exception e) {

                analizador.abortado = true;
            }

        } finally {
            System.setOut(original);
        }

        return analizador;
    }

    /*
     * Descripcion:
     * Leo todas las lineas del archivo fuente.
     *
     * Entrada:
     * El archivo .cmm que se desea leer.
     *
     * Salida:
     * Una lista con las lineas del archivo.
     *
     * Restricciones:
     * El archivo debe estar disponible y usar UTF-8.
     *
     * Objetivo:
     * Tener las lineas disponibles para mostrar los errores.
     */
    private static List<String> leerLineas(File fuente) {
        try {

            return Files.readAllLines(  fuente.toPath(), StandardCharsets.UTF_8);

        } catch (Exception e) {

            return new ArrayList<>();
        }
    }

    /*
     * Descripcion:
     * Muestro la informacion de un error sintactico.
     *
     * Entrada:
     * El numero del error, sus datos y las lineas del archivo.
     *
     * Salida:
     * Un cuadro con los detalles del error.
     *
     * Restricciones:
     * El error debe contener informacion de linea y columna.
     *
     * Objetivo:
     * Facilitar la ubicacion del problema en el codigo.
     */
    private static void mostrarErrorSintactico(  int numero, ErrorSintactico e,  List<String> lineas) {

        final int W = ANCHO - 4;

        String ubicacion = e.linea > 0
                ? "línea " + e.linea + ", columna " + e.columna
                : "final del archivo";

        String titulo = " ERROR " + String.format("%02d", numero)
                + " · " + ubicacion + " ";

        /* ---- Borde superior con título ---- */

        System.out.println(
                col("31", "┌─")
                        + col("1;31", titulo)
                        + col("31", "─".repeat(  Math.max(1, ANCHO - 3 - titulo.length())) + "┐"));

        fila("", "");

        /* ---- Línea de código con indicador ---- */

        if (e.linea >= 1 && e.linea <= lineas.size()) {

            int gw = Math.max(3, String.valueOf(e.linea).length());

            String texto = lineas.get(e.linea - 1)

                    .replace("\t", " ")

                    .replaceAll("\\s+$", "");

            int disp = W - gw - 3;

            int pos = Math.max(0, e.columna - 1);

            int inicio = 0;

            /* Si la línea es muy larga, se muestra la zona del error. */
            if (texto.length() > disp) {
                inicio = Math.max(0, Math.min(pos - disp / 3, texto.length() - disp));
                texto = texto.substring(inicio,    Math.min(texto.length(), inicio + disp));
            }

            String num = String.format("%" + gw + "d", e.linea);

            fila(num + " │ " + texto,  col("90", num + " │ ") + texto);

            if (!e.fin && e.columna > 0) {

                String lexema = e.lexema == null ? "" : e.lexema;

                int c = Math.max(0, pos - inicio);

                int largo = Math.max(1,   Math.min(Math.max(1, lexema.length()), disp - c));

                String guia = " ".repeat(gw) + " │ ";

                fila(guia + " ".repeat(c) + "^".repeat(largo),   col("90", guia) + " ".repeat(c)   + col("1;31", "^".repeat(largo)));
            }

            fila("", "");
        }

        if (e.mensaje != null) {

            /* El parser lo corrigió por su cuenta: se muestra con qué técnica */
            detalle("Técnica", e.tecnica, "36");

            if (!e.esperados.isEmpty()) {
                detalle("Se esperaba", String.join(", ", e.esperados), "32");
            }

            detalle("Detalle", e.mensaje, "1;33");

        } else {

            /* ---- Qué se encontró ---- */

            if (e.fin) {

                detalle("Se encontró", "fin de archivo", "1;33");

            } else {

                String lexema = e.lexema == null ? "" : e.lexema;

                if (lexema.length() > 40) {

                    lexema = lexema.substring(0, 39) + "…";
                }

                String hallado = "'" + lexema + "'";

                String token = "(" + e.token + ")";

                String prefijo = String.format("%-11s : ", "Se encontró");

                fila(prefijo + hallado + "  " + token,
                        col("1", prefijo)
                                + col("1;33", hallado)
                                + "  " + col("90", token));
            }

            /* ---- Qué se esperaba (máximo 6 opciones) ---- */

            if (!e.esperados.isEmpty()) {

                List<String> lista = new ArrayList<>(e.esperados);

                boolean hayMas = lista.remove("…");

                if (lista.size() > 6) {
                    lista = new ArrayList<>(lista.subList(0, 6));
                    hayMas = true;
                }

                detalle("Se esperaba",
                        String.join(", ", lista) + (hayMas ? ", …" : ""),
                        "32");
            }

            if (e.fin) {
                detalle("Sugerencia",
                        "Puede faltar un cierre de bloque o un terminador.",
                        "36");
            }
        }

        /* ---- Borde inferior y espacio entre errores ---- */

        fila("", "");

        System.out.println(  col("31", "└" + "─".repeat(ANCHO - 2) + "┘"));

        System.out.println();
    }

    /*
     * Descripcion:
     * Guarda la informacion de un error sintactico.
     *
     * Entrada:
     * Los datos obtenidos durante el analisis.
     *
     * Salida:
     * Un objeto con la informacion del error.
     *
     * Restricciones:
     * Depende de los datos proporcionados por el parser.
     *
     * Objetivo:
     * Mantener organizada la informacion de cada error.
     */
    private static class ErrorSintactico {
        int linea;
        int columna;
        boolean fin;
        String token;
        String lexema;
        List<String> esperados = new ArrayList<>();

        /* Técnica de recuperación que lo produjo y qué dice (null en un error normal) */
        String tecnica;
        String mensaje;

        /* Para la corrección global: dónde está en el flujo de tokens y qué tokens se esperaban */
        int indice = -1;
        List<Integer> ids = new ArrayList<>();
    }

    /*
     * ========================================================
     * CORRECCIÓN GLOBAL
     * ========================================================
     */

    /* Una edición del archivo: insertar, reemplazar o eliminar un token */
    private static class Edicion {
        String tipo;
        String sobra;
        String nuevo;
        int linea;
        int columna;
        List<Symbol> flujo;
        int errores;
    }

    /* Entrega al parser los tokens de una lista, en vez de leerlos del archivo */
    private static class ListaDeTokens implements java_cup.runtime.Scanner {

        private final List<Symbol> tokens;
        private int pos = 0;

        ListaDeTokens(List<Symbol> tokens) {
            this.tokens = tokens;
        }

        @Override
        public Symbol next_token() {
            Symbol s = tokens.get(Math.min(pos, tokens.size() - 1));
            pos++;
            return new Symbol(s.sym, s.left, s.right, s.value);
        }
    }

    /*
     * Descripcion:
     * Corrección global: busca el menor número de ediciones (insertar, reemplazar o
     * eliminar un token) que dejan el archivo con menos errores sintácticos.
     * Cada edición posible se prueba volviendo a analizar el archivo COMPLETO, y se
     * queda con la que más errores elimina.
     *
     * Entrada:
     * El parser que ya analizó el archivo (con sus errores y su flujo de tokens).
     *
     * Salida:
     * Una sugerencia en pantalla con las ediciones. No cambia el archivo ni los
     * conteos de errores.
     *
     * Restricciones:
     * Solo se usa si el modo pánico tuvo que intervenir. Prueba como máximo 6
     * ediciones y solo con tokens estructurales (no inventa identificadores ni números).
     *
     * Objetivo:
     * Decirle al programador qué cambiar para que el archivo sea sintácticamente válido.
     */
    private static void mostrarCorreccionGlobal(ParserConErrores base) {

        boolean hayPanico = false;

        for (ErrorSintactico e : base.errores) {
            if (e.mensaje == null) {
                hayPanico = true;
            }
        }

        if (!hayPanico) {
            return;
        }

        List<Symbol> flujo = completarFlujo(base.flujo);
        ParserConErrores actual = base;
        int inicial = costo(base);
        List<Edicion> aplicadas = new ArrayList<>();

        for (int ronda = 0; ronda < 6 && costo(actual) > 0; ronda++) {

            ErrorSintactico objetivo = null;

            for (ErrorSintactico e : actual.errores) {
                if (e.mensaje == null && e.indice >= 0) {
                    objetivo = e;
                    break;
                }
            }

            if (objetivo == null) {
                break;
            }

            Edicion mejor = null;

            for (Edicion c : edicionesPosibles(flujo, objetivo)) {

                ParserConErrores p = analizarFlujo(c.flujo);

                c.errores = costo(p);

                if (c.errores < costo(actual) && (mejor == null || c.errores < mejor.errores)) {
                    mejor = c;
                }
            }

            if (mejor == null) {
                break;
            }

            aplicadas.add(mejor);
            flujo = mejor.flujo;
            actual = analizarFlujo(flujo);
        }

        if (aplicadas.isEmpty()) {
            return;
        }

        String cian = "36";

        System.out.println(col(cian, "┌─ CORRECCIÓN GLOBAL (sugerencia) "
                + "─".repeat(Math.max(1, ANCHO - 35)) + "┐"));

        filaCuadro("", "", cian);

        List<String> lineas = new ArrayList<>();

        lineas.add("Se buscó el menor número de ediciones (insertar, reemplazar o eliminar un token) "
                + "que deja el archivo con menos errores sintácticos. Cada edición se probó "
                + "volviendo a analizar el archivo completo.");
        lineas.add("");

        int antes = inicial;
        int numero = 1;

        for (Edicion c : aplicadas) {

            String que;

            if (c.tipo.equals("insertar")) {
                que = "Insertar '" + c.nuevo + "' antes de '" + c.sobra + "'";
            } else if (c.tipo.equals("reemplazar")) {
                que = "Reemplazar '" + c.sobra + "' por '" + c.nuevo + "'";
            } else {
                que = "Eliminar '" + c.sobra + "'";
            }

            lineas.add(numero++ + ". " + que + " (línea " + c.linea + ", columna " + c.columna
                    + "): errores sintácticos " + cuenta(antes) + " → " + cuenta(c.errores));

            antes = c.errores;
        }

        lineas.add("");
        lineas.add(costo(actual) == 0
                ? "Con " + aplicadas.size() + " edición(es) el archivo queda sin errores sintácticos."
                : "Con " + aplicadas.size() + " edición(es) los errores sintácticos pasan de "
                        + cuenta(inicial) + " a " + cuenta(costo(actual)) + ".");

        for (String texto : lineas) {
            if (texto.isEmpty()) {
                filaCuadro("", "", cian);
                continue;
            }
            for (String parte : ajustar(texto, ANCHO - 4)) {
                filaCuadro(parte, parte, cian);
            }
        }

        filaCuadro("", "", cian);

        System.out.println(col(cian, "└" + "─".repeat(ANCHO - 2) + "┘"));
        System.out.println();
    }

    /* Qué tan malo es un análisis: sus errores, o muchísimo más si el parser tuvo que rendirse */
    private static int costo(ParserConErrores p) {
        return p.abortado ? 1_000_000 + p.errores.size() : p.errores.size();
    }

    private static String cuenta(int costo) {
        return costo >= 1_000_000
                ? (costo - 1_000_000) + " (se interrumpe)"
                : String.valueOf(costo);
    }

    /* Analiza una lista de tokens con un parser nuevo y silencioso */
    private static ParserConErrores analizarFlujo(List<Symbol> tokens) {

        PrintStream original = System.out;

        System.setOut(new PrintStream(OutputStream.nullOutputStream()));

        ParserConErrores p = new ParserConErrores(new ListaDeTokens(tokens));

        try {
            p.parse();
        } catch (Exception e) {
            p.abortado = true;
        } finally {
            System.setOut(original);
        }

        return p;
    }

    /* Copia del flujo, asegurando que termine con EOF */
    private static List<Symbol> completarFlujo(List<Symbol> original) {

        List<Symbol> copia = new ArrayList<>(original);

        if (copia.isEmpty() || copia.get(copia.size() - 1).sym != sym.EOF) {

            Symbol ultimo = copia.isEmpty() ? null : copia.get(copia.size() - 1);

            copia.add(new Symbol(sym.EOF, ultimo == null ? 1 : ultimo.left,
                    ultimo == null ? 1 : ultimo.right, null));
        }

        return copia;
    }

    /* Solo se insertan o usan tokens estructurales: no se inventan identificadores ni números */
    private static boolean esEstructural(int id) {

        String nombre = nombreToken(id);

        return id != 1
                && !nombre.equals("ID")
                && !nombre.startsWith("LITERAL_")
                && !nombre.equals("EOF")
                && !nombre.equals("DESCONOCIDO");
    }

    /* Texto con el que se muestra un token */
    private static String textoDe(Symbol s) {

        if (s.sym != sym.EOF && s.value != null && !String.valueOf(s.value).isEmpty()) {
            return String.valueOf(s.value);
        }

        return s.sym == sym.EOF ? "fin de archivo" : legible(nombreToken(s.sym));
    }

    /* Las ediciones que se prueban en el lugar del error. Si dos arreglan lo mismo,
       gana la primera: eliminar, después insertar y por último reemplazar. */
    private static List<Edicion> edicionesPosibles(List<Symbol> flujo, ErrorSintactico e) {

        List<Edicion> lista = new ArrayList<>();

        int k = e.indice;

        if (k < 0 || k >= flujo.size()) {
            return lista;
        }

        Symbol actual = flujo.get(k);

        /* Eliminar el token del error y, si hay, el que lo precede (un operador de más, por ejemplo) */
        for (int i = k; i >= Math.max(0, k - 1); i--) {

            Symbol t = flujo.get(i);

            if (t.sym == sym.EOF) {
                continue;
            }

            Edicion ed = new Edicion();
            ed.tipo = "eliminar";
            ed.sobra = textoDe(t);
            ed.linea = t.left;
            ed.columna = t.right;
            ed.flujo = new ArrayList<>(flujo);
            ed.flujo.remove(i);
            lista.add(ed);
        }

        Set<Integer> esperados = new LinkedHashSet<>(e.ids);

        /* Insertar un token que se esperaba, justo antes del token del error */
        for (int id : esperados) {

            if (!esEstructural(id)) {
                continue;
            }

            String nuevo = legible(nombreToken(id));

            Edicion ed = new Edicion();
            ed.tipo = "insertar";
            ed.sobra = textoDe(actual);
            ed.nuevo = nuevo;
            ed.linea = actual.left;
            ed.columna = actual.right;
            ed.flujo = new ArrayList<>(flujo);
            ed.flujo.add(k, new Symbol(id, actual.left, actual.right, nuevo));
            lista.add(ed);
        }

        /* Reemplazar el token del error por uno que se esperaba */
        if (actual.sym != sym.EOF) {

            for (int id : esperados) {

                if (!esEstructural(id)) {
                    continue;
                }

                String nuevo = legible(nombreToken(id));

                Edicion ed = new Edicion();
                ed.tipo = "reemplazar";
                ed.sobra = textoDe(actual);
                ed.nuevo = nuevo;
                ed.linea = actual.left;
                ed.columna = actual.right;
                ed.flujo = new ArrayList<>(flujo);
                ed.flujo.set(k, new Symbol(id, actual.left, actual.right, nuevo));
                lista.add(ed);
            }
        }

        return lista;
    }

    /*
     * ========================================================
     * PARSER CON CONTROL DE ERRORES
     * ========================================================
     *
     * Descripcion:
     * Esta clase controla los errores encontrados por el parser.
     *
     * Entrada:
     * Recibe los tokens generados por el Lexer.
     *
     * Salida:
     * Guarda los errores para que Main los muestre.
     *
     * Restricciones:
     * Depende del parser generado por CUP.
     *
     * Objetivo:
     * Controlar los errores sin mostrar mensajes directamente.
     */
    private static class ParserConErrores extends parser {

        /* Errores sintacticos encontrados. */
        private final List<ErrorSintactico> errores = new ArrayList<>();

        /* Indica si el analisis fue interrumpido por un error grave. */
        private boolean abortado = false;

        /* Ultimo token real (no EOF), para ubicar errores al final del archivo. */
        private Symbol ultimoReal = null;

        /*
         * Descripcion:
         * Creo el parser usando el Lexer recibido.
         *
         * Entrada:
         * El objeto Lexer que genera los tokens.
         *
         * Salida:
         * Un parser preparado para analizar el archivo.
         *
         * Restricciones:
         * El Lexer debe estar disponible.
         *
         * Objetivo:
         * Conectar el lexer con el parser.
         */
        public ParserConErrores(java_cup.runtime.Scanner lexer) {
            super(lexer);
        }

        /* Todos los tokens que el parser leyó, en orden (la corrección global los vuelve a analizar) */
        private final List<Symbol> flujo = new ArrayList<>();

        /* Posición de un token dentro del flujo (-1 si no está) */
        private int indiceDe(Symbol t) {
            for (int i = flujo.size() - 1; i >= 0; i--) {
                if (flujo.get(i) == t) {
                    return i;
                }
            }
            return -1;
        }

        /*
         * Descripcion:
         * Defino la cantidad de tokens usada para recuperarse de un error.
         *
         * Entrada:
         * El estado actual del parser.
         *
         * Salida:
         * Retorna la cantidad de tokens usada para sincronizar.
         *
         * Restricciones:
         * Se utiliza un solo token para recuperar el analisis.
         *
         * Objetivo:
         * Permitir que el parser continue rapidamente despues de un error.
         */
        @Override
        protected int error_sync_size() {
            return 1;
        }

        /*
         * Descripcion:
         * Obtengo el siguiente token del lexer.
         *
         * Entrada:
         * Los tokens generados por el Lexer.
         *
         * Salida:
         * Retorna el siguiente token disponible.
         *
         * Restricciones:
         * El token EOF no se guarda como ultimo token real.
         *
         * Objetivo:
         * Mantener registrado el ultimo token valido.
         */
        @Override
        public Symbol scan() throws Exception {

            Symbol s = super.scan();

            if (s != null) {
                flujo.add(s);
            }

            if (s != null && s.sym != sym.EOF) {
                ultimoReal = s;
            }

            return s;
        }

        /*
         * Descripcion:
         * Registro la informacion de un error sintactico.
         *
         * Entrada:
         * El token donde se encontro el error.
         *
         * Salida:
         * Un error guardado en la lista de errores.
         *
         * Restricciones:
         * La ubicacion depende de la informacion entregada por el Lexer.
         *
         * Objetivo:
         * Guardar todos los datos necesarios para mostrar el error.
         */
        @Override
        public void syntax_error(Symbol t) {

            ErrorSintactico e = new ErrorSintactico();

            if (t != null) {

                e.fin = t.sym == sym.EOF;

                e.token = nombreToken(t.sym);

                e.lexema = t.value == null ? null : String.valueOf(t.value);

                /* El Lexer ya entrega línea y columna con base 1. */
                if (t.left > 0) {
                    e.linea = t.left;

                    e.columna = t.right;

                } else if (e.fin && ultimoReal != null) {

                    e.linea = ultimoReal.left;

                    e.columna = 0;
                }
            }

            e.esperados = esperados();

            e.indice = indiceDe(t);

            try {
                for (int id : expected_token_ids()) {
                    e.ids.add(id);
                }
            } catch (Throwable ex) {
                /* sin tokens esperados */
            }

            errores.add(e);
        }

        /*
         * Descripcion:
         * Evito mostrar directamente los errores del parser.
         *
         * Entrada:
         * El mensaje y la informacion del error.
         *
         * Salida:
         * No muestra ningun mensaje.
         *
         * Restricciones:
         * Los errores se manejan desde Main.
         *
         * Objetivo:
         * Mantener un solo formato para mostrar los errores.
         */
        @Override
        public void report_error(String message, Object info) {
        }

        /*
         * Descripcion:
         * Marco cuando ocurre un error que interrumpe el analisis.
         *
         * Entrada:
         * El mensaje y la informacion del error.
         *
         * Salida:
         * El estado abortado queda activo.
         *
         * Restricciones:
         * Solo se activa cuando CUP reporta un error fatal.
         *
         * Objetivo:
         * Informar a Main que el analisis no pudo continuar.
         */
        @Override
        public void report_fatal_error(String message, Object info) {
            abortado = true;
        }

        /*
         * Descripcion:
         * Registro un token que faltaba y que el parser insertó por su cuenta
         * (recuperación a nivel de frase).
         *
         * Entrada:
         * El token que faltaba y el lugar donde debía ir.
         *
         * Salida:
         * Un error sintáctico con un mensaje específico.
         *
         * Restricciones:
         * Solo lo llama el parser, desde la regla fin_o_falta.
         *
         * Objetivo:
         * Informar el error exacto y seguir leyendo sin descartar código.
         */
        @Override
        public void errorProduccion(int linea, int columna, String mensaje) {

            ErrorSintactico e = new ErrorSintactico();

            e.linea = linea;
            e.columna = columna;
            e.token = "ERROR";
            e.lexema = null;
            e.tecnica = "Producción de error";
            e.mensaje = Character.toUpperCase(mensaje.charAt(0)) + mensaje.substring(1) + ".";

            errores.add(e);
        }

        @Override
        public void tokenFaltante(String token, int linea, int columna) {

            ErrorSintactico e = new ErrorSintactico();

            e.linea = linea;
            e.columna = columna;
            e.lexema = null;

            String que;

            switch (token) {
                case ":э":
                    e.token = "PARENCL";
                    que = "Falta el paréntesis de cierre :э.";
                    break;
                case ":?":
                    e.token = "CLBLOCK";
                    que = "Falta el cierre de bloque :? de la función.";
                    break;
                default:
                    e.token = "TERMINATOR";
                    que = "Falta el terminador » al final de la sentencia.";
            }

            e.tecnica = "Recuperación a nivel de frase";
            e.mensaje = que + " Se insertó y se siguió leyendo.";
            e.esperados.add(token);

            errores.add(e);
        }

        /*
         * Descripcion:
         * Obtengo los tokens que la gramatica esperaba encontrar.
         *
         * Entrada:
         * Los identificadores de tokens esperados por CUP.
         *
         * Salida:
         * Una lista con los nombres de los tokens esperados.
         *
         * Restricciones:
         * Depende de los tokens definidos en la gramatica.
         *
         * Objetivo:
         * Mostrar informacion util para entender el error.
         */
        private List<String> esperados() {

            Set<String> lista = new LinkedHashSet<>();

            try {
                for (int id : expected_token_ids()) {

                    if (id == 1) {
                        continue; /* símbolo interno "error" */
                    }

                    lista.add(legible(nombreToken(id)));
                }
            } catch (Throwable ex) {
                return new ArrayList<>();
            }

            List<String> resultado = new ArrayList<>(lista);

            if (resultado.size() > 10) {
                resultado = new ArrayList<>(resultado.subList(0, 10));
                resultado.add("…");
            }

            return resultado;
        }
    }

    /*
     * Descripcion:
     * Convierto el nombre interno de un token a su forma visible.
     *
     * Entrada:
     * El nombre interno del token.
     *
     * Salida:
     * El nombre o simbolo que usa el lenguaje.
     *
     * Restricciones:
     * Si no existe una representacion, uso el nombre del token.
     *
     * Objetivo:
     * Mostrar los tokens de una forma facil de entender.
     */
    private static String legible(String nombre) {
        return VISIBLE.getOrDefault(nombre, nombre.toLowerCase());
    }

    /*
     * Descripcion:
     * Busco el nombre de un token usando su numero.
     *
     * Entrada:
     * El numero asignado al token por CUP.
     *
     * Salida:
     * El nombre correspondiente al token.
     *
     * Restricciones:
     * El token debe estar definido en la clase sym.
     *
     * Objetivo:
     * Identificar los tokens durante el analisis.
     */
    private static String nombreToken(int numero) {
        try {
            for (Field campo : sym.class.getFields()) {

                if (campo.getType() == int.class && Modifier.isStatic(campo.getModifiers())  && campo.getInt(null) == numero) {

                    return campo.getName();
                }
            }
        } catch (Exception e) {

            return "DESCONOCIDO";
        }
        return "DESCONOCIDO";
    }

    /*
     * Descripcion:
     * Creo un mapa con los numeros y nombres de los tokens.
     *
     * Entrada:
     * Los tokens definidos en la clase sym.
     *
     * Salida:
     * Un mapa que relaciona cada numero con su nombre.
     *
     * Restricciones:
     * Los tokens deben estar definidos como campos en sym.
     *
     * Objetivo:
     * Facilitar la identificacion de los tokens.
     */
    private static Map<Integer, String> nombresDeTokens()

            throws IllegalAccessException {

        Map<Integer, String> mapa = new HashMap<>();

        for (Field campo : sym.class.getFields()) {

            if (campo.getType() == int.class  && Modifier.isStatic(campo.getModifiers())) {

                mapa.put(campo.getInt(null), campo.getName());
            }
        }

        return mapa;
    }
}