package frege.imp.wizards;

import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaElementComparator;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.StandardJavaElementContentProvider;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.ide.undo.CreateFileOperation;
import org.eclipse.ui.ide.undo.WorkspaceUndoUtil;

import frege.FregePlugin;

public class NewFregeModuleWizardPage extends WizardPage {
	private IStructuredSelection currentSelection;
	private IContainer sourceFolder;
	private String moduleName;
	private String moduleSimpleName;
	private IPath modulePath;

	private IFile newFile;
	private Text textSourceFolder;
	private Text textName;

	public NewFregeModuleWizardPage(IStructuredSelection selection) {
		super("NewFregeModuleWizardPage");
		this.currentSelection = selection;
	}

	@Override
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);

		Composite topLevel = new Composite(parent, SWT.NONE);
		GridLayout topLevelLayout = new GridLayout();
		topLevelLayout.numColumns = 3;
		topLevel.setLayout(topLevelLayout);
		topLevel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));
		topLevel.setFont(parent.getFont());

		Label lbSourceFolder = new Label(topLevel, SWT.NONE);
		lbSourceFolder.setFont(topLevel.getFont());
		lbSourceFolder.setText("Source Folder:");

		textSourceFolder = new Text(topLevel, SWT.BORDER);
		textSourceFolder.setFont(topLevel.getFont());
		textSourceFolder.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		textSourceFolder.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				if (e.keyCode != SWT.CR)
					setSourceFolder(textSourceFolder.getText());
			}
		});

		Button btnSourceFolderBrowse = new Button(topLevel, SWT.PUSH);
		btnSourceFolderBrowse.setFont(topLevel.getFont());
		btnSourceFolderBrowse.setText("Browse...");
		btnSourceFolderBrowse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(getShell(),
					new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT),
					new StandardJavaElementContentProvider());
				dialog.setValidator(new ISelectionStatusValidator() {
					@Override
					public IStatus validate(Object[] selection) {
						if (selection.length != 1)
							return new Status(IStatus.ERROR, FregePlugin.getInstance().getID(), "");
						Object selectedObject = selection[0];
						try {
							if (selectedObject instanceof IPackageFragmentRoot
								&& ((IPackageFragmentRoot) selectedObject).getKind() == IPackageFragmentRoot.K_SOURCE)
								return new Status(IStatus.OK, FregePlugin.getInstance().getID(), "");
							else
								return new Status(IStatus.ERROR, FregePlugin.getInstance().getID(), "");
						} catch (JavaModelException e) {
							return new Status(IStatus.ERROR, FregePlugin.getInstance().getID(), e.getMessage(), e);
						}
					}
				});
				dialog.setComparator(new JavaElementComparator());
				dialog.setTitle("Source Folder Selection");
				dialog.setMessage("Choose a source folder:");
				dialog.addFilter(new ViewerFilter() {
					@Override
					public boolean select(Viewer viewer, Object parentElement, Object element) {
						try {
							if (element instanceof IPackageFragmentRoot)
								return ((IPackageFragmentRoot) element).getKind() == IPackageFragmentRoot.K_SOURCE;
							else if (element instanceof IJavaProject)
								return true;
							else
								return false;
						} catch (JavaModelException e) {
							return false;
						}
					}
				});
				dialog.setInput(JavaCore.create(ResourcesPlugin.getWorkspace().getRoot()));
				dialog.setHelpAvailable(false);
				dialog.setAllowMultiple(false);
				if (dialog.open() == Window.OK) {
					IPackageFragmentRoot sourcePackage = ((IPackageFragmentRoot) dialog.getFirstResult());
					setSourceFolder(sourcePackage.getJavaProject().getProject()
						.getFolder(sourcePackage.getPath().removeFirstSegments(1)));
				}
			}
		});

		Label lbName = new Label(topLevel, SWT.NONE);
		lbName.setFont(topLevel.getFont());
		lbName.setText("Name:");

		textName = new Text(topLevel, SWT.BORDER);
		textName.setFont(topLevel.getFont());
		textName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		textName.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				if (e.keyCode != SWT.CR)
					setModuleName(textName.getText(), false);
			}
		});

		if (currentSelection != null) {
			Iterator<?> it = currentSelection.iterator();
			if (it.hasNext()) {
				Object object = it.next();
				IResource selectedResource = Adapters.adapt(object, IResource.class);
				if (selectedResource != null) {
					IProject project = selectedResource.getProject();
					getSourceDirs(project).map(path -> {
						if (path.equals(project.getFullPath())) {
							return project;
						} else {
							return ResourcesPlugin.getWorkspace().getRoot().getFolder(path);
						}
					}).findFirst().ifPresent(this::setSourceFolder);
					IPath path = selectedResource.getFullPath();
					IPath moduleNamePath = getSourceDirs(project).filter(srcDir -> srcDir.isPrefixOf(path))
						.map(srcDir -> path.removeFirstSegments(srcDir.segmentCount())).map(filePath -> {
							if (selectedResource.getType() == IResource.FILE)
								return filePath.removeLastSegments(1);
							else
								return filePath;
						}).findFirst().orElse(Path.EMPTY);
					String moduleName = makeModuleName(moduleNamePath);
					setModuleName(moduleName.isEmpty() ? "" : moduleName + ".");
				}
			}
		}

		setErrorMessage(null);
		setMessage(null);
		setControl(topLevel);
	}

	public IFile createNewFile() {
		if (newFile != null)
			return newFile;
		IFile newFile = sourceFolder.getFile(modulePath);

		IRunnableWithProgress op = monitor -> {
			// @formatter:off
			String contents =
				"module " + moduleName + " where\n" +
				"\n" +
				"data " + moduleSimpleName + " = " + moduleSimpleName + "\n";
			// @formatter:on
			try {
				Charset charset = Charset.forName(sourceFolder.getDefaultCharset());
				CreateFileOperation op1 = new CreateFileOperation(newFile, null,
					new ByteArrayInputStream(contents.getBytes(charset)), "NewFile");
				op1.execute(monitor, WorkspaceUndoUtil.getUIInfoAdapter(getShell()));
			} catch (Exception e) {
				FregePlugin.getInstance().logException(e.getMessage(), e);
			}
		};
		try {
			getContainer().run(true, true, op);
		} catch (InterruptedException e) {
			return null;
		} catch (InvocationTargetException e) {
			FregePlugin.getInstance().logException(e.getMessage(), e);
			return null;
		}
		this.newFile = newFile;
		return newFile;
	}

	private Stream<IPath> getSourceDirs(IProject project) {
		try {
			if (project.isOpen() && project.hasNature(JavaCore.NATURE_ID)) {
				IJavaProject javaProject = JavaCore.create(project);
				return Arrays.stream(javaProject.getResolvedClasspath(true))
					.filter(entry -> entry.getContentKind() == IPackageFragmentRoot.K_SOURCE)
					.map(IClasspathEntry::getPath);
			}
		} catch (CoreException e) {
			FregePlugin.getInstance().logException(e.getMessage(), e);
		}
		return Stream.empty();
	}

	private void setSourceFolder(String sourceFolder) {
		if (!Path.isValidPosixPath(sourceFolder)) {
			setErrorMessage("Invalid source folder path");
			return;
		}
		IPath path = Path.forPosix(sourceFolder);
		IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(path, false);
		if (resource == null) {
			setErrorMessage("Folder does not exists");
			return;
		}
		if (!(resource instanceof IContainer)) {
			setErrorMessage("Folder does not exists");
			return;
		}
		setSourceFolder((IContainer) resource, false);
	}

	private void setSourceFolder(IContainer sourceFolder) {
		setSourceFolder(sourceFolder, true);
	}

	private void setSourceFolder(IContainer sourceFolder, boolean updateText) {
		this.sourceFolder = sourceFolder;
		if (updateText)
			textSourceFolder.setText(sourceFolder.getFullPath().makeRelative().toString());
		setModuleName(textName.getText(), false);
	}

	private String makeModuleName(IPath modulePath) {
		return String.join(".", modulePath.segments());
	}

	private boolean isValidIdentifier(String name) {
		if (name.isEmpty())
			return false;
		if (!Character.isJavaIdentifierStart(name.charAt(0)))
			return false;
		for (int i = 1; i < name.length(); ++i)
			if (!Character.isJavaIdentifierPart(name.charAt(i)))
				return false;
		return true;
	}

	private boolean isValidModuleName(String moduleName) {
		if (moduleName.startsWith(".") || moduleName.endsWith("."))
			return false;
		String[] identifiers = moduleName.split("\\.");
		return Arrays.stream(identifiers).allMatch(this::isValidIdentifier)
			&& Character.isUpperCase(identifiers[identifiers.length - 1].charAt(0));
	}

	private Optional<IPath> parseModuleName(String moduleName) {
		if (!isValidModuleName(moduleName))
			return Optional.empty();
		String pathString = moduleName.replace('.', '/');
		if (!Path.isValidPosixPath(pathString))
			return Optional.empty();
		return Optional.of(Path.forPosix(pathString));
	}

	private void setModuleName(String moduleName) {
		setModuleName(moduleName, true);
	}

	private void setModuleName(String moduleName, boolean updateText) {
		this.moduleName = null;
		this.moduleSimpleName = null;
		this.modulePath = null;
		try {
			if (updateText)
				textName.setText(moduleName);
			if (sourceFolder != null) {
				if (moduleName.isEmpty()) {
					setErrorMessage("Empty module name");
					return;
				}
				Optional<IPath> modulePathO = parseModuleName(moduleName);
				if (!modulePathO.isPresent()) {
					setErrorMessage("Invalid module name");
					return;
				}
				IPath moduleNamePath = modulePathO.get();
				if (sourceFolder.exists(moduleNamePath)) {
					setErrorMessage("Module already exists");
					return;
				}
				this.moduleName = moduleName;
				this.moduleSimpleName = moduleNamePath.segments()[moduleNamePath.segmentCount() - 1];
				this.modulePath = moduleNamePath.addFileExtension("fr");
				setErrorMessage(null);
			}
		} finally {
			setPageComplete(this.modulePath != null);
		}
	}
}
