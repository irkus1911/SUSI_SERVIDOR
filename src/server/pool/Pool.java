package server.pool;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ResourceBundle;
import java.util.Stack;
import java.util.TooManyListenersException;
import lib.exceptions.ConnectException;

/**
 * Esta clase pretende crear conexiones con la base de datos. Limitando las
 * conexiones mediante una coleccion.
 *
 * @author Steven Arce
 */
public class Pool {

    private static Pool instance;
    private static Stack<Connection> pool = new Stack<>();
    private static int MAX_POOL_SIZE = 10;
    private Connection con;
    private String driver;
    private String url;
    private String user;
    private String passwd;
    private ResourceBundle configFile;

    /**
     * Para que solo haya una sola instancia, el constructor es sprivado para
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
     * referencia al creado anteriormente. De lo contrario, crea devuelve una
     * nueva instancia.
     */
    public static Pool getInstance() {
        if (instance == null) {
            instance = new Pool();
        }
        return instance;
    }

    /**
     * Este metodo pretende obtener una conexion con la base de datos y añadir
     * la conexion a la coleccion de pool.
     *
     * @return 
     * @throws java.sql.SQLException 
     * @throws lib.exceptions.ConnectException 
     */
    public synchronized Connection getConnection() throws SQLException, ConnectException {     
        if (pool.isEmpty()) {
            con = DriverManager.getConnection(this.url, this.user, this.passwd);
            pool.push(con);
        }
        return pool.pop();
    }
    /**
     * 
     * @param con 
     */
    public synchronized void releaseConnection(Connection con) {
        pool.push(con);
    }

}
