package frege.imp.referenceResolvers;


import org.eclipse.imp.parser.IParseController;
import org.eclipse.imp.services.IReferenceResolver;

import frege.List.TTree;
import frege.compiler.BaseTypes.IShow_Token;
import frege.compiler.BaseTypes.TToken;
import frege.compiler.BaseTypes.TTokenID;
import frege.compiler.Data;
import frege.compiler.Data.TGlobal;
import frege.compiler.Data.TQName;
import frege.compiler.Data.TSymbol;
import frege.compiler.EclipseUtil;
import frege.imp.parser.FregeParseController;
import frege.prelude.PreludeBase.TEither;
import frege.prelude.PreludeBase.TEither.DLeft;
import frege.prelude.PreludeBase.TEither.DRight;
import frege.prelude.PreludeBase.TMaybe;
import frege.prelude.PreludeBase.TMaybe.DJust;
import frege.runtime.Delayed;


public class FregeReferenceResolver implements IReferenceResolver {

	public static class Symbol {
		public final TGlobal g;
		public final TSymbol sym;
		public Symbol(TGlobal g, TSymbol sym) { this.g = g; this.sym = sym; }
		public String toString() {
			String s = Delayed.<String> forced(FregeParseController.funStG(
					EclipseUtil.symbolDocumentation(sym), g));
			return s; // Data.INice_QName.nicer(TSymbol.M.name(sym), g);
		}
	}
	
	public static class Namespace {
		public final TGlobal g;
		public final String  ns;
		public final String pack;
		public Namespace(TGlobal g, String ns, String p) { 
			this.g = g; 
			this.ns = ns;
			this.pack = p;
		}
		public String toString() {
			String s = Delayed.<String>forced(FregeParseController.funStG(
						EclipseUtil.packDocumentation(pack), g));
			return s;
		}
	}
	
	public FregeReferenceResolver() {
	}

	/**
	 * Get the text associated with the given node for use in a link
	 * from (or to) that node
	 */
	public String getLinkText(Object node) {
		// TODO Replace the following with an implementation suitable to your language and reference types
		return "unimplemented";    // node.toString();
	}

	/**
	 * Get the target for the given source node in the AST produced by the
	 * given Parse Controller.
	 */
	public Object getLinkTarget(Object node, IParseController controller) {
		// START_HERE
		// TODO Replace the following with an implementation suitable for your language and reference types
		TGlobal g = null;
		
		if (controller != null) {
			Object o = controller.getCurrentAst();
			if (o != null && o instanceof TGlobal) g = (TGlobal) o;
		}
		
		if (g != null && node != null && node instanceof TToken) {
			TToken tok = (TToken) node;
			System.err.println("getLinkTarget: " + IShow_Token.show(tok));
			final int tid = TToken.tokid(tok);
			if (tid != TTokenID.VARID 
					&& tid != TTokenID.CONID
					&& tid != TTokenID.QUALIFIER
					&& (tid < TTokenID.LOP0 ||  tid > TTokenID.SOMEOP)) return null;
			final TMaybe mb = TGlobal.resolved(g, tok);
			final DJust just = mb._Just();
			if (just == null) return null;
			final TEither lr = Delayed.<TEither>forced( just.mem1 );
			final DRight right = lr._Right();
			if (right != null) {
				// this is a QName
				TQName q = Delayed.<TQName>forced( right.mem1 );
				final TMaybe mbsym = TQName.M.findit(q, g).<TMaybe>forced();
				final DJust  jsym  = mbsym._Just();
				if (jsym == null)	return null; 	// not found?
				final TSymbol sym = Delayed.<TSymbol>forced( jsym.mem1 );
				System.err.println("getLinkTarget: " + Data.IShow_QName.show(q));
				return new Symbol(g, sym);
			}
			final DLeft  left = lr._Left();
			if (left != null) {
				// this is a namespace
				String ns = TToken.value(tok);
				final TTree tree = TGlobal.namespaces(g);
				final TMaybe mbpack = TTree.M.lookupS(tree, ns);
				final DJust jpack = mbpack._Just();
				if (jpack == null) return null;
				String pack = Delayed.<java.lang.String>forced(jpack.mem1);
				return new Namespace(g, ns, pack);
			}
		}

		return null;
	}
}
