/*
 * Descripcion:
 * Genera el codigo intermedio de tres direcciones.
 *
 * Entrada:
 * Instrucciones que recibe del parser.
 *
 * Salida:
 * Codigo intermedio que se guarda en un archivo.
 *
 * Restricciones:
 * Solo genera codigo si el programa no tiene errores.
 * La validacion de tipos la realiza el parser.
 *
 * Objetivo:
 * Mantener toda la generacion de codigo en esta clase.
 *
 * Formato de etiquetas:
 * funcion$estructura$numero$parte, por ejemplo principal$while$6$condicion
 * Las funciones usan tipo$nombre: y terminan con tipo$nombre$final.
 * Se usa $ porque ningun identificador puede tenerlo: asi una etiqueta
 * nunca se confunde con una variable (los identificadores si usan _).
 */
public class Generador {

    private final StringBuilder codigo = new StringBuilder();

    private int contadorTemporales = 0;

    private int contadorEtiquetas = 0;

    private String funcionActual = "global";

    private String tipoFuncionActual = "";

    private static final String TIPO_PRINCIPAL = "void";

    /* Separa las partes de una etiqueta. */
    public static final String SEPARADOR = "$";

    /* Nombres de las partes de las etiquetas. */
    private static final String PARTE_INICIALIZACION = SEPARADOR + "inicializacion";
    private static final String PARTE_CONDICION = SEPARADOR + "condicion";
    private static final String PARTE_INCREMENTO = SEPARADOR + "inc";
    private static final String PARTE_BLOQUE = SEPARADOR + "bloque";
    private static final String PARTE_SIGUIENTE = SEPARADOR + "sig";
    private static final String PARTE_FINAL = SEPARADOR + "final";

    /*
     * Descripcion: Crea un nombre temporal.
     * Entrada: Tipo de dato.
     * Salida: Nombre del temporal.
     * Restricciones: El numero no se repite.
     */
    public String nuevoTemporal(String tipo) {
        contadorTemporales++;
        return tipo + contadorTemporales;
    }

    /*
     * Descripcion: Crea la etiqueta de una funcion.
     * Entrada: Tipo de retorno y nombre de la funcion.
     * Salida: La etiqueta, por ejemplo int$suma.
     * Restricciones: La usan la definicion de la funcion y cada call.
     */
    public static String etiquetaDeFuncion(String tipo, String nombre) {
        return tipo + SEPARADOR + nombre;
    }

    /* Crea el nombre base de una estructura: principal$if$3 */
    private String prefijo(String estructura) {
        contadorEtiquetas++;
        return funcionActual + SEPARADOR + estructura + SEPARADOR + contadorEtiquetas;
    }

    /* Agrega una etiqueta al codigo. */
    private void etiqueta(String nombre) {
        emitir("");
        emitir(nombre + ":");
    }

    /*
     * Descripcion: Genera una asignacion.
     * Entrada: Variable destino y valor.
     * Salida: Instruccion de asignacion.
     * Restricciones: Usa el lugar donde se encuentra el valor.
     */
    public void asignacion(String destino, Atrib valor) {
        emitir(destino + " = " + valor.lugar);
    }

    /*
     * Descripcion: Agrega una instruccion al codigo.
     * Entrada: Instruccion que se desea guardar.
     * Salida: Ninguna.
     * Restricciones: Agrega un salto de linea al final.
     */
    public void emitir(String instruccion) {
        codigo.append(instruccion).append("\n");
    }

    /*
     * Descripcion: Obtiene el codigo generado.
     * Entrada: Ninguna.
     * Salida: Texto del codigo intermedio.
     * Restricciones: Devuelve el codigo actual.
     */
    public String obtenerCodigo() {
        return codigo.toString();
    }

    /*
     * Descripcion: Limpia el codigo y reinicia los datos.
     * Entrada: Ninguna.
     * Salida: Ninguna.
     * Restricciones: Reinicia contadores, funciones y estructuras.
     */
    public void limpiar() {
        codigo.setLength(0);
        contadorTemporales = 0;
        contadorEtiquetas = 0;
        funcionActual = "global";
        tipoFuncionActual = "";
        hayFuncion = false;
        condicionales.clear();
        ciclos.clear();
        ultimaCarga = null;
        marcaAsignacion = 0;
    }

