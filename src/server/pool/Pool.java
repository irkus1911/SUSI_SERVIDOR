package server.pool;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ResourceBundle;
import java.util.Stack;
import java.util.logging.Logger;
import lib.exceptions.ConnectException;

/**
 * Esta clase pretende crear conexiones con la base de datos, estas conexiones
 * puedes ser reutilizadas por los diferentes usuarios.
 *
 * @author Steven Arce
 */
public class Pool {

    private final static Logger logger = Logger.getLogger("server.pool");
    private static Pool instance;
    private static Stack<Connection> pool = new Stack<>();
    private Connection con;
    private String driver;
    private String url;
    private String user;
    private String passwd;
    private ResourceBundle configFile;

    /**
     * Para que solo haya una sola instancia, el constructor es privado para
     * impedir la creacion de nuevas instancias.
     */
    private Pool() {
        this.configFile = ResourceBundle.getBundle("server.pool.BDconfig");
        this.driver = this.configFile.getString("driver");
        this.url = this.configFile.getString("url");
        this.user = this.configFile.getString("user");
        this.passwd = this.configFile.getString("passwd");
    }

    /**
     * Este metodo pretende crear una instancia de esta clase, asegurando que
     * solo haya una unica instacia de esta misma.
     *
     * @return Si el Pool ya fue instanciado anteriormente, devuelve la
     * referencia al creado anteriormente. De lo contrario, crea una nueva
     * instancia.
     */
    public static Pool getInstance() {
        if (instance == null) {
            instance = new Pool();
        }
        return instance;
    }

    /**
     * Este metodo pretende obtener una conexion con la base de datos y añadir
     * la conexion a la coleccion pool.
     *
     * @return Devuelve una conexion de la coleccion pool.
     * @throws lib.exceptions.ConnectException Si hay un error con el acceso a
     * la base de datos.
     */
    public synchronized Connection getConnection() throws ConnectException {
        if (pool.isEmpty()) {
            try {
                con = DriverManager.getConnection(this.url, this.user, this.passwd);
            } catch (SQLException ex) {
                logger.info("");
                throw new ConnectException("Error al crear una conexion con la base de datos");
            }
            pool.push(con);
        }
        return pool.pop();
    }

    /**
     * Este metodo pretende recoger la conexion del usuario a fin de dejarlo
     * libre, para poder ser reutilizado.
     *
     * @param con Conexion devuelta por el usuario.
     */
    public synchronized void releaseConnection(Connection con) {
        pool.push(con);
    }

    /**
     * Este metodo pretende cerrar todas las conexiones del pool cuando el
     * servidor se vaya a cerrar.
     */
    public static void shutDownPool() {
        for (Connection c : pool) {
            try {
                c.close();
            } catch (SQLException ex) {
                logger.info("Error al cerrar la conexion del pool");
            }
        }
        logger.info("Se ha cerrado todas las conexiones del pool");
    }

}
