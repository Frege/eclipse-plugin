package frege.imp.parser;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// import lpg.runtime.ILexStream;
// import lpg.runtime.IPrsStream;
// import lpg.runtime.Monitor;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.imp.builder.MarkerCreatorWithBatching;
import org.eclipse.imp.builder.ProblemLimit.LimitExceededException;
import org.eclipse.imp.model.ISourceProject;
import org.eclipse.imp.parser.IMessageHandler;
import org.eclipse.imp.parser.IParseController;
import org.eclipse.imp.parser.IParser;
import org.eclipse.imp.parser.ISourcePositionLocator;
import org.eclipse.imp.parser.ParseControllerBase;
import org.eclipse.imp.parser.SimpleAnnotationTypeInfo;
import org.eclipse.imp.preferences.IPreferencesService;
import org.eclipse.imp.services.IAnnotationTypeInfo;
import org.eclipse.imp.services.ILanguageSyntaxProperties;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.source.LineRange;

import frege.FregePlugin;
import frege.rt.Array;
import frege.rt.Lambda;
import frege.rt.Box;
import frege.rt.FV;
import frege.rt.Lazy;
import frege.prelude.PreludeBase.TList.DCons;
import frege.prelude.PreludeBase.TTuple2;
import frege.prelude.PreludeBase.TList;
import frege.prelude.PreludeBase.TTuple3;
import frege.prelude.PreludeBase.TState;
import frege.prelude.PreludeList.IListLike__lbrack_rbrack;
import frege.compiler.BaseTypes.TFlag;
import frege.compiler.BaseTypes.IEnum_Flag;
import frege.compiler.Data.TGlobal;
import frege.compiler.Data.TMessage;
import frege.compiler.Data.TOptions;
import frege.compiler.BaseTypes.TPosition;
import frege.compiler.Data.TSeverity;
import frege.compiler.Data.TSubSt;
import frege.compiler.BaseTypes.TToken;
import frege.compiler.BaseTypes.TTokenID;
import frege.compiler.Utilities;
import frege.compiler.EclipseUtil;
import frege.compiler.Main;
import frege.imp.builders.FregeBuilder;
import frege.imp.preferences.FregePreferencesConstants;

/**
 * NOTE:  This version of the Parse Controller is for use when the Parse
 * Controller and corresponding Node Locator are generated separately from
 * a corresponding set of LPG grammar templates and possibly in the absence
 * of the lexer, parser, and AST-related types that would be generated from
 * those templates.  It is assumed that either a) the Controller will be
 * used with a suitable set of lexer, parser, and AST-related types
 * that are provided by some means other than LPG, or b) the Controller will
 * be used with a set of lexer, parser, and AST types that have been, or will
 * be, separately generated based on LPG.  In order to enable this version of
 * the Parse Controller to compile, dummy lexer, parser, and AST-related types
 * have been included as member types in the Controller.  These types are not
 * operational and are merely placeholders for types that would support a
 * functioning implementation.  Apart from the inclusion of these dummy types,
 * this representation of the Parse Controller is the same as that used
 * with LPG.
 * 	
 * @author Stan Sutton (suttons@us.ibm.com)
 * @since May 1,  2007	Addition of marker types
 * @since May 10, 2007	Conversion IProject -> ISourceProject
 * @since May 15, 2007	Addition of dummy types
 */
