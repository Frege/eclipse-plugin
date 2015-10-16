package frege.imp.hoverHelper;

import java.util.List;






import io.usethesource.impulse.editor.AnnotationHoverBase;
// import io.usethesource.impulse.language.ServiceFactory;
import io.usethesource.impulse.parser.IParseController;
import io.usethesource.impulse.parser.ISourcePositionLocator;
// import io.usethesource.impulse.parser.SimpleLPGParseController;
import io.usethesource.impulse.services.IDocumentationProvider;
import io.usethesource.impulse.services.IHoverHelper;
import io.usethesource.impulse.services.IReferenceResolver;
import io.usethesource.impulse.services.base.HoverHelperBase;
// import io.usethesource.impulse.utils.ExtensionException;
// import io.usethesource.impulse.utils.ExtensionFactory;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.graphics.Point;

// import frege.FregePlugin;
// import frege.imp.parser.FregeParseController;

public class FregeHoverHelper extends HoverHelperBase implements IHoverHelper {
	IReferenceResolver fResolver = new frege.imp.referenceResolvers.FregeReferenceResolver();
	IDocumentationProvider docProvider = new frege.imp.documentation.FregeDocumentationProvider();

	public String getHoverHelpAt(IParseController parseController,
			ISourceViewer srcViewer, int offset) {
		// If there are any annotations associated with the line that contains
		// the given offset, return those
		try {
			List<Annotation> annotations = AnnotationHoverBase.getSourceAnnotationsForLine(
					srcViewer, srcViewer.getDocument().getLineOfOffset(offset));
			if (annotations != null && annotations.size() > 0) {
				// Some annotations have no text, such as breakpoint annotations;
				// if that's all we have, then don't bother returning it
				String msg = AnnotationHoverBase
						.formatAnnotationList(annotations);
				if (msg != null) {
					return msg;
				}
			}
		} catch (BadLocationException e) {
			return "??? (BadLocationException for annotation)";
		}

		// Otherwise, return a message determined directly or indirectly based
		// on the node whose representation occurs at the given offset

		// Get the current AST; no AST implies no message
		Object ast = parseController.getCurrentAst();
		if (ast == null)
			return null;

		// Declare variables used in formulating the message
		Object sourceNode = null; // node at current hover point
		Object targetNode = null; // node referenced from current hover point
		Object helpNode = null; // node for which a help message is to be constructed
		String msg = null; // the help message for helpNode
		
		// Get the node at the given offset; no node implies no message
		ISourcePositionLocator nodeLocator = parseController
				.getSourcePositionLocator();
		sourceNode = nodeLocator.findNode(ast, offset);
		if (sourceNode == null)
			return null;

		
		if (fResolver != null) {
				targetNode = fResolver.getLinkTarget(sourceNode,
						parseController);
		}
		

		// If the target node is not null, provide help based on that;
		// otherwise, provide help based on the source node
		if (targetNode != null)
			helpNode = targetNode;
		else
			helpNode = sourceNode;
		

		// Now need to determine whether the help message should be determined
		// based on the text represented by the node or based on some separate
		// text provided through an IDocumentationProvider

		if (docProvider != null) {
			msg = docProvider.getDocumentation(helpNode, parseController);
			if (msg != null)
				return msg;
		}

//		// Otherwise, base the help message on the text that is represented
//		// by the help node
//		if (helpNode instanceof ASTNode) {
//			ASTNode def = (ASTNode) helpNode;
//			msg = getSubstring(parseController, def.getLeftIToken()
//					.getStartOffset(), def.getRightIToken().getEndOffset());
//			int maxMsgLen = 80;
//			if (msg == null || msg.length() == 0)
//				return "No help available";
//			else if (msg.length() <= maxMsgLen)
//				return msg;
//			else
//				return msg.subSequence(0, maxMsgLen) + "...";
//		} else {
//			return "No help available";
//		}
		return "no help available";
	}

}
