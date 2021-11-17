package server.serverSocket;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import lib.dataModel.User;
import lib.exceptions.ConnectException;
import lib.exceptions.EmailExistException;
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
public class Worker extends Thread {

    private final static Logger logger = Logger.getLogger("server.serverSocket.Hilo");
    private Socket socket;
    private boolean maxThreads;
    private Message msg;
    ObjectInputStream ois = null;
    ObjectOutputStream oos = null;
    private User usu;

    /**
     * Constructor para el hilo Worker.
     * @param clientSocket socket del cliente
     * @param maxThreads numero maximo de clientes permitidos
     */
    public Worker(Socket clientSocket, boolean maxThreads) {
        this.socket = clientSocket;
        this.maxThreads = maxThreads;
    }

    /**
     * Este metodo pretende leer el mensaje recibido por el cliente, si no se ha
     * superado el numero maximo de clientes permitidos, se hara una peticion a 
     * la base de datos y mandara un mensaje diciendo que se ha hecho correctamente.
     * En caso de haberse superado el limite, devolvera un mensaje al cliente 
     * diciendo que se ha superado el limite de clientes.
     */
    @Override
    public void run() {
        try {
            oos = new ObjectOutputStream(socket.getOutputStream());
            ois = new ObjectInputStream(socket.getInputStream());
            msg = (Message) ois.readObject();
            logger.info("Se ha leido el objeto recibido");
            if (!maxThreads) {
                LogicableFactory log = new LogicableFactory();
                switch (msg.getMsg()) {
                    case SIGNUP:
                        usu = log.getDao().signUp(msg.getUser());
                        break;
                    case SIGNIN:
                        usu = log.getDao().signIn(msg.getUser());
                        break;
                }
                logger.info("Se ha hecho la peticion en la base de datos");
                msg.setMsg(Msg.OK);
                msg.setUser(usu);
            } else {
                msg = new Message();
                msg.setMsg(Msg.TOOMANYUSERSEXCEPTION);
                logger.info("Se ha superado el limite maximo de clientes");
            }
        } catch (IOException | ClassNotFoundException ex) {
            Logger.getLogger(Worker.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IncorrectUserException ex) {
            logger.info("Usuario invalido");
            msg.setMsg(Msg.INCORRECTUSEREXCEPTION);
        } catch (IncorrectPasswordException ex) {
            logger.info("La contraseña no existe en la base de datos");
            msg.setMsg(Msg.INCORRECTPASSWORDEXCEPTION);
        } catch (IncorrectEmailException ex) {
            logger.info("Email no valido");
            msg.setMsg(Msg.INCORRECTEMAILEXCEPTION);
        } catch (UserExistException ex) {
            logger.info("El usuario ya existe");
            msg.setMsg(Msg.USEREXISTEXCEPTION);
        } catch (PasswordDontMatchException ex) {
            logger.info("La contraseña no coincide");
            msg.setMsg(Msg.PASSWORDDONTMATCHEXCEPTION);
        } catch (ConnectException ex) {
            logger.info("Se ha producido un error con la base de datos");
            msg.setMsg(Msg.CONNECTEXCEPTION);
        } catch (UserDontExistException ex) {
            logger.info("El usuario no existe");
            msg.setMsg(Msg.USERDONTEXISTEXCEPTION);
        } catch (TooManyUsersException ex) {
            logger.info("Se ha superado el limite maximo de clientes");
        } catch (EmailExistException ex) {
            logger.info("El email es el mismo que el de la base de datos");
            msg.setMsg(Msg.EMAILEXISTEXCEPTION);
        } finally {
            closeReadWriteObject();
        }
    }

    /**
     * Este metodo pretende devolver la respuesta del servidor al cliente mediante
     * un mensaje. Ademas de cerrar el Output, Input y el socket del cliente.
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