package frege.imp.wizards;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageOne;
import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageTwo;

import frege.imp.builders.FregeNature;

public class NewFregeProjectWizardPageTwo extends NewJavaProjectWizardPageTwo {
	public NewFregeProjectWizardPageTwo(NewJavaProjectWizardPageOne mainPage) {
		super(mainPage);
	}

	@Override
	public void configureJavaProject(String newProjectCompliance, IProgressMonitor monitor)
		throws CoreException, InterruptedException {
		super.configureJavaProject(newProjectCompliance, monitor);

		IProject project = getJavaProject().getProject();

		if (!project.hasNature(FregeNature.k_natureID)) {
			new FregeNature().addToProject(project);
		}
	}
}
