/*
 * File    : TRHostImpl.java
 * Created : 24-Oct-2003
 * By      : parg
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
 
package org.gudy.azureus2.core3.tracker.host.impl;

/**
 * @author parg
 */

import java.util.*;

import org.gudy.azureus2.core3.tracker.host.*;
import org.gudy.azureus2.core3.tracker.server.*;
import org.gudy.azureus2.core3.torrent.*;

public class 
TRHostImpl
	implements TRHost 
{
	public static final int RETRY_DELAY 	= 60;	// seconds
	public static final int DEFAULT_PORT	= 80;	// port to use if none in announce URL
	
	protected static TRHostImpl		singleton;
	
	protected Hashtable	server_map 	= new Hashtable();
	
	protected List	torrents	= new ArrayList();
	
	protected List	listeners	= new ArrayList();
	
	public static synchronized TRHost
	create()
	{
		if ( singleton == null ){
			
			singleton = new TRHostImpl();
		}
		
		return( singleton );
	}
	
	public synchronized void
	addTorrent(
		TOTorrent		torrent )
	{
		for (int i=0;i<torrents.size();i++){
			
			TRHostTorrent	ht = (TRHostTorrent)torrents.get(i);
			
			if ( ht.getTorrent() == torrent ){
		
					// already there
							
				return;
			}
		}
		
		int	port = torrent.getAnnounceURL().getPort();
		
		if ( port == -1 ){
			
			port = DEFAULT_PORT;
		}
		
		TRTrackerServer	server = (TRTrackerServer)server_map.get( new Integer( port ));
		
		if ( server == null ){
			
			try{
			
				server = TRTrackerServerFactory.create( port, RETRY_DELAY );
			
				server_map.put( new Integer( port ), server );
				
			}catch( TRTrackerServerException e ){
				
				e.printStackTrace();
			}
		}
		
		TRHostTorrent host_torrent = new TRHostTorrentImpl( this, server, torrent );
		
		torrents.add( host_torrent );
		
		for (int i=0;i<listeners.size();i++){
			
			((TRHostListener)listeners.get(i)).torrentAdded( host_torrent );
		}
	}
	
	protected synchronized void
	remove(
		TRHostTorrent	host_torrent )
	{
		torrents.remove( host_torrent );
		
		for (int i=0;i<listeners.size();i++){
			
			((TRHostListener)listeners.get(i)).torrentRemoved( host_torrent );
		}		
	}
	
	public TRHostTorrent[]
	getTorrents()
	{
		TRHostTorrent[]	res = new TRHostTorrent[torrents.size()];
		
		torrents.toArray( res );
		
		return( res );
	}
	
	public synchronized void
	addListener(
		TRHostListener	l )
	{
		listeners.add( l );
		
		for (int i=0;i<torrents.size();i++){
			
			l.torrentAdded((TRHostTorrent)torrents.get(i));
		}
	}
		
	public synchronized void
	removeListener(
		TRHostListener	l )
	{
		listeners.remove( l );
	}
}
