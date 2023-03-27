package org.codeartisans.gradle.wsdl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.workers.WorkQueue;
import org.gradle.workers.WorkerExecutor;

public abstract class WsdlToJava extends DefaultTask {
    private Configuration jaxwsToolsConfiguration;
    private final NamedDomainObjectContainer<Wsdl> wsdls = getObjectFactory().domainObjectContainer(Wsdl.class);

    @InputFiles
    public Configuration getJaxwsToolsConfiguration() {
        return jaxwsToolsConfiguration;
    }

    public void setJaxwsToolsConfiguration( Configuration jaxwsToolsConfiguration ) {
        this.jaxwsToolsConfiguration = jaxwsToolsConfiguration;
    }

    @Input
    public NamedDomainObjectContainer<Wsdl> getWsdls() {
        return wsdls;
    }

    @InputFiles
    public FileCollection getWsdlFiles() {
        ConfigurableFileCollection wsdlFiles = getProject().files();
        for( Wsdl wsdl : wsdls ) {
            wsdlFiles.from( wsdl.getWsdl() );
        }
        return wsdlFiles;
    }

    @OutputDirectory
    abstract public DirectoryProperty getOutputDirectory();

    @TaskAction
    public void processWsdls() {
        WorkQueue queue = getWorkerExecutor().classLoaderIsolation(spec -> spec.getClasspath().from(jaxwsToolsConfiguration));
        wsdls.forEach(wsdl ->
            queue.submit(WsImportWorkAction.class,
                    wsImportParameters -> wsImportParameters.getArguments().addAll(wsImportArgumentsFor(wsdl))));
    }

    private List<String> wsImportArgumentsFor( Wsdl wsdl ) {
        File wsdlFile = getProject().file( wsdl.getWsdl() );

        List<String> arguments = new ArrayList<>();
        if( wsdl.getPackageName() != null ) {
            arguments.add( "-p" );
            arguments.add( wsdl.getPackageName() );
        }
        arguments.add( "-wsdllocation" );
        arguments.add( wsdlFile.getName() );
        arguments.add( "-s" );
        arguments.add(getOutputDirectory().getAsFile().get().getAbsolutePath() );
        arguments.add( "-extension" );
        arguments.add( "-Xnocompile" );
        if( getProject().getLogger().isDebugEnabled() ) {
            arguments.add( "-Xdebug" );
        } else {
            arguments.add( "-quiet" );
        }
        arguments.add( wsdlFile.getAbsolutePath() );
        return arguments;
    }

    @Inject
    protected abstract WorkerExecutor getWorkerExecutor();

    @Inject
    protected abstract ObjectFactory getObjectFactory();
}
