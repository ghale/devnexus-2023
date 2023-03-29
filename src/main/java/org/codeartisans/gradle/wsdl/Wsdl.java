package org.codeartisans.gradle.wsdl;

import java.io.File;
import java.io.Serializable;
import org.gradle.api.Named;

public class Wsdl implements Named, Serializable {

    private String name;
    private File wsdl;
    private String packageName;

    public Wsdl() {}

    public Wsdl( String name ) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public File getWsdl() {
        return wsdl;
    }

    public void setWsdl( File wsdl ) {
        this.wsdl = wsdl;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName( String packageName ) {
        this.packageName = packageName;
    }

    @Override
    public String toString() {
        return name + " WSDL to " + packageName;
    }
}
