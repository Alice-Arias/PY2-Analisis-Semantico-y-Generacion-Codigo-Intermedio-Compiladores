import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * ============================================================
 *                    TABLA DE SÍMBOLOS
 * ============================================================
 *
 * Descripcion:
 * Implemento la tabla de símbolos con una tabla por ámbito, encadenada
 * al ámbito que la contiene (Aho et al., sección 2.7).
 * Este archivo contiene cinco clases:
 *   TablaSimbolos          -> administra la cadena de ámbitos
 *   Simbolo                -> una entrada de la tabla        (clase Symbol de la lectura)
 *   Ambito                 -> una tabla por ámbito           (clase Env de la lectura)
 *   ReporteTablaSimbolos   -> arma el reporte .txt
 *   CuadroTexto            -> dibuja tablas con bordes para el reporte
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

    /* Nombre de la carpeta donde se guarda el reporte */
    public static final String CARPETA_REPORTE = "tabla_simbolos";

    /*
     * Descripcion:
     * Ubico la carpeta tabla_simbolos dentro de la carpeta Programa:
     *   ...\Programa\bin             <- aquí están los .class
     *   ...\Programa\tabla_simbolos  <- aquí se guarda el reporte
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


/*
 * ============================================================
 *                         SIMBOLO
 * ============================================================
 * Descripcion: una entrada de la tabla con todos sus atributos.
 */
class Simbolo {

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


/*
 * ============================================================
 *                          AMBITO
 * ============================================================
 * Descripcion: una tabla por ámbito, encadenada al ámbito que la contiene
 * (clase Env de la lectura). También guarda sus ámbitos hijos y el marco
 * donde se reservan las direcciones.
 */
class Ambito {

    private final Map<String, Simbolo> simbolosPorNombre = new LinkedHashMap<>();

    public final Ambito padre;                  /* el ámbito que contiene a este (null en el global) */
    public final String nombre;
    public final int nivel;                     /* 0 = global, 1 = función, 2 o más = bloques */
    public final List<Ambito> hijos = new ArrayList<>();

    /*
     * Marco: el ámbito que reserva las direcciones. El global y cada función
     * tienen el suyo; los bloques internos (if, while, for) usan el de su función.
     */
    public final Ambito marco;
    public int siguienteDireccion = 0;          /* solo se usa en los ámbitos que son marco */
    public Simbolo funcionDuena;                /* la función a la que pertenece este cuerpo */

    public Ambito(Ambito padre, String nombre, boolean tieneMarcoPropio) {
        this.padre = padre;
        this.nombre = nombre;
        this.nivel = padre == null ? 0 : padre.nivel + 1;
        this.marco = (tieneMarcoPropio || padre == null) ? this : padre.marco;

        if (padre != null) {
            padre.hijos.add(this);
        }
    }

    public boolean esGlobal() {
        return padre == null;
    }

    /* put de la lectura: agrega un símbolo a este ámbito */
    public void agregar(Simbolo simbolo) {
        simbolosPorNombre.put(simbolo.nombre, simbolo);
    }

    /* get de la lectura: busca desde este ámbito hacia los que lo contienen */
    public Simbolo buscar(String nombre) {

        for (Ambito ambito = this; ambito != null; ambito = ambito.padre) {

            Simbolo encontrado = ambito.simbolosPorNombre.get(nombre);

            if (encontrado != null) {
                return encontrado;
            }
        }

        return null;
    }

    /* Solo en este ámbito, para detectar nombres repetidos */
    public Simbolo buscarSoloAqui(String nombre) {
        return simbolosPorNombre.get(nombre);
    }

    public Iterable<Simbolo> simbolos() {
        return simbolosPorNombre.values();
    }

    public int cantidad() {
        return simbolosPorNombre.size();
    }

    /* Ruta desde el global: global › funcion f › if */
    public String ruta() {
        return esGlobal() ? nombre : padre.ruta() + " › " + nombre;
    }

