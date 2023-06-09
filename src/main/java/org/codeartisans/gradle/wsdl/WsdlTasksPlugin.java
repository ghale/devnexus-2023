package org.codeartisans.gradle.wsdl;

import java.io.File;

import org.gradle.api.*;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;

import javax.inject.Inject;

public abstract class WsdlTasksPlugin implements Plugin<Project> {

    public static final String JAXWSTOOLS_LIBRARY = "com.sun.xml.ws:jaxws-tools:2.2.10";

    @Inject
    protected abstract ObjectFactory getObjectFactory();

    @Override
    public void apply(Project project) {

        Configuration jaxWsTools = project.getConfigurations().create( "jaxwsTools" );
        project.getDependencies().add( "jaxwsTools", JAXWSTOOLS_LIBRARY);

        // Create a conventional wsdlToJava task in the project
        TaskProvider<WsdlToJava> wsdlToJava = project.getTasks().register("wsdlToJava", WsdlToJava.class, task -> {
                    task.setGroup("wsdl");
                    task.setDescription("Generate Java classes from WSDL");
        });

        // For every WsdlToJava task, set a conventional output directory and use the default jaxwsTools configuration
        project.getTasks().withType(WsdlToJava.class).configureEach(task -> {
            DirectoryProperty defaultOutputDirectory = getObjectFactory().directoryProperty();
            defaultOutputDirectory.set(new File( project.getBuildDir(),
                    "generated-sources/" + task.getName() + "/java" ));
            task.getOutputDirectory().convention(defaultOutputDirectory);

            task.getJaxwsToolsConfiguration().from( jaxWsTools);
        });

        // Add the wsdlToJava output directory to the main source set
        project.getPluginManager().withPlugin("java", appliedPlugin -> {
            SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
            sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).getJava().srcDir(wsdlToJava.map(WsdlToJava::getOutputDirectory));
            String implementationConfigurationName = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).getImplementationConfigurationName();
            project.getConfigurations().getByName(implementationConfigurationName).extendsFrom(jaxWsTools);
        });
    }
}
