package frege.imp;

import org.eclipse.imp.services.IAutoEditStrategy;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextUtilities;

import frege.FregePlugin;
import frege.imp.preferences.FregePreferencesConstants;

public class AutoEditStrategy implements IAutoEditStrategy {
	int tabWidth = FregePlugin.getInstance().getPreferencesService().
			getIntPreference(FregePreferencesConstants.P_TABWITH);
	// private static java.util.regex.Pattern spaces = java.util.regex.Pattern.compile("^(\\s+)");   
	public void customizeDocumentCommand(IDocument doc, DocumentCommand cmd) {
		if (cmd.doit == false)
			return;
		// START_HERE
		if (cmd.length == 0 && cmd.text != null
				&& isLineDelimiter(doc, cmd.text)) {
			try {
				smartIndentAfterNewline(doc, cmd);
			} catch (BadLocationException e) {
				System.err.println(this.getClass().getName() + e.getMessage());
				return;
			}
		} else { // if (cmd.text.length() == 1) {
			System.err.println("DocumentCommand: " + cmd.offset + ", " + cmd.length + ", '" + cmd.text + "'");
			// smartIndentOnKeypress(doc, cmd);
		}
	}

	private void smartIndentAfterNewline(IDocument doc, DocumentCommand cmd) throws BadLocationException {
		// TODO Set fields of 'cmd' to reflect desired action,
		// or do nothing to proceed with cmd as is
		IRegion thisLine = doc.getLineInformationOfOffset(cmd.offset);
		String text      = doc.get(thisLine.getOffset(), thisLine.getLength());
		int i = 0;
		for (i = 0; i < text.length() 
				&& Character.isSpaceChar(text.charAt(i)); i++);
		cmd.text += text.substring(0, i);
	}

	private void smartIndentOnKeypress(IDocument doc, DocumentCommand cmd) {
		// TODO Set fields of 'cmd' to reflect desired action,
		// or do nothing to proceed with cmd as is
	}

	private boolean isLineDelimiter(IDocument doc, String text) {
		String[] delimiters = doc.getLegalLineDelimiters();
		if (delimiters != null) {
			return TextUtilities.equals(delimiters, text) > -1;
		}
		return false;
	}
}
