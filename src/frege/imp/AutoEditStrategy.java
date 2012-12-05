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
		try {
			if (cmd.length == 0 && cmd.text != null
					&& isLineDelimiter(doc, cmd.text)) {
				smartIndentAfterNewline(doc, cmd);
				
			} else if (cmd.length == 1 && 
						(cmd.text == null || cmd.text.length() == 0)) {
				
				// backspace
				smartIndentOnKeypress(doc, cmd);
			}
		} catch (BadLocationException e) {
			System.err.println(this.getClass().getName() + e.getMessage());
			return;
		}
	}

	/**
	 * Put as much spaces on the start of the next line as there are on the current line
	 * 
	 * @param doc
	 * @param cmd
	 * @throws BadLocationException
	 */
	private void smartIndentAfterNewline(IDocument doc, DocumentCommand cmd) throws BadLocationException {
		
		IRegion thisLine = doc.getLineInformationOfOffset(cmd.offset);
		String text      = doc.get(thisLine.getOffset(), thisLine.getLength());
		int i = 0;
		for (i = 0; i < text.length() 
				&& Character.isSpaceChar(text.charAt(i)); i++);
		cmd.text += text.substring(0, i);
	}

	/**
	 * If this is deletion of a character, it deletes all characters till
	 * the previous tab stop if all of those characters are spaces.
	 * @param doc the document
	 * @param cmd the command
	 * @throws BadLocationException
	 */
	private void smartIndentOnKeypress(IDocument doc, DocumentCommand cmd) throws BadLocationException {
//		System.err.println("smartIndentOnKeypress: offset=" + cmd.offset 
//				+ ", length=" + cmd.length 
//				+ ", text='" + cmd.text 
//				+ "', caret=" + cmd.caretOffset);
		IRegion line = doc.getLineInformationOfOffset(cmd.offset);
		String  text = doc.get(line.getOffset(), line.getLength());
		int offsetInLine = cmd.offset - line.getOffset();
		char charAtOffset = text.charAt(offsetInLine);
		
		int tab = offsetInLine;
		while (tabWidth > 0 && tab % tabWidth != 0) tab--;
//		System.err.println("smartIndentOnKeypress: offset=" + offsetInLine
//				+ ", tab=" + tab
//				+ ", char='" + charAtOffset + "'");
		boolean allSpaces = true;
		for (int i=tab; i <= offsetInLine; i++)
			allSpaces = allSpaces && Character.isWhitespace(text.charAt(i));
		if (allSpaces) {
			cmd.offset = line.getOffset() + tab;
			cmd.length = offsetInLine - tab + 1;
		}
	}

	private boolean isLineDelimiter(IDocument doc, String text) {
		String[] delimiters = doc.getLegalLineDelimiters();
		if (delimiters != null) {
			return TextUtilities.equals(delimiters, text) > -1;
		}
		return false;
	}
}
