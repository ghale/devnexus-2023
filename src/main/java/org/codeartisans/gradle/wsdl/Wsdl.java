package org.codeartisans.gradle.wsdl;

import org.gradle.api.Named;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;

public interface Wsdl extends Named {
    @Override
    @Input
    String getName();

    @InputFile
    RegularFileProperty getWsdl();

    @Input
    Property<String> getPackageName();
}
