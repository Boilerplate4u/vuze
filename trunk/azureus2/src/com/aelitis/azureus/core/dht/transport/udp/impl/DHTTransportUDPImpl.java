/*
 * Created on 21-Jan-2005
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

package com.aelitis.azureus.core.dht.transport.udp.impl;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.*;

import org.gudy.azureus2.core3.ipchecker.extipchecker.ExternalIPChecker;
import org.gudy.azureus2.core3.ipchecker.extipchecker.ExternalIPCheckerFactory;
import org.gudy.azureus2.core3.ipchecker.extipchecker.ExternalIPCheckerService;
import org.gudy.azureus2.core3.ipchecker.extipchecker.ExternalIPCheckerServiceListener;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.plugins.logging.LoggerChannel;

import com.aelitis.azureus.core.dht.transport.*;
import com.aelitis.azureus.core.dht.transport.udp.*;
import com.aelitis.azureus.core.dht.transport.util.DHTTransportRequestCounter;
import com.aelitis.azureus.core.dht.transport.util.DHTTransportStatsImpl;
import com.aelitis.azureus.core.versioncheck.VersionCheckClient;
import com.aelitis.net.udp.*;

/**
 * @author parg
 *
 */

public class 
DHTTransportUDPImpl 
	implements DHTTransportUDP, PRUDPRequestHandler
{
	private static boolean TEST_EXTERNAL_IP	= false;
	
	static{
		
		DHTUDPPacket.registerCodecs();
	}
	
	private static String	external_address;
	
	
	private int					port;
	private int					max_fails;
	private long				request_timeout;
	private LoggerChannel		logger;
	
	private PRUDPPacketHandler			packet_handler;
	
	private DHTTransportRequestHandler	request_handler;
	
	private DHTTransportUDPContactImpl		local_contact;
	
	private long last_address_change;
	
	private List listeners	= new ArrayList();
	
	
	private DHTTransportStatsImpl	stats = new DHTTransportStatsImpl();

		// TODO: secure enough?
	
	private	Random		random = new Random( SystemTime.getCurrentTime());
	
	public
	DHTTransportUDPImpl(
		int				_port,
		int				_max_fails,
		long			_timeout,
		LoggerChannel	_logger )
	{
		port			= _port;
		max_fails		= _max_fails;
		request_timeout	= _timeout;
		logger			= _logger;
		
			// DHTPRUDPPacket relies on the request-handler being an instanceof THIS so watch out
			// if you change it :)
		
		packet_handler = PRUDPPacketHandlerFactory.getHandler( _port, this );		

		
		InetSocketAddress	address = 
			new InetSocketAddress( getExternalAddress(false,"127.0.0.1", logger), port );

		logger.log( "Initial external address: " + address );
		
		local_contact = new DHTTransportUDPContactImpl( this, address, random.nextInt());
	}
	
	public void
	testInstanceIDChange()
	{
		local_contact = new DHTTransportUDPContactImpl( this, local_contact.getAddress(), random.nextInt());		
	}
	
	public void
	testTransportIDChange()
	{
		if ( external_address.equals("127.0.0.1")){
			
			external_address = "192.168.0.2";
		}else{
			
			external_address = "127.0.0.1";
		}
		
		local_contact = new DHTTransportUDPContactImpl( this, new InetSocketAddress( external_address, port ), local_contact.getInstanceID());		

		for (int i=0;i<listeners.size();i++){
			
			try{
				((DHTTransportListener)listeners.get(i)).localContactChanged( local_contact );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
	}
	
	protected static synchronized String
	getExternalAddress(
		boolean				force,
		String				default_address,
		final LoggerChannel	log )
	{
		if ( force || external_address == null ){
			
			external_address = null;
			
			if ( TEST_EXTERNAL_IP ){
				
				external_address	= "192.168.0.2";
				
				return( external_address );
			}
			
			try{
				String	vc_ip = VersionCheckClient.getSingleton().getExternalIpAddress();
				
				if ( vc_ip != null && vc_ip.length() > 0 ){
					
					log.log( "External IP address obtained from version-check: " + vc_ip );
					
					external_address	= vc_ip;
					
				}else{
					
					ExternalIPChecker	checker = ExternalIPCheckerFactory.create();
					
					ExternalIPCheckerService[]	services = checker.getServices();
					
					final String[]	ip = new String[]{ null };
					
					for (int i=0;i<services.length && ip[0] == null;i++){
						
						final ExternalIPCheckerService	service = services[i];
						
						if ( service.supportsCheck()){
		
							final AESemaphore	sem = new AESemaphore("DHTUDP:getExtIP");
							
							ExternalIPCheckerServiceListener	listener = 
								new ExternalIPCheckerServiceListener()
								{
									public void
									checkComplete(
										ExternalIPCheckerService	_service,
										String						_ip )
									{
										log.log( "External IP address obtained from " + service.getName() + ": " + _ip );

										ip[0]	= _ip;
										
										sem.release();
									}
										
									public void
									checkFailed(
										ExternalIPCheckerService	service,
										String						reason )
									{
										sem.release();
									}
										
									public void
									reportProgress(
										ExternalIPCheckerService	service,
										String						message )
									{
									}
								};
								
							services[i].addListener( listener );
							
							try{
								
								services[i].initiateCheck( 60000 );
								
								sem.reserve( 60000 );
								
							}finally{
								
								services[i].removeListener( listener );
							}
						}
					}
					
					if ( ip[0] != null ){
						
						external_address	= ip[0];
					}
				}
				
			}catch( Throwable e ){
				
				Debug.printStackTrace( e );
			}
			
			if ( external_address == null ){
				
				external_address =	default_address;
				
				log.log( "External IP address defaulted:  " + default_address );
			}
		}
		
		return( external_address );
	}
	
	protected synchronized boolean
	externalAddressChange(
		InetSocketAddress		new_address )
	{
			/*
			 * A node has reported that our external address and the one he's seen a 
			 * message coming from differ. Natural explanations are along the lines of
			 * 1) my address is dynamically allocated by my ISP and it has changed
			 * 2) I have multiple network interfaces 
			 * 3) there's some kind of proxy going on
			 * 4) this is a DOS attempting to stuff me up
			 * 
			 * We assume that our address won't change more frequently than once every
			 * 5 minutes
			 * We assume that if we can successfully obtain an external address by
			 * using the above explicit check then this is accurate
			 * Only in the case where the above check fails do we believe the address
			 * that we've been told about
			 */
		
		if ( new_address.getPort() != port ){

			// port has changed. we don't support anything automatic here as it is
			// indicative of a configuration problem
		
			logger.logAlert( 
					LoggerChannel.LT_WARNING,
					"Node has reported differing port (current=" + port + ", reported = " + new_address.getPort() + ") - check configuration" );
			
			return( false );
		}
		
		String	new_ip = new_address.getAddress().getHostAddress();
		
		if ( new_ip.equals( external_address )){
			
				// probably just be a second notification of an address change
							
			return( true );
		}

		long	now = SystemTime.getCurrentTime();

		if ( now - last_address_change < 5*60*1000 ){
			
			return( false );
		}
		
		last_address_change	= now;
		
		String	a = getExternalAddress( true, new_ip, logger );
		
		InetSocketAddress	s_address = new InetSocketAddress( a, port );
		
		logger.log( "External address changed: " + s_address );
		
		local_contact = new DHTTransportUDPContactImpl( this, s_address, random.nextInt());

		for (int i=0;i<listeners.size();i++){
			
			try{
				((DHTTransportListener)listeners.get(i)).localContactChanged( local_contact );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
		
		return( true );
	}
	
	protected int
	getMaxFailCount()
	{
		return( max_fails );
	}
	
	public DHTTransportContact
	getLocalContact()
	{
		return( local_contact );
	}
	
	public void
	importContact(
		DataInputStream		is )
	
		throws IOException
	{
		request_handler.contactImported( 
				DHTUDPUtils.deserialiseContact( this, is ));
	}
	
	public void
	importContact(
		InetSocketAddress	address )
	{
		request_handler.contactImported( 
			new DHTTransportUDPContactImpl( this, address, 0 ));
	}
	
	public void
	exportContact(
		DHTTransportContact	contact,
		DataOutputStream	os )
	
		throws IOException
	{
		DHTUDPUtils.serialiseContact( os, contact );
	}
	
	public void
	setRequestHandler(
		DHTTransportRequestHandler	_request_handler )
	{
		request_handler = new DHTTransportRequestCounter( _request_handler, stats );
	}
	
	public DHTTransportStats
	getStats()
	{
		return( stats );
	}
	
	protected void
	dispatch(
		Runnable	r )
	{
		// TODO: request queue limits?
		
		r.run();
	}
	
	protected void
	sendPing(
		final DHTTransportUDPContactImpl	contact,
		final DHTTransportReplyHandler		handler )
	{
		AERunnable	runnable = 
			new AERunnable()
			{
				public void
				runSupport()
				{
					sendPingSupport( contact, handler );
				}
			};
		
		dispatch( runnable );
	}
	
	protected void
	sendPingSupport(
		final DHTTransportUDPContactImpl	contact,
		final DHTTransportReplyHandler		handler )
	{
		stats.pingSent();

		final long	connection_id = getConnectionID();
		
		final DHTUDPPacketRequestPing	request = 
			new DHTUDPPacketRequestPing( connection_id, local_contact );
			
		try{
			packet_handler.sendAndReceive(
				request,
				contact.getAddress(),
				new PRUDPPacketReceiver()
				{
					private int	retry_count	= 0;
					
					public void
					packetReceived(
						PRUDPPacket			_packet,
						InetSocketAddress	from_address )
					{
						try{
							DHTUDPPacketReply	packet = (DHTUDPPacketReply)_packet;
							
							if ( packet.getConnectionId() != connection_id ){
								
								throw( new Exception( "connection id mismatch" ));
							}
							
							if ( handleErrorReply( packet )){
								
								retry_count++;
								
								if ( retry_count > 1 ){
									
									error( new PRUDPPacketHandlerException("retry limit exceeded"));
									
								}else{
									
									request.setOriginatorID(local_contact.getID());
									
									packet_handler.sendAndReceive(
											request,
											contact.getAddress(),
											this,
											request_timeout );
								}
							}else{
								
								stats.pingOK();
							
								handler.pingReply( contact );
							}
						}catch( Throwable e ){
							
							Debug.printStackTrace(e);
							
							stats.pingFailed();
							
							handler.failed( contact );
						}
					}
					
					public void
					error(
						PRUDPPacketHandlerException	e )
					{
						stats.pingFailed();
						
						handler.failed( contact );
					}
				},
				request_timeout);
			
		}catch( Throwable e ){
			
			stats.pingFailed();
			
			handler.failed( contact );
		}
	}
		
		// STORE
	
	public void
	sendStore(
		final DHTTransportUDPContactImpl	contact,
		final DHTTransportReplyHandler		handler,
		final byte[]						key,
		final DHTTransportValue				value )
	{
		AERunnable	runnable = 
			new AERunnable()
			{
				public void
				runSupport()
				{
					sendStoreSupport( contact, handler, key, value );
				}
			};
		
		dispatch( runnable );
	}
	
	public void
	sendStoreSupport(
		final DHTTransportUDPContactImpl	contact,
		final DHTTransportReplyHandler		handler,
		byte[]								key,
		DHTTransportValue					value )
	{
		stats.storeSent();
		
		final long	connection_id = getConnectionID();
		
		try{
			final DHTUDPPacketRequestStore	request = 
				new DHTUDPPacketRequestStore( connection_id, local_contact );
			
			request.setKey( key );
			
			request.setValue( value );
			
			packet_handler.sendAndReceive(
				request,
				contact.getAddress(),
				new PRUDPPacketReceiver()
				{
					private int	retry_count	= 0;
					
					public void
					packetReceived(
						PRUDPPacket			_packet,
						InetSocketAddress	from_address )
					{
						try{
							DHTUDPPacketReply	packet = (DHTUDPPacketReply)_packet;
							
							if ( packet.getConnectionId() != connection_id ){
								
								throw( new Exception( "connection id mismatch" ));
							}
							
							if ( handleErrorReply( packet )){
								
								retry_count++;
								
								if ( retry_count > 1 ){
									
									error( new PRUDPPacketHandlerException("retry limit exceeded"));
									
								}else{
									
									request.setOriginatorID(local_contact.getID());
									
									packet_handler.sendAndReceive(
											request,
											contact.getAddress(),
											this,
											request_timeout );
								}
							}else{
								
								stats.storeOK();
							
								handler.storeReply( contact );
							}
							
						}catch( Throwable e ){
							
							Debug.printStackTrace(e);
							
							stats.storeFailed();
							
							handler.failed( contact );
						}
					}
					
					public void
					error(
						PRUDPPacketHandlerException	e )
					{
						stats.storeFailed();
						
						handler.failed( contact );
					}
				},
				request_timeout );
			
		}catch( Throwable e ){
			
			stats.storeFailed();
			
			handler.failed( contact );
		}
	}
	
		// FIND NODE
	
	public void
	sendFindNode(
		final DHTTransportUDPContactImpl	contact,
		final DHTTransportReplyHandler		handler,
		final byte[]						nid )
	{
		AERunnable	runnable = 
			new AERunnable()
			{
				public void
				runSupport()
				{
					sendFindNodeSupport( contact, handler, nid );
				}
			};
		
		dispatch( runnable );
	}
	
	public void
	sendFindNodeSupport(
		final DHTTransportUDPContactImpl	contact,
		final DHTTransportReplyHandler		handler,
		byte[]								nid )
	{
		stats.findNodeSent();
		
		final long	connection_id = getConnectionID();
		
		try{
			final DHTUDPPacketRequestFindNode	request = 
				new DHTUDPPacketRequestFindNode( connection_id, local_contact );
			
			request.setID( nid );
			
			packet_handler.sendAndReceive(
				request,
				contact.getAddress(),
				new PRUDPPacketReceiver()
				{
					private int	retry_count	= 0;
					
					public void
					packetReceived(
						PRUDPPacket			_packet,
						InetSocketAddress	from_address )
					{
						try{
							DHTUDPPacketReply	packet = (DHTUDPPacketReply)_packet;
														
							if ( packet.getConnectionId() != connection_id ){
								
								throw( new Exception( "connection id mismatch" ));
							}

							if ( handleErrorReply( packet )){
								
								retry_count++;
								
								if ( retry_count > 1 ){
									
									error( new PRUDPPacketHandlerException("retry limit exceeded"));
									
								}else{
									
									request.setOriginatorID(local_contact.getID());
									
									packet_handler.sendAndReceive(
											request,
											contact.getAddress(),
											this,
											request_timeout );
								}
							}else{
								
								DHTUDPPacketReplyFindNode	reply = (DHTUDPPacketReplyFindNode)packet;
							
								stats.findNodeOK();
								
								handler.findNodeReply( contact, reply.getContacts());
							}
							
						}catch( Throwable e ){
							
							Debug.printStackTrace(e);
							
							stats.findNodeFailed();
							
							handler.failed( contact );
						}
					}
					
					public void
					error(
						PRUDPPacketHandlerException	e )
					{
						stats.findNodeFailed();
						
						handler.failed( contact );
					}
				},
				request_timeout);
			
		}catch( Throwable e ){
			
			stats.findNodeFailed();
			
			handler.failed( contact );
		}
	}
	
		// FIND VALUE
	
	public void
	sendFindValue(
		final DHTTransportUDPContactImpl	contact,
		final DHTTransportReplyHandler		handler,
		final byte[]						key )
	{
		AERunnable	runnable = 
			new AERunnable()
			{
				public void
				runSupport()
				{
					sendFindValueSupport( contact, handler, key );
				}
			};
		
		dispatch( runnable );
	}
	
	public void
	sendFindValueSupport(
		final DHTTransportUDPContactImpl	contact,
		final DHTTransportReplyHandler		handler,
		byte[]								key )
	{
		stats.findValueSent();

		final long	connection_id = getConnectionID();
		
		try{
			final DHTUDPPacketRequestFindValue	request = 
				new DHTUDPPacketRequestFindValue( connection_id, local_contact );
			
			request.setID( key );
			
			packet_handler.sendAndReceive(
				request,
				contact.getAddress(),
				new PRUDPPacketReceiver()
				{
					private int	retry_count;
					
					public void
					packetReceived(
						PRUDPPacket			_packet,
						InetSocketAddress	from_address )
					{
						try{
							DHTUDPPacketReply	packet = (DHTUDPPacketReply)_packet;
							
							if ( packet.getConnectionId() != connection_id ){
								
								throw( new Exception( "connection id mismatch" ));
							}
							
							if ( handleErrorReply( packet )){
								
								retry_count++;
								
								if ( retry_count > 1 ){
									
									error( new PRUDPPacketHandlerException("retry limit exceeded"));
									
								}else{
									
									request.setOriginatorID(local_contact.getID());
									
									packet_handler.sendAndReceive(
											request,
											contact.getAddress(),
											this,
											request_timeout );
								}
							}else{
								
								DHTUDPPacketReplyFindValue	reply = (DHTUDPPacketReplyFindValue)packet;
								
								stats.findValueOK();
								
								DHTTransportValue	res = reply.getValue();
								
								if ( res != null ){
									
									handler.findValueReply( contact, res );
									
								}else{
									
									handler.findValueReply( contact, reply.getContacts());
								}
							}
						}catch( Throwable e ){
							
							Debug.printStackTrace(e);
							
							stats.findValueFailed();
							
							handler.failed( contact );
						}
					}
					
					public void
					error(
						PRUDPPacketHandlerException	e )
					{
						stats.findValueFailed();
						
						handler.failed( contact );
					}
				},
				request_timeout);
			
		}catch( Throwable e ){
			
			stats.findValueFailed();
			
			handler.failed( contact );
		}
	}
	
	public void
	process(
		PRUDPPacketRequest	_request )
	{
		try{
			DHTUDPPacketRequest	request = (DHTUDPPacketRequest)_request;
			
			DHTTransportUDPContactImpl	originating_contact = new DHTTransportUDPContactImpl( this, request.getAddress(), request.getOriginatorInstanceID());
			
			if ( !Arrays.equals( request.getOriginatorID(), originating_contact.getID())){
				
				logger.log( "Node " + originating_contact.getString() + " has incorrect ID, reporting it to them" );
				
				DHTUDPPacketReplyError	reply = 
					new DHTUDPPacketReplyError(
							request.getTransactionId(),
							request.getConnectionId());
				
				reply.setErrorType( DHTUDPPacketReplyError.ET_ORIGINATOR_ADDRESS_WRONG );
				
				reply.setOriginatingAddress( originating_contact.getAddress());
				
				packet_handler.send( reply, request.getAddress());

			}else{
				
				if ( request instanceof DHTUDPPacketRequestPing ){
					
					request_handler.pingRequest( originating_contact );
					
					DHTUDPPacketReplyPing	reply = 
						new DHTUDPPacketReplyPing(
								request.getTransactionId(),
								request.getConnectionId());
					
					packet_handler.send( reply, request.getAddress());
					
				}else if ( request instanceof DHTUDPPacketRequestStore ){
					
					DHTUDPPacketRequestStore	store_request = (DHTUDPPacketRequestStore)request;
					
					request_handler.storeRequest(
							originating_contact, 
							store_request.getKey(), 
							store_request.getValue());
					
					DHTUDPPacketReplyStore	reply = 
						new DHTUDPPacketReplyStore(
								request.getTransactionId(),
								request.getConnectionId());
					
					packet_handler.send( reply, request.getAddress());
					
				}else if ( request instanceof DHTUDPPacketRequestFindNode ){
					
					DHTUDPPacketRequestFindNode	find_request = (DHTUDPPacketRequestFindNode)request;
					
					DHTTransportContact[] res = 
						request_handler.findNodeRequest(
									originating_contact,
									find_request.getID());
					
					DHTUDPPacketReplyFindNode	reply = 
						new DHTUDPPacketReplyFindNode(
								request.getTransactionId(),
								request.getConnectionId());
								
					reply.setContacts( res );
					
					packet_handler.send( reply, request.getAddress());
					
				}else if ( request instanceof DHTUDPPacketRequestFindValue ){
					
					DHTUDPPacketRequestFindValue	find_request = (DHTUDPPacketRequestFindValue)request;
				
					Object res = 
						request_handler.findValueRequest(
									originating_contact,
									find_request.getID());
					
					DHTUDPPacketReplyFindValue	reply = 
						new DHTUDPPacketReplyFindValue(
							request.getTransactionId(),
							request.getConnectionId());
					
					if ( res instanceof DHTTransportValue ){
						
						reply.setValue((DHTTransportValue)res);
	
					}else{
						
						reply.setContacts((DHTTransportContact[])res );
					}
					
					packet_handler.send( reply, request.getAddress());
					
				}else{
					
					Debug.out( "Unexpected packet:" + request.toString());
				}
			}
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		}
	}
	
	protected boolean
	handleErrorReply(
		DHTUDPPacketReply		reply )
	{
		if ( reply.getAction() == DHTUDPPacket.ACT_REPLY_ERROR ){
			
			DHTUDPPacketReplyError	error = (DHTUDPPacketReplyError)reply;
			
			switch( error.getErrorType()){
			
				case DHTUDPPacketReplyError.ET_ORIGINATOR_ADDRESS_WRONG:
				{
					return( externalAddressChange( error.getOriginatingAddress()));
				}
				default:
				{
					Debug.out( "Unknown error type received" );
				}
			}
			
			return( false );
			
		}else{
			
			return( false );
		}
	}
	
	protected long
	getConnectionID()
	{
			// unfortunately, to reuse the UDP port with the tracker protocol we 
			// have to distinguish our connection ids by setting the MSB. This allows
			// the decode to work as there is no common header format for the request
			// and reply packets
		
			// note that tracker usage of UDP via this handler is only for outbound
			// messages, hence for that use a request will never be received by the
			// handler
		
		return( 0x8000000000000000L | random.nextLong());
	}
	
	public void
	addListener(
		DHTTransportListener	l )
	{
		listeners.add(l);
	}
	
	public void
	removeListener(
		DHTTransportListener	l )
	{
		listeners.remove(l);
	}
}
