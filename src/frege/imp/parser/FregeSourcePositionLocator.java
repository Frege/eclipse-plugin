package frege.imp.parser;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.imp.editor.ModelTreeNode;
import org.eclipse.imp.parser.ISourcePositionLocator;

import frege.compiler.types.Tokens.IShow_Token;
import frege.compiler.types.Global;
import frege.compiler.types.Global.TGlobal;
import frege.compiler.types.QNames;
import frege.compiler.types.QNames.TQName;
import frege.compiler.types.Positions.TPosition;
import frege.compiler.types.Global.TSubSt;
import frege.compiler.types.Tokens.TToken;
import frege.compiler.types.Symbols.TSymbolT;
import frege.imp.referenceResolvers.FregeReferenceResolver;
import frege.imp.tree.ITreeItem;


/**
 * NOTE:  This version of the ISourcePositionLocator is for use when the Source
 * Position Locator and corresponding Parse Controller are generated separately from
 * a corresponding set of LPG grammar templates and possibly in the absence
 * of the lexer, parser, and AST-related types that would be generated from
 * those templates.  To enable compilation of the Locator and Controller,
 * dummy types have been defined as member types of the Controller in place
 * of possibly missing lexer, parser, and AST-related types.  This version
 * of the Node Locator refers to some of those types.  When those types
 * are replaced by real implementation types, the Locator must be modified
 * to refer to those.  Apart from statements to import needed types from
 * the Parse Controller, this SourcePositionLocator is the same as that used
 * with LPG.
 * @see the corresponding ParseController type
 * 
 * @author Stan Sutton (suttons@us.ibm.com)
 * @since May 15, 2007
 */
public class FregeSourcePositionLocator implements ISourcePositionLocator {
//	private final Object[] fNode = new Object[1];
//	private int fStartOffset;
//	private int fEndOffset;
	final private FregeParseController parser;

	public FregeSourcePositionLocator(FregeParseController parser) {
		this.parser = parser;
	}

	public Object findNode(Object ast, int offset) {
		return findNode(ast, offset, offset);
	}

	/**
	 * Given an offset, find the index of the previous token.
	 * @param arr		an Array of Tokens
	 * @param offset	the offset in the text
	 * @return the index of the very next previous token
	 */
	public static int previous(TToken[] arr, int before) {
		int from = 0;
		int to = arr.length;
		
		while (from +2 < to) {
			int it = (from + to) / 2;
			// if (it==from || it == to) break;
			// System.err.println("previous: before=" + before + ", from=" + from + ", to=" + to);
			TToken at = tokenAt(arr, it);
			int off = TToken.offset(at);
			// System.err.println("previous: it=" + it + ", token=" + IShow_Token.show(at));

			if (off >= before) {	// its more left
				to = it; continue;
			}
			from = it;			
		}
		// linear search down
		while (to >= 0) {
			// System.err.println("previous: before=" + before +  ", to=" + to);
			TToken at = tokenAt(arr, to);
			if (at == null || TToken.col(at) == 0) { to--; continue; }		// no inserted ';' and '}'
			// System.err.println("previous: token=" + IShow_Token.show(at));
			if (TToken.offset(at) < before) return to;
			to--;
		}
		return (-1);		
	}
	/**
	 * Binary search for a token that starts at start and ends not after end.
	 * 
	 * @param arr     an Array of Tokens
	 * @param start   start of selected range
	 * @param end     end of selected range (inklusive)
	 * @return        the index of a Token or (-1) if not found
	 */
	public static int binsearch(TToken[] arr, int start, int end) {
		int from = 0;
		int to = arr.length;
		while (from < to) {
			int it = (from + to) / 2;
			TToken at = arr[it];
			int off = TToken.offset(at);
			int len = TToken.length(at);
			if (off + len <= start) {	// the searched token is more right
				from = it+1; continue;
			}
			if (off > end) {	// its more left
				to = it; continue;
			}
			if (off + len >= start && off+len > end) return it;
			return (-1);
		}
		return (-1);
	}
	
	/**
	 * return the token at a given index or null
	 */
	public static TToken tokenAt(TToken[] arr, int at) {
		if (at < 0 || at >= arr.length)
			return null;
		return arr[at];
	}
	
