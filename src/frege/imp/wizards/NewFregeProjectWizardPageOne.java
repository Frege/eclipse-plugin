package frege.imp.wizards;

import java.util.Arrays;
import java.util.stream.Stream;

import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageOne;

import frege.FregePlugin;

public class NewFregeProjectWizardPageOne extends NewJavaProjectWizardPageOne {
	@Override
	public IClasspathEntry[] getDefaultClasspathEntries() {
		return Stream
			.concat(Arrays.stream(super.getDefaultClasspathEntries()),
				Stream.of(JavaCore.newLibraryEntry(new Path(FregePlugin.fregeLib), null, null)))
			.toArray(IClasspathEntry[]::new);
	}
}
