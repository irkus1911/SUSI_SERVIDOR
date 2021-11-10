package server.serverSocket;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import lib.dataModel.User;
import lib.exceptions.ConnectException;
import lib.exceptions.IncorrectEmailException;
import lib.exceptions.IncorrectPasswordException;
import lib.exceptions.IncorrectUserException;
import lib.exceptions.PasswordDontMatchException;
import lib.exceptions.TooManyUsersException;
import lib.exceptions.UserDontExistException;
import lib.exceptions.UserExistException;
import lib.message.Message;
import lib.message.Msg;
import static server.Server.returnThread;
import server.factory.LogicableFactory;

/**
 * Esta clase pretende recibir y enviar objetos de tipo Message entre el cliente
 * y el servidor mediante este hilo.
 *
 * @author Steven Arce
 */
public class Worker {

    private final static Logger logger = Logger.getLogger("server.serverSocket.Hilo");
    private Socket socket;
    private boolean maxThreads;
    private Message msg;
    ObjectInputStream ois = null;
    ObjectOutputStream oos = null;
    private User usu;

    /**
     * Constructor para el hilo Worker.
     *
     * @param clientSocket socket del cliente
     * @param maxThreads numero maximo de clientes permitidos
     */
    public Worker(Socket clientSocket, boolean maxThreads) {
        this.socket = clientSocket;
        this.maxThreads = maxThreads;
    }

    /**
     * Este metodo pretende devolver la respuesta del servidor al cliente
     * mediante un mensaje. Ademas de cerrar el Output, Input y el socket del
     * cliente.
     */
    public void closeReadWriteObject() {
        try {
            logger.info("Escribiendo el objeto para mandarlo al cliente");
            oos.writeObject(msg);
            logger.info("Devolviendo el objeto al cliente");
            oos.close();
            ois.close();
            if (!maxThreads) {
                this.sleep(10000);
                returnThread();
            }
            socket.close();
            logger.info("Conexion cerrada");
        } catch (InterruptedException | IOException ex) {
            Logger.getLogger(Worker.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
