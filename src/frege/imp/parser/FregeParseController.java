package frege.imp.parser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import io.usethesource.impulse.builder.MarkerCreatorWithBatching;
import io.usethesource.impulse.builder.ProblemLimit.LimitExceededException;
import io.usethesource.impulse.model.ISourceProject;
import io.usethesource.impulse.parser.IMessageHandler;
import io.usethesource.impulse.parser.IParseController;
// import io.usethesource.impulse.parser.IParser;
import io.usethesource.impulse.parser.ISourcePositionLocator;
import io.usethesource.impulse.parser.ParseControllerBase;
import io.usethesource.impulse.parser.SimpleAnnotationTypeInfo;
import io.usethesource.impulse.preferences.IPreferencesService;
import io.usethesource.impulse.services.IAnnotationTypeInfo;
import io.usethesource.impulse.services.ILanguageSyntaxProperties;

import org.eclipse.jface.text.IRegion;
import org.eclipse.swt.graphics.Region;

import frege.FregePlugin;
import frege.runtime.Delayed;
import frege.runtime.Fun1;
import frege.runtime.Lambda;
import frege.runtime.Lazy;
import frege.prelude.PreludeBase;
import frege.prelude.PreludeBase.TList.DCons;
import frege.prelude.PreludeBase.TTuple2;
import frege.prelude.PreludeBase.TList;
import frege.control.monad.State.TState;
import frege.control.monad.State.TStateT;
import frege.prelude.PreludeList;
import frege.prelude.PreludeList.IListView__lbrack_rbrack;
import frege.compiler.enums.Flags.TFlag;
import frege.compiler.enums.Flags.IEnum_Flag;
import frege.compiler.types.Global.TGlobal;
import frege.compiler.types.Global.TMessage;
import frege.compiler.types.Global.TOptions;
import frege.compiler.types.Positions.TPosition;
import frege.compiler.types.Global.TSeverity;
import frege.compiler.types.Global.TSubSt;
import frege.compiler.types.Tokens.TToken;
import frege.compiler.common.CompilerOptions;
import frege.ide.Utilities;
import frege.imp.builders.FregeBuilder;
import frege.imp.preferences.FregePreferencesConstants;
import frege.data.Bits.TBitSet;
import frege.data.TreeMap.TTreeMap;


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

	public static class TokensIterator implements Iterator<Object> {
		/** current token array */
		final private TToken[] toks;
		private IRegion region;
		private int  inx;
		
		/** check if token is within region */
		public static boolean within(TToken tok, IRegion region) {
			return (TToken.offset(tok) + TToken.value(tok).length() >= region.getOffset()
					&& TToken.offset(tok) <= region.getOffset() + region.getLength());
		}
		
		/** construct an Iterator */
		public TokensIterator(TToken[] it, IRegion reg) { 
			toks = it;
			region = reg;
			inx = 0;
			while (inx < toks.length) {
				TToken t = toks[inx]; 
				if (within(t, reg)) break;
				inx++;
			}
		}
		
		/*
		public static int skipBraces(final Array toks, int j) {
			while (j < toks.length()) {
				TToken tok = Delayed.<TToken>forced(toks.getAt(j));
				if (tok.mem$tokid == TTokenID.CHAR
						&& (tok.mem$value.charAt(0) == '{'
								|| tok.mem$value.charAt(0) == '}'
								|| tok.mem$value.charAt(0) == ';')) {
					j++;
				}
				else if (tok.mem$tokid == TTokenID.PURE
						&& !tok.mem$value.equals("pure")) {
					j++;
				}
				else break;
			}
			return j;
		}
		*/
		
		@Override
		public boolean hasNext() {
			// skip { ; }
			// inx = skipBraces(toks, inx);
			// we have a next if we are not the empty list and the token is in the region
			return inx < toks.length
					&& within(toks[inx], region);
		}
		
		@Override
		public TToken next() {
			// give back next token
			if (inx < toks.length) {
				return toks[inx++];
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
		private String prefix = "";
		private IPath  projectPath = null;
		private IJavaProject javaProject = null;
		private ISourceProject project = null;
		public FregeData(ISourceProject sourceProject) {
			project = sourceProject;
			final String projName = sourceProject.getName();
			
			// find out if the preferences specify "project:pfx"
			// and take pfx as prefix, if so
			IPreferencesService service = FregePlugin.getInstance().getPreferencesService();
			if (service != null) {
				String option = service.getStringPreference(FregePreferencesConstants.P_PREFIX);
				if (option != null 
						&& projName != null
						&& option.startsWith(projName)
						&& option.length() >= 2+projName.length()
						&& option.charAt(projName.length())==':') {
					prefix = option.substring(1+projName.length());
					System.err.println("sourceProject=" + project.getName()
							+ ", prefix=" + prefix);
				}
			}
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
					javaProject = jp; 
					projectPath = jp.getPath();
					try {
						IResource bpres = workspace.getRoot().findMember(jp.getOutputLocation());
						bp = bpres != null 
									? bpres.getLocation().toString() 
									: wroot.append(jp.getOutputLocation()).toPortableString();
						IClasspathEntry[] cpes = jp.getResolvedClasspath(true);
						fp = bp;
						sp = ".";
						for (IClasspathEntry cpe: cpes) {
							if (cpe.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
								if (sp.length() > 0) sp += System.getProperty("path.separator");
								sp += cpe.getPath().makeRelativeTo(jp.getPath()).toString();
							}
							else {
								if (fp.length() > 0) fp += System.getProperty("path.separator");
								
								IResource res = workspace.getRoot().findMember(cpe.getPath());
								String lib = res != null ? res.getLocation().toString() : "no res";
								String lib1 =  cpe.getPath().toString();
								String lib2 =  cpe.getPath().makeRelativeTo(jp.getPath()).toString();
								String lib3 =  cpe.getPath().makeRelativeTo(wroot).makeRelative().toString();
								String lib4 =  cpe.getPath().makeRelativeTo(wroot).toString();
								String lib5 =  cpe.getPath().makeRelativeTo(wroot).makeAbsolute().toString();
								if (new java.io.File(lib).exists()) fp += lib;
								else if (new java.io.File(lib1).exists()) fp += lib1;
								else if (new java.io.File(lib2).exists()) fp += lib2;
								else if (new java.io.File(lib3).exists()) fp += lib3;
								else if (new java.io.File(lib4).exists()) fp += lib4;
								else if (new java.io.File(lib5).exists()) fp += lib5;
								else {
									System.err.println("WHOA!!! Neither of the following do exist: "
											+ lib + ", " + lib1 + ", " + lib2 + ", " + lib3
											+ ", " + lib4 + ", " + lib5);
									System.err.println("JavaProject.getPath: " + jp.getPath());
								}
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
		public IPath getProjectPath() { return projectPath; }
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
		 * @return the java project
		 */
		public IJavaProject getJp() { return javaProject; }
		
		/**
		 * @return the prefix for this project
		 */
		public String getPrefix() { return prefix; }
		
		/**
		 * get all frege source files in the work space
		 */
		public List<String> getAllSources(final String hint) {
			final FregeBuilder builder = new FregeBuilder();
			final Set<String> result = new HashSet<String>();
			try {
				project.getRawProject()
					.getWorkspace().getRoot()
					.accept(builder.fResourceVisitor);
			} catch (CoreException e) {
				// problems getting the file names
				return new ArrayList<String>(result);
			}
			LineNumberReader rdr = null;
			for (IFile file : builder.fChangedSources) try {
				rdr = null;
				rdr = new LineNumberReader(new InputStreamReader(file.getContents(true)));
				String line;
				java.util.regex.Pattern pat = Pattern.compile("\\b(package|module)\\s+((\\w|\\.)+)");
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
				project.getRawProject()
					// .getWorkspace().getRoot()
					.accept(builder.fResourceVisitor);
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
	private boolean tokensIteratorDone = false;
	private final ISourcePositionLocator   fSourcePositionLocator   
					= new FregeSourcePositionLocator(this);
    private final SimpleAnnotationTypeInfo fSimpleAnnotationTypeInfo
    				= new SimpleAnnotationTypeInfo();
	public IMessageHandler msgHandler = null;
	private FregeData fregeData = null;
	private static Map<String, Lazy> packs = new HashMap<>();
	
	void initPacks() {
		boolean make = false;
		synchronized (packs) {
			make = packs.get(fregeData.getBp()) == null;
			if (make) {
				// Object value = PreludeBase.TST.performUnsafe(EclipseUtil.getpacks());
				packs.put(fregeData.getBp(), frege.lib.Modules.noPacks);
			}
		}
		if (make) {
			Job job = new Job("Getting Modules") {
				public IStatus run(IProgressMonitor moni) {
					moni.worked(10);
					Lazy value = Delayed.<Lazy>forced(
							PreludeBase.TST.performUnsafe(
									Utilities.initRoot(
											fregeData.getFp()
											) // .<Lambda>forced()
											));
					moni.worked(75);
					synchronized (packs) {
						packs.put(fregeData.getBp(),value);
					}
					System.err.println("Job done, value=" + value);
					moni.done();
					return Status.OK_STATUS;
				}
			};
			job.schedule();
		}
	}
	
	public Lazy ourRoot() {
		synchronized (packs) {
			return packs.get(fregeData.getBp());
		}
	}
	
	public static Lazy ourRoot(FregeParseController parser) {
		return parser.ourRoot();
	}
	
	public void justCompiled() {
		synchronized (packs) {
			Lazy x = ourRoot();
			Lazy y = Delayed.<Lazy>forced(
				PreludeBase.TST.performUnsafe(Utilities.justCompiled(global, x))
			);
			packs.put(fregeData.getBp(), y);
		}
	}
	
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
	public static TGlobal runSTG(Lazy action, TGlobal g) {
		Lambda stg = Delayed.<Lambda>forced(action);				// State (g -> (a, g)) 
		TTuple2 r = TState.run(stg, g).<TTuple2>forced();
		return Delayed.<TGlobal>forced( r.mem2 );
	}
	
	/**
	 * Run a {@link frege.prelude.PreludeBase.TState} action and return the result.
	 * The state must not be changed by the action. 
	 * @return the result
	 */
	public static Object funSTG(Lazy action, TGlobal g) {
		Lambda stg = action.<Lambda>forced();				// State (g -> (a, g)) 
		TTuple2 r = TState.run(stg, g).<TTuple2>forced();
		return r.mem1;
	}
	
	/**
	 * run a {@link frege.prelude.PreludeBase.TStateT TGlobal IO} action and return the new TGlobal state
	 * @return the new state
	 */
	public static TGlobal runSTIO(Lazy action, TGlobal g) {
		Lambda stg = Delayed.<Lambda>forced(action);				// StateT (g -> IO (a, g)) 
		Lambda r   = Delayed.<Lambda>forced( TStateT.run(stg, g));
		TTuple2 t  = r.apply(42).result().<TTuple2>forced();
		return Delayed.<TGlobal>forced(t.mem2);
	}
	
	/**
	 * Run a {@link frege.prelude.PreludeBase.TState} action and return the result.
	 * The state must not be changed by the action. 
	 * @return the result
	 */
	public static Object funSTIO(Lazy action, TGlobal g) {
		Lambda stg = Delayed.<Lambda>forced(action);				// StateT (g -> IO (a, g)) 
		Lambda r   = Delayed.<Lambda>forced( TStateT.run(stg, g));
		TTuple2 t  = r.apply(42).result().<TTuple2>forced();
		return t.mem1;
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

		global = Delayed.<TGlobal> forced(frege.prelude.PreludeBase.TST
				.performUnsafe(CompilerOptions.eclipseOptions
						.<Lambda> forced()));
		fregeData = new FregeData(project);
		initPacks();
		createLexerAndParser(fullFilePath, project);

		msgHandler = handler;
	}

	public FregeParseController getParser() {
		new Exception("getParser: called").printStackTrace(System.out);
		return null; // parser;
	}

	
	public ISourcePositionLocator getNodeLocator() {
		return fSourcePositionLocator;
	}
	

	
	private void createLexerAndParser(IPath filePath, ISourceProject project) {
		System.err.println("createLexerAndParser: " + filePath.toPortableString());
		System.err.println("classpath: " + System.getProperty("java.class.path"));
		

		final FregeData data = fregeData;
		final String fp = data.getFp();   
		final String bp = data.getBp(); 
		final String sp = data.getSp();
		final IPath  pp = data.getProjectPath();
		final IPath  wk = ResourcesPlugin.getWorkspace().getRoot().getLocation();
		final String src = pp != null ? filePath
				.makeRelativeTo(wk)
				.makeAbsolute()
				.makeRelativeTo(pp).toString() : filePath.toString();

		// set source file into global
		global = TGlobal.upd$options(global, TOptions.upd$source(
				TGlobal.options(global), 
				src));

		System.err.println("project Path: " + pp);
		System.err.println("source File: " + src);
				
		System.err.println("FregePath: " + fp);
		global = TGlobal.upd$options(global, TOptions.upd$path(
				TGlobal.options(global),
				frege.java.util.Regex.TRegex.splitted(
						Delayed.<Pattern>forced(CompilerOptions.pathRE), 
						fp)));
		System.err.println("SourcePath: " + sp);
		global = TGlobal.upd$options(global, TOptions.upd$sourcePath(
				TGlobal.options(global),
				frege.java.util.Regex.TRegex.splitted(
						Delayed.<Pattern>forced(CompilerOptions.pathRE), 
						sp)));
		System.err.println("Destination: " + bp);
		global = TGlobal.upd$options(global, TOptions.upd$dir(
				TGlobal.options(global), 
				bp));
		global = runSTIO(Utilities.newLoader, global);
			
		IPreferencesService service = FregePlugin.getInstance().getPreferencesService();
		if (service != null) {
			timeout = service.getIntPreference(FregePreferencesConstants.P_PARSETIMEOUT);
			if (service.getBooleanPreference(FregePreferencesConstants.P_INLINE)) {
				global = TGlobal.upd$options(global, TOptions.upd$flags(
							TGlobal.options(global),
							Delayed.<Long> forced(
								TBitSet.unionE(new IEnum_Flag(),
									TOptions.flags(TGlobal.options(global)),
									TFlag.INLINE))
						));
			} else {
				global = TGlobal.upd$options(global, TOptions.upd$flags(
						TGlobal.options(global),
						Delayed.<Long> forced(
							TBitSet.differenceE(new IEnum_Flag(),
								TOptions.flags(TGlobal.options(global)),
								TFlag.INLINE))
					));
			}
			if (service.getBooleanPreference(FregePreferencesConstants.P_COMMENTS)) {
				global = TGlobal.upd$options(global, TOptions.upd$flags(
							TGlobal.options(global),
							Delayed.<Long> forced(
								TBitSet.unionE(new IEnum_Flag(),
									TOptions.flags(TGlobal.options(global)),
									TFlag.COMMENTS))
						));
			} else {
				global = TGlobal.upd$options(global, TOptions.upd$flags(
						TGlobal.options(global),
						Delayed.<Long> forced(
							TBitSet.differenceE(new IEnum_Flag(),
								TOptions.flags(TGlobal.options(global)),
								TFlag.COMMENTS))
					));
			}
			if (service.getBooleanPreference(FregePreferencesConstants.P_USEUNICODE)) {
				global = TGlobal.upd$options(global, TOptions.upd$flags(
							TGlobal.options(global),
							Delayed.<Long> forced(
								TBitSet.unionE(new IEnum_Flag(),
									TOptions.flags(TGlobal.options(global)),
									TFlag.USEUNICODE))
						));
			} else {
				global = TGlobal.upd$options(global, TOptions.upd$flags(
						TGlobal.options(global),
						Delayed.<Long> forced(
							TBitSet.differenceE(new IEnum_Flag(),
								TOptions.flags(TGlobal.options(global)),
								TFlag.USEUNICODE))
					));
			}
			if (service.getBooleanPreference(FregePreferencesConstants.P_USEGREEK)) {
				global = TGlobal.upd$options(global, TOptions.upd$flags(
							TGlobal.options(global),
							Delayed.<Long> forced(
								TBitSet.unionE(new IEnum_Flag(),
									TOptions.flags(TGlobal.options(global)),
									TFlag.USEGREEK))
						));
			} else {
				global = TGlobal.upd$options(global, TOptions.upd$flags(
						TGlobal.options(global),
						Delayed.<Long> forced(
							TBitSet.differenceE(new IEnum_Flag(),
								TOptions.flags(TGlobal.options(global)),
								TFlag.USEGREEK))
					));
			}
			if (service.getBooleanPreference(FregePreferencesConstants.P_USEFRAKTUR)) {
				global = TGlobal.upd$options(global, TOptions.upd$flags(
							TGlobal.options(global),
							Delayed.<Long> forced(
								TBitSet.unionE(new IEnum_Flag(),
									TOptions.flags(TGlobal.options(global)),
									TFlag.USEFRAKTUR))
						));
			} else {
				global = TGlobal.upd$options(global, TOptions.upd$flags(
						TGlobal.options(global),
						Delayed.<Long> forced(
							TBitSet.differenceE(new IEnum_Flag(),
								TOptions.flags(TGlobal.options(global)),
								TFlag.USEFRAKTUR))
					));
			}
			final String prefix = fregeData.getPrefix(); 
			if (prefix != null && prefix.length() > 0) {
				global = TGlobal.upd$options(global, TOptions.upd$prefix(
							TGlobal.options(global), prefix));
			}
		}
		else timeout = 250;
		goodglobal = global;
	}

	public void resetHash() {
		leng = 0;
		hash = 0;
		tokensIteratorDone = false;
		global = runSTIO(Utilities.refreshPackages, global);
		System.err.println("packages cleared");
	}
	
	/**
	 * The msgHandler must be in place
	 */
	synchronized public TGlobal parse(String contents, boolean scanOnly,
			IProgressMonitor monitor) {
		
		long t0 = System.nanoTime();
		long te = 0;
		long t1 = 0;
		TList passes = null;
		DCons pass = null;
		int index;
		
		{
			
			if (monitor.isCanceled()) return global;
		
			if (contents.length() == leng && contents.hashCode() == hash) {
				return global;			// nothing really updated here
			}
		
		
			msgHandler.clearMessages();
		
			final IProgressMonitor myMonitor = monitor;
			Lambda cancel = new Fun1<Boolean>() {			
				public Boolean eval(Object realworld) {
					return myMonitor.isCanceled();	
				}
			};
		
			global = TGlobal.upd$sub(global,  TSubSt.upd$cancelled(
				TGlobal.sub(global), 
				cancel));
			global = TGlobal.upd$sub(global, TSubSt.upd$numErrors(TGlobal.sub(global), 0));
			global = TGlobal.upd$sub(global, TSubSt.upd$resErrors(TGlobal.sub(global), 0));
		
			passes = frege.compiler.Main.passes.<TList>forced();
			
			monitor.beginTask(this.getClass().getName() + " parsing", 
					1 + IListView__lbrack_rbrack.length(passes));

			index = 0;

			while (!monitor.isCanceled()
					&& (pass = passes._Cons()) != null
					&& errors(global) == 0
					&& index < 2) {		// do lexer and parser synchronized
				t1 = System.nanoTime();
				index++;
				passes = pass.mem2.<TList>forced();
				final TTuple2 adx = Delayed.<TTuple2>forced( pass.mem1 );
				final Lazy action = index == 1 ? Utilities.lexPassIDE(contents) : Delayed.delayed(adx.mem1);
				final String   desc   = Delayed.<String>forced(adx.mem2);
				final TGlobal g = runSTIO(action, global);
				te = System.nanoTime();
				System.err.println(desc + " took " 
					+ (te-t1)/1000000 + "ms, cumulative "
					+ (te-t0)/1000000 + "ms");
				
				monitor.worked(1);
				global = runSTG(Utilities.passDone, g);
			}
			if (achievement(global) >= achievement(goodglobal))
				goodglobal = global;			// when opening a file with errors
			else {
				// update token array in goodglobal
				TToken[] toks = TSubSt.toks(TGlobal.sub(global));
				goodglobal = TGlobal.upd$sub(goodglobal, TSubSt.upd$toks(
						TGlobal.sub(goodglobal), toks));
			}
//			Array gtoks = TSubSt.toks(TGlobal.sub(global));
//			System.err.println("global.toks==good.toks is " + (toks == gtoks));
		}
		
		int needed = (int) ((te-t0) / 1000000);
				
		if (scanOnly && timeout - needed > 0 && errors(global) == 0 && !monitor.isCanceled())
			try { Thread.sleep(timeout - needed); } catch (InterruptedException e) {}
		t0 = System.nanoTime() - (te-t0);

		
		while (!monitor.isCanceled()
					&& errors(global) == 0
					&& (pass = passes._Cons()) != null) {			// do the rest unsynchronized
				t1 = System.nanoTime();
				passes = pass.mem2.<TList>forced();
				index++;
				final TTuple2 adx = Delayed.<TTuple2>forced( pass.mem1 );
				final Lazy action = Delayed.delayed(adx.mem1);
				final String   desc   = Delayed.<String>forced(adx.mem2);
				final TGlobal g = runSTIO(action, global);
				te = System.nanoTime();
				System.err.println(desc + " took " 
					+ (te-t1)/1000000 + "ms, cumulative "
					+ (te-t0)/1000000 + "ms");
				
				monitor.worked(1);
				global = runSTG(Utilities.passDone, g);
				
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
	public TGlobal getCurrentAst() {
		// System.err.println("delivered goodglobal");
		return global;
	}
	
	synchronized public TGlobal getGoodAst() {
		// System.err.println("delivered goodglobal");
		return goodglobal;
	}
	
	private static String ourJar = null; 
	
	@Override
	public TGlobal parse(String input, IProgressMonitor monitor) {
		MarkerCreatorWithBatching mcwb = msgHandler instanceof MarkerCreatorWithBatching ?
				(MarkerCreatorWithBatching) msgHandler : null;
		// when we build, we'll get a MarkerCreatorWithBatching
		// Hence, if we do not have one, we just scan&parse, otherwise we do a full compile
		TGlobal g = parse(input, mcwb == null, monitor);
		int u = TGlobal.unique(g);
		System.err.printf("frege parse: done, unique=%d, adding errors ", u);
		tokensIteratorDone = false;
		TList msgs = PreludeList.reverse(TSubSt.messages(TGlobal.sub(g)));
		int maxmsgs = 9;
		
		// emit an error here if we don't have the correct fregec.jar
		if (ourJar == null) {
			final String[] fp = fregeData.getFp().split(System.getProperty("path.separator"));
			// final String correct = frege.FregePlugin.fregeLib;
		
			for (int i=0; i < fp.length;i++) {
				if (fp[i].endsWith("fregec.jar")) { 
					ourJar = fp[i];
					ourJar = java.util.regex.Pattern.compile("\\\\").matcher(ourJar).replaceAll("/");
					break; 
				}
			}
		}
		
		if (ourJar == null) {
			if (mcwb != null) try {
				mcwb.addMarker(IMarker.SEVERITY_ERROR,
						"fregec.jar is missing in the build path."
						+ "Please 'Enable Frege Builder' from the Project context menu.",
						1, 0, 10);
			} catch (LimitExceededException e) {
				// leck mich
			}
			else msgHandler.handleSimpleMessage("fregec.jar is missing in the build path."
						+ "Please 'Enable Frege Builder' from the Project context menu.",
						0, 10, 0, 0, 0, 0);
		}
		else if (!ourJar.equals(frege.FregePlugin.fregeLib)) {
			if (mcwb != null) try {
				mcwb.addMarker(IMarker.SEVERITY_ERROR,
						"Build Path references unexpected " + ourJar,
						1, 0, 1);
				mcwb.addMarker(IMarker.SEVERITY_ERROR, 
						"It should be " + frege.FregePlugin.fregeLib, 
						1, 1, 2);
				mcwb.addMarker(IMarker.SEVERITY_ERROR, 
						"1. Please remove " + ourJar + " from Build Path", 
						1, 2, 3);
				mcwb.addMarker(IMarker.SEVERITY_ERROR, 
						"2. Please 'Enable Frege Builder' from the Project context menu.", 
						1, 3, 4);
			} catch (LimitExceededException e) {
				// leck mich
			}
			else {
				msgHandler.handleSimpleMessage("Build Path references unexpected " + ourJar,
						0, 1, 0, 0, 0, 0);
				msgHandler.handleSimpleMessage("It should be " + frege.FregePlugin.fregeLib,
						1, 2, 0, 0, 0, 0);
				msgHandler.handleSimpleMessage("1. Please remove " + ourJar + " from Build Path",
						2, 3, 0, 0, 0, 0);
				msgHandler.handleSimpleMessage("2. Please 'Enable Frege Builder' from the Project context menu.",
						3, 4, 0, 0, 0, 0);
			}
		}
		 
		while (!monitor.isCanceled() && maxmsgs > 0) {
			TList.DCons node = msgs._Cons();
			if (node == null) break;
			msgs = node.mem2.<TList>forced();
			TMessage msg = Delayed.<TMessage>forced( node.mem1 );
			if (mcwb != null) {
				// do also warnings and hints
				int sev = IMarker.SEVERITY_ERROR;
				if (TMessage.level(msg) == TSeverity.HINT) sev = IMarker.SEVERITY_INFO;
				else if (TMessage.level(msg) == TSeverity.WARNING)
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
			if (TMessage.level(msg) != TSeverity.ERROR) continue;
			maxmsgs--;
			System.err.print(".");
			msgHandler.handleSimpleMessage(TMessage.text(msg), 
					TPosition.start(TMessage.pos(msg)), 
					TPosition.end(TMessage.pos(msg))-1, 
					0, 0, 0, 0);
		}
		if (mcwb == null) {
			monitor.done();
		}
		System.err.println(" returning to imp framework");
		return g;
	}
	
	@Override
	synchronized public Iterator<Object> getTokenIterator(IRegion region) {
		System.err.print("getTokenIterator(): " + 
				(global != null ? TGlobal.thisPack(global) : "???"));
		if (!tokensIteratorDone) {
			System.err.println("  some");
			tokensIteratorDone = true;
			return new TokensIterator(TSubSt.toks(TGlobal.sub(global)), region);
		}
		else {
			System.err.println("  none");
			return null; // new TokensIterator(new frege.runtime.Array(0), region);
		}
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

				@Override
				public boolean isIdentifierStart(char ch) {
					return Character.isJavaIdentifierStart(ch);
				}

				@Override
				public boolean isIdentifierPart(char ch) {
					return Character.isJavaIdentifierPart(ch);
				}

				@Override
				public boolean isWhitespace(char ch) {
					return Character.isSpaceChar(ch);
				}

				@Override
				public IRegion getDoubleClickRegion(int offset,
						IParseController pc) {
					// TODO Auto-generated method stub
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
	
	/**
	 * look for a path that contains the source code for pack in the context of this parser
	 */
	public IPath getSource(final String pack) {
		// find it in the sources of this project
		final IPath psrc = getFD().getSource(pack);
		if (psrc != null) return psrc;
		// get it via classloader
		final String fr = pack.replaceAll("\\.", "/") + ".fr";		// the file name
		final String segments[] = pack.split("\\.");
		if (this.fProject == null) return null;                     // too bad, doesn't work with project. 
		// final IProject rp = this.fProject.getRawProject();
		final IJavaProject jp = getFD().getJp();
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		// IPath wroot = workspace.getRoot().getLocation();
		
		IFile newsrc = null;
		IPath srcpath = null;
		IPath binPath = null;
		try {
			binPath = jp.getOutputLocation();
		} catch (JavaModelException e) {
			return null;
		}

		srcpath = binPath.append(fr);
		newsrc = workspace.getRoot().getFile(srcpath);
		if (newsrc == null) return null;	// couldn't get file handle here
		if (newsrc.exists()) return srcpath;
		
		IFolder folder = workspace.getRoot().getFolder(binPath);
		// System.err.println("Parser.getSource start in folder " + folder.getLocation());
		InputStream stream = null;
		
		try {
			// create the intermediate directories
			for (int i=0; i < segments.length-1; i++) {
				binPath = binPath.append(segments[i]);
				folder = workspace.getRoot().getFolder(binPath);
				// System.err.println("Parser.getSource continue in folder " + folder.getLocation());
				if (folder.exists()) {
					// System.err.println("Parser.getSource exists " + folder.getLocation());
				}
				else {
					// System.err.println("Parser.getSource creating " + folder.getLocation());
					folder.create(true, true, null);
				}
			}

			final TSubSt subst = TGlobal.sub(this.global);
			final URLClassLoader loader = TSubSt.loader(subst);
			stream = loader.getResourceAsStream(fr);
			if (stream == null) return null;				// not here :-(
			newsrc.create(stream, IResource.FORCE | IResource.DERIVED, new NullProgressMonitor());
		} catch (CoreException e) {
			System.err.println(e.getMessage());
			return null;
		} finally {
			try { if (stream != null) stream.close(); } catch (IOException ioe) {}
		}
		return srcpath;
	}
	
}
