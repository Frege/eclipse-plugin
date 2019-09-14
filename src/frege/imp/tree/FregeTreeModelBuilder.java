package frege.imp.tree;

import io.usethesource.impulse.services.base.TreeModelBuilderBase;
import frege.data.TreeMap.TTreeMap;
import frege.compiler.types.Expression.TExprT;
import frege.compiler.types.Global.TGlobal;
import frege.compiler.types.Positions.TPosition;
import frege.compiler.types.Global.TSubSt;
import frege.compiler.types.Symbols.TSymbolT;
import frege.control.monad.State;
import frege.control.monad.State.TState;
import frege.ide.Utilities;
import frege.imp.parser.FregeParseController;
import frege.prelude.PreludeBase.TList;
import frege.prelude.PreludeBase.TList.DCons;
import frege.prelude.PreludeBase.TMaybe;
import frege.prelude.PreludeBase.TMaybe.DJust;
import frege.prelude.PreludeBase.TTuple3;

public class FregeTreeModelBuilder extends TreeModelBuilderBase {
	private TGlobal prev = null;
	@Override
	public void visitTree(Object root) {
		if (root == null || !(root instanceof TGlobal))
			return;
		TGlobal global = (TGlobal) root;
		if (prev == null || FregeParseController.achievement(prev) <=  FregeParseController.achievement(global))
			prev = global;
		else global = prev;
		
		// fModelRoot = createTopItem(global, ModelTreeNode.DEFAULT_CATEGORY);
		
		FregeModelVisitor visitor = new FregeModelVisitor();

		// rootNode.accept(visitor);
		visitor.visit(global);
	}

	final static public int data = 0;
	final static public int link = 1;
	final static public int dcon = 2;
	final static public int clas = 3;
	final static public int inst = 4;
	final static public int func = 5;
	final static public int type = 6;
	final static public String[] categories = new String[] {
		"Data Types", "Imported Items", "Constructors", "Type Classes", "Instances", 
		"Functions and Values", "Type Aliases" 
	};
	final static public int[] order = new int[] {
		link, clas, inst, type, data, dcon, func
	};

	
	public class FregeModelVisitor /* extends AbstractVisitor */ {		
		
		public boolean visit(TGlobal g, TTreeMap<String, TSymbolT<TGlobal>> env, boolean top) {
			final TList<TSymbolT<TGlobal>> syms = Utilities.symbols(env);
			// do one category after the other according to the predefined order
			for (int cat : order) {
				if (!top) { // avoid unneeded list traversals
					if (cat != func && cat != dcon) continue;
				} 
				else if (cat == dcon) continue;
				
				// go through the list of symbols and do the ones that equal the current category
				DCons<TSymbolT<TGlobal>> elem = syms.asCons();
				boolean found = false;
				while (elem != null) {
					final TSymbolT<TGlobal> sym = elem.mem1.call();
					elem = (elem.mem2.call()).asCons();
					if (sym.constructor() != cat) continue;
					if (sym.constructor() == link && TGlobal.our(g, TSymbolT.alias(sym))) continue;
					if (top) {            // category labels at the top only before first item
						if (!found) {
							pushSubItem(new CategoryItem(categories[cat], TSymbolT.pos(sym)));
							found = true;
						}
					}
					visit(g, sym);
				}
				if (found) popSubItem();
				found = false;
			}
			return true;
		}
		
		public boolean visit(TGlobal g, TSymbolT<TGlobal> sym) {
			pushSubItem(new SymbolItem(g, sym));
			if (TSymbolT.has$env(sym))  visit(g, TSymbolT.env(sym), false);
			else if (TSymbolT.has$expr(sym)) {
				final TMaybe<TState<TGlobal, TExprT>> mbex       = TSymbolT.expr(sym);
				final DJust<TState<TGlobal, TExprT>> just = mbex.asJust();
				if (just != null) {
					TState<TGlobal, TExprT> lam = just.mem1.call();
					final TExprT expr = State.evalState(lam, g);
					visit(g, expr);
				}
			}
			popSubItem();
			return true;
		}
		
		public boolean visit(TGlobal g, TExprT expr) {
			// System.err.println("visiting: " + g.toString() + ", " + expr.toString());
			TList<TSymbolT<TGlobal>> symbols = FregeParseController.funSTG(
					Utilities.exprSymbols(expr), g);
			DCons<TSymbolT<TGlobal>> node = symbols.asCons();
			while (node != null) {
				TSymbolT<TGlobal> sym =  node.mem1.call();
				visit(g, sym);
				node = node.mem2.call().asCons();
			}
			return true;
		}
		
		public boolean visit(TGlobal g) {
			final TSubSt sub = TGlobal.sub(g);
			final String pack = TSubSt.thisPack(sub);
			
			pushSubItem(new PackageItem(pack, TSubSt.thisPos(sub)));
			if  (! "".equals(pack)) {
				final TList<TTuple3<TPosition, String, String>> pnps =  Utilities.imports(g).call();
				DCons<TTuple3<TPosition, String, String>> elem = pnps.asCons();
				while (elem != null) {
					final TTuple3<TPosition, String, String> tuple = elem.mem1.call();
					elem = elem.mem2.call().asCons();
					final TPosition pos = tuple.mem1.call();
					final String ns     = tuple.mem2.call();
					final String p      = tuple.mem3.call();
					createSubItem(new ImportItem(pos, ns, p));
				}
			}
			popSubItem();
			
			if  (! "".equals(pack)) 
				return visit(g, 
						Utilities.thisTab(g), 
						true);
			return true;
		}
	}
}
