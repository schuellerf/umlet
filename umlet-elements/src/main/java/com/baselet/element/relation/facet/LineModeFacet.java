package com.baselet.element.relation.facet;

import com.baselet.control.enums.LineMode;
import com.baselet.element.facet.FirstRunKeyValueFacet;
import com.baselet.element.facet.PropertiesParserState;

public class LineModeFacet extends FirstRunKeyValueFacet {

	static final String KEY = "linemode";

	public static final LineModeFacet INSTANCE = new LineModeFacet();

	private LineModeFacet() {}

	@Override
	public KeyValue getKeyValue() {
		return new KeyValue("linemode",
				new ValueInfo(LineMode.LINEAR, "Linear line segments"),
				new ValueInfo(LineMode.CIRCULAR, "Circular line segments"));
	}

	@Override
	public void handleValue(String value, PropertiesParserState state) {
		state.getDrawer().setLineMode(LineMode.valueOf(value.toUpperCase()));
	}

}
