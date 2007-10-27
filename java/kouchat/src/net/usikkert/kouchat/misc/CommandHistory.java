
/***************************************************************************
 *   Copyright 2006-2007 by Christian Ihle                                 *
 *   kontakt@usikkert.net                                                  *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU General Public License     *
 *   along with this program; if not, write to the                         *
 *   Free Software Foundation, Inc.,                                       *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *
 ***************************************************************************/

package net.usikkert.kouchat.misc;

import java.util.ArrayList;
import java.util.List;

/**
 * Saves a number of commands in a history list.
 * The current position in the history is marked by a cursor which
 * can be moved up or down to return the previous or next command.
 * 
 * @author Christian Ihle
 */
public class CommandHistory
{
	/**
	 * Defines the max number of commands to save in the history.
	 */
	private static final int MAX_COMMANDS = 50;
	
	private enum Direction { UP, MIDDLE, DOWN };
	private Direction direction;
	private int cursor;
	private List<String> history;

	/**
	 * Default constructor.
	 */
	public CommandHistory()
	{
		history = new ArrayList<String>();
		direction = Direction.MIDDLE;
	}

	/**
	 * Adds a new command to the list, and resets the cursor.
	 * The command will only be added if it is not empty, and
	 * not identical to the previous command.
	 * 
	 * @param command The command to add to the list.
	 */
	public void add( String command )
	{
		boolean add = true;

		if ( command.trim().length() == 0 )
			add = false;
		else if ( history.size() > 0 && command.equals( history.get( history.size() -1 ) ) )
			add = false;

		if ( add )
		{
			history.add( command );

			if ( history.size() > MAX_COMMANDS )
				history.remove( 0 );
		}

		if ( history.size() > 0 )
			cursor = history.size() -1;

		direction = Direction.MIDDLE;
	}

	/**
	 * Moves the cursor up in the history list, to find the previous command.
	 * If the list is empty, it will return an empty string.
	 * 
	 * @return The previous command.
	 */
	public String goUp()
	{
		String up = "";

		if ( history.size() > 0 )
		{
			if ( direction != Direction.MIDDLE && cursor > 0 )
				cursor--;
			
			direction = Direction.UP;
			up = history.get( cursor );
		}
		
		return up;
	}

	/**
	 * Moves the cursor down in the history list, to find the next command.
	 * If the list is empty, or at the end, it will return an empty string.
	 * 
	 * @return The next command.
	 */
	public String goDown()
	{
		String down = "";

		if ( history.size() > 0 )
		{
			if ( cursor < history.size() -1 )
			{
				cursor++;
				direction = Direction.DOWN;
				down = history.get( cursor );
			}
			
			else
				direction = Direction.MIDDLE;
		}

		return down;
	}
}
