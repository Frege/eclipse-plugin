package frege.imp.contentProposer;

import frege.compiler.BaseTypes.TToken;
import frege.compiler.BaseTypes.TTokenID;
import frege.compiler.BaseTypes;
import frege.compiler.Data.TGlobal;
import frege.compiler.Data.TSubSt;
import frege.compiler.EclipseUtil;
import frege.compiler.EclipseUtil.IShow_Proposal;
import frege.compiler.EclipseUtil.TProposal;
import frege.imp.parser.*;
import frege.prelude.PreludeBase.TList;
import frege.rt.Array;
import frege.rt.FV;
import frege.rt.Lazy;
import frege.rt.Box;

import java.util.*;

import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.source.ContentAssistantFacade;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.imp.services.IContentProposer;
import org.eclipse.imp.editor.ErrorProposal;
import org.eclipse.imp.editor.SourceProposal;
import org.eclipse.imp.editor.UniversalEditor;
import org.eclipse.imp.parser.IParseController;
import org.eclipse.imp.parser.ISourcePositionLocator;

public class ContentProposer implements IContentProposer {
	/**
	 * An extension of {@link SourceProposal} that computes additional information
	 * lazily.
	 */
	static class Proposal extends SourceProposal {
		Lazy<FV> additional;
		public Proposal(String proposal, String newText, String prefix, int offset, int length, 
				int cursor, Lazy<FV> additional) {
			super(proposal, newText, prefix, new Region(offset, length), cursor);
			this.additional = additional;
		}
		
		public String getAdditionalProposalInfo() {
			return Box.<String>box(additional._e()).j;
		}
		public static Proposal convert(final TProposal p) {
			final String newT = TProposal.newText(p);
			final int off     = TProposal.off(p); 
			return new Proposal(
					TProposal.proposal(p),
					newT,
					"",			// TProposal.prefix(p),
					off,
					TProposal.len(p),
					TProposal.cursor(p)+off+newT.length(),
					p.mem7
					);
		}
	}
	
		
	/**
	 * Returns an array of content proposals applicable relative to the AST of the given
	 * parse controller at the given position.
	 * 
	 * (The provided ITextViewer is not used in the default implementation provided here
	 * but but is stipulated by the IContentProposer interface for purposes such as accessing
	 * the IDocument for which content proposals are sought.)
	 * 
	 * @param controller	A parse controller from which the AST of the document being edited
	 * 						can be obtained
	 * @param int			The offset for which content proposals are sought
	 * @param viewer		The viewer in which the document represented by the AST in the given
	 * 						parse controller is being displayed (may be null for some implementations)
	 * @return				An array of completion proposals applicable relative to the AST of the given
	 * 						parse controller at the given position
	 */
	public ICompletionProposal[] getContentProposals(IParseController ctlr,
			int offset, ITextViewer viewer) {
		FregeParseController parser = (FregeParseController) ctlr;
		
		
//		SourceViewer srcViewer = viewer instanceof SourceViewer ? (SourceViewer) viewer : null ;
//		if (srcViewer != null) {
//			ContentAssistantFacade facade = srcViewer.getContentAssistantFacade();
//			if (facade != null) {
//				facade.getHandler(ContentAssistant.SELECT_NEXT_PROPOSAL_COMMAND_ID);
//			}
//		}
//		String src = viewer.getDocument().get();
//		UniversalEditor editor = (UniversalEditor) viewer;
//		editor.getDocumentProvider();
//		
//		if (src.length() != parser.getLeng() 
//					|| src.hashCode() != parser.getHash()) {
//			return result.toArray(new ICompletionProposal[result.size()]);		// buffer dirty, return previous result
//		}

		List<ICompletionProposal> result = new ArrayList<ICompletionProposal>();
		final TGlobal g = parser.getGoodAst();
		final Array tokens = TSubSt.toks(TGlobal.sub(g));	
			
		if (g != null) {
			int inx = FregeSourcePositionLocator.previous(tokens, offset);
			
			TToken token = FregeSourcePositionLocator.tokenAt(tokens, inx);
			TToken tprev = FregeSourcePositionLocator.tokenAt(tokens, inx-1);
			boolean direct = false;
			boolean inside = false;
			String  id = "none";
			String  idprev = tprev == null ? "" 
								: BaseTypes.IShow_TokenID.show(TToken.tokid(tprev).j) + ",";
			String  pref = ""; 
			String  val = null;
			if (token != null) {
				direct = TToken.offset(token) + TToken.length(token) == offset;
				inside = TToken.offset(token) + TToken.length(token) >  offset;
				id = BaseTypes.IShow_TokenID.show(TToken.tokid(token).j);
				val  = TToken.value(token);
				try {
					pref = inside ? val.substring(0, offset - TToken.offset(token)) : "";
				} catch (IndexOutOfBoundsException e) {
					// stay on the safe side
					pref = "";
				}
			}
			System.err.println("getContentProposal offset=" + offset
						+ ", tokenID=" + idprev + id
						+ ", value=\"" + val + '"'
						+ ", direct=" + direct
						+ ", inside=" + inside);
				
			TList ps = EclipseUtil.proposeContent(g, offset, tokens, inx);
			boolean first = true;
			while (true) {
				final TList.DCons node = ps._Cons();
				if (node == null) break;
				TProposal p = (TProposal) node.mem1._e();
				if (first) {
					first = false;
					pref = TProposal.prefix(p);
					System.err.println("getContentProposal: " + IShow_Proposal.show(p));
				}
				result.add(Proposal.convert(p));
				ps = (TList) node.mem2._e();
			}
			if (first) {			// empty proposal list
				if (TGlobal.errors(g) > 0) {
					result.add(new ErrorProposal(
							"No proposals available, please correct syntax errors first.", offset));
				}
				else if (pref.length() > 0) {
					result.add(new ErrorProposal(
							"No proposals available, maybe prefix \"" + pref + "\" too restrictive?", offset));
				}
				else {
					result.add(new ErrorProposal(
							"Can't help you here. Sorry.", offset));
				}
			}
		} else {
			result.add(new ErrorProposal(
					"No proposals available - syntax errors", offset));
		}
		return result.toArray(new ICompletionProposal[result.size()]);
	}

}
