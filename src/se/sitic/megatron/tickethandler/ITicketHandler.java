package se.sitic.megatron.tickethandler;


import java.util.Map;

import se.sitic.megatron.core.MegatronException;

public interface ITicketHandler {
    
    public void init() throws MegatronException;
    
    /**
     * The method getNewTicketId returns a new ticket-ID from the ticketing 
     * system. It takes a key/value map with the necessary input parameters. 
     * 
     * @param values
     * @return
     */
    
    public String getNewTicketId(Map<String,String> values);

    public void updateTicketStatus(String status, String ticketId);
    
}
