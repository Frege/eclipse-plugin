/******************************************/
/* WARNING: GENERATED FILE - DO NOT EDIT! */
/******************************************/
package frege.imp.preferences;

import org.eclipse.swt.widgets.TabFolder;
import io.usethesource.impulse.preferences.IPreferencesService;
import io.usethesource.impulse.preferences.PreferencesInitializer;
import io.usethesource.impulse.preferences.PreferencesTab;
import io.usethesource.impulse.preferences.TabbedPreferencesPage;
import frege.FregePlugin;

/**
 * A preference page class.
 */
public class FregePreferencesPage extends TabbedPreferencesPage {
	public FregePreferencesPage() {
		super();
		prefService = FregePlugin.getInstance().getPreferencesService();
	}

	protected PreferencesTab[] createTabs(IPreferencesService prefService,
		TabbedPreferencesPage page, TabFolder tabFolder) {
		PreferencesTab[] tabs = new PreferencesTab[1];

		FregeInstancePreferencesTab instanceTab = new FregeInstancePreferencesTab(prefService);
		instanceTab.createTabContents(page, tabFolder);
		tabs[0] = instanceTab;

		return tabs;
	}

	public PreferencesInitializer getPreferenceInitializer() {
		return new FregePreferencesInitializer();
	}
}
