/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ltde.gephi.io.spigot.plugin.email.wizard;

import java.awt.Component;
import javax.swing.event.ChangeListener;
import org.openide.WizardDescriptor;
import org.openide.util.HelpCtx;

/**
 *
 * @author Hendrik
 */
public class DynamicMailWizardPanel1 implements WizardDescriptor.Panel {


    public DynamicMailWizardPanel1() {
        super();
    }
    
    private Component _component;
    
    @Override
    public Component getComponent() {
        if (_component == null) {
            _component = new DynamicMailSpigotSwing1();
        }
        return _component;
    }

    @Override
    public HelpCtx getHelp() {
        return HelpCtx.DEFAULT_HELP;
    }

    @Override
    public void readSettings(Object data) {
//        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void storeSettings(Object data) {
//        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void addChangeListener(ChangeListener cl) {
//        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void removeChangeListener(ChangeListener cl) {
//        throw new UnsupportedOperationException("Not supported yet.");
    }


}
