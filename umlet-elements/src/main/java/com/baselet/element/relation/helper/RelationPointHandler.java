package com.baselet.element.relation.helper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.baselet.control.SharedUtils;
import com.baselet.control.basics.geom.Line;
import com.baselet.control.basics.geom.Point;
import com.baselet.control.basics.geom.Rectangle;
import com.baselet.control.enums.LineMode;
import com.baselet.diagram.draw.DrawHandler;
import com.baselet.diagram.draw.helper.StyleException;
import com.baselet.element.sticking.PointChange;
import com.baselet.element.sticking.PointDoubleIndexed;

public class RelationPointHandler implements ResizableObject {

	/**
	 * Points of this relation (point of origin is the upper left corner of the relation element (not the drawpanel!))
	 */
	private RelationPointList points = new RelationPointList();
	private final RelationPointHolder relation;

	public RelationPointHandler(RelationPointHolder relation, RelationPointList points) {
		super();
		this.relation = relation;
		this.points = points;
	}

	public RelationSelection getSelection(Point point, LineMode lineMode) {
		if (isPointOverDragBox(point, lineMode)) {
			return RelationSelection.DRAG_BOX;
		}
		else if (RelationPointHandlerUtils.getRelationPointContaining(point, points) != null) {
			return RelationSelection.RELATION_POINT;
		}
		else if (getLineContaining(point) != null) {
			return RelationSelection.LINE;
		}
		else {
			return RelationSelection.NOTHING;
		}
	}

	private PointDoubleIndexed relationPointOfCurrentDrag = null;

	/**
	 * this method is basically the same as {@link #getSelection(Point, LineMode)}, but also applies changes to the relationpoints
	 * (the order of checks is the same, but they do different things, therefore they are separated)
	 */
	public RelationSelection getSelectionAndMovePointsIfNecessary(Point point, Integer diffX, Integer diffY, boolean firstDrag, LineMode lineMode) {
		// Special case: if this is not the first drag and a relation-point is currently dragged, it has preference
		// Necessary to avoid changing the currently moved point if moving over another point and to avoid losing the current point if it's a new line point and the mouse is dragged very fast
		if (!firstDrag && relationPointOfCurrentDrag != null) {
			relationPointOfCurrentDrag = movePointAndResizeRectangle(relationPointOfCurrentDrag, diffX, diffY, lineMode);
			return RelationSelection.RELATION_POINT;
		}
		// If the special case doesn't apply, forget the relationPointOfFirstDrag, because its a new first drag
		relationPointOfCurrentDrag = null;
		if (isPointOverDragBox(point, lineMode)) {
			return RelationSelection.DRAG_BOX;
		}
		PointDoubleIndexed pointOverRelationPoint = RelationPointHandlerUtils.getRelationPointContaining(point, points);
		if (pointOverRelationPoint != null) {
			relationPointOfCurrentDrag = movePointAndResizeRectangle(pointOverRelationPoint, diffX, diffY, lineMode);
			return RelationSelection.RELATION_POINT;
		}
		Line lineOnPoint = getLineContaining(point);
		if (lineOnPoint != null) {
			relationPointOfCurrentDrag = points.addPointOnLine(lineOnPoint, SharedUtils.realignToGridRoundToNearest(false, point.x), SharedUtils.realignToGridRoundToNearest(false, point.y));
			relationPointOfCurrentDrag = movePointAndResizeRectangle(relationPointOfCurrentDrag, diffX, diffY, lineMode);
			return RelationSelection.LINE;
		}
		return RelationSelection.NOTHING;
	}

	private boolean isPointOverDragBox(Point point, LineMode lineMode) {
		return getDragBox(lineMode).contains(point);
	}

	private Line getLineContaining(Point point) {
		for (Line line : points.getRelationPointLines()) {
			double distanceToPoint = line.getDistanceToPoint(point.toPointDouble());
			if (distanceToPoint < RelationPointConstants.NEW_POINT_DISTANCE) {
				return line;
			}
		}
		return null;
	}

	public List<PointDoubleIndexed> movePointAndResizeRectangle(List<PointChange> changedPoints, LineMode lineMode) {
		points.applyChangesToPoints(changedPoints);
		resizeRectAndReposPoints(lineMode);
		List<PointDoubleIndexed> updatedChangedPoint = new ArrayList<PointDoubleIndexed>();
		for (PointChange c : changedPoints) {
			updatedChangedPoint.add(points.get(c.getIndex()));
		}
		return updatedChangedPoint;
	}

