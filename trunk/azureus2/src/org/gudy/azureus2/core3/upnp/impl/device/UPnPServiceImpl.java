/*
 * Created on 15-Jun-2004
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

package org.gudy.azureus2.core3.upnp.impl.device;

/**
 * @author parg
 *
 */

import java.io.InputStream;
import java.net.*;
import java.util.*;

import org.gudy.azureus2.core3.upnp.*;
import org.gudy.azureus2.core3.xml.simpleparser.*;
import org.gudy.azureus2.plugins.utils.resourcedownloader.*;
import org.gudy.azureus2.pluginsimpl.local.utils.resourcedownloader.*;

public class 
UPnPServiceImpl
	extends 	ResourceDownloaderAdapter
	implements 	UPnPService
{
	protected UPnPDeviceImpl	device;
	
	protected String			service_type;
	protected String			desc_url;
	protected String			control_url;
	
	protected List				actions;
	
	protected
	UPnPServiceImpl(
		UPnPDeviceImpl					_device,
		String							indent,
		SimpleXMLParserDocumentNode		service_node )
	{
		device		= _device;
		
		service_type 		= service_node.getChild("ServiceType").getValue();
		
		desc_url	= service_node.getChild("SCPDURL").getValue();
		
		control_url	= service_node.getChild("controlURL").getValue();
		
		device.getUPnP().log( indent + desc_url + ", " + control_url );
	}
	
	public String
	getServiceType()
	{
		return( service_type );
	}
	
	public UPnPAction[]
	getActions()
	
		throws UPnPException
	{
		if ( actions == null ){
			
			loadDescription();
		}
		
		UPnPAction[]	res = new UPnPAction[actions.size()];
		
		actions.toArray( res );
		
		return( res );
	}
	
	public UPnPAction
	getAction(
		String	name )
	
		throws UPnPException
	{
		UPnPAction[]	actions = getActions();
		
		for (int i=0;i<actions.length;i++){
			
			if ( actions[i].getName().equalsIgnoreCase( name )){
				
				return( actions[i] );
			}
		}
		
		return( null );
	}
	
	public URL
	getDescriptionURL()
	
		throws UPnPException
	{
		return( getURL( desc_url ));
	}
	
	public URL
	getControlURL()
	
		throws UPnPException
	{
		return( getURL( control_url ));
	}
	
	protected URL
	getURL(
		String	basis )
	
		throws UPnPException
	{
		URL	root_location = device.getRootDevice().getLocation();
		
		try{
			URL	target;
			
			if ( basis.toLowerCase().startsWith( "http" )){
				
				target = new URL( basis );
				
			}else{
				
				target = new URL( root_location.getProtocol() + "://" +
									root_location.getHost() + 
									(root_location.getPort() == -1?"":":" + root_location.getPort()) + 
									(basis.startsWith( "/" )?"":"/") + basis );
				
			}
			
			return( target );
			
		}catch( MalformedURLException e ){
			
			throw( new UPnPException( "Malformed URL", e ));
		}
	}
	
	protected void
	loadDescription()
	
		throws UPnPException
	{		
		try{
			ResourceDownloaderFactory rdf = ResourceDownloaderFactoryImpl.getSingleton();
			
			ResourceDownloader rd = rdf.getRetryDownloader( rdf.create( getDescriptionURL()), 3 );
			
			rd.addListener( this );
			
			try{
				InputStream	data = rd.download();
				
				SimpleXMLParserDocument	doc = SimpleXMLParserDocumentFactory.create( data );

				parseActions( doc.getChild( "ActionList" ));
				
			}catch( Throwable e ){
				
				e.printStackTrace();
				
				device.getUPnP().log( e );
			}
		}catch( Throwable e ){
			
			throw( new UPnPException( "Failed to load service description '" + desc_url + "'", e ));
		}
	}
	
	protected void
	parseActions(
		SimpleXMLParserDocumentNode	action_list )
	{
		actions	= new ArrayList();
		
		SimpleXMLParserDocumentNode[]	kids = action_list.getChildren();
		
		for (int i=0;i<kids.length;i++){
			
			actions.add( new UPnPActionImpl( this, kids[i] ));
		}
	}
	
	public void
	reportActivity(
		ResourceDownloader	downloader,
		String				activity )
	{
		device.getUPnP().log( activity );
	}
		
	public void
	failed(
		ResourceDownloader			downloader,
		ResourceDownloaderException e )
	{
		device.getUPnP().log( e );
	}
}