    /* Carpeta donde se guarda el codigo intermedio. */
    public static final String CARPETA = "salida" + java.io.File.separator + "codigo_intermedio";

    /*
     * Descripcion: Busca la carpeta de salida.
     * Entrada: Ninguna.
     * Salida: Ubicacion de la carpeta.
     * Restricciones: Usa la ubicacion de los archivos .class.
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
     * Descripcion: Guarda el codigo en un archivo .tac.
     * Entrada: Archivo fuente analizado.
     * Salida: Archivo .tac creado.
     * Restricciones: Usa el nombre del archivo fuente.
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
     * Descripcion: Genera una operacion con dos valores.
     * Entrada: Resultado validado, dos operandos y operador.
     * Salida: Resultado con un temporal nuevo.
     * Restricciones: Si hay un error, no genera codigo.
     */
    public Atrib binaria(Atrib validado, Atrib a, String op, Atrib b) {

        if (validado.tipo.equals("error")) {
            return validado;
        }

        String temporal = nuevoTemporal(validado.tipo);

        emitir(temporal + " = " + a.lugar + " " + op + " " + b.lugar);

        validado.lugar = temporal;

        return validado;
    }

    /*
     * Descripcion: Genera una operacion con un solo valor.
     * Entrada: Resultado validado, operador y operando.
     * Salida: Resultado con un temporal nuevo.
     * Restricciones: Si hay un error, no genera codigo.
     */
    public Atrib unaria(Atrib validado, String op, Atrib e) {

        if (validado.tipo.equals("error")) {
            return validado;
        }

        String temporal = nuevoTemporal(validado.tipo);

        emitir(temporal + " = " + op + " " + e.lugar);

        validado.lugar = temporal;

        return validado;
    }

    /*
     * Descripcion: Guarda el valor de una posicion del arreglo.
     * Entrada: Tipo de dato y posicion del arreglo.
     * Salida: Nombre del temporal.
     * Restricciones: Guarda el valor antes de usarlo.
     */
    public String cargarElemento(String tipo, String elemento) {
        String temporal = nuevoTemporal(tipo);
        emitir(temporal + " = " + elemento);
        return temporal;
    }

    private String ultimaCarga = null;
    private String valorUltimaCarga = null;
    private int inicioUltimaCarga = -1;
    private int finUltimaCarga = -1;

    /* Guarda la posicion donde empieza el valor de una asignacion. */
    private int marcaAsignacion = 0;

    /*
     * Descripcion: Copia un valor a un temporal.
     * Entrada: Dato que se desea copiar.
     * Salida: Dato guardado en un temporal.
     * Restricciones: Si hay un error, no genera codigo.
     */
    public Atrib cargar(Atrib dato) {
        if (dato == null || dato.tipo.equals("error")) {
            return dato;
        }

        String temporal = nuevoTemporal(dato.tipo);
        inicioUltimaCarga = codigo.length();
        emitir(temporal + " = " + dato.lugar);
        finUltimaCarga = codigo.length();
        ultimaCarga = temporal;
        valorUltimaCarga = dato.lugar;

        Atrib cargado = new Atrib(dato.tipo, temporal);
        cargado.valor = dato.valor;
        return cargado;
    }

    /*
     * Descripcion: Evita una copia que no es necesaria.
     * Entrada: Dato que se desea revisar.
     * Salida: Dato original o sin la copia.
     * Restricciones: Solo elimina la ultima carga realizada.
     */
    public Atrib sinCarga(Atrib dato) {
        boolean esLaUltima = dato != null && dato.lugar.equals(ultimaCarga)
                && codigo.length() == finUltimaCarga;

        if (!esLaUltima) {
            return dato;
        }

        codigo.setLength(inicioUltimaCarga);
        contadorTemporales--;
        ultimaCarga = null;

        Atrib directo = new Atrib(dato.tipo, valorUltimaCarga);
        directo.valor = dato.valor;
        return directo;
    }

