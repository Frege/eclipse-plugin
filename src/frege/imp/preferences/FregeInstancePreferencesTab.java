
package frege.imp.preferences;

import java.util.List;
import java.util.ArrayList;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import io.usethesource.impulse.preferences.*;
import io.usethesource.impulse.preferences.fields.*;


/**
 * The instance level preferences tab.
 */
public class FregeInstancePreferencesTab extends InstancePreferencesTab {

	public FregeInstancePreferencesTab(IPreferencesService prefService) {
		super(prefService, false);
	}

	/**
	 * Creates specific preference fields with settings appropriate to
	 * the instance preferences level.
	 *
	 * Overrides an unimplemented method in PreferencesTab.
	 *
	 * @return    An array that contains the created preference fields
	 *
	 */
	protected FieldEditor[] createFields(TabbedPreferencesPage page, Composite parent)
	{
		List<FieldEditor> fields = new ArrayList<FieldEditor>();

		
		FontFieldEditor sourceFont = fPrefUtils.makeNewFontField(
			page, this, fPrefService,
			"instance", "sourceFont", "Editor Font",
			"Editor font",
			parent,
			true, true,
			false);
		fields.add(sourceFont);

		IntegerFieldEditor tabWidth = fPrefUtils.makeNewIntegerField(
				page, this, fPrefService,
				"instance", "tabWidth", "Tabulator Width",
				"Tells how many spaces the tab key will insert.",
				parent,
				true, true,
				true, "4",
				false);
		fields.add(tabWidth);
		
		GridData data = new GridData();
        data.horizontalAlignment = SWT.END;
        data.widthHint = 128;


		BooleanFieldEditor spacesForTabs = fPrefUtils.makeNewBooleanField(
			page, this, fPrefService,
			"instance", "spacesForTabs", "Tab inserts spaces",
			"Is it strongly suggested to not have tabulator characters in Frege source code.",
			parent,
			true, true,
			true, true,
			false);
		fields.add(spacesForTabs);
		
		BooleanFieldEditor enableInline = fPrefUtils.makeNewBooleanField(
				page, this, fPrefService,
				"instance", "enableInline", "Enable Inline",
				"Optimize by inlining functions option to the compiler.",
				parent,
				true, true,
				true, false,
				false);
		fields.add(enableInline);

		
		BooleanFieldEditor enableComments = fPrefUtils.makeNewBooleanField(
				page, this, fPrefService,
				"instance", "enableComments", "Enable Comments",
				"Create commented source code. Builds will take much longer.",
				parent,
				true, true,
				true, false,
				false);
		fields.add(enableComments);
		
		BooleanFieldEditor italicImports = fPrefUtils.makeNewBooleanField(
				page, this, fPrefService,
				"instance", "italicImports", "Show imported items in italics",
				"Gives a visual clue about whether a name was imported.",
				parent,
				true, true,
				true, false,
				false);
			fields.add(italicImports);
			
		BooleanFieldEditor boldNS = fPrefUtils.makeNewBooleanField(
			page, this, fPrefService,
			"instance", "boldNS", "Show name spaces in bold face",
			"Gives a visual clue about whether a qualifier is not a type, but a namespace.",
			parent,
			true, true,
			true, false,
			false);
		fields.add(boldNS);
		
		BooleanFieldEditor unicode = fPrefUtils.makeNewBooleanField(
				page, this, fPrefService,
				"instance", "useUnicode", "Use Unicode Symbols",
				"Use ∀ and → instead of 'forall' and -> when showing types (not yet implemented)",
				parent,
				true, true,
				true, false,
				false);
		fields.add(unicode);

		BooleanFieldEditor greek = fPrefUtils.makeNewBooleanField(
				page, this, fPrefService,
				"instance", "useGreek", "Greek Type Variables",
				"Construct type variable names from greek letters when showing types (not yet implemented)",
				parent,
				true, true,
				true, false,
				false);
		fields.add(greek);
		
		BooleanFieldEditor fraktur = fPrefUtils.makeNewBooleanField(
				page, this, fPrefService,
				"instance", "useFraktur", "Fraktur Type Variables",
				"Construct type variable names from fraktur letters when showing types (not yet implemented)",
				parent,
				true, true,
				true, false,
				false);
		fields.add(fraktur);		
		ColorFieldEditor docuColor = fPrefUtils.makeNewColorField(
			page, this, fPrefService,
			"instance", "docuColor", "Documentation",
			"Color for documentation comments",
			parent,
			true, true,
			false);
		docuColor.getLabelControl(docuColor.getParent()).setLayoutData(data);
		fields.add(docuColor);

		ColorFieldEditor commColor = fPrefUtils.makeNewColorField(
			page, this, fPrefService,
			"instance", "commColor", "Comments        ",
			"Color for ordinary comments",
			parent,
			true, true,
			false);
		commColor.getLabelControl(commColor.getParent()).setLayoutData(data);
		fields.add(commColor);

		ColorFieldEditor tconColor = fPrefUtils.makeNewColorField(
			page, this, fPrefService,
			"instance", "tconColor", "Types etc.",
			"Color for type constrcutors, type aliases, classes and namespaces",
			parent,
			true, true,
			false);
		tconColor.getLabelControl(tconColor.getParent()).setLayoutData(data);
		fields.add(tconColor);

		ColorFieldEditor dconColor = fPrefUtils.makeNewColorField(
			page, this, fPrefService,
			"instance", "dconColor", "Constructors    ",
			"Color for data constrcutor names",
			parent,
			true, true,
			false);
		dconColor.getLabelControl(dconColor.getParent()).setLayoutData(data);
		fields.add(dconColor);


		ColorFieldEditor varidColor = fPrefUtils.makeNewColorField(
			page, this, fPrefService,
			"instance", "varidColor", "Top level vars",
			"Color for non-local variables defined in the current package",
			parent,
			true, true,
			false);
		varidColor.getLabelControl(varidColor.getParent()).setLayoutData(data);
		fields.add(varidColor);
		
		ColorFieldEditor importColor = fPrefUtils.makeNewColorField(
			page, this, fPrefService,
			"instance", "importColor", "Imported vars",
			"Color for variables and functions defined in an imported package",
			parent,
			true, true,
			false);
		importColor.getLabelControl(importColor.getParent()).setLayoutData(data);
		fields.add(importColor);
		
		ColorFieldEditor keywordColor = fPrefUtils.makeNewColorField(
			page, this, fPrefService,
			"instance", "keywordColor", "Reserved words",
			"Color for key words.",
			parent,
			true, true,
			false);
		keywordColor.getLabelControl(keywordColor.getParent()).setLayoutData(data);
		fields.add(keywordColor);

		ColorFieldEditor specialColor = fPrefUtils.makeNewColorField(
			page, this, fPrefService,
			"instance", "specialColor", "Special symbols ",
			"Color for symbols ::, ->, <-, => and |",
			parent,
			true, true,
			false);
		specialColor.getLabelControl(specialColor.getParent()).setLayoutData(data);
		fields.add(specialColor);

		ColorFieldEditor literalColor = fPrefUtils.makeNewColorField(
			page, this, fPrefService,
			"instance", "literalColor", "Literal values",
			"Colors for literal numbers, strings, characters and regular expressions",
			parent,
			true, true,
			false);
		literalColor.getLabelControl(literalColor.getParent()).setLayoutData(data);
		fields.add(literalColor);

		ColorFieldEditor errorColor = fPrefUtils.makeNewColorField(
			page, this, fPrefService,
			"instance", "errorColor", "Lexical errors",
			"Color that signals unfinished block comments and quoted constructs",
			parent,
			true, true,
			false);
		
        errorColor.getLabelControl(errorColor.getParent()).setLayoutData(data);
		fields.add(errorColor);
		
//		RadioGroupFieldEditor tstyle = fPrefUtils.makeNewRadioGroupField(
//		page, this, fPrefService, 
//		"instance", "typeStyle", "Type Presentation Style", 
//		"Decide how to show types",
//		3, new String[] {"ascii", "greek", "fraktur"}, 
//		// new String[]{"forall a.a->a", "∀α.α→α", "∀𝖆.𝖆→𝖆"}, 
//		new String[]{"A", "G", "F"},
//		parent, true, true, false);
// 		fields.add(tstyle);

		StringFieldEditor prefix = fPrefUtils.makeNewStringField(
				page, this, fPrefService, 
				"instance", "prefix", "Prefix", 
				"Used in compiler development", 
				parent, 
				true, true, 
				true, "", 
				false);
		fields.add(prefix);
		
		
		IntegerFieldEditor parseTimeout = fPrefUtils.makeNewIntegerField(
			page, this, fPrefService,
			"instance", "parseTimeout", "Parser Timeout",
			"Time in ms before the parser starts after a keystroke.",
			parent,
			true, true,
			true, "250",
			false);
		fields.add(parseTimeout);
		
		
		return fields.toArray(new FieldEditor[fields.size()]);
	}
}