public class FregeParseController extends ParseControllerBase implements
		IParseController {

	public static class TokensIterator implements Iterator<TToken> {
		/** current token array */
		final private Array toks;
		private IRegion region;
		private int  inx;
		
		/** check if token is within region */
		public static boolean within(TToken tok, IRegion region) {
			return (TToken.offset(tok) + TToken.value(tok).length() >= region.getOffset()
					&& TToken.offset(tok) <= region.getOffset() + region.getLength());
		}
		
		/** construct an Iterator */
		public TokensIterator(Array it, IRegion reg) { 
			toks = it;
			region = reg;
			inx = 0;
			while (inx < toks.length()) {
				TToken t = (TToken) toks.getAt(inx)._e(); 
				if (within(t, reg)) break;
				inx++;
			}
		}
		
		public static int skipBraces(final Array toks, int j) {
			while (j < toks.length()) {
				TToken tok = (TToken) toks.getAt(j)._e();
				if (tok.mem1 == TTokenID.CHAR.j
						&& (tok.mem2.charAt(0) == '{'
								|| tok.mem2.charAt(0) == '}'
								|| tok.mem2.charAt(0) == ';')) {
					j++;
				}
				else break;
			}
			return j;
		}
		
		@Override
		public boolean hasNext() {
			// skip { ; }
			inx = skipBraces(toks, inx);
			// we have a next if we are not the empty list and the token is in the region
			return inx < toks.length()
					&& within((TToken)toks.getAt(inx)._e(), region);
		}
		
		@Override
		public TToken next() {
			// give back next token
			if (inx < toks.length()) {
				return (TToken) toks.getAt(inx++)._e();
			}
			return null;
		}
		
		@Override
		public void remove() {
			throw new UnsupportedOperationException("TokensIterator");
		}		
	}
	
	public static class FregeData {
		private String sp = ".";
		private String fp = ".";
		private String bp = ".";
		private ISourceProject project = null;
		public FregeData(ISourceProject sourceProject) {
			project = sourceProject;
			if (project != null) {
				IProject rp = project.getRawProject();
				// System.out.println("The raw project has type: " + jp.getClass());
				IWorkspace workspace = ResourcesPlugin.getWorkspace();
				IPath wroot = workspace.getRoot().getLocation();
				// IProjectNatureDescriptor[] nds = workspace.getNatureDescriptors();
				boolean isJava = false;
				
				try {
						isJava = rp.hasNature("org.eclipse.jdt.core.javanature");
						
				} catch (CoreException e) {
						// e.printStackTrace();
						// System.out.println("The " + nd.getNatureId() + " is not supported, or so it seems.");
				}

				if (isJava) {
					IJavaProject jp = JavaCore.create(rp);
					try {
						bp = wroot.append(jp.getOutputLocation()).toPortableString();
						IClasspathEntry[] cpes = jp.getResolvedClasspath(true);
						fp = "";
						sp = ".";
						for (IClasspathEntry cpe: cpes) {
							if (cpe.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
								if (sp.length() > 0) sp += System.getProperty("path.separator");
								sp += cpe.getPath().makeRelativeTo(jp.getPath()).toString();
							}
							else {
								if (fp.length() > 0) fp += System.getProperty("path.separator");
								fp += cpe.getPath().toString();
							}
						}
					} catch (JavaModelException e) {
					} catch (NullPointerException np) {
					}
				}
			}
			if (fp.equals("")) fp=".";
			if (sp.equals("")) sp=".";
		}
		/**
		 * The source path always includes the project directory, as otherwise source resolution in
		 * linked directories will work only if one links below a source directory, which may not be
		 * possible always.
		 * @return the sp
		 */
		public String getSp() {
			return sp;
		}
		/**
		 * @return the fp
		 */
		public String getFp() {
			return fp;
		}
		/**
		 * @return the bp
		 */
		public String getBp() {
			return bp;
		}
		/**
		 * get all frege source files in the work space
		 */
		public List<String> getAllSources(final String hint) {
			final FregeBuilder builder = new FregeBuilder();
			final Set<String> result = new HashSet<String>();
			try {
				project.getRawProject().getWorkspace().getRoot().accept(builder.fResourceVisitor);
			} catch (CoreException e) {
				// problems getting the file names
				return new ArrayList<String>(result);
			}
			LineNumberReader rdr = null;
			for (IFile file : builder.fChangedSources) try {
				rdr = null;
				rdr = new LineNumberReader(new InputStreamReader(file.getContents(true)));
				String line;
				java.util.regex.Pattern pat = Pattern.compile("\\b(package|module)\\s+(\\S+)");
				while ((line = rdr.readLine()) != null) {
					Matcher m = pat.matcher(line);
					if (m.find()) {
						String p = m.group(2);
						if (p.startsWith(hint))
								result.add(p);
					}
				}
				rdr.close(); rdr = null;
			} catch (Exception e) {
				if (rdr != null)
					try {
						rdr.close();
					} catch (IOException e1) {
						rdr = null;
					}
				rdr = null;
			}
			ArrayList<String> sorted = new ArrayList<String>(result); 
			Collections.sort(sorted);
			return sorted;
		}
		/**
		 * look for a path that contains the source code for pack
		 */
		public IPath getSource(final String pack) {
			final String fr = pack.replaceAll("\\.", "/") + ".fr";
			// final String[] srcs = getSp().split(System.getProperty("path.separator"));
			final FregeBuilder builder = new FregeBuilder();
			try {
				project.getRawProject().getWorkspace().getRoot().accept(builder.fResourceVisitor);
			} catch (CoreException e) {
				// problems getting the file names
				return null;
			}
			for (IFile file : builder.fChangedSources) {
				IPath it = file.getFullPath();
				if (it.toString().endsWith(fr)) return it;
			}
			/*
			for (String sf: srcs) {
				final IPath p = new Path(sf + "/" + fr);
				
				final IResource toRes = project.getRawProject().findMember(p);  
				
				if (toRes != null && toRes instanceof IFile) {
					final IFile to = (IFile) toRes;
					return to.getFullPath();
				}
			}
			*/
			return null;
		}
	}
	
	public FregeParseController() {
		super(FregePlugin.getInstance().getLanguageID());
	}

	private int timeout;
	private TGlobal global, goodglobal;
	private int  hash = 0;
	private int  leng = 0;
	private final ISourcePositionLocator   fSourcePositionLocator   
					= new FregeSourcePositionLocator(this);
    private final SimpleAnnotationTypeInfo fSimpleAnnotationTypeInfo
    				= new SimpleAnnotationTypeInfo();
	private IMessageHandler msgHandler = null;
	private FregeData fregeData = null;
	
	/**
	 * @author ingo
	 * @return the frege data structure
	 */
	public FregeData getFD() { return fregeData; }
	/**
	 * tell if we have errors
	 */
	public static int errors(TGlobal global) { return global == null ? 1 : TGlobal.errors(global); }
	
	/**
	 * tell how far we are advanced
	 */
	public static int achievement(TGlobal global) {
		if (global == null) return 0;
		final TSubSt sub = TGlobal.sub(global);
		return 2 * TSubSt.nextPass(sub) - (errors(global) > 0 ? 1 : 0);
	}
	
	/**
	 * run a {@link frege.prelude.PreludeBase.TState} action and return the new TGlobal state
	 * @return the new state
	 */
	public static TGlobal runStG(Lazy<FV> action, TGlobal g) {
		Lambda stg = (Lambda) action._e();				// State (g -> (a, g)) 
		TTuple2 r = (TTuple2)TState.run(stg, g)._e();
		return (TGlobal) r.mem2._e();
	}
	
	/**
	 * Run a {@link frege.prelude.PreludeBase.TState} action and return the result.
	 * The state must not be changed by the action. 
	 * @return the result
	 */
	public static FV funStG(Lazy<FV> action, TGlobal g) {
		Lambda stg = (Lambda) action._e();				// State (g -> (a, g)) 
		TTuple2 r = (TTuple2)TState.run(stg, g)._e();
		return r.mem1._e();
	}

	/**
	 * @param filePath		Project-relative path of file
	 * @param project		Project that contains the file
	 * @param handler		A message handler to receive error messages (or any others)
	 * 						from the parser
	 */
	public void initialize(IPath filePath, ISourceProject project,
			IMessageHandler handler) {
		super.initialize(filePath, project, handler);
		IPath fullFilePath = project != null ?
				project.getRawProject().getLocation().append(filePath)
				: filePath;

		global =  (TGlobal) 
				frege.prelude.PreludeBase.TST.performUnsafe(
						(Lambda) frege.compiler.Main.eclipseOptions._e())._e();
		fregeData = new FregeData(project);
		createLexerAndParser(fullFilePath, project);

		msgHandler = handler;
	}

	public IParser getParser() {
		new Exception("getParser: called").printStackTrace(System.out);
		return null; // parser;
	}

	
	public ISourcePositionLocator getNodeLocator() {
		return fSourcePositionLocator;
	}
	

	
	private void createLexerAndParser(IPath filePath, ISourceProject project) {
		System.err.println("createLexerAndParser: " + filePath.toPortableString());
		System.err.println("classpath: " + System.getProperty("java.class.path"));
		global = TGlobal.upd$options(global, TOptions.upd$source(
				TGlobal.options(global), 
				filePath.toPortableString()));

		final FregeData data = fregeData;
		final String fp = data.getFp();   
		final String bp = data.getBp(); 
		final String sp = data.getSp();
		
				
		System.err.println("FregePath: " + fp);
		global = TGlobal.upd$options(global, TOptions.upd$path(
				TGlobal.options(global),
				frege.prelude.Arrays.IStringSplitter_Regex.splitted(
						((frege.rt.Box<Pattern>)frege.compiler.Utilities.pathRE._e()).j, 
						fp)));
		System.err.println("SourcePath: " + sp);
		global = TGlobal.upd$options(global, TOptions.upd$sourcePath(
				TGlobal.options(global),
				frege.prelude.Arrays.IStringSplitter_Regex.splitted(
						((frege.rt.Box<Pattern>)frege.compiler.Utilities.pathRE._e()).j, 
						sp)));
		System.err.println("Destination: " + bp);
		global = TGlobal.upd$options(global, TOptions.upd$dir(
				TGlobal.options(global), 
				bp));
		global = runStG(frege.compiler.Main.newLoader, global);
		
		IPreferencesService service = FregePlugin.getInstance().getPreferencesService();
		if (service != null) {
			timeout = service.getIntPreference(FregePreferencesConstants.P_PARSETIMEOUT);
			if (service.getBooleanPreference(FregePreferencesConstants.P_INLINE)) {
				global = TGlobal.upd$options(global, TOptions.upd$flags(
							TGlobal.options(global),
							Utilities.setFlag(
									new IEnum_Flag(),
									TOptions.flags(TGlobal.options(global)).j, 
								    TFlag.INLINE).j
						));
			}
			final String prefix = service.getStringPreference(FregePreferencesConstants.P_PREFIX); 
			if (prefix != null && prefix.length() > 0) {
				global = TGlobal.upd$options(global, TOptions.upd$prefix(
							TGlobal.options(global), prefix));
			}
		}
		else timeout = 250;
		goodglobal = global;
	}

	/**
	 * The msgHandler must be in place
	 */
	public TGlobal parse(String contents, boolean scanOnly,
			IProgressMonitor monitor) {
		
		long t0 = System.nanoTime();
		long te = 0;
		long t1 = 0;
		TList passes = null;
		DCons pass = null;
		int index;
		
		synchronized (this) {
			
			if (monitor.isCanceled()) return global;
		
			if (contents.length() == leng && contents.hashCode() == hash)
				return global;			// nothing really updated here
		
		
			msgHandler.clearMessages();
		
			final IProgressMonitor myMonitor = monitor;
			Lambda cancel = new frege.rt.Lam1() {			
				public Lazy<FV> eval(Lazy<FV> realworld) {
					return (myMonitor.isCanceled()) ? Box.Bool.t : Box.Bool.f;	
				}
			};
		
			global = TGlobal.upd$sub(global,  TSubSt.upd$cancelled(
				TGlobal.sub(global), 
				cancel));
			global = TGlobal.upd$sub(global, TSubSt.upd$errors(TGlobal.sub(global), 0));
		
			passes = (TList) frege.compiler.Main.passes._e();
			
			monitor.beginTask(this.getClass().getName() + " parsing", 
					1 + IListLike__lbrack_rbrack.length(passes));

			index = 0;

			while (!monitor.isCanceled()
					&& (pass = passes._Cons()) != null
					&& errors(global) == 0
					&& index < 2) {		// do lexer and parser synchronized
				t1 = System.nanoTime();
				index++;
				passes = (TList) pass.mem2._e();
				final TTuple3 adx = (TTuple3) pass.mem1._e();
				final Lazy<FV> action = index == 1 ? Main.lexPassIDE(contents) : adx.mem1;
				final String   desc   = Box.<String>box(adx.mem2._e()).j;
				final TGlobal g = runStG(action, global);
				te = System.nanoTime();
				System.err.println(desc + " took " 
					+ (te-t1)/1000000 + "ms, cumulative "
					+ (te-t0)/1000000 + "ms");
				
				monitor.worked(1);
				global = runStG(EclipseUtil.passDone._e(), g);
			}
			if (achievement(global) >= achievement(goodglobal))
				goodglobal = global;			// when opening a file with errors
			else {
				// update token array in goodglobal
				Array toks = TSubSt.toks(TGlobal.sub(global));
				goodglobal = TGlobal.upd$sub(goodglobal, TSubSt.upd$toks(
						TGlobal.sub(goodglobal), toks));
			}
//			Array gtoks = TSubSt.toks(TGlobal.sub(global));
//			System.err.println("global.toks==good.toks is " + (toks == gtoks));
		}
		
		// adjust timeout
		int needed = (int) (te-t0) / 1000000;
		if (needed > 0) {
			System.err.print("needed=" + needed + "ms, timeout=" + timeout + "ms, ");
			if (needed > timeout) 
				timeout = 5 * timeout / 4;
			else 
				timeout = 4 * timeout / 5;
			System.err.println("new timeout=" + timeout + "ms");
		}
		
		if (scanOnly && timeout - needed > 0 && errors(global) == 0)
			try { Thread.sleep(timeout - needed); } catch (InterruptedException e) {}

		
		while (!monitor.isCanceled()
					&& errors(global) == 0
					&& (pass = passes._Cons()) != null) {			// do the rest unsynchronized
				t1 = System.nanoTime();
				passes = (TList) pass.mem2._e();
				index++;
				final TTuple3 adx = (TTuple3) pass.mem1._e();
				final Lazy<FV> action = adx.mem1;
				final String   desc   = Box.<String>box(adx.mem2._e()).j;
				final TGlobal g = runStG(action, global);
				te = System.nanoTime();
				System.err.println(desc + " took " 
					+ (te-t1)/1000000 + "ms, cumulative "
					+ (te-t0)/1000000 + "ms");
				
				monitor.worked(1);
				global = runStG(EclipseUtil.passDone._e(), g);
				
				if (achievement(global) >= achievement(goodglobal))
					goodglobal = global;
				else if (index >= 6) {
					// give token resolve table to goodglobal
					goodglobal = TGlobal.upd$sub(goodglobal, TSubSt.upd$idKind(
							TGlobal.sub(goodglobal), TSubSt.idKind(TGlobal.sub(global))));
					// give locals to goodglobals
					goodglobal = TGlobal.upd$locals(goodglobal, TGlobal.locals(global));
				}
				if (scanOnly && desc.startsWith("type check")) {
					goodglobal = global;
					break;
				}
		}
		
		leng = contents.length();
		hash = contents.hashCode();
		return global;
	}

	@Override
	synchronized public TGlobal getCurrentAst() {
		// System.err.println("delivered goodglobal");
		return global;
	}
	
	synchronized public TGlobal getGoodAst() {
		// System.err.println("delivered goodglobal");
		return goodglobal;
	}
	
	@Override
	public TGlobal parse(String input, IProgressMonitor monitor) {
		MarkerCreatorWithBatching mcwb = msgHandler instanceof MarkerCreatorWithBatching ?
				(MarkerCreatorWithBatching) msgHandler : null;
		// when we build, we'll get a MarkerCreatorWithBatching
		// Hence, if we do not have one, we just scan&parse, otherwise we do a full compile
		TGlobal g = parse(input, mcwb == null, monitor);
		TList msgs = TSubSt.messages(TGlobal.sub(g));
		 
		while (true) {
			TList.DCons node = msgs._Cons();
			if (node == null) break;
			msgs = (TList) node.mem2._e();
			TMessage msg = (TMessage) node.mem1._e();
			if (mcwb != null) {
				// do also warnings and hints
				int sev = IMarker.SEVERITY_ERROR;
				if (TMessage.level(msg).j == TSeverity.HINT.j) sev = IMarker.SEVERITY_INFO;
				else if (TMessage.level(msg).j == TSeverity.WARNING.j)
					sev = IMarker.SEVERITY_WARNING;
				try {
					mcwb.addMarker(sev, 
							TMessage.text(msg)
								.replaceAll("\n", "   "), 
							TToken.line( TPosition.first(TMessage.pos(msg)) ), 
							TPosition.start(TMessage.pos(msg)), 
							TPosition.end(TMessage.pos(msg)));
				} catch (LimitExceededException e) {
					break;
				}
				continue;
			}
			// normal message handling
			if (TMessage.level(msg).j != TSeverity.ERROR.j) continue;
			msgHandler.handleSimpleMessage(TMessage.text(msg), 
					TPosition.start(TMessage.pos(msg)), 
					TPosition.end(TMessage.pos(msg))-1, 
					0, 0, 0, 0);
		}
		if (mcwb == null) monitor.done();
		return g;
	}
	
	@Override
	public Iterator<TToken> getTokenIterator(IRegion region) {
		System.err.println("getTokenIterator(): ");
		return new TokensIterator(TSubSt.toks(TGlobal.sub(global)), region);
		
	}

	@Override
	public ISourcePositionLocator getSourcePositionLocator() {
		return fSourcePositionLocator;
	}

	@Override
	public IAnnotationTypeInfo getAnnotationTypeInfo() {
		return fSimpleAnnotationTypeInfo;
	}
	
	private ILanguageSyntaxProperties lsp;
	/**
     * @return an implementation of {@link ILanguageSyntaxProperties} that
     * describes certain syntactic features of this language
     */
	@Override 
	public ILanguageSyntaxProperties getSyntaxProperties() {
		if (lsp == null) {
			lsp = new ILanguageSyntaxProperties() {
				
				@Override
				public String getSingleLineCommentPrefix() {
					return "--";
				}
				
				@Override
				public String getIdentifierConstituentChars() {
					return null;
				}
				
				@Override
				public int[] getIdentifierComponents(String ident) {
					return null;
				}
				
				@Override
				public String[][] getFences() {
					return new String[][] { {"(", ")"}, {"{", "}"}, {"[", "]"}};
				}
				
				@Override
				public String getBlockCommentStart() {
					return "{-";
				}
				
				@Override
				public String getBlockCommentEnd() {
					return "-}";
				}
				
				@Override
				public String getBlockCommentContinuation() {
					return null;
				}
			};
		}
		return lsp;
	}
	public synchronized final int getHash() {
		return hash;
	}
	public synchronized final int getLeng() {
		return leng;
	}
}
