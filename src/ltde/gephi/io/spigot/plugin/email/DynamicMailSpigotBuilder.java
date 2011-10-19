/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ltde.gephi.io.spigot.plugin.email;

import org.gephi.io.importer.spi.SpigotImporter;
import org.gephi.io.importer.spi.SpigotImporterBuilder;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Hendrik
 */
@ServiceProvider(service = SpigotImporterBuilder.class)
public class DynamicMailSpigotBuilder implements SpigotImporterBuilder {

    @Override
    public SpigotImporter buildImporter() {
        return new DynamicMailSpigot();
    }

    @Override
    public String getName() {
        return "Dynamic Email Spigot Builder";
    }
}
