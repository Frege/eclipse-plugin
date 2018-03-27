package frege.imp.wizards;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

public class FregeProjectWizard extends BasicNewResourceWizard {
	private NewFregeProjectWizardPageOne firstPage;
	private NewFregeProjectWizardPageTwo secondPage;

	public FregeProjectWizard() {
		firstPage = new NewFregeProjectWizardPageOne();
		firstPage.setTitle("Create a Frege project");
		firstPage.setDescription("Create a Frege project in the workspace or in an external location.");

		secondPage = new NewFregeProjectWizardPageTwo(firstPage);
		secondPage.setTitle("Frege Settings");
		secondPage.setDescription("Define the Frege build settings.");
	}

	private IWorkbenchPart getActivePart() {
		IWorkbenchWindow activeWindow = getWorkbench().getActiveWorkbenchWindow();
		if (activeWindow != null) {
			IWorkbenchPage activePage = activeWindow.getActivePage();
			if (activePage != null) {
				return activePage.getActivePart();
			}
		}
		return null;
	}

	@Override
	public void addPages() {
		addPage(firstPage);
		addPage(secondPage);

		firstPage.init(getSelection(), getActivePart());
	}

	@Override
	public boolean performFinish() {
		final IJavaElement newElement = secondPage.getJavaProject();

		IWorkingSet[] workingSets = firstPage.getWorkingSets();
		if (workingSets.length > 0) {
			PlatformUI.getWorkbench().getWorkingSetManager().addToWorkingSets(newElement, workingSets);
		}

		selectAndReveal(secondPage.getJavaProject().getProject());
		return true;
	}

	@Override
	public void init(IWorkbench theWorkbench, IStructuredSelection currentSelection) {
		super.init(theWorkbench, currentSelection);
		setWindowTitle("New Frege Project");
	}

	@Override
	public boolean performCancel() {
		secondPage.performCancel();
		return super.performCancel();
	}
}
