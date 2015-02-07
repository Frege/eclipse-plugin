package frege.imp.referenceResolvers;


import org.eclipse.imp.parser.IParseController;
import org.eclipse.imp.services.IReferenceResolver;

import frege.data.TreeMap.TTree;
import frege.compiler.types.Tokens.IShow_Token;
import frege.compiler.types.Tokens.TToken;
import frege.compiler.enums.TokenID.TTokenID;
import frege.compiler.types.Global.TGlobal;
import frege.compiler.types.QNames;
import frege.compiler.types.QNames.TQName;
import frege.compiler.types.Symbols.TSymbolT;
import frege.ide.Utilities;
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
		public final TSymbolT sym;
		public Symbol(TGlobal g, TSymbolT sym) { this.g = g; this.sym = sym; }
		public String toString() {
			String s = Delayed.<String> forced(FregeParseController.funSTIO(
					Utilities.symbolDocumentation(sym), g));
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
			String s = Delayed.<String>forced(FregeParseController.funSTIO(
						Utilities.packDocumentation(pack), g));
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
		TGlobal g = null;
		
		if (controller != null) {
			Object o = controller.getCurrentAst();
			if (o != null && o instanceof TGlobal) g = (TGlobal) o;
		}
		
		if (g != null && node != null && node instanceof TToken) {
			TToken tok = (TToken) node;
			System.err.println("getLinkTarget: " + IShow_Token.show(tok));
			int tid = TToken.tokid(tok);
			if (tid == TTokenID.CHAR && "_".equals(TToken.value(tok))) {
				tid = TTokenID.VARID;
				tok = TToken.upd$tokid(tok, TTokenID.VARID);
				tok = TToken.upd$value(tok, "it");
			}
			
			// is this '-' ?
			final boolean isMinus = tid == TTokenID.CHAR && "-".equals(TToken.value(tok));
			
			if (tid != TTokenID.VARID 
					&& tid != TTokenID.CONID
					&& tid != TTokenID.QUALIFIER
					&& tid != TTokenID.SOMEOP
					&& !isMinus
				) return null;
			TMaybe mb = TGlobal.resolved(g, tok);
			DJust just = mb._Just();
			if (just == null) {
				if (isMinus) {
					TToken neg = TToken.upd$value(
									TToken.upd$tokid(tok, TTokenID.VARID),
									"negate");
					mb = TGlobal.resolved(g, neg);
					just = mb._Just();
					if (just == null) return null;
				}
				else return null;
			}
			final TEither lr = Delayed.<TEither>forced( just.mem1 );
			final DRight right = lr._Right();
			if (right != null) {
				// this is a QName
				TQName q = Delayed.<TQName>forced( right.mem1 );
				final TMaybe mbsym = TGlobal.findit(g, q).<TMaybe>forced();
				final DJust  jsym  = mbsym._Just();
				if (jsym == null)	return null; 	// not found?
				final TSymbolT sym = Delayed.<TSymbolT>forced( jsym.mem1 );
				System.err.println("getLinkTarget: " + QNames.IShow_QName.show(q));
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
