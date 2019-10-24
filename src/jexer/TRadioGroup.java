/*
 * Jexer - Java Text User Interface
 *
 * The MIT License (MIT)
 *
 * Copyright (C) 2019 Kevin Lamonte
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *
 * @author Kevin Lamonte [kevin.lamonte@gmail.com]
 * @version 1
 */
package jexer;

import jexer.bits.CellAttributes;
import jexer.bits.StringUtils;

/**
 * TRadioGroup is a collection of TRadioButtons with a box and label.
 */
public class TRadioGroup extends TWidget {

    // ------------------------------------------------------------------------
    // Variables --------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Label for this radio button group.
     */
    private String label;

    /**
     * Only one of my children can be selected.
     */
    private TRadioButton selectedButton = null;

    /**
     * If true, one of the children MUST be selected.  Note package private
     * access.
     */
    boolean requiresSelection = true;

    // ------------------------------------------------------------------------
    // Constructors -----------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Public constructor.
     *
     * @param parent parent widget
     * @param x column relative to parent
     * @param y row relative to parent
     * @param label label to display on the group box
     */
    public TRadioGroup(final TWidget parent, final int x, final int y,
        final String label) {

        // Set parent and window
        super(parent, x, y, StringUtils.width(label) + 4, 2);

        this.label = label;
    }

    // ------------------------------------------------------------------------
    // TWidget ----------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Override TWidget's width: we can only set width at construction time.
     *
     * @param width new widget width (ignored)
     */
    @Override
    public void setWidth(final int width) {
        // Do nothing
    }

    /**
     * Override TWidget's height: we can only set height at construction
     * time.
     *
     * @param height new widget height (ignored)
     */
    @Override
    public void setHeight(final int height) {
        // Do nothing
    }

    /**
     * Draw a radio button with label.
     */
    @Override
    public void draw() {
        CellAttributes radioGroupColor;

        if (isAbsoluteActive()) {
            radioGroupColor = getTheme().getColor("tradiogroup.active");
        } else {
            radioGroupColor = getTheme().getColor("tradiogroup.inactive");
        }

        drawBox(0, 0, getWidth(), getHeight(), radioGroupColor, radioGroupColor,
            3, false);

        hLineXY(1, 0, StringUtils.width(label) + 2, ' ', radioGroupColor);
        putStringXY(2, 0, label, radioGroupColor);
    }

    // ------------------------------------------------------------------------
    // TRadioGroup ------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
     * Get the radio button ID that was selected.
     *
     * @return ID of the selected button, or 0 if no button is selected
     */
    public int getSelected() {
        if (selectedButton == null) {
            return 0;
        }
        return selectedButton.getId();
    }

    /**
     * Set the new selected radio button.  Note package private access.
     *
     * @param button new button that became selected
     */
    void setSelected(final TRadioButton button) {
        assert (button.isSelected());
        if ((selectedButton != null) && (selectedButton != button)) {
            selectedButton.setSelected(false);
        }
        selectedButton = button;
    }

    /**
     * Set the new selected radio button.  1-based.
     *
     * @param id ID of the selected button, or 0 to unselect
     */
    public void setSelected(final int id) {
        if ((id < 0) || (id > getChildren().size())) {
            return;
        }

        if (id == 0) {
            for (TWidget widget: getChildren()) {
                ((TRadioButton) widget).setSelected(false);
            }
            selectedButton = null;
            return;
        }
        assert ((id > 0) && (id <= getChildren().size()));
        TRadioButton button = (TRadioButton) (getChildren().get(id - 1));
        button.setSelected(true);
        selectedButton = button;
    }

    /**
     * Convenience function to add a radio button to this group.
     *
     * @param label label to display next to (right of) the radiobutton
     * @return the new radio button
     */
    public TRadioButton addRadioButton(final String label) {
        int buttonX = 1;
        int buttonY = getChildren().size() + 1;
        if (StringUtils.width(label) + 4 > getWidth()) {
            super.setWidth(StringUtils.width(label) + 7);
        }
        super.setHeight(getChildren().size() + 3);
        TRadioButton button = new TRadioButton(this, buttonX, buttonY, label,
            getChildren().size() + 1);

        if (getParent().getLayoutManager() != null) {
            getParent().getLayoutManager().resetSize(this);
        }

        // Default to the first item on the list.
        activate(getChildren().get(0));

        return button;
    }

}