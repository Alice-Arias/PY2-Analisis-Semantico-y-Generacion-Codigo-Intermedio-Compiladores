import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/*
 * AMBITO
 * Descripcion: Guarda las variables y funciones de cada bloque.
 * Tambien permite buscar nombres en los bloques anteriores.
 */
public class Ambito {

    private final Map<String, Simbolo> simbolosPorNombre = new LinkedHashMap<>();
    public final Ambito padre;
    public final String nombre;
    public final int nivel;
    public final List<Ambito> hijos = new ArrayList<>();

    /*
     * Guarda el bloque donde se reservan las direcciones.
     */
    public final Ambito marco;
    public int siguienteDireccion = 0;
    public Simbolo funcionDuena;

    /*
     * Descripcion: Crea un nuevo ambito.
     * Entrada: Ambito padre, nombre y si tiene espacio propio.
     * Salida: Un nuevo ambito.
     * Restricciones: El ambito global no tiene padre.
     */
    public Ambito(Ambito padre, String nombre, boolean tieneMarcoPropio) {
        this.padre = padre;
        this.nombre = nombre;
        this.nivel = padre == null ? 0 : padre.nivel + 1;
        this.marco = (tieneMarcoPropio || padre == null) ? this : padre.marco;

        if (padre != null) {
            padre.hijos.add(this);
        }
    }

    /*
     * Descripcion: Comprueba si es el ambito global.
     * Entrada: Ninguna.
     * Salida: true si es global, false si no lo es.
     * Restricciones: No tiene.
     */
    public boolean esGlobal() {
        return padre == null;
    }

    /*
     * Descripcion: Agrega un simbolo al ambito.
     * Entrada: El simbolo que se desea guardar.
     * Salida: Ninguna.
     * Restricciones: Se guarda usando su nombre.
     */
    public void agregar(Simbolo simbolo) {
        simbolosPorNombre.put(simbolo.nombre, simbolo);
    }

    /*
     * Descripcion: Busca un simbolo en este ambito y sus padres.
     * Entrada: Nombre del simbolo.
     * Salida: El simbolo encontrado o null.
     * Restricciones: Busca desde el ambito actual hacia arriba.
     */
    public Simbolo buscar(String nombre) {

        for (Ambito ambito = this; ambito != null; ambito = ambito.padre) {
            Simbolo encontrado = ambito.simbolosPorNombre.get(nombre);

            if (encontrado != null) {
                return encontrado;
            }
        }
        return null;
    }

    /*
     * Descripcion: Busca un simbolo solo en este ambito.
     * Entrada: Nombre del simbolo.
     * Salida: El simbolo encontrado o null.
     * Restricciones: No busca en los ambitos padres.
     */
    public Simbolo buscarSoloAqui(String nombre) {
        return simbolosPorNombre.get(nombre);
    }

    /*
     * Descripcion: Obtiene los simbolos guardados.
     * Entrada: Ninguna.
     * Salida: Lista de simbolos.
     * Restricciones: Solo devuelve los de este ambito.
     */
    public Iterable<Simbolo> simbolos() {
        return simbolosPorNombre.values();
    }

    /*
     * Descripcion: Cuenta los simbolos guardados.
     * Entrada: Ninguna.
     * Salida: Cantidad de simbolos.
     * Restricciones: Solo cuenta los de este ambito.
     */
    public int cantidad() {
        return simbolosPorNombre.size();
    }

    /*
     * Descripcion: Obtiene la ruta del ambito.
     * Entrada: Ninguna.
     * Salida: Ruta desde el ambito global.
     * Restricciones: Incluye los ambitos padres.
     */
    public String ruta() {
        return esGlobal() ? nombre : padre.ruta() + " › " + nombre;
    }

    /*
     * Descripcion: Obtiene los nombres de los ambitos hijos.
     * Entrada: Ninguna.
     * Salida: Nombres separados por comas o — si no tiene hijos.
     * Restricciones: Solo muestra los hijos directos.
     */
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