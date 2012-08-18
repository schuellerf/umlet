package com.umlet.element.experimental.settings;

import com.baselet.control.Constants.AlignHorizontal;
import com.baselet.control.Constants.AlignVertical;
import com.umlet.element.experimental.settings.text.ActiveClass;
import com.umlet.element.experimental.settings.text.Facet;
import com.umlet.element.experimental.settings.text.InnerClass;
import com.umlet.element.experimental.settings.text.SeparatorLine;

public class SettingsClass extends Settings {

	@Override
	public float[] getXValues(float y, int height, int width) {
		return new float[] {0, width};
	}

	@Override
	public AlignVertical getVAlign() {
		return AlignVertical.TOP;
	}

	@Override
	public AlignHorizontal getHAlign() {
		return AlignHorizontal.CENTER;
	}

	@Override
	public Facet[] createFacets() {
		return new Facet[]{new ActiveClass(), new InnerClass(), new SeparatorLine(true)};
	}

}