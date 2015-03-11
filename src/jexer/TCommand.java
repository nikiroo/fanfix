/**
 * Jexer - Java Text User Interface
 *
 * License: LGPLv3 or later
 *
 * This module is licensed under the GNU Lesser General Public License
 * Version 3.  Please see the file "COPYING" in this directory for more
 * information about the GNU Lesser General Public License Version 3.
 *
 *     Copyright (C) 2015  Kevin Lamonte
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, see
 * http://www.gnu.org/licenses/, or write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA
 *
 * @author Kevin Lamonte [kevin.lamonte@gmail.com]
 * @version 1
 */
package jexer;

/**
 * This class encapsulates a user command event.  User commands can be
 * generated by menu actions, keyboard accelerators, and other UI elements.
 * Commands can operate on both the application and individual widgets.
 */
public class TCommand {

    /**
     * Immediately abort the application (e.g. remote side closed
     * connection).
     */
    public static final int ABORT       = 1;

    /**
     * File open dialog.
     */
    public static final int OPEN        = 2;

    /**
     * Exit application.
     */
    public static final int EXIT        = 3;

    /**
     * Spawn OS shell window.
     */
    public static final int SHELL       = 4;

    /**
     * Cut selected text and copy to the clipboard.
     */
    public static final int CUT         = 5;

    /**
     * Copy selected text to clipboard.
     */
    public static final int COPY        = 6;

    /**
     * Paste from clipboard.
     */
    public static final int PASTE       = 7;

    /**
     * Clear selected text without copying it to the clipboard.
     */
    public static final int CLEAR       = 8;

    /**
     * Tile windows.
     */
    public static final int TILE        = 9;

    /**
     * Cascade windows.
     */
    public static final int CASCADE     = 10;

    /**
     * Close all windows.
     */
    public static final int CLOSE_ALL   = 11;

    /**
     * Move (move/resize) window.
     */
    public static final int WINDOW_MOVE = 12;

    /**
     * Zoom (maximize/restore) window.
     */
    public static final int WINDOW_ZOOM = 13;

    /**
     * Next window (like Alt-TAB).
     */
    public static final int WINDOW_NEXT = 14;

    /**
     * Previous window (like Shift-Alt-TAB).
     */
    public static final int WINDOW_PREVIOUS = 15;

    /**
     * Close window.
     */
    public static final int WINDOW_CLOSE = 16;

    /**
     * Type of command, one of EXIT, CASCADE, etc.
     */
    private int type;

    /**
     * Protected constructor.  Subclasses can be used to define new commands.
     *
     * @param type the Type of command, one of EXIT, CASCADE, etc.
     */
    protected TCommand(final int type) {
        this.type = type;
    }

    /**
     * Make human-readable description of this TCommand.
     *
     * @return displayable String
     */
    @Override
    public final String toString() {
        return String.format("%s", type);
    }

    /**
     * Comparison check.  All fields must match to return true.
     *
     * @param rhs another TCommand instance
     * @return true if all fields are equal
     */
    @Override
    public final boolean equals(final Object rhs) {
        if (!(rhs instanceof TCommand)) {
            return false;
        }

        TCommand that = (TCommand) rhs;
        return (type == that.type);
    }

    public static final TCommand cmAbort      = new TCommand(ABORT);
    public static final TCommand cmExit       = new TCommand(EXIT);
    public static final TCommand cmQuit       = new TCommand(EXIT);
    public static final TCommand cmOpen       = new TCommand(OPEN);
    public static final TCommand cmShell      = new TCommand(SHELL);
    public static final TCommand cmCut        = new TCommand(CUT);
    public static final TCommand cmCopy       = new TCommand(COPY);
    public static final TCommand cmPaste      = new TCommand(PASTE);
    public static final TCommand cmClear      = new TCommand(CLEAR);
    public static final TCommand cmTile       = new TCommand(TILE);
    public static final TCommand cmCascade    = new TCommand(CASCADE);
    public static final TCommand cmCloseAll   = new TCommand(CLOSE_ALL);
    public static final TCommand cmWindowMove = new TCommand(WINDOW_MOVE);
    public static final TCommand cmWindowZoom = new TCommand(WINDOW_ZOOM);
    public static final TCommand cmWindowNext = new TCommand(WINDOW_NEXT);
    public static final TCommand cmWindowPrevious = new TCommand(WINDOW_PREVIOUS);
    public static final TCommand cmWindowClose = new TCommand(WINDOW_CLOSE);

}