    /* Marca donde empieza el codigo del valor de una asignacion. */
    public void marcarAsignacion() {
        marcaAsignacion = codigo.length();
    }

    /*
     * Descripcion: Genera una declaracion de variable.
     * Entrada: Tipo de dato y nombre de la variable.
     * Salida: Instruccion de declaracion.
     * Restricciones: Se usa cuando la variable no tiene valor inicial.
     */
    public void declarar(String tipo, String nombre) {
        emitir("declaracion# " + tipo + " " + nombre);
    }

    /*
     * Descripcion: Coloca la declaracion antes del valor inicial.
     * Entrada: Tipo de dato y nombre de la variable.
     * Salida: Declaracion agregada al codigo.
     * Restricciones: Usa la posicion marcada al leer el signo =.
     */
    public void declararAntesDelValor(String tipo, String nombre) {
        codigo.insert(marcaAsignacion, "declaracionPrevia# " + tipo + " " + nombre + "\n");
        ultimaCarga = null;
    }

    /*
     * Descripcion: Agrega un parametro de una funcion.
     * Entrada: Tipo de dato y nombre del parametro.
     * Salida: Instruccion del parametro.
     * Restricciones: Se coloca al inicio del cuerpo de la funcion.
     */
    public void parametro(String tipo, String nombre) {
        emitir("param_" + tipo + " " + nombre);
    }

    /*
     * Descripcion: Genera una instruccion para leer un dato.
     * Entrada: Variable donde se guarda el dato.
     * Salida: Instruccion read.
     * Restricciones: Necesita un destino.
     */
    public void leer(String destino) {
        emitir("read " + destino);
    }

    /*
     * Descripcion: Genera una instruccion para mostrar un dato.
     * Entrada: Valor que se desea mostrar.
     * Salida: Instruccion write.
     * Restricciones: Recibe el lugar donde esta el valor.
     */
    public void escribir(String valor) {
        emitir("write " + valor);
    }

    /*
     * Descripcion: Genera un aumento o disminucion de uno.
     * Entrada: Tipo de dato, variable y operador.
     * Salida: Instrucciones para actualizar la variable.
     * Restricciones: Solo trabaja con ++ y --.
     */
    public void incremento(String tipo, String id, String op) {
        String valor = nuevoTemporal(tipo);
        emitir(valor + " = " + id);

        String resultado = nuevoTemporal(tipo);
        emitir(resultado + " = " + valor + (op.equals("++") ? " + 1" : " - 1"));

        emitir(id + " = " + resultado);
    }

    /* ---------- funciones ---------- */

    private boolean hayFuncion = false;

    /*
     * Descripcion: Genera la etiqueta de inicio de una funcion.
     * Entrada: Tipo de retorno y nombre de la funcion.
     * Salida: Etiqueta de inicio.
     * Restricciones: Antes de la primera funcion, salta a principal.
     */
    public void etiquetaFuncion(String tipo, String nombre) {
        if (!hayFuncion && !nombre.equals("principal")) {
            emitir("goto " + etiquetaDeFuncion(TIPO_PRINCIPAL, "principal"));
        }

        hayFuncion = true;
        funcionActual = nombre;
        tipoFuncionActual = tipo;

        if (codigo.length() > 0) {
            emitir("");
        }
        emitir(etiquetaDeFuncion(tipo, nombre) + ":");
    }

    /*
     * Descripcion: Marca el final de una funcion.
     * Entrada: Ninguna.
     * Salida: Etiqueta final de la funcion.
     * Restricciones: Regresa al estado global.
     */
    public void finFuncion() {
        emitir(etiquetaDeFuncion(tipoFuncionActual, funcionActual) + PARTE_FINAL);
        funcionActual = "global";
        tipoFuncionActual = "";
    }