	private PointDoubleIndexed movePointAndResizeRectangle(PointDoubleIndexed point, Integer diffX, Integer diffY, LineMode lineMode) {
		return movePointAndResizeRectangle(Arrays.asList(new PointChange(point.getIndex(), diffX, diffY)), lineMode).get(0);
	}

	public void resizeRectAndReposPoints(LineMode lineMode) {
		// now rebuild width and height of the relation, based on the new positions of the relation-points
		Rectangle newRect = RelationPointHandlerUtils.calculateRelationRectangleBasedOnPoints(relation.getRectangle().getUpperLeftCorner(), relation.getGridSize(), points, lineMode);
		relation.setRectangle(newRect);

		// move relation points to their new position (their position is relative to the relation-position)
		points.moveRelationPointsAndTextSpacesByToUpperLeftCorner(lineMode);
	}

	public boolean removeRelationPointIfOnLineBetweenNeighbourPoints() {
		return points.removeRelationPointIfOnLineBetweenNeighbourPoints();
	}

	// HELPER METHODS

	public Line getFirstLine() {
		return points.getFirstLine();
	}

	public Line getMiddleLine() {
		return points.getMiddleLine();
	}

	public Line getLastLine() {
		return points.getLastLine();
	}

	public Collection<PointDoubleIndexed> getStickablePoints() {
		return points.getStickablePoints();
	}

	public Rectangle getDragBox(LineMode lineMode) {
		return points.getDragBox(lineMode);
	}

	// DRAW METHODS

	public void drawLinesBetweenPoints(DrawHandler drawer, boolean shortFirstLine, boolean shortLastLine) {
		List<Line> lines = points.getRelationPointLines();
		boolean linear = false;
		boolean finalCircuarException = false;
		if (drawer.getLineMode() == LineMode.CIRCULAR) {
			if (lines.size() == 0 || lines.size() % 2 != 0) {
				linear = true;
				finalCircuarException = true;
			}
			else {
				for (int i = 0; i < lines.size(); i += 2) {
					drawer.drawArcThroughPoints(lines.get(i).getStart(), lines.get(i).getEnd(), lines.get(i + 1).getEnd());
				}
			}
		}
		else {
			linear = true;
		}
		if (linear) {
			for (int i = 0; i < lines.size(); i++) {
				Line lineToDraw = lines.get(i);
				if (i == 0 && shortFirstLine) {
					lineToDraw = lineToDraw.getShorterVersion(true, RelationDrawer.SHORTEN_IF_ARROW);
				}
				else if (i == lines.size() - 1 && shortLastLine) {
					lineToDraw = lineToDraw.getShorterVersion(false, RelationDrawer.SHORTEN_IF_ARROW);
				}
				drawer.drawLine(lineToDraw);
			}
		}

		if (finalCircuarException) {
			throw new StyleException("Circular linemode needs a multiple of 2 lines (not " + lines.size() + ")");
		}
	}

	public void drawCirclesAndDragBox(DrawHandler drawer, LineMode lineMode) {
		for (RelationPoint p : points.getPointHolders()) {
			drawer.drawCircle(p.getPoint().getX(), p.getPoint().getY(), RelationPointConstants.POINT_SELECTION_RADIUS);
		}
		drawer.drawRectangle(getDragBox(lineMode));
	}

	public String toAdditionalAttributesString() {
		return points.toAdditionalAttributesString();
	}

	public void drawSelectionSpace(DrawHandler drawer) {
		for (RelationPoint rp : points.getPointHolders()) {
			drawer.drawRectangle(rp.getSizeAbsolute());
		}
	}

	public void setTextBox(int index, Rectangle size) {
		points.setTextBox(index, size);
	}

	/**
	 * resets all textbox indexes except those which are contained in the excludedList
	 */
	public void resetTextBoxIndexesExcept(Set<Integer> excludedList) {
		Set<Integer> unusedTextBoxIndexes = new HashSet<Integer>(points.getTextBoxIndexes());
		unusedTextBoxIndexes.removeAll(excludedList);
		for (Integer index : unusedTextBoxIndexes) {
			points.removeTextBox(index);
		}
	}

	@Override
	public void setPointMinSize(int index, Rectangle size) {
		size = SharedUtils.realignToGrid(size, true);
		points.setSize(index, size);
	}

	@Override
	public void resetPointMinSize(int index) {
		points.setSize(index, RelationPoint.DEFAULT_SIZE);
	}

	@Override
	public String toString() {
		return points.toString();
	}

}