    /* "if, elif, else" o "—" si no tiene hijos */
    public String nombresHijos() {

        if (hijos.isEmpty()) {
            return "—";
        }

        List<String> nombres = new ArrayList<>();

        for (Ambito hijo : hijos) {
            nombres.add(hijo.nombre);
        }

        return String.join(", ", nombres);
    }
}


/*
 * ============================================================
 *                 REPORTE DE LA TABLA DE SÍMBOLOS
 * ============================================================
 * Descripcion: arma el texto del reporte .txt en cuatro partes: árbol de
 * ámbitos, símbolos por ámbito, funciones y errores.
 */
class ReporteTablaSimbolos {

    private static final String LINEA_DOBLE = "═".repeat(140);

    private static final Pattern PATRON_ERROR = Pattern.compile("Error \\S+ en línea (\\d+), columna (\\d+): (.*)");

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final TablaSimbolos tabla;
    private final StringBuilder texto = new StringBuilder();

    ReporteTablaSimbolos(TablaSimbolos tabla) {
        this.tabla = tabla;
    }

    /* El reporte completo */
    String generar(String nombreFuente) {

        agregarEncabezado(nombreFuente);
        agregarConvenciones();
        agregarArbol();
        agregarSimbolosPorAmbito();
        agregarFunciones();
        agregarErrores();

        texto.append("\n").append(LINEA_DOBLE).append("\n");

        return texto.toString();
    }

    private void agregarEncabezado(String nombreFuente) {

        texto.append(LINEA_DOBLE).append("\n");
        texto.append("  TABLA DE SÍMBOLOS\n");
        texto.append(LINEA_DOBLE).append("\n\n");

        texto.append("  Archivo fuente : ").append(nombreFuente).append("\n");
        texto.append("  Generado       : ").append(LocalDateTime.now().format(FORMATO_FECHA)).append("\n");
        texto.append("  Ámbitos        : ").append(tabla.getAmbitos().size()).append("\n");
        texto.append("  Símbolos       : ").append(tabla.cantidadSimbolos()).append("\n");
        texto.append("  Errores        : ").append(tabla.getErrores().size()).append("\n\n");
    }

    private void agregarConvenciones() {

        texto.append("  CONVENCIONES\n");
        texto.append("  ─────────────\n");
        texto.append("  Tamaños    : int 4 B · float 4 B · bool 1 B · char 1 B · string 4 B (referencia a la cadena)\n");
        texto.append("  Dirección  : global+n = desplazamiento en la zona estática · pila+n = desplazamiento en el registro de activación\n");
        texto.append("  Acceso     : público = declarado en el ámbito global · privado = declarado dentro de una función o bloque\n");
        texto.append("  Parámetros : el lenguaje solo tiene paso por valor\n\n");
    }

    /* ---------- 1. Árbol de ámbitos ---------- */

    private void agregarArbol() {
        agregarTitulo("1. ÁRBOL DE ÁMBITOS (tablas encadenadas)");
        texto.append(dibujarArbol(tabla.ambitoGlobal())).append("\n");
    }

    /* Árbol completo desde el ámbito global, con ├── y └── */
    static String dibujarArbol(Ambito global) {
        StringBuilder arbol = new StringBuilder();
        dibujarRama(arbol, global, "", true);
        return arbol.toString();
    }

    /* Un ámbito y, debajo, sus hijos con más sangría */
    private static void dibujarRama(StringBuilder arbol, Ambito ambito, String sangria, boolean esUltimoHijo) {

        arbol.append("  ");

        if (!ambito.esGlobal()) {
            arbol.append(sangria).append(esUltimoHijo ? "└── " : "├── ");
        }

        arbol.append(ambito.nombre).append("  (").append(textoCantidadSimbolos(ambito)).append(")\n");

        String sangriaHijos = ambito.esGlobal() ? "" : sangria + (esUltimoHijo ? "    " : "│   ");

        for (int i = 0; i < ambito.hijos.size(); i++) {
            dibujarRama(arbol, ambito.hijos.get(i), sangriaHijos, i == ambito.hijos.size() - 1);
        }
    }

    private static String textoCantidadSimbolos(Ambito ambito) {
        return ambito.cantidad() + (ambito.cantidad() == 1 ? " símbolo" : " símbolos");
    }

    /* ---------- 2. Símbolos por ámbito ---------- */

