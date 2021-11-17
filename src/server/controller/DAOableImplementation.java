package server.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;
import lib.dataModel.User;
import lib.dataModel.UserPrivilege;
import lib.dataModel.UserStatus;
import lib.exceptions.ConnectException;
import lib.exceptions.EmailExistException;
import lib.exceptions.IncorrectEmailException;
import lib.exceptions.IncorrectPasswordException;
import lib.exceptions.IncorrectUserException;
import lib.exceptions.PasswordDontMatchException;
import lib.exceptions.UserDontExistException;
import lib.exceptions.UserExistException;
import lib.interfaces.Logicable;
import server.pool.Pool;

/**
 * Esta clase maneja la logica de los metodos de signIn y signUp
 * @author Irkus de la Fuente, Steven Arce
 */
public class DAOableImplementation implements Logicable {
    //logger
    private final static Logger logger = Logger.getLogger("server.controller.Dao");
    //atributos
    private Connection con;
    private Pool pool;
    private ResultSet rs;
    private PreparedStatement stmt;
    //querys
    private final String insertarUsuario = "insert into user (login,email,fullname,status,privilege,password,lastPasswordChange) values(?,?,?,?,?,?,?)";
    private final String buscarUsuario = "select * from user where login=?";
    private final String buscarCorreo = "select * from user where email=?";
    private final String procedimientoSignIn = "{CALL last_ten_sign_in(?)}";
    /**
     * Constructor vacio construye el dao y asigna valor al pool
     */
    public DAOableImplementation() {
        //Asignar valor al pool
        this.pool = Pool.getInstance();
    }
    /**
     * Metodo de cerrar el statement y resulSet
     */
    public void closeResulAndStatement() {
        if (rs != null) {
            try {
                //cerrar resultSet
                rs.close();
            } catch (SQLException ex) {
                logger.info("Error al cerrar el resultSet");
            }
        }
        if (stmt != null) {
            try {
                //Cerrar statement
                stmt.close();
            } catch (SQLException ex) {
                logger.info("Error al cerrar el preparedStatement");
            }
        }
    }
    /**
     * Este metodo loguea a un usuario
     * @param user Objeto usuario recibido mediante el socket 
     * @return objeto User Devuelve el usuario en caso de no encontrar nada devuelve nulo
     * @throws IncorrectUserException El usuario no es alfanumerico
     * @throws IncorrectPasswordException La contraseña no es alfanumerica
     * @throws UserDontExistException El usuario no esta registrado en la base de datos
     * @throws PasswordDontMatchException La contraseña no esta registrada en la base de datos
     * @throws ConnectException Hay un error de conexion con la base de datos
     */
      //SignIn  Recibe Usuario/Devuelve Usuario
    @Override
    public synchronized User signIn(User user) throws IncorrectUserException, IncorrectPasswordException, UserDontExistException, PasswordDontMatchException, ConnectException {
        logger.info("SignIn iniciado");
        User usua;
        //Pedir conexion al pool
        con = pool.getConnection();
        try {
            //Buscar si existe usuario
            usua = buscarUser(user);
            if (usua == null) {
                //Usuario no existe
                throw new UserDontExistException("Usuario no existe");
            } else {
                if (user.getPassword().equals(usua.getPassword())) {
                    //Procedimiento guardar log ultimos 10 1ogins
                    stmt = con.prepareStatement(procedimientoSignIn);
                    stmt.setString(1, user.getLogin());
                    stmt.executeUpdate();
                } else {
                    //Error contraseña no coincide con la de base de datos
                    logger.info("Error contraseña signin");
                    throw new PasswordDontMatchException("Contraseña incorrecta");
                }
            }
        } catch (SQLException ex) {
            //Error de conexion con la base de datos
            logger.info("Error conexion signin");
            throw new ConnectException("error de conexion a base de datos");
        }
        //Devolver la conexion al pool y cerrar todo
        pool.releaseConnection(con);
        closeResulAndStatement();
        //Devolcer usuario
        return user;
    }
     /**
     * Este metodo registra un usuario en la base de datos
     * @param user Objeto usuario recibido mediante el socket 
     * @return objeto User Devuelve el usuario creado
     * @throws IncorrectUserException El usuario no es alfanumerico
     * @throws IncorrectPasswordException La contraseña no es alfanumerica
     * @throws IncorrectEmailException Patron de correo incorrecto 
     * @throws UserExistException   Usuario ya existe en la base de datos
     * @throws PasswordDontMatchException Las contraseñas no coinciden entre si(contraseña y confirmar contraseña)
     * @throws ConnectException Hay un error de conexion con la base de datos
     */
    
