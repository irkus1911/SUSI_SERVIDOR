package server.factory;

import lib.interfaces.Logicable;
import server.controller.DAOableImplementation;

/**
 * Esta clase es la factoria que crea un DAO
 * @author Unai Urtiaga
 */
public class LogicableFactory {
    
    /**
     * Metodo de la factoria de la parte del servidor para implementar la clase 
     * DAOableImplementation
     * @return Devuelve un objeto de la interfaz Logicable el cual va a servir
     * para luego implementar la siguiente clase
     */
    
    public Logicable getDao(){
        
        Logicable dao = new DAOableImplementation();
        
        return dao;
    }
    
}