    private void agregarSimbolosPorAmbito() {

        agregarTitulo("2. SÍMBOLOS POR ÁMBITO");

        for (Ambito ambito : tabla.getAmbitos()) {
            agregarDatosDelAmbito(ambito);

            if (ambito.cantidad() == 0) {
                texto.append("  (sin declaraciones)\n\n");
            } else {
                texto.append(cuadroDeSimbolos(ambito).dibujar("  ")).append("\n");
            }
        }
    }

    private void agregarDatosDelAmbito(Ambito ambito) {

        texto.append("  ÁMBITO: ").append(ambito.ruta()).append("\n");
        texto.append("  Nivel ").append(ambito.nivel)
             .append(ambito.esGlobal() ? " (global)" : " (local)")
             .append(" · padre: ").append(ambito.esGlobal() ? "—" : ambito.padre.nombre)
             .append(" · tablas hijas: ").append(ambito.nombresHijos()).append("\n");
    }

    private static CuadroTexto cuadroDeSimbolos(Ambito ambito) {

        CuadroTexto cuadro = new CuadroTexto(
                new String[] {"Nº", "NOMBRE", "CATEGORÍA", "TIPO", "BYTES", "DIMENSIÓN",
                              "DIRECCIÓN", "VALOR INICIAL", "ACCESO", "LÍN:COL"},
                new int[] {3, 16, 13, 7, 6, 12, 11, 30, 8, 8});

        int numero = 1;

        for (Simbolo simbolo : ambito.simbolos()) {
            cuadro.agregarFila(
                    String.valueOf(numero++),
                    simbolo.nombre,
                    simbolo.categoria,
                    simbolo.tipo,
                    textoBytes(simbolo),
                    simbolo.textoDimensionCorta(),
                    simbolo.direccion(),
                    textoValorInicial(simbolo),
                    simbolo.acceso,
                    simbolo.linea + ":" + simbolo.columna);
        }

        return cuadro;
    }

    /* Las funciones no ocupan datos; "?" si el tamaño no se conoce */
    private static String textoBytes(Simbolo simbolo) {

        if (simbolo.esFuncion()) {
            return "—";
        }

        return simbolo.bytes > 0 ? String.valueOf(simbolo.bytes) : "?";
    }

    private static String textoValorInicial(Simbolo simbolo) {
        return simbolo.esFuncion() || simbolo.valorInicial == null ? "—" : simbolo.valorInicial;
    }

    /* ---------- 3. Funciones y procedimientos ---------- */

    private void agregarFunciones() {

        agregarTitulo("3. FUNCIONES Y PROCEDIMIENTOS (estructura de parámetros)");

        CuadroTexto cuadro = new CuadroTexto(
                new String[] {"NOMBRE", "CATEGORÍA", "RETORNO", "Nº PARÁM.", "PARÁMETROS FORMALES",
                              "PASO", "REG. ACTIV.", "TABLA HIJA", "LÍN:COL"},
                new int[] {14, 13, 8, 9, 30, 9, 11, 24, 8});

        boolean hayFunciones = false;

        for (Simbolo simbolo : tabla.ambitoGlobal().simbolos()) {
            if (simbolo.esFuncion()) {
                agregarFilaDeFuncion(cuadro, simbolo);
                hayFunciones = true;
            }
        }

        texto.append(hayFunciones ? cuadro.dibujar("  ") : "  (no hay funciones)\n").append("\n");
    }

    private static void agregarFilaDeFuncion(CuadroTexto cuadro, Simbolo funcion) {

        boolean sinParametros = funcion.parametros.isEmpty();

        cuadro.agregarFila(
                funcion.nombre,
                funcion.categoria,
                funcion.tipo,
                String.valueOf(funcion.parametros.size()),
                sinParametros ? "(ninguno)" : funcion.listaParametros(),
                sinParametros ? "—" : "por valor",
                funcion.bytes + " B",
                funcion.tablaHija == null ? "—" : funcion.tablaHija.nombre,
                funcion.linea + ":" + funcion.columna);
    }

    /* ---------- 4. Errores ---------- */

