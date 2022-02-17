package map.utils;

import mclp_tools.config.CfgMngController;
import mclp_tools.config.elements.network.ConnectionCfgData;

import java.net.Authenticator;
import java.net.PasswordAuthentication;

public class SimpleAuthenticator extends Authenticator {
    private ConnectionCfgData connectionCfgData;

    public SimpleAuthenticator() {
        this.connectionCfgData = CfgMngController.getInstance().getCurrentProfile().getConnectionCfgData();
    }

    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(connectionCfgData.getUserName(),
                                          connectionCfgData.getPassword().toCharArray
                                                  ());
    }

}