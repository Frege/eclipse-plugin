package frege.imp.parser;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.imp.editor.ModelTreeNode;
import org.eclipse.imp.parser.ISourcePositionLocator;

import frege.compiler.BaseTypes.IShow_Token;
import frege.compiler.Data;
import frege.compiler.Data.TGlobal;
import frege.compiler.Data.TPack;
import frege.compiler.Data.TQName;
import frege.compiler.BaseTypes.TPosition;
import frege.compiler.Data.TSubSt;
import frege.compiler.BaseTypes.TToken;
import frege.compiler.BaseTypes.TTokenID;
import frege.compiler.Data.TSymbol;
import frege.imp.referenceResolvers.FregeReferenceResolver;
import frege.imp.tree.ITreeItem;
import frege.prelude.PreludeBase.TEither;
import frege.prelude.PreludeBase.TEither.DRight;
import frege.prelude.PreludeBase.TMaybe;
import frege.prelude.PreludeBase.TMaybe.DJust;
import frege.rt.Array;
import frege.rt.Box.Int;
import frege.rt.FV;
import frege.rt.Lazy;

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
	 * Binary search for a token that starts at start and ends not after end.
	 * 
	 * @param arr     an Array of Tokens
	 * @param start   start of selected range
	 * @param end     end of selected range (inklusive)
	 * @return        a Token or null if not found
	 */
	public static TToken binsearch(Array<FV> arr, int start, int end) {
		int from = 0;
		int to = arr.length();
		while (from < to) {
			int it = (from + to) / 2;
			TToken at = (TToken) arr.getAt(it)._e();
			int off = TToken.offset(at);
			int len = TToken.length(at);
			if (off + len <= start) {	// the searched token is more right
				from = it+1; continue;
			}
			if (off > end) {	// its more left
				to = it; continue;
			}
			if (off + len >= start && off+len > end) return at;
			return null;
		}
		return null;
	}
	
	public Object findNode(Object ast, int startOffset, int endOffset) {
		System.err.print("findNode( " + ast + ", " + startOffset + ", " +  endOffset + " ) called: ");
		if (ast != null && ast instanceof TGlobal) {
			// find out the token we are working with
			TGlobal global = (TGlobal) ast;
			Array<FV> arr = TSubSt.toks( TGlobal.sub(global) );
			TToken res = binsearch(arr, startOffset, endOffset);
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
			if (nmsp.pack.equals(TGlobal.thisPack(nmsp.g).j))
				return TToken.offset(TPosition.first((TPosition)Data.packageStart(nmsp.g)._e()));
			return -1;	// different package
		}
		
		if (node != null && node instanceof FregeReferenceResolver.Symbol) {
			final FregeReferenceResolver.Symbol sym = (FregeReferenceResolver.Symbol) node;
			final TQName  qname = TSymbol.M.name(sym.sym);
			final boolean our = TQName.M.our(qname, sym.g);
			final int off = getStartOffset(TSymbol.M.pos(sym.sym)); 
			System.err.println("getStartOffSet( " + Data.IShow_QName.show(qname) 
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
			final TQName  qname = TSymbol.M.name(sym.sym);
			// final boolean our = TQName.M.our(qname, sym.g);
			return getLength(TSymbol.M.pos(sym.sym));
			// return -1;	// different package
		}
		
		if (node != null && node instanceof ModelTreeNode) return 0;
		System.err.println("getLength( " + node + " ) called");
		return 1; 
	}	

	public IPath getPath(Object node) {
		if (node != null && node instanceof FregeReferenceResolver.Namespace) {
			final FregeReferenceResolver.Namespace nmsp = (FregeReferenceResolver.Namespace) node;
			final IPath p = parser.getFD().getSource(nmsp.pack);
			return p;
		}
		if (node != null && node instanceof FregeReferenceResolver.Symbol) {
			final FregeReferenceResolver.Symbol sym = (FregeReferenceResolver.Symbol) node;
			final TQName  qname = TSymbol.M.name(sym.sym);
			final boolean our = TQName.M.our(qname, sym.g);
			final String  pack  = our ? TGlobal.thisPack(sym.g).j : TQName.M.getpack(qname).j;
			IPath p = parser.getFD().getSource(pack);
			System.err.println("getPath( " + Data.IShow_QName.show(qname) 
					+ " ), our=" + our + ", pack=" + pack + ", path=" + p);
			return p;
		}
		System.err.println("getPath( " + node + " )  called");
		// TODO Determine path of compilation unit containing this node
		return new Path("");
	}
}
