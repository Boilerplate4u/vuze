/*
 * Created on 03-Mar-2005
 * Created by Paul Gardner
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.plugins.magnet;

import org.gudy.azureus2.plugins.*;

import com.aelitis.net.magneturi.*;

/**
 * @author parg
 *
 */

public class 
MagnetPlugin
	implements Plugin, MagnetURIHandlerListener
{
	private PluginInterface		plugin_interface;
	
	
	public void
	initialize(
		PluginInterface	_plugin_interface )
	{
		plugin_interface	= _plugin_interface;
		
		plugin_interface.getPluginProperties().setProperty( "plugin.name", "Magnet URI Handler" );
		
		MagnetURIHandler.getSingleton().addListener( this );
	}
	
	public byte[]
	badge()
	{
		return( null );
	}
	
	public byte[]
	download(
		byte[]		hash )
	{
		System.out.println( "MagnetPlugin: download( " + plugin_interface.getUtilities().getFormatters().encodeBytesToString( hash ) + ")");
		
		return( null );
	}
}
