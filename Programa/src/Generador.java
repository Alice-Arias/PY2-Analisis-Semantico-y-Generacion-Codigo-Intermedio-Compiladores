/*
 * Descripcion:
 * Genero el código intermedio (código de tres direcciones) del programa.
 * Cada instrucción tiene como máximo tres direcciones: "t1 = a + b".
 *
 * Entrada:
 * Las instrucciones que le pide el parser (Parser.cup), una por una, mientras
 * analiza el archivo fuente.
 *
 * Salida:
 * El texto del código intermedio, que Main guarda en un archivo.
 *
 * Restricciones:
 * Solo tiene sentido guardar el código cuando el programa no tiene errores.
 * Este archivo no valida tipos: eso lo hace el parser antes de pedir el código.
 *
 * Objetivo:
 * Que el Parser.cup quede solo con la gramática y las reglas semánticas, y que
 * todo lo del código intermedio viva en un solo lugar.
 */
public class Generador {

    private final StringBuilder codigo = new StringBuilder();

    private int contadorTemporales = 0;

    private int contadorEtiquetas = 0;

    /* Un temporal nuevo: t1, t2, t3... */
    public String nuevoTemporal() {
        contadorTemporales++;
        return "t" + contadorTemporales;
    }

    /* Una etiqueta nueva para los saltos: L1, L2, L3... */
    public String nuevaEtiqueta() {
        contadorEtiquetas++;
        return "L" + contadorEtiquetas;
    }

    /* Instrucción de una asignación: destino = lugar del valor */
    public void asignacion(String destino, Atrib valor) {
        emitir(destino + " = " + valor.lugar);
    }

    /* Agrega una instrucción al código */
    public void emitir(String instruccion) {
        codigo.append(instruccion).append("\n");
    }

    /* El código generado hasta ahora */
    public String obtenerCodigo() {
        return codigo.toString();
    }

    /* Borra el código y reinicia los contadores */
    public void limpiar() {
        codigo.setLength(0);
        contadorTemporales = 0;
        contadorEtiquetas = 0;
    }

    /* Nombre de la carpeta donde se guarda el código intermedio */
    public static final String CARPETA = "codigo_intermedio";

    /*
     * Descripcion:
     * Ubico la carpeta codigo_intermedio dentro de la carpeta Programa
     * (al lado de bin y de tabla_simbolos).
     *
     * Restricciones:
     * Se calcula a partir de la ubicación de los .class, así funciona
     * sin importar desde qué carpeta se ejecute el programa.
     */
    public static java.io.File carpeta() {
        try {
            java.io.File bin = new java.io.File(Generador.class
                    .getProtectionDomain().getCodeSource().getLocation().toURI());

            return new java.io.File(bin.getParentFile(), CARPETA);

        } catch (Exception e) {
            return new java.io.File(CARPETA);
        }
    }

    /*
     * Descripcion:
     * Guardo el código intermedio en un archivo .tac.
     *
     * Entrada: el archivo fuente analizado (de él sale el nombre).
     * Salida : el archivo creado, por ejemplo codigo_intermedio/prueba.tac
     */
    public java.io.File guardar(java.io.File fuente) throws java.io.IOException {

        java.io.File carpeta = carpeta();

        if (!carpeta.exists()) {
            carpeta.mkdirs();
        }

        String base = fuente.getName();
        int punto = base.lastIndexOf('.');

        if (punto > 0) {
            base = base.substring(0, punto);
        }

        java.io.File salida = new java.io.File(carpeta, base + ".tac");

        java.nio.file.Files.writeString(salida.toPath(), obtenerCodigo(),
                java.nio.charset.StandardCharsets.UTF_8);

        return salida;
    }

    /*
     * Descripcion:
     * Genero la instrucción de una operación binaria: t = a op b.
     *
     * Entrada:
     * El resultado que ya validó el parser (con su tipo), los dos operandos y el operador.
     *
     * Salida:
     * El mismo resultado, con el temporal nuevo como lugar. Si el tipo es "error" no
     * genera nada y lo devuelve igual.
     */
    public Atrib binaria(Atrib validado, Atrib a, String op, Atrib b) {

        if (validado.tipo.equals("error")) {
            return validado;
        }

        String temporal = nuevoTemporal();

        emitir(temporal + " = " + a.lugar + " " + op + " " + b.lugar);

        validado.lugar = temporal;

        return validado;
    }
    /* Operación de un solo operando: t = not e */
    public Atrib unaria(Atrib validado, String op, Atrib e) {

        if (validado.tipo.equals("error")) {
            return validado;
        }

        String temporal = nuevoTemporal();

        emitir(temporal + " = " + op + " " + e.lugar);

        validado.lugar = temporal;

        return validado;
    }

    /* Instrucción de lectura: read destino */
    public void leer(String destino) {
        emitir("read " + destino);
    }

    /* Instrucción de escritura: write valor */
    public void escribir(String valor) {
        emitir("write " + valor);
    }
}
