package dev.vfyjxf.cumulus.build

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Registers the {@code checkNoInlineFqn} verification task and wires it into
 * the standard {@code check} lifecycle, so inline fully-qualified type names
 * fail the build.
 *
 * <p>Applied from the main build via {@code id 'cumulus.no-inline-fqn'}.
 */
class NoInlineFqnPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        def task = project.tasks.register('checkNoInlineFqn', CheckNoInlineFqnTask) {
            group = 'verification'
            description =
                'Fails if any non-import, non-comment, non-string code references ' +
                'a known type by fully-qualified name.'
        }
        project.tasks.named('check').configure {
            dependsOn(task)
        }
    }
}
