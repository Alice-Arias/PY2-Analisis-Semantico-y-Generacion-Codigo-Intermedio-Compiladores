import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * ============================================================
 *                 REPORTE DE LA TABLA DE SÍMBOLOS
 * ============================================================
 * Descripcion: arma el texto del reporte .txt en cuatro partes: árbol de
 * ámbitos, símbolos por ámbito, funciones y errores.
 */
public class ReporteTablaSimbolos {

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
