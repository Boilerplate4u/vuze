/*
 * Created on 12-Jan-2005
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

package com.aelitis.azureus.core.dht.transport;

/**
 * @author parg
 *
 */

import java.io.*;

public interface 
DHTTransport 
{
		// all defined transports need to be declared here to support the export and import
		// of state
	
	public static final int	TT_LOOPBACK		= 1;
	
		/**
		 * Gives access to the node ID for this transport 
		 * @return
		 */
	
	public DHTTransportContact
	getLocalContact();
	
	public void
	importContact(
		InputStream		is )
	
		throws IOException;
	
		/**
		 * Set the handler for incoming requests
		 * @param receiver
		 */
	
	public void
	setRequestHandler(
		DHTTransportRequestHandler	receiver );
	
	public DHTTransportStats
	getStats();
}
