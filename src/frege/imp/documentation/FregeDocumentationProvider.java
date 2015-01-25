package frege.imp.documentation;

import org.eclipse.imp.parser.IParseController;
import org.eclipse.imp.services.IDocumentationProvider;

import frege.ide.Utilities;
import frege.compiler.types.Tokens;
import frege.imp.referenceResolvers.FregeReferenceResolver;
import frege.imp.parser.FregeParseController;

public class FregeDocumentationProvider implements IDocumentationProvider {
	public String getDocumentation(Object entity, IParseController ctlr) {
		if (entity == null)
			return null;

		if (entity instanceof FregeReferenceResolver.Symbol
				|| entity instanceof FregeReferenceResolver.Namespace) {
			final String s = entity.toString();
			// System.err.println("MARKUP: " + s);
			return s;
		}
		if (entity instanceof Tokens.TToken) {
			final Tokens.TToken token = (Tokens.TToken) entity;
			final FregeParseController fp = (FregeParseController) ctlr; // what else?
			final String s = (String) FregeParseController.funSTIO(Utilities.tokenDocumentation(token), fp.getCurrentAst());
			return (s.length() > 0 ? s : null);
		}
		// System.err.println("getDocumentation(" + entity.toString() + ")");
		return null;
	}

}
