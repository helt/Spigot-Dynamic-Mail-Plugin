/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ltde.gephi.io.spigot.plugin.email;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 *
 * @author Hendrik
 */
class EmailNode {

    public String address = null;
    public String name = null;

    EmailNode(Address a) {
        InternetAddress address = (InternetAddress) a;
        this.address = address.getAddress();
        this.name = address.getPersonal();
    }
}
