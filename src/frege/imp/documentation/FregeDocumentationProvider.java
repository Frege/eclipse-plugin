package frege.imp.documentation;

import org.eclipse.imp.parser.IParseController;
import org.eclipse.imp.services.IDocumentationProvider;

import frege.imp.referenceResolvers.FregeReferenceResolver;

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
		return null;
	}

}
