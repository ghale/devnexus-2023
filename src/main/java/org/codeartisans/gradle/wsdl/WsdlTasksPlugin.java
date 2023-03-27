package org.codeartisans.gradle.wsdl;

import java.io.File;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;

import javax.inject.Inject;

public abstract class WsdlTasksPlugin implements Plugin<Project> {

    @Inject
    protected abstract ObjectFactory getObjectFactory();

    @Override
    public void apply(Project project) {

        Configuration jaxWsTools = project.getConfigurations().create( "jaxwsTools" );
        project.getDependencies().add( "jaxwsTools", "com.sun.xml.ws:jaxws-tools:2.2.10" );

        TaskProvider<WsdlToJava> wsdlToJava = project.getTasks().register("wsdlToJava", WsdlToJava.class, task -> {
            task.setGroup( "wsdl" );
            task.setDescription( "Generate Java classes from WSDL" );

            DirectoryProperty defaultOutputDirectory = getObjectFactory().directoryProperty();
            defaultOutputDirectory.set(new File( project.getBuildDir(),
                    "generated-sources/" + task.getName() + "/java" ));
            task.getOutputDirectory().convention(defaultOutputDirectory);

            task.setJaxwsToolsConfiguration( jaxWsTools);
        });

        project.getPluginManager().withPlugin("java", appliedPlugin -> {
            SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
            sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).getJava().srcDir(wsdlToJava.map(WsdlToJava::getOutputDirectory));
        });
    }
}
