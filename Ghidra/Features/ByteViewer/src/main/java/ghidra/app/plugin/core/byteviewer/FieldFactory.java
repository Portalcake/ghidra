/* ###
 * IP: GHIDRA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ghidra.app.plugin.core.byteviewer;

import java.awt.Color;
import java.awt.FontMetrics;
import java.math.BigInteger;

import docking.widgets.fieldpanel.field.Field;
import docking.widgets.fieldpanel.support.Highlight;
import docking.widgets.fieldpanel.support.HighlightFactory;
import ghidra.app.plugin.core.format.*;
import ghidra.app.util.HighlightProvider;
import ghidra.program.model.address.AddressOutOfBoundsException;

/**
 * Implementation of Field for showing dated formatted according to a
 * DataFormatModel.
 */
class FieldFactory {

	private IndexMap indexMap; // maps index to a block and offset into the block
	private ByteBlockSet blockSet;
	private DataFormatModel model;
	private int charWidth; // width in pixels
	private int fieldOffset;
	private FontMetrics fm;
	private int width; // field width
	private String noValueStr;
	private String readErrorStr; // string to use when there is a read-only exception
	private int startX;
	private Color editColor;
	private Color separatorColor;
	private int unitByteSize;
	private HighlightFactory highlightFactory;

	/**
	 * Constructor
	 * @param model data format model that knows how to represent the data
	 * @param fieldCount number of fields in a row
	 * @param label label that is used as a renderer in the field viewer
	 */
	FieldFactory(DataFormatModel model, int bytesPerLine, int fieldOffset, FontMetrics fm,
			HighlightProvider highlightProvider) {
		this.model = model;
		this.fieldOffset = fieldOffset;
		this.fm = fm;
		this.highlightFactory = new SimpleHighlightFactory(highlightProvider);
		charWidth = fm.charWidth('W');
		width = charWidth * model.getDataUnitSymbolSize();
		editColor = ByteViewerComponentProvider.CHANGED_VALUE_COLOR;
		separatorColor = ByteViewerComponentProvider.SEPARATOR_COLOR;
		unitByteSize = model.getUnitByteSize();
	}

	/**
	 * Sets the starting x position for the fields generated by this factory.
	 */
	public void setStartX(int x) {
		startX = x;
	}

	/**
	 * Returns the starting x position for the fields generated by this factory.
	 */
	public int getStartX() {
		return startX;
	}

	/**
	 * Gets a Field object for the given index.
	 * This method is called for the given index and the fieldOffset
	 * that is defined in the constructor.
	 */
	public Field getField(BigInteger index) {
		if (indexMap == null) {
			return null;
		}
		// translate index to block and offset into the block
		ByteBlockInfo info = indexMap.getBlockInfo(index, fieldOffset);
		if (info == null) {
			if (indexMap.showSeparator(index)) {
				ByteField bf = new ByteField(noValueStr, fm, startX, width, false, fieldOffset,
					index, highlightFactory);
				bf.setForeground(separatorColor);
				return bf;
			}
			return null;
		}
		try {
			ByteBlock block = info.getBlock();
			BigInteger offset = info.getOffset();

			if (!block.hasValue(offset)) {
				// if the ByteBlock doesn't have initialized values at the offset, don't try to read
				// as it causes visual slowness when exceptions are thrown and caught.
				// The following line is the same as the catch (ByteBlockAccessException) handler.
				return getByteField(readErrorStr, index);
			}
			String str = model.getDataRepresentation(block, offset);
			ByteField bf =
				new ByteField(str, fm, startX, width, false, fieldOffset, index, highlightFactory);
			if (blockSet.isChanged(block, offset, unitByteSize)) {
				bf.setForeground(editColor);
			}
			return bf;
		}
		catch (ByteBlockAccessException e) {
			// usually caused by unInitialized memory block
			return getByteField(readErrorStr, index);
		}
		catch (AddressOutOfBoundsException e) {
			return getByteField(noValueStr, index);
		}
		catch (IndexOutOfBoundsException e) {
			return getByteField(noValueStr, index);
		}
	}

	/**
	 * Returns the width (in pixels) of the fields generated by this factory.
	 */
	public int getWidth() {
		return width;
	}

	public FontMetrics getMetrics() {
		return fm;
	}

	///////////////////////////////////////////////////////////////////
	// *** package-level methods
	///////////////////////////////////////////////////////////////////
	/**
	 * Set the index map.
	 */
	void setIndexMap(IndexMap indexMap) {
		this.indexMap = indexMap;
		if (indexMap != null) {
			noValueStr = getString(".");
			readErrorStr = getString("?");
			blockSet = indexMap.getByteBlockSet();
		}
		else {
			blockSet = null;
		}
	}

	int getFieldOffset() {
		return fieldOffset;
	}

	/**
	 * Get the column position given the byte offset.
	 * @param block byte block
	 * @param byteOffset byte offset into the unit
	 *
	 * @return int column position within the field
	 */
	int getColumnPosition(ByteBlock block, int byteOffset) {
		return model.getColumnPosition(block, byteOffset);
	}

	/**
	 * Set the color used to denote changes.
	 */
	void setEditColor(Color c) {
		editColor = c;
	}

	/**
	 * Set the color that indicates gaps in memory.
	 */
	void setSeparatorColor(Color c) {
		separatorColor = c;
	}

	///////////////////////////////////////////////////////////////////
	/**
	 * Get the padded string that has the given char value.
	 */
	private String getString(String value) {
		StringBuffer sb = new StringBuffer();
		int count = model.getDataUnitSymbolSize();
		for (int i = 0; i < count; i++) {
			sb.append(value);
		}
		return sb.toString();
	}

	private ByteField getByteField(String value, BigInteger index) {
		return new ByteField(value, fm, startX, width, false, fieldOffset, index, highlightFactory);
	}

	static class SimpleHighlightFactory implements HighlightFactory {
		private final HighlightProvider provider;

		public SimpleHighlightFactory(HighlightProvider provider) {
			this.provider = provider;
		}

		@Override
		public Highlight[] getHighlights(Field field, String text, int cursorTextOffset) {
			return provider.getHighlights(text, null, null, -1);
		}
	}
}
