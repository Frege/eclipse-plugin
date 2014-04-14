package frege.imp.editorActions;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.imp.editor.UniversalEditor;
import org.eclipse.imp.model.ISourceProject;
import org.eclipse.imp.model.ModelFactory;
import org.eclipse.imp.parser.IParseController;
import org.eclipse.imp.services.ILanguageActionsContributor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;

import frege.compiler.types.Global.TGlobal;
import frege.data.TreeMap.TTree;
import frege.imp.builders.FregeBuilder;
import frege.imp.parser.FregeParseController;
// import org.eclipse.ui.IFileEditorInput;

public class FregeEditorActionContributions implements
		ILanguageActionsContributor {
	
	
	private Action fgAction(final UniversalEditor uditor) {
		Action it = new Action("f • g") {
			public void run() {
				UniversalEditor editor = uditor;
				if (editor == null) {
					IEditorPart ed = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
					if (ed == null) return;
					if (ed instanceof UniversalEditor)
						editor = (UniversalEditor) ed;
					else return;
				}
				// editor is not null
				try {
					if (!editor.isEditable()) return;
					final Point where = editor.getSelection();
					// System.err.println("we are at " + where);
			        final IDocument document= editor.
			        		getDocumentProvider().
			        		getDocument(editor.getEditorInput());
			        if (document != null) {
			        	document.replace(where.x, where.y, "•");
			        	editor.selectAndReveal(where.x+1, 0);
			        }
				} 
			    catch (BadLocationException e) {
					// TODO Auto-generated catch block
					// e.printStackTrace();
				}
			}
		};
		it.setDescription("Insert the • symbol for the function composition operator.");
		it.setToolTipText(it.getDescription());
		it.setId("frege-editoraction-bullet");
		return it;
	}
	
	private Action rgxAction(final UniversalEditor uditor) {
		Action it = new Action("´regex´") {
			public void run() {
				UniversalEditor editor = uditor;
				if (editor == null) {
					IEditorPart ed = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
					if (ed == null) return;
					// System.err.println(ed.getClass().getName());
					if (ed instanceof UniversalEditor)
						editor = (UniversalEditor) ed;
					else return;
				}
				// editor is not null
				try {
					if (!editor.isEditable()) return;
					final Point where = editor.getSelection();
					// System.err.println("we are at " + where);
			        final IDocument document= editor.
			        		getDocumentProvider().
			        		getDocument(editor.getEditorInput());
			        if (document != null) {
			        	String w = document.get(where.x, where.y);
			        	document.replace(where.x, where.y, "´" + w + "´");
			        	if (w.length() == 0)
			        		editor.selectAndReveal(where.x+1, 0);
			        }
				} 
			    catch (BadLocationException e) {
					// TODO Auto-generated catch block
					// e.printStackTrace();
				}
			}
		};
		it.setDescription("Enclose the selected text in grave accent marks to form a regular expression.");
		it.setToolTipText(it.getDescription());
		it.setId("frege-editoraction-rgx");
		return it;
	}

	private Action opAction(final UniversalEditor uditor) {
		Action it = new Action("`op`") {
			public void run() {
				UniversalEditor editor = uditor;
				if (editor == null) {
					IEditorPart ed = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
					if (ed == null) return;
					// System.err.println(ed.getClass().getName());
					if (ed instanceof UniversalEditor)
						editor = (UniversalEditor) ed;
					else return;
				}
				// editor is not null
				try {
					if (!editor.isEditable()) return;
					final Point where = editor.getSelection();
					// System.err.println("we are at " + where);
			        final IDocument document= editor.
			        		getDocumentProvider().
			        		getDocument(editor.getEditorInput());
			        if (document != null) {
			        	String w = document.get(where.x, where.y);
			        	document.replace(where.x, where.y, "`" + w + "`");
			        	if (w.length() == 0)
			        		editor.selectAndReveal(where.x+1, 0);
			        }
				} 
			    catch (BadLocationException e) {
					// TODO Auto-generated catch block
					// e.printStackTrace();
				}
			}
		};
		it.setDescription("Enclose the selected text in accent marks to form an operator.");
		it.setToolTipText(it.getDescription());
		it.setId("frege-editoraction-op");
		return it;
	}

	private Action refreshAction(final UniversalEditor uditor) {
		Action it = new Action("Refresh") {
			public void run() {
				UniversalEditor editor = uditor;
				if (editor == null) {
					IEditorPart ed = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
					if (ed == null) return;
					// System.err.println(ed.getClass().getName());
					if (ed instanceof UniversalEditor)
						editor = (UniversalEditor) ed;
					else return;
				}
				final UniversalEditor theEditor = editor; 
				// editor is not null
				try {
					if (!editor.isEditable()) return;
					IParseController pc = editor.getParseController();
					if (pc == null || !(pc instanceof FregeParseController)) return;
					final FregeParseController fpc = (FregeParseController) pc;
					TGlobal global = fpc.getCurrentAst();
			        if (global == null) return;
			        // global.mem$sub.mem$cache.put(TTree.DNil.it);
			        // fpc.global = FregeParseController.runSTG(action, global);
			        
			        final IDocument document= editor.
			        		getDocumentProvider().
			        		getDocument(editor.getEditorInput());
			        
			        fpc.resetHash();
			        fpc.msgHandler.clearMessages();
					theEditor.removeParserAnnotations();
					document.replace(0,1, document.get(0, 1));			        
				} 
			    catch (Exception e) {
					// e.printStackTrace();
				}
			}
		};
		it.setDescription("Notify parser about changes in imported modules.");
		it.setToolTipText(it.getDescription());
		it.setId("frege-editoraction-refresh");
		return it;
	}

	
	private Action compAction(final UniversalEditor uditor) {
		Action it = new Action("Compile") {
			public void run() {
				UniversalEditor editor = uditor;
				if (editor == null) {
					IEditorPart ed = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
					if (ed == null) return;
					// System.err.println(ed.getClass().getName());
					if (ed instanceof UniversalEditor)
						editor = (UniversalEditor) ed;
					else return;
				}
				// editor is not null
				try {
					if (!editor.isEditable()) return;
					IParseController pc = editor.getParseController();
					if (pc == null) return;
					IPath dpath = pc.getPath();
					ISourceProject project = pc.getProject();
					dpath = project != null ?
							project.getRawProject().getLocation().append(dpath)
							: dpath;
			        System.err.println("The path is " + dpath);
			        IWorkspace workspace= ResourcesPlugin.getWorkspace();
			        final IFile dfile = workspace.getRoot().getFileForLocation(dpath);
			        Job job = new Job("Compile " + dpath) {
			        	@Override protected IStatus run(IProgressMonitor monitor) {
			        		if (new FregeBuilder().compiled(dfile, monitor))
			        			return Status.OK_STATUS;
			        		return Status.CANCEL_STATUS;
			        	}
			        	
			        };
			        job.schedule();
			        System.err.println("Job " + job.getName() + " scheduled.");
				} 
			    catch (Exception e) {
					// e.printStackTrace();
				}
			}
		};
		it.setDescription("Invoke the builder on the active file.");
		it.setToolTipText(it.getDescription());
		it.setId("frege-editoraction-comp");
		return it;
	}
	
	public void contributeToEditorMenu(final UniversalEditor editor,
			IMenuManager menuManager) {
		IMenuManager languageMenu = new MenuManager("Frege");
		menuManager.add(languageMenu);
		languageMenu.add(fgAction(editor));
		languageMenu.add(rgxAction(editor));
		languageMenu.add(opAction(editor));
		languageMenu.add(compAction(editor));
		languageMenu.add(refreshAction(editor));
	}

	public void contributeToMenuBar(UniversalEditor editor, IMenuManager menu) {
		// TODO implement contributions and add them to the menu
		
	}

	public void contributeToStatusLine(final UniversalEditor editor,
			IStatusLineManager statusLineManager) {
		// TODO add ControlContribution objects to the statusLineManager
	}

	public void contributeToToolBar(UniversalEditor editor,
			IToolBarManager toolbarManager) {
		
		Action fg = fgAction(null);
		if (toolbarManager.find(fg.getId()) == null)
			toolbarManager.add(fg);
		Action rgx = rgxAction(null);
		if (toolbarManager.find(rgx.getId()) == null)
			toolbarManager.add(rgx);
		Action op = opAction(null);
		if (toolbarManager.find(op.getId()) == null)
			toolbarManager.add(op);
		Action comp = compAction(null);
		if (toolbarManager.find(comp.getId()) == null)
			toolbarManager.add(comp);
		Action refresh = refreshAction(null);
		if (toolbarManager.find(refresh.getId()) == null)
			toolbarManager.add(refresh);
	}
}
