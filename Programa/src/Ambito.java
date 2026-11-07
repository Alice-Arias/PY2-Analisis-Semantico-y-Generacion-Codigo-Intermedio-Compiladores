import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/*
 * ============================================================
 *                          AMBITO
 * ============================================================
 * Descripcion: una tabla por ámbito, encadenada al ámbito que la contiene
 * (clase Env de la lectura). También guarda sus ámbitos hijos y el marco
 * donde se reservan las direcciones.
 */
public class Ambito {

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
