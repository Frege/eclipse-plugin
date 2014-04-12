package frege.imp.tree;

import org.eclipse.imp.services.base.TreeModelBuilderBase;

import frege.data.TreeMap.TTree;
import frege.compiler.types.Definitions.TExprT;
import frege.compiler.types.Global.TGlobal;
import frege.compiler.types.Positions.TPosition;
import frege.compiler.types.Global.TSubSt;
import frege.compiler.types.Symbols.TSymbol;
import frege.ide.Utilities;
import frege.imp.parser.FregeParseController;
import frege.prelude.PreludeBase.TList;
import frege.prelude.PreludeBase.TList.DCons;
import frege.prelude.PreludeBase.TMaybe;
import frege.prelude.PreludeBase.TTuple3;
import frege.runtime.Delayed;

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
		
		public boolean visit(TGlobal g, TTree env, boolean top) {
			final TList syms = Utilities.symbols(env).<TList>forced();
			// do one category after the other according to the predefined order
			for (int cat : order) {
				if (!top) { // avoid unneeded list traversals
					if (cat != func && cat != dcon) continue;
				} 
				else if (cat == dcon) continue;
				
				// go through the list of symbols and do the ones that equal the current category
				TList.DCons elem = syms._Cons();
				boolean found = false;
				while (elem != null) {
					final TSymbol sym = Delayed.<TSymbol>forced( elem.mem1 );
					elem = (elem.mem2.<TList>forced())._Cons();
					if (sym._constructor() != cat) continue;
					if (sym._constructor() == link && TGlobal.our(g, TSymbol.M.alias(sym))) continue;
					if (top) {            // category labels at the top only before first item
						if (!found) {
							pushSubItem(new CategoryItem(categories[cat], TSymbol.M.pos(sym)));
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
		
		public boolean visit(TGlobal g, TSymbol sym) {
			pushSubItem(new SymbolItem(g, sym));
			if (TSymbol.M.has$env(sym))  visit(g, TSymbol.M.env(sym), false);
			else if (TSymbol.M.has$expr(sym)) {
				final TMaybe mbex       = TSymbol.M.expr(sym);
				final TMaybe.DJust just = mbex._Just();
				if (just != null) {
					final TExprT expr = Delayed.<TExprT>forced( just.mem1 );
					visit(g, expr);
				}
			}
			popSubItem();
			return true;
		}
		
		public boolean visit(TGlobal g, TExprT expr) {
			// System.err.println("visiting: " + g.toString() + ", " + expr.toString());
			TList symbols = (TList) FregeParseController.funSTG(
					Utilities.exprSymbols(expr), g);
			TList.DCons node = symbols._Cons();
			while (node != null) {
				TSymbol sym = Delayed.<TSymbol>forced( node.mem1);
				visit(g, sym);
				node = (node.mem2.<TList>forced())._Cons();
			}
			return true;
		}
		
		public boolean visit(TGlobal g) {
			final TSubSt sub = TGlobal.sub(g);
			final String pack = TSubSt.thisPack(sub);
			
			pushSubItem(new PackageItem(pack, TSubSt.thisPos(sub)));
			if  (! "".equals(pack)) {
				final TList pnps =  Utilities.imports(g).<TList>forced();
				DCons elem = pnps._Cons();
				while (elem != null) {
					final TTuple3 tuple = Delayed.<TTuple3>forced( elem.mem1 );
					elem = (elem.mem2.<TList>forced())._Cons();
					final TPosition pos = Delayed.<TPosition>forced(tuple.mem1);
					final String ns     = Delayed.<String>forced(tuple.mem2);
					final String p      = Delayed.<String>forced(tuple.mem3);
					createSubItem(new ImportItem(pos, ns, p));
				}
			}
			popSubItem();
			
			if  (! "".equals(pack)) 
				return visit(g, 
						Utilities.thisTab(g).<TTree>forced(), 
						true);
			return true;
		}
	}
}