	public Object findNode(Object ast, int startOffset, int endOffset) {
		System.err.print("findNode( " + ast + ", " + startOffset + ", " +  endOffset + " ) called: ");
		if (ast != null && ast instanceof TGlobal) {
			// find out the token we are working with
			TGlobal global = (TGlobal) ast;
			TToken[] arr  = TSubSt.toks( TGlobal.sub(global) );
			int at = binsearch(arr, startOffset, endOffset);
			TToken res = tokenAt(arr, at);
			if (res == null)
				System.err.println(" no such token");
			else {
				System.err.println(IShow_Token.show(res));
			}
			return res;
		}
		else {
			System.err.println("no compiler state");
		}
		return null;
	}

	public int getStartOffset(TToken node) {   return node==null ? 0 : TToken.offset(node); }
	public int getStartOffset(TPosition pos) { return TPosition.start(pos); }
	public int getStartOffset(Object node) {
		
		if (node != null && node instanceof TToken)
			return TToken.offset((TToken)node);
		
		if (node != null && node instanceof ITreeItem)
			return TPosition.start( ((ITreeItem)node).getPosition() );
		
		if (node != null && node instanceof FregeReferenceResolver.Namespace) {
			final FregeReferenceResolver.Namespace nmsp = (FregeReferenceResolver.Namespace) node;
			if (nmsp.pack.equals(TGlobal.thisPack(nmsp.g)))
				return TToken.offset(TPosition.first(Global.packageStart(nmsp.g).<TPosition>forced()));
			return -1;	// different package
		}
		
		if (node != null && node instanceof FregeReferenceResolver.Symbol) {
			final FregeReferenceResolver.Symbol sym = (FregeReferenceResolver.Symbol) node;
			final TQName  qname = TSymbolT.M.name(sym.sym);
			final boolean our = TGlobal.our(sym.g, qname);
			final int off = getStartOffset(TSymbolT.M.pos(sym.sym)); 
			System.err.println("getStartOffSet( " + QNames.IShow_QName.show(qname) 
					+ " ), our=" + our
					+ " ), off=" + off);
			return off; 
			// return -1;	// different package
		}
		
		if (node != null && node instanceof ModelTreeNode) return 0;
		System.err.println("getStartOffSet( " + node + " ) called");
		return 0; 
	}

	public int getEndOffset(TPosition pos) { return TPosition.end(pos)-1; }
	public int getEndOffset(Object node) { return getStartOffset(node) + getLength(node) - 1; }
		
	public int getLength(TToken node) { return TToken.length(node); }
	public int getLength(TPosition pos) { return TPosition.end(pos)-TPosition.start(pos); }

	public int getLength(Object node) {
		if (node != null && node instanceof TToken)
			return getLength((TToken)node);
		
		if (node != null && node instanceof ITreeItem)
			return getLength(((ITreeItem)node).getPosition());
		
		if (node != null && node instanceof FregeReferenceResolver.Namespace) {
			final FregeReferenceResolver.Namespace nmsp = (FregeReferenceResolver.Namespace) node;
			return nmsp.pack.length();
		}
		
		if (node != null && node instanceof FregeReferenceResolver.Symbol) {
			final FregeReferenceResolver.Symbol sym = (FregeReferenceResolver.Symbol) node;
			// final TQName  qname = TSymbol.M.name(sym.sym);
			// final boolean our = TQName.M.our(qname, sym.g);
			return getLength(TSymbolT.M.pos(sym.sym));
			// return -1;	// different package
		}
		
		if (node != null && node instanceof ModelTreeNode) return 0;
		System.err.println("getLength( " + node + " ) called");
		return 1; 
	}	

	public IPath getPath(Object node) {
		if (node != null && node instanceof FregeReferenceResolver.Namespace) {
			final FregeReferenceResolver.Namespace nmsp = (FregeReferenceResolver.Namespace) node;
			final IPath p = parser.getSource(nmsp.pack);
			System.err.println("getPath( " + nmsp.pack + " ), path=" + p);

			return p;
		}
		if (node != null && node instanceof FregeReferenceResolver.Symbol) {
			final FregeReferenceResolver.Symbol sym = (FregeReferenceResolver.Symbol) node;
			final TQName  qname = TSymbolT.M.name(sym.sym);
			final boolean our = TGlobal.our(sym.g, qname);
			final String  pack  = our ? TGlobal.thisPack(sym.g) : TQName.M.getpack(qname);
			IPath p = parser.getSource(pack);
			System.err.println("getPath( " + QNames.IShow_QName.show(qname) 
					+ " ), our=" + our + ", pack=" + pack + ", path=" + p);
			return p;
		}
		return new Path("");
	}
}
