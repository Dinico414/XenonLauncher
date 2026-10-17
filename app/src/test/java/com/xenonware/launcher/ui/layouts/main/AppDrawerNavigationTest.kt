package com.xenonware.launcher.ui.layouts.main

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for the pure grid-navigation maths in [focusIndexAfterMove]. These cover the edge
 * cases that are hard to exercise by hand on a device: row wrapping, short final rows, clamping at
 * the top/bottom, paging and empty grids.
 */
class AppDrawerNavigationTest {

    private val cols = 4

    @Test
    fun `right steps one cell`() {
        assertEquals(5, focusIndexAfterMove(current = 4, columns = cols, itemCount = 20, action = DrawerNavAction.Right))
    }

    @Test
    fun `right on last column of a full row wraps to the next row's first cell`() {
        // Index 3 is the end of row 0; right should land on index 4.
        assertEquals(4, focusIndexAfterMove(current = 3, columns = cols, itemCount = 20, action = DrawerNavAction.Right))
    }

    @Test
    fun `right on the very last item stays put`() {
        assertEquals(19, focusIndexAfterMove(current = 19, columns = cols, itemCount = 20, action = DrawerNavAction.Right))
    }

    @Test
    fun `left on first column of a row wraps to the previous row's last cell`() {
        // Index 4 is the first cell of row 1; left should land on index 3, the last of row 0.
        assertEquals(3, focusIndexAfterMove(current = 4, columns = cols, itemCount = 20, action = DrawerNavAction.Left))
    }

    @Test
    fun `left on the very first item stays put`() {
        assertEquals(0, focusIndexAfterMove(current = 0, columns = cols, itemCount = 20, action = DrawerNavAction.Left))
    }

    @Test
    fun `down steps a full row`() {
        assertEquals(6, focusIndexAfterMove(current = 2, columns = cols, itemCount = 20, action = DrawerNavAction.Down))
    }

    @Test
    fun `down clamps on the last row when the grid does not divide evenly`() {
        // 18 items -> last row is 18,19 (a short row). From 17, down has no full row ahead.
        assertEquals(17, focusIndexAfterMove(current = 17, columns = cols, itemCount = 18, action = DrawerNavAction.Down))
    }

    @Test
    fun `up steps a full row`() {
        assertEquals(2, focusIndexAfterMove(current = 6, columns = cols, itemCount = 20, action = DrawerNavAction.Up))
    }

    @Test
    fun `up clamps on the first row`() {
        assertEquals(1, focusIndexAfterMove(current = 1, columns = cols, itemCount = 20, action = DrawerNavAction.Up))
    }

    @Test
    fun `page down moves by the page size and clamps`() {
        assertEquals(16, focusIndexAfterMove(current = 0, columns = cols, itemCount = 20, action = DrawerNavAction.PageDown, pageSize = 16))
        assertEquals(19, focusIndexAfterMove(current = 10, columns = cols, itemCount = 20, action = DrawerNavAction.PageDown, pageSize = 16))
    }

    @Test
    fun `page up moves by the page size and clamps`() {
        assertEquals(4, focusIndexAfterMove(current = 19, columns = cols, itemCount = 20, action = DrawerNavAction.PageUp, pageSize = 15))
        assertEquals(0, focusIndexAfterMove(current = 2, columns = cols, itemCount = 20, action = DrawerNavAction.PageUp, pageSize = 15))
    }

    @Test
    fun `single column list steps vertically`() {
        assertEquals(5, focusIndexAfterMove(current = 4, columns = 1, itemCount = 10, action = DrawerNavAction.Down))
        assertEquals(3, focusIndexAfterMove(current = 4, columns = 1, itemCount = 10, action = DrawerNavAction.Up))
    }

    @Test
    fun `nothing focused yet starts at the first item`() {
        assertEquals(0, focusIndexAfterMove(current = -1, columns = cols, itemCount = 20, action = DrawerNavAction.Down))
        assertEquals(0, focusIndexAfterMove(current = -1, columns = cols, itemCount = 20, action = DrawerNavAction.Right))
    }

    @Test
    fun `empty grid returns -1`() {
        assertEquals(-1, focusIndexAfterMove(current = 0, columns = cols, itemCount = 0, action = DrawerNavAction.Down))
    }

    @Test
    fun `non movement actions keep the index`() {
        assertEquals(5, focusIndexAfterMove(current = 5, columns = cols, itemCount = 20, action = DrawerNavAction.Select))
        assertEquals(5, focusIndexAfterMove(current = 5, columns = cols, itemCount = 20, action = DrawerNavAction.ContextMenu))
    }

    @Test
    fun `six column wide layout wraps correctly`() {
        // 6-column grid: index 5 is the last of row 0, right wraps to index 6.
        assertEquals(6, focusIndexAfterMove(current = 5, columns = 6, itemCount = 24, action = DrawerNavAction.Right))
        // Left from index 6 wraps back to index 5.
        assertEquals(5, focusIndexAfterMove(current = 6, columns = 6, itemCount = 24, action = DrawerNavAction.Left))
    }

    // ---- Scan-code fallback (Keycode UNKNOWN vertical arrows on some keyboards) ----

    @Test
    fun `scan code 544 maps to up and 545 to down`() {
        assertEquals(DrawerNavAction.Up, scanCodeToNavAction(544))
        assertEquals(DrawerNavAction.Down, scanCodeToNavAction(545))
    }

    @Test
    fun `standard HID arrow usages map correctly`() {
        assertEquals(DrawerNavAction.Up, scanCodeToNavAction(0x52))
        assertEquals(DrawerNavAction.Down, scanCodeToNavAction(0x51))
        assertEquals(DrawerNavAction.Left, scanCodeToNavAction(0x50))
        assertEquals(DrawerNavAction.Right, scanCodeToNavAction(0x4F))
    }

    @Test
    fun `unknown scan codes map to null`() {
        assertEquals(null, scanCodeToNavAction(0))
        assertEquals(null, scanCodeToNavAction(30))
    }
}