    /*
     * Descripcion: Genera la llamada a una funcion.
     * Entrada: Nombre, tipo, argumentos y si devuelve un valor.
     * Salida: Temporal con el resultado o null.
     * Restricciones: Genera un parametro por cada argumento.
     */
    public String llamar(String nombre, String tipo, java.util.ArrayList args, boolean devuelve) {
        for (Object o : args) {
            emitir("param " + ((Atrib) o).lugar);
        }

        if (!devuelve) {
            emitir("call " + nombre + ", " + args.size());
            return null;
        }

        String temporal = nuevoTemporal(tipo);
        emitir(temporal + " = call " + nombre + ", " + args.size());
        return temporal;
    }

    /*
     * Descripcion: Genera una instruccion de retorno.
     * Entrada: Lugar del valor o null.
     * Salida: Instruccion return.
     * Restricciones: Puede regresar un valor o ninguno.
     */
    public void retorno(String lugar) {
        emitir(lugar == null ? "return" : "return " + lugar);
    }

    /* ---------- if / elif / else ---------- */

    /* Guarda los datos necesarios de cada condicional. */
    private static class Condicional {
        String base;
        int rama = 0;
        String siguiente;
    }

    private final java.util.ArrayList<Condicional> condicionales = new java.util.ArrayList<>();

    /* Obtiene el condicional que se esta trabajando. */
    private Condicional ifActual() {
        return condicionales.isEmpty() ? null : condicionales.get(condicionales.size() - 1);
    }

    /*
     * Descripcion: Inicia un condicional if.
     * Entrada: Ninguna.
     * Salida: Agrega un nuevo condicional.
     * Restricciones: Guarda el condicional para trabajar con el mas interno.
     */
    public void iniciarIf() {
        Condicional c = new Condicional();
        c.base = prefijo("if");
        condicionales.add(c);
    }

    /*
     * Descripcion: Genera los saltos de una condicion.
     * Entrada: Lugar donde se encuentra el resultado.
     * Salida: Saltos hacia el bloque correspondiente.
     * Restricciones: Se usa para if y elif.
     */
    public void condicionIf(String lugar) {
        Condicional c = ifActual();

        if (c == null) {
            return;
        }

        c.rama++;
        String bloque = c.base + PARTE_BLOQUE + c.rama;
        c.siguiente = c.base + PARTE_SIGUIENTE + c.rama;

        emitir("if " + lugar + " goto " + bloque);
        emitir("goto " + c.siguiente);
        etiqueta(bloque);
    }

    /*
     * Descripcion: Inicia otra rama del condicional.
     * Entrada: Ninguna.
     * Salida: Salto hacia el final y etiqueta de la siguiente rama.
     * Restricciones: Se usa para elif y else.
     */
    public void otraRama() {
        Condicional c = ifActual();

        if (c == null) {
            return;
        }

        emitir("goto " + c.base + PARTE_FINAL);

        if (c.siguiente != null) {
            etiqueta(c.siguiente);
            c.siguiente = null;
        }
    }

    /*
     * Descripcion: Finaliza un condicional.
     * Entrada: Ninguna.
     * Salida: Etiquetas pendientes.
     * Restricciones: Trabaja con el condicional mas interno.
     */
    public void finIf() {
        if (condicionales.isEmpty()) {
            return;
        }

        Condicional c = condicionales.remove(condicionales.size() - 1);

        if (c.siguiente != null) {
            etiqueta(c.siguiente);
        }

        etiqueta(c.base + PARTE_FINAL);
    }

    /* ---------- while y for ---------- */

    /* Guarda los datos necesarios de cada ciclo. */
    private static class Ciclo {
        String base;
        boolean esFor;
        int marca = -1;
    }

    private final java.util.ArrayList<Ciclo> ciclos = new java.util.ArrayList<>();

    /*
     * Descripcion: Crea y guarda un nuevo ciclo.
     * Entrada: Tipo de ciclo.
     * Salida: Datos del ciclo creado.
     * Restricciones: Indica si es un for o un while.
     */
    private Ciclo nuevoCiclo(String estructura) {
        Ciclo c = new Ciclo();
        c.base = prefijo(estructura);
        c.esFor = estructura.equals("for");
        ciclos.add(c);
        return c;
    }

    /* Obtiene el ciclo que se esta trabajando. */
    private Ciclo cicloActual() {
        return ciclos.isEmpty() ? null : ciclos.get(ciclos.size() - 1);
    }

