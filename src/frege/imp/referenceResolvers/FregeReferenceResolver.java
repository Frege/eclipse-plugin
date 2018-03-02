package frege.imp.referenceResolvers;


import io.usethesource.impulse.parser.IParseController;
import io.usethesource.impulse.services.IReferenceResolver;
import frege.data.TreeMap.TTreeMap;
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
import frege.run8.Thunk;



public class FregeReferenceResolver implements IReferenceResolver {

	public static class Symbol {
		public final TGlobal g;
		public final TSymbolT<TGlobal> sym;
		public Symbol(TGlobal g, TSymbolT<TGlobal> sym) { this.g = g; this.sym = sym; }
		public String toString() {
			String s = FregeParseController.funSTIO(
						Utilities.symbolDocumentation(sym), g);
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
			String s = FregeParseController.funSTIO(
						Utilities.packDocumentation(Thunk.lazy(pack)), g);
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
			TMaybe<TEither<Short, TQName>> mb = TGlobal.resolved(g, tok);
			DJust<TEither<Short, TQName>> just = mb.asJust();
			if (just == null) {
				if (isMinus) {
					TToken neg = TToken.upd$value(
									TToken.upd$tokid(tok, TTokenID.VARID),
									"negate");
					mb = TGlobal.resolved(g, neg);
					just = mb.asJust();
					if (just == null) return null;
				}
				else return null;
			}
			final TEither<Short, TQName> lr =  just.mem1.call();
			final DRight<Short, TQName> right = lr.asRight();
			if (right != null) {
				// this is a QName
				TQName q = right.mem1.call();
				final TMaybe<TSymbolT<TGlobal>> mbsym = TGlobal.findit(g, q).call();
				final DJust<TSymbolT<TGlobal>>  jsym  = mbsym.asJust();
				if (jsym == null)	return null; 	// not found?
				final TSymbolT<TGlobal> sym = jsym.mem1.call();
				System.err.println("getLinkTarget: " + QNames.IShow_QName.show(q));
				return new Symbol(g, sym);
			}
			final DLeft<Short, TQName>  left = lr.asLeft();
			if (left != null) {
				// this is a namespace
				String ns = TToken.value(tok);
				final TTreeMap<String, String> tree = TGlobal.namespaces(g);
				final TMaybe<String> mbpack = TTreeMap.lookupS(tree, ns);
				final DJust<String> jpack = mbpack.asJust();
				if (jpack == null) return null;
				String pack = jpack.mem1.call();
				return new Namespace(g, ns, pack);
			}
		}

		return null;
	}
}
