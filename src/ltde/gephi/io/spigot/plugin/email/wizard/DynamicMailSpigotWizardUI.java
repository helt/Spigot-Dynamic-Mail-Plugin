/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ltde.gephi.io.spigot.plugin.email.wizard;

import ltde.gephi.io.spigot.plugin.email.DynamicMailSpigot;
import org.openide.WizardDescriptor.Panel;
import org.gephi.io.importer.spi.Importer;
import org.gephi.io.importer.spi.ImporterWizardUI;
import org.gephi.io.importer.spi.SpigotImporter;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Hendrik
 */
@ServiceProvider(service = ImporterWizardUI.class)
public class DynamicMailSpigotWizardUI implements ImporterWizardUI {

    private Panel[] panels = null;

    @Override
    public String getDisplayName() {
        return NbBundle.getMessage(DynamicMailSpigotWizardUI.class, "DynamicMailSpigotWizardUI.DisplayName");
    }

    @Override
    public String getCategory() {
        return NbBundle.getMessage(DynamicMailSpigotWizardUI.class, "DynamicMailSpigotWizardUI.Category");
    }

    @Override
    public String getDescription() {
        return NbBundle.getMessage(DynamicMailSpigotWizardUI.class, "DynamicMailSpigotWizardUI.Description");

//        return "Dynamic Mail Spigot imports emails from a folder with dynamic date";
    }

    @Override
    public Panel[] getPanels() {
        if (panels == null) {
            panels = new Panel[1];
            panels[0] = new DynamicMailWizardPanel1();
        }
        return panels;
    }

    @Override
    public void setup(Panel panel) {
        //Before opening the wizard
    }

    public void unsetup(SpigotImporter importer, Panel panel) {
        //When the wizard has been closed
        ((DynamicMailSpigotSwing1) ((Panel) panels[0]).getComponent()).unsetup(importer);

        panels = null;
    }

    @Override
    public boolean isUIForImporter(Importer importer) {
        return importer instanceof DynamicMailSpigot;

    }
}
