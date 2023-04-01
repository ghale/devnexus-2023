package org.codeartisans.gradle.wsdl;

import org.gradle.api.Named;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.work.NormalizeLineEndings;

public interface Wsdl extends Named {
    @Override
    @Input
    String getName();

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    @NormalizeLineEndings
    RegularFileProperty getWsdl();

    @Input
    Property<String> getPackageName();
}