    private void agregarErrores() {

        agregarTitulo("4. ERRORES SEMÁNTICOS DETECTADOS CON LA TABLA");

        List<String> errores = tabla.getErrores();

        if (errores.isEmpty()) {
            texto.append("  No se encontraron errores.\n");
            return;
        }

        CuadroTexto cuadro = new CuadroTexto(
                new String[] {"Nº", "LÍNEA", "COL", "DESCRIPCIÓN"},
                new int[] {3, 6, 5, 80});

        int numero = 1;

        for (String error : errores) {
            agregarFilaDeError(cuadro, numero++, error);
        }

        texto.append(cuadro.dibujar("  "));
    }

    /* Separa "Error ... en línea L, columna C: detalle" en sus partes */
    private static void agregarFilaDeError(CuadroTexto cuadro, int numero, String error) {

        Matcher partes = PATRON_ERROR.matcher(error);

        if (partes.matches()) {
            cuadro.agregarFila(String.valueOf(numero), partes.group(1), partes.group(2), partes.group(3));
        } else {
            cuadro.agregarFila(String.valueOf(numero), "-", "-", error);
        }
    }

    /* "  TITULO" y debajo una linea del mismo largo */
    private void agregarTitulo(String titulo) {
        texto.append("  ").append(titulo).append("\n");
        texto.append("  ").append("─".repeat(titulo.length())).append("\n\n");
    }
}


/*
 * ============================================================
 *                       CUADRO DE TEXTO
 * ============================================================
 * Descripcion: dibuja una tabla con bordes. Si un texto no cabe en su
 * columna, continúa en la línea siguiente de la misma fila.
 */
class CuadroTexto {

    private final String[] titulos;
    private final int[] anchos;
    private final List<String[]> filas = new ArrayList<>();

    public CuadroTexto(String[] titulos, int[] anchos) {
        this.titulos = titulos;
        this.anchos = anchos;
    }

    public void agregarFila(String... celdas) {
        filas.add(celdas);
    }

    /* El cuadro completo; cada linea empieza con el margen indicado */
    public String dibujar(String margen) {

        StringBuilder cuadro = new StringBuilder();

        cuadro.append(margen).append(borde('┌', '┬', '┐')).append("\n");
        cuadro.append(dibujarFila(margen, titulos));
        cuadro.append(margen).append(borde('╞', '╪', '╡').replace('─', '═')).append("\n");

        for (int i = 0; i < filas.size(); i++) {

            cuadro.append(dibujarFila(margen, filas.get(i)));

            boolean esUltimaFila = i == filas.size() - 1;

            if (!esUltimaFila) {
                cuadro.append(margen).append(borde('├', '┼', '┤')).append("\n");
            }
        }

        cuadro.append(margen).append(borde('└', '┴', '┘')).append("\n");

        return cuadro.toString();
    }

    /* Linea de borde: ┌───┬───┐ */
    private String borde(char izquierda, char medio, char derecha) {

        StringBuilder borde = new StringBuilder().append(izquierda);

        for (int i = 0; i < anchos.length; i++) {
            borde.append("─".repeat(anchos[i] + 2));
            borde.append(i < anchos.length - 1 ? medio : derecha);
        }

        return borde.toString();
    }

    /* Una fila; si alguna celda es larga, la fila ocupa varias lineas */
    private String dibujarFila(String margen, String[] celdas) {

        List<List<String>> lineasPorCelda = new ArrayList<>();
        int alto = 1;

        for (int i = 0; i < anchos.length; i++) {

            String contenido = i < celdas.length && celdas[i] != null ? celdas[i] : "";
            List<String> lineas = partirEnLineas(contenido, anchos[i]);

            lineasPorCelda.add(lineas);
            alto = Math.max(alto, lineas.size());
        }

        StringBuilder fila = new StringBuilder();

        for (int linea = 0; linea < alto; linea++) {

            fila.append(margen).append("│");

            for (int i = 0; i < anchos.length; i++) {

                List<String> lineasCelda = lineasPorCelda.get(i);
                String contenido = linea < lineasCelda.size() ? lineasCelda.get(linea) : "";

                fila.append(" ").append(String.format("%-" + anchos[i] + "s", contenido)).append(" │");
            }

            fila.append("\n");
        }

        return fila.toString();
    }

    /*
     * Corta un texto en lineas de maximo "ancho" caracteres sin partir palabras.
     * Si una palabra sola es mas larga que el ancho, esa si se parte.
     */
    private static List<String> partirEnLineas(String texto, int ancho) {

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
}