     //SignUp  Recibe Usuario/Devuelve Usuario
    @Override
    public synchronized User signUp(User user) throws IncorrectUserException, IncorrectPasswordException, IncorrectEmailException, UserExistException, PasswordDontMatchException, ConnectException,EmailExistException {
        logger.info("SignUp iniciado");
        User usua,usuar;
        //Pedir conexion al pool
        con = pool.getConnection();
        //Buscar si existe usuario
        usua = buscarUser(user);
        usuar=buscarCorreo(user);

        try {
            
            if (usua==null) {
                if(usuar==null){
                //Query insertar usuario
                stmt = con.prepareStatement(insertarUsuario);
                stmt.setString(1, user.getLogin());
                stmt.setString(2, user.getEmail());
                stmt.setString(3, user.getFullName());
                stmt.setString(4, user.getStatus().toString());
                stmt.setString(5, user.getPrivilege().toString());
                stmt.setString(6, user.getPassword());
                stmt.setTimestamp(7, user.getLastPasswordChange());
                stmt.executeUpdate();
                //Guardamos el log in en el registro de los 10 ultimos
                stmt = con.prepareStatement(procedimientoSignIn);
                stmt.setString(1, user.getLogin());
                stmt.executeUpdate();
                
            }else{
                //correo ya existe 
                throw new EmailExistException("Email ya existe");    
                }
                    
                    
            } else{
               //usuario ya existe
                    throw new UserExistException("Usuario ya existe"); 
            }
                
             

        } catch (ConnectException ex) {
            //Error con la base de datos
            logger.info("Error de conexion SignUp");
            System.out.println("error conexion");
        } catch (SQLException ex) {
            //Error con la base de datos
            logger.info("Error de conexion SQL signUp");
            throw new ConnectException("error de conexion a base de datos");
        }
        //Devolcer conexion al pool
        pool.releaseConnection(con);
        //cerrar todo
        closeResulAndStatement();
        //devolver usuario
        return user;
    }

    
    /**
     * Este metodo busca un usuario determinado buscado mediante el loggin y lo devuelve con todos los datos
     * @param user Objeto usuario recibido desde el socket
     * @return objeto User Devuelve un objeto usuario con todos los datos introducidos en caso de no encontrarlo nulo
     */
    //Busca usuario recibe User y devuelve User
    public User buscarUser(User user) throws ConnectException {
        logger.info("Buscar usuario iniciado");
        try {
         
            //ejecutar query buscar usuario
            stmt = con.prepareStatement(buscarUsuario);
            stmt.setString(1, user.getLogin());
            rs = stmt.executeQuery();

            user=null;
            while (rs.next()) {
                //asignar valores al objeto usuario
                user = new User();
                user.setId(rs.getInt("id"));
                user.setLogin(rs.getString("login"));
                user.setEmail(rs.getString("email"));
                user.setPassword(rs.getString("password"));
                user.setFullName(rs.getString("fullName"));
                user.setPrivilege(UserPrivilege.USER);
                user.setStatus(UserStatus.ENABLED);
            }

        } catch (SQLException ex) {
            //Error con la base de datos
            logger.info("Error de conexion buscar usuario SQL");
            throw new ConnectException("error de conexion a base de datos");
        }
        //devolver usuario
        return user;
    }
    public User buscarCorreo(User user) throws ConnectException{
        logger.info("Buscar usuario iniciado");
        try {
            
            //ejecutar query buscar usuario
            stmt = con.prepareStatement(buscarCorreo);
            stmt.setString(1, user.getEmail());
            rs = stmt.executeQuery();

            user=null;
            while (rs.next()) {
                //asignar valores al objeto usuario
                user = new User();
                user.setId(rs.getInt("id"));
                user.setLogin(rs.getString("login"));
                user.setEmail(rs.getString("email"));
                user.setPassword(rs.getString("password"));
                user.setFullName(rs.getString("fullName"));
                user.setPrivilege(UserPrivilege.USER);
                user.setStatus(UserStatus.ENABLED);
            }

        } catch (SQLException ex) {
            //Error con la base de datos
            logger.info("Error de conexion buscar usuario SQL");
            throw new ConnectException("error de conexion a base de datos");
        }
        //devolver usuario
        return user;
    }
}