    /*
     * Descripcion: Inicia un ciclo while.
     * Entrada: Ninguna.
     * Salida: Etiqueta de la condicion.
     * Restricciones: La etiqueta va antes de evaluar la condicion.
     */
    public void iniciarWhile() {
        etiqueta(nuevoCiclo("while").base + PARTE_CONDICION);
    }

    /*
     * Descripcion: Genera los saltos de la condicion del ciclo.
     * Entrada: Lugar donde esta el resultado.
     * Salida: Saltos hacia el bloque o el final.
     * Restricciones: Se usa cuando la condicion ya fue generada.
     */
    public void saltoSiFalsoCiclo(String lugar) {
        Ciclo c = cicloActual();

        if (c == null) {
            return;
        }

        emitir("if " + lugar + " goto " + c.base + PARTE_BLOQUE);
        emitir("goto " + c.base + PARTE_FINAL);
        etiqueta(c.base + PARTE_BLOQUE);
    }

    /*
     * Descripcion: Inicia un ciclo for.
     * Entrada: Ninguna.
     * Salida: Etiqueta de inicializacion.
     * Restricciones: Se genera antes de la inicializacion.
     */
    public void iniciarFor() {
        etiqueta(nuevoCiclo("for").base + PARTE_INICIALIZACION);
    }

    /*
     * Descripcion: Marca el inicio de la condicion del for.
     * Entrada: Ninguna.
     * Salida: Etiqueta de condicion.
     * Restricciones: Se usa despues de la inicializacion.
     */
    public void inicioCondicionFor() {
        Ciclo c = cicloActual();

        if (c != null) {
            etiqueta(c.base + PARTE_CONDICION);
        }
    }

    /*
     * Descripcion: Marca donde empieza el incremento del for.
     * Entrada: Ninguna.
     * Salida: Guarda la posicion actual del codigo.
     * Restricciones: Se usa para mover el incremento despues.
     */
    public void marcaIncremento() {
        Ciclo c = cicloActual();

        if (c != null) {
            c.marca = codigo.length();
        }
    }

    /*
     * Descripcion: Organiza las instrucciones de la cabecera del for.
     * Entrada: Lugar donde esta el resultado de la condicion.
     * Salida: Saltos y bloque del ciclo.
     * Restricciones: Coloca el incremento despues de la condicion.
     */
    public void cabeceraFor(String lugarCondicion) {
        Ciclo c = cicloActual();

        if (c == null) {
            return;
        }

        String incremento = "";

        if (c.marca >= 0) {
            incremento = codigo.substring(c.marca);
            codigo.setLength(c.marca);
        }

        if (lugarCondicion != null) {
            emitir("if " + lugarCondicion + " goto " + c.base + PARTE_BLOQUE);
            emitir("goto " + c.base + PARTE_FINAL);
        } else {
            emitir("goto " + c.base + PARTE_BLOQUE);
        }

        etiqueta(c.base + PARTE_INCREMENTO);
        codigo.append(incremento);
        emitir("goto " + c.base + PARTE_CONDICION);

        etiqueta(c.base + PARTE_BLOQUE);
    }

    /*
     * Descripcion: Finaliza un ciclo.
     * Entrada: Ninguna.
     * Salida: Salto para repetir el ciclo y etiqueta final.
     * Restricciones: El for vuelve al incremento y el while a la condicion.
     */
    public void finCiclo() {
        if (ciclos.isEmpty()) {
            return;
        }

        Ciclo c = ciclos.remove(ciclos.size() - 1);

        emitir("goto " + c.base + (c.esFor ? PARTE_INCREMENTO : PARTE_CONDICION));
        etiqueta(c.base + PARTE_FINAL);
    }

    /*
     * Descripcion: Genera la salida inmediata de un ciclo.
     * Entrada: Ninguna.
     * Salida: Salto hacia el final del ciclo.
     * Restricciones: Sale del ciclo mas interno.
     */
    public void saltoBreak() {
        Ciclo c = cicloActual();

        if (c != null) {
            emitir("goto " + c.base + PARTE_FINAL);
        }
    }
}