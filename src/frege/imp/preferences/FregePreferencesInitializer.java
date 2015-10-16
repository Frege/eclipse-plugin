/******************************************/
/* WARNING: GENERATED FILE - DO NOT EDIT! */
/******************************************/
package frege.imp.preferences;

import io.usethesource.impulse.preferences.PreferencesInitializer;
import io.usethesource.impulse.preferences.IPreferencesService;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.swt.graphics.RGB;

import frege.FregePlugin;

/**
 * Initializations of default values for preferences.
 */
public class FregePreferencesInitializer extends PreferencesInitializer {
	/**
	 * Convert a hex number to RGB
	 */
	public static RGB hexRGB(int color) {
		final int red   = (color & 0xff0000) >> 16;
		final int green = (color & 0x00ff00) >>  8;
		final int blue  = (color & 0x0000ff);
		return new RGB(red, green, blue);
	}
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
	 */
	public void initializeDefaultPreferences() {
		IPreferencesService service = FregePlugin.getInstance().getPreferencesService();

		// in the following, we set some "light solarized colours as defaults"
		String base01  = StringConverter.asString(hexRGB(0x586e75));
		String brgreen = base01;
		String base1   = StringConverter.asString(hexRGB(0x93a1a1));
		String brcyan  = base1;
		String yellow  = StringConverter.asString(hexRGB(0xb58900));
		String orange  = StringConverter.asString(hexRGB(0xcb4b16));
		String red     = StringConverter.asString(hexRGB(0xdc322f));
		String magenta = StringConverter.asString(hexRGB(0xd33682));
		String violet  = StringConverter.asString(hexRGB(0x6c71c4));
		String blue    = StringConverter.asString(hexRGB(0x268bd2));
		String cyan    = StringConverter.asString(hexRGB(0x2aa198));
		String green   = StringConverter.asString(hexRGB(0x859900));
		service.setStringPreference(IPreferencesService.DEFAULT_LEVEL, FregePreferencesConstants.P_SOURCEFONT, "Consolas");
		service.setIntPreference(IPreferencesService.DEFAULT_LEVEL, FregePreferencesConstants.P_TABWITH, 4);
		service.setIntPreference(IPreferencesService.DEFAULT_LEVEL, FregePreferencesConstants.P_PARSETIMEOUT, 250);
		// service.setBooleanPreference(IPreferencesService.DEFAULT_LEVEL, FregePreferencesConstants.P_SPACESFORTABS, true);
		service.setBooleanPreference(IPreferencesService.DEFAULT_LEVEL, FregePreferencesConstants.P_INLINE, true);
		service.setBooleanPreference(IPreferencesService.DEFAULT_LEVEL, FregePreferencesConstants.P_INLINE, false);
		service.setBooleanPreference(IPreferencesService.DEFAULT_LEVEL, FregePreferencesConstants.P_USEUNICODE, false);
		service.setBooleanPreference(IPreferencesService.DEFAULT_LEVEL, FregePreferencesConstants.P_USEGREEK, false);
		service.setBooleanPreference(IPreferencesService.DEFAULT_LEVEL, FregePreferencesConstants.P_USEFRAKTUR, false);
		service.setBooleanPreference(IPreferencesService.DEFAULT_LEVEL, FregePreferencesConstants.P_ITALICIMPORTS, true);
		service.setBooleanPreference(IPreferencesService.DEFAULT_LEVEL, FregePreferencesConstants.P_BOLDNS, true);
		service.setStringPreference(IPreferencesService.DEFAULT_LEVEL, FregePreferencesConstants.P_DOCUCOLOR, brgreen);
		service.setStringPreference(IPreferencesService.DEFAULT_LEVEL, FregePreferencesConstants.P_COMMCOLOR, brcyan);
		service.setStringPreference(IPreferencesService.DEFAULT_LEVEL, FregePreferencesConstants.P_TCONCOLOR, orange);
		service.setStringPreference(IPreferencesService.DEFAULT_LEVEL, FregePreferencesConstants.P_DCONCOLOR, green);
		service.setStringPreference(IPreferencesService.DEFAULT_LEVEL, FregePreferencesConstants.P_VARIDCOLOR, blue);
		service.setStringPreference(IPreferencesService.DEFAULT_LEVEL, FregePreferencesConstants.P_IMPORTCOLOR, violet);
		service.setStringPreference(IPreferencesService.DEFAULT_LEVEL, FregePreferencesConstants.P_KEYWORDCOLOR, magenta);
		service.setStringPreference(IPreferencesService.DEFAULT_LEVEL, FregePreferencesConstants.P_SPECIALCOLOR, magenta);
		service.setStringPreference(IPreferencesService.DEFAULT_LEVEL, FregePreferencesConstants.P_LITERALCOLOR, cyan);
		service.setStringPreference(IPreferencesService.DEFAULT_LEVEL, FregePreferencesConstants.P_ERRORCOLOR, red);
		service.setStringPreference(IPreferencesService.DEFAULT_LEVEL, FregePreferencesConstants.P_PREFIX, "");
				
		/*
		System.err.println("darkYellow is " + darkYellow);
		System.err.println("darkRed is " + darkRed);
		System.err.println("black is " + black);
		System.err.println("red is " + red);
		System.err.println("darkMagenta is " + darkMagenta);
		System.err.println("darkGreen is " + darkGreen);
		System.err.println("darkCyan is " + darkCyan);
		*/
	}

	/*
	 * Clear (remove) any preferences set on the given level.
	 */
	public void clearPreferencesOnLevel(String level) {
		IPreferencesService service = FregePlugin.getInstance().getPreferencesService();
		service.clearPreferencesAtLevel(level);

	}
}
