package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import server.pool.Pool;
import server.serverSocket.Worker;

/**
 * Esta clase pretende ser el punto de entrada de la aplicacion servidor, 
 * donde se establece una conexion en un puerto determinado para ser capaz  
 * de recibir conexiones de cliente, y atender a dichos clientes mediante hilos.
 * 
 * @author Steven Arce, Adrian Franco
 */
public class Server {

    private final static Logger logger = Logger.getLogger("server");
    private static ResourceBundle configFile;
    private static int PORT;
    private static int MAXTHREADS;
    private static int CONT;

    /**
     * Este metodo pretende iniciar la ejecucion del programa, coge el puerto y 
     * el numero maximo de hilos de un archivo de configuracion para luego entrar
     * en un bucle infinito y atender a todos los clientes que establezcan una 
     * conexion. Ademas, controla el numero de clientes que pueden registrarse o
     * iniciar sesion en la aplicacion. Por ultimo, se controla el cierre de las
     * conexiones del pool mediante un hook que se ejecuta cuando se cierra el JVM.
     * @param args una matriz de argumentos de la línea de comandos para la aplicación.
     * @throws InterruptedException Si un hilo se interrumpe. 
     */
    public static void main(String[] args) throws InterruptedException {

        logger.info("Servidor iniciado");
        configFile =  ResourceBundle.getBundle("lib.message.Properties");
        PORT = Integer.valueOf(configFile.getString("PORT"));
        MAXTHREADS = Integer.valueOf(configFile.getString("MAXCONNECTIONS"));

        ServerSocket serverSocket = null;
        Socket clientSocket;
        boolean maxConnections = false;

        //Hilo para agregarle un addShutdownHook(), e inciar ciertas acciones.
        Thread shutdownThread = new Thread() {
            public void run() {
                //Esto se ejecutara antes de que la maquina virtual finalice
                Pool.shutDownPool();
            }
        };
        //addShutdownHook() registrará acciones que se realizarán en la terminación de un Programa.
        Runtime.getRuntime().addShutdownHook(shutdownThread);

        try {
            CONT = 0;
            serverSocket = new ServerSocket(PORT);
            while (true) {
                clientSocket = serverSocket.accept();
                logger.info("Cliente aceptado");
                CONT++;
                if (CONT <= MAXTHREADS) {
                    maxConnections = false;
                } else {
                    maxConnections = true;
                    CONT--;
                    logger.info("Cliente rechazado por el limite de conexiones");
                }
                Worker hilo = new Worker(clientSocket, maxConnections);
                logger.info("Hilo creado para atender al cliente");
                hilo.start();
            }
        } catch (IOException ex) {
            logger.info("Error con el socket del servidor");
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                serverSocket.close();
            } catch (IOException ex) {
                logger.info("Error al cerrar el ServerSocket");
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    /**
     * Este metodo pretende restar -1 al contador de MAXTHREADS, para dejar libre
     * el hilo para el proximo cliente.
     */
    public static void returnThread() {
        CONT--;
        logger.info("Numero de clientes: " + CONT);
    }

}
