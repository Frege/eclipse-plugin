package frege.imp.wizards;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

import frege.FregePlugin;

public class FregeModuleWizard extends BasicNewResourceWizard {
	private NewFregeModuleWizardPage mainPage;

	@Override
	public void addPages() {
		mainPage = new NewFregeModuleWizardPage(getSelection());
		mainPage.setTitle("Frege module");
		mainPage.setDescription("Create a new Frege module.");
		addPage(mainPage);
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
		super.init(workbench, currentSelection);
		setWindowTitle("New Frege module");
		setNeedsProgressMonitor(true);
	}

	@Override
	public boolean performFinish() {
		IFile file = mainPage.createNewFile();
		if (file == null) {
			return false;
		}

		selectAndReveal(file);

		// Open editor on new file.
		IWorkbenchWindow dw = getWorkbench().getActiveWorkbenchWindow();
		try {
			if (dw != null) {
				IWorkbenchPage page = dw.getActivePage();
				if (page != null) {
					IDE.openEditor(page, file, true);
				}
			}
		} catch (PartInitException e) {
			FregePlugin.getInstance().logException(e.getMessage(), e);
		}

		return true;
	}
}
