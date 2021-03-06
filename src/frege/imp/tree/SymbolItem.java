package frege.imp.tree;

import org.eclipse.swt.graphics.Image;

import frege.compiler.types.Global.TGlobal;
import frege.compiler.types.QNames.TQName;
import frege.compiler.enums.Visibility.TVisibility;
import frege.compiler.types.Positions.TPosition;
import frege.compiler.types.Symbols.TSymbolT;
import frege.ide.Utilities;

public class SymbolItem implements ITreeItem {
	final TSymbolT<TGlobal> symbol;
	final TGlobal global;
	
	public SymbolItem(TGlobal g, TSymbolT<TGlobal> sy) { global = g; symbol = sy; }

	@Override
	public Image getImage() {
		final int c = symbol.constructor();
		if (c >= 0 && c < FregeLabelProvider.SYMBOL_IMAGES.length) {
			Image image = FregeLabelProvider.SYMBOL_IMAGES[c];
			if (image == FregeLabelProvider.VAR_IMAGE 
					&& (TSymbolT.vis(symbol) != TVisibility.Public
						|| TQName.isLocal(TSymbolT.name(symbol))))
				image = FregeLabelProvider.LOCAL_IMAGE;
			return image;
		}
		return FregeLabelProvider.OUTLINE_IMAGE;
	}

	@Override
	public String getLabel() {
		return Utilities.label(global, symbol);
	}

	@Override
	public TPosition getPosition() {
		return TSymbolT.pos(symbol);
	}

}
