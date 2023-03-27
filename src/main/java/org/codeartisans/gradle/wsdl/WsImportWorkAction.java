package org.codeartisans.gradle.wsdl;

import com.sun.tools.ws.wscompile.WsimportTool;
import org.gradle.api.provider.ListProperty;
import org.gradle.workers.WorkAction;
import org.gradle.workers.WorkParameters;

import java.util.List;

public abstract class WsImportWorkAction implements WorkAction<WsImportWorkAction.WsImportParameters> {
    interface WsImportParameters extends WorkParameters {
        ListProperty<String> getArguments();
    }

    @Override
    public void execute() {
        try {
            List<String> arguments = getParameters().getArguments().get();
            if( !new WsimportTool( System.out ).run( arguments.toArray( new String[ arguments.size() ] ) ) ) {
                throw new RuntimeException( "Unable to import WSDL, see output for more details" );
            }
        } catch( Throwable throwable ) {
            throw new RuntimeException( "Unable to import WSDL", throwable );
        }
    }
}
