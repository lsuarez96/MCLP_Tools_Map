package map.utils;

import mclp_tools.config.CfgMngController;
import mclp_tools.config.elements.network.ConnectionCfgData;

import java.net.Authenticator;
import java.util.Properties;

public class Network {

    public static void setupHostConnection() {
        ConnectionCfgData connectionCfgData = CfgMngController.getInstance().getCurrentProfile()
                                                              .getConnectionCfgData();
        if (connectionCfgData.usesProxy()) {
            System.setProperty("proxySet", "true");
            System.setProperty("proxyHost", connectionCfgData.getProxyAddress());
            System.setProperty("proxyPort", connectionCfgData.getPort());
            System.setProperty("proxyUserName", connectionCfgData.getUserName());
            System.setProperty("proxyPassword", connectionCfgData.getPassword());
            System.setProperty("http.proxyHost", connectionCfgData.getProxyAddress());
            System.setProperty("http.proxyPort", connectionCfgData.getPort());
            System.setProperty("http.proxyUserName", connectionCfgData.getUserName());
            System.setProperty("http.proxyPassword", connectionCfgData.getPassword());
            Authenticator.setDefault(new SimpleAuthenticator());
            Properties systemProperties = System.getProperties();
            systemProperties.setProperty("proxyHost", connectionCfgData.getProxyAddress());
            systemProperties.setProperty("proxyPort", connectionCfgData.getPort());
        } else {
            System.setProperty("proxySet", "true");
            System.setProperty("proxyHost", "");
            System.setProperty("proxyPort", "");
            System.setProperty("proxyUserName", "");
            System.setProperty("proxyPassword", "");
            System.setProperty("http.proxyHost", "");
            System.setProperty("http.proxyPort", "");
            System.setProperty("http.proxyUserName", "");
            System.setProperty("http.proxyPassword", "");
            Authenticator.setDefault(new SimpleAuthenticator());
            Properties systemProperties = System.getProperties();
            systemProperties.setProperty("proxyHost", "");
            systemProperties.setProperty("proxyPort", "");
        }
    }
}
