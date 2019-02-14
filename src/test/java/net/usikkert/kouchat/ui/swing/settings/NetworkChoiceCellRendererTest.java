
/***************************************************************************
 *   Copyright 2006-2018 by Christian Ihle                                 *
 *   contact@kouchat.net                                                   *
 *                                                                         *
 *   This file is part of KouChat.                                         *
 *                                                                         *
 *   KouChat is free software; you can redistribute it and/or modify       *
 *   it under the terms of the GNU Lesser General Public License as        *
 *   published by the Free Software Foundation, either version 3 of        *
 *   the License, or (at your option) any later version.                   *
 *                                                                         *
 *   KouChat is distributed in the hope that it will be useful,            *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU      *
 *   Lesser General Public License for more details.                       *
 *                                                                         *
 *   You should have received a copy of the GNU Lesser General Public      *
 *   License along with KouChat.                                           *
 *   If not, see <http://www.gnu.org/licenses/>.                           *
 ***************************************************************************/

package net.usikkert.kouchat.ui.swing.settings;

import static org.junit.Assert.*;

import javax.swing.JList;

import org.junit.Before;
import org.junit.Test;

/**
 * Test of {@link NetworkChoiceCellRenderer}.
 *
 * @author Christian Ihle
 */
public class NetworkChoiceCellRendererTest {

    private NetworkChoiceCellRenderer cellRenderer;

    @Before
    public void setUp() {
        cellRenderer = new NetworkChoiceCellRenderer();
    }

    @Test
    public void getListCellRendererComponentShouldSetToolTipToDisplayName() {
        final JList<NetworkChoice> list = new JList<>();
        final NetworkChoice networkChoice = new NetworkChoice("deviceName", "displayName");

        assertNotNull(cellRenderer.getListCellRendererComponent(list, networkChoice, 0, false, false));

        assertEquals("displayName", list.getToolTipText());
    }
}
