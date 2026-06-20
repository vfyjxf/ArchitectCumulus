package dev.vfyjxf.cumulus.build

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction

/**
 * Verification task: fails the build if any Java source references a known
 * external type by its fully-qualified name inline, instead of importing it
 * and using the simple name.
 *
 * <p>Covered prefixes: {@code java.(util|lang|io|stream|function)},
 * {@code net.minecraft}, {@code net.neoforged}, {@code dev.vfyjxf}.
 *
 * <p>Excluded so legitimate usage isn't flagged:
 * <ul>
 *   <li>{@code import} declarations</li>
 *   <li>line comments ({@code //}), block comments ({@code /* ... *\/}),
 *       and javadoc ({@code /** ... *\/} — including {@code {@link fqn}})</li>
 *   <li>string literals (e.g. reflection {@code Class.forName("dev.vfyjxf...")})</li>
 * </ul>
 *
 * <p>The check keeps tests and production code uniform and grep-friendly: every
 * type is referenced by its simple name with an explicit import.
 */
class CheckNoInlineFqnTask extends DefaultTask {

    /** Type prefix we require to be imported rather than spelled out inline. */
    private static final String FQN_PATTERN =
        /(?m)\b(?:java\.(?:util|lang|io|stream|function)|net\.minecraft|net\.neoforged|dev\.vfyjxf)(?:\.[a-z][\w]*)*\.[A-Z][A-Za-z0-9_]*/

    private static final String STRING_PATTERN = /"(?:[^"\\]|\\.)*"/

    @InputFiles
    FileTree getSourceFiles() {
        project.fileTree(dir: 'src', includes: ['**/*.java'])
    }

    @TaskAction
    void check() {
        def typePattern = ~FQN_PATTERN
        def stringPattern = ~STRING_PATTERN
        def offenses = []

        sourceFiles.each { f ->
            def inBlock = false
            f.eachLine { line, idx ->
                def stripped = line.trim()
                if (inBlock) {
                    if (stripped.contains('*/')) inBlock = false
                    return
                }
                if (stripped.startsWith('/*')) {
                    if (!stripped.contains('*/')) inBlock = true
                    return
                }
                // skip line comments, javadoc/body lines, and import declarations
                if (stripped.startsWith('//') ||
                    stripped.startsWith('*') ||
                    stripped.startsWith('/**') ||
                    stripped.startsWith('import ')) {
                    return
                }
                def masked = line.replaceAll(stringPattern, '""')
                if (masked =~ typePattern) {
                    offenses << "${f.path}:${idx + 1}: ${line.trim()}"
                }
            }
        }

        if (!offenses.isEmpty()) {
            def msg = "Found ${offenses.size()} inline fully-qualified type reference(s).\n" +
                "Import the type and use its simple name instead.\n" +
                offenses.collect { "  $it" }.join('\n')
            throw new GradleException(msg)
        }
    }
}
