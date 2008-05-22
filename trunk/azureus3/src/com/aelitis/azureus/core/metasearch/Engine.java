package com.aelitis.azureus.core.metasearch;

import java.io.*;
import java.util.Map;


public interface 
Engine 
{	
	public static final Object	VUZE_FILE_COMPONENT_ENGINE_KEY = new Object();
	
	public static final int FIELD_NAME = 1;
	public static final int FIELD_DATE = 2;
	public static final int FIELD_SIZE = 3;
	public static final int FIELD_PEERS = 4;
	public static final int FIELD_SEEDS = 5;
	public static final int FIELD_CATEGORY = 6;
	public static final int FIELD_COMMENTS = 7;
	public static final int FIELD_CONTENT_TYPE = 8;
	
	public static final int FIELD_TORRENTLINK = 102;
	public static final int FIELD_CDPLINK = 103;
	public static final int FIELD_PLAYLINK = 104;
	
	
	public static final int ENGINE_TYPE_REGEX		= 1;
	public static final int ENGINE_TYPE_JSON		= 2;
	
	public static final int	ENGINE_SOURCE_UNKNOWN				= 0;
	public static final int	ENGINE_SOURCE_VUZE					= 1;
	public static final int	ENGINE_SOURCE_LOCAL					= 2;
	
	public static final int	SEL_STATE_DESELECTED			= 0;
	public static final int	SEL_STATE_AUTO_SELECTED			= 1;
	public static final int	SEL_STATE_MANUAL_SELECTED		= 2;
	
		/**
		 * don't change these as they are externalised
		 */
	public static final String[] ENGINE_SOURCE_STRS = { "unknown","vuze","local","unused","unused" };
	public static final String[] SEL_STATE_STRINGS	= { "no", "auto", "manual" };
	public static final String[] ENGINE_TYPE_STRS 	= { "unknown","regexp","json" };
	
	public int getType();
	
	public Result[]
  	search(
  		SearchParameter[] 	searchParameters )
  	
  		throws SearchException;
	
	public Result[]
	search(
		SearchParameter[] 	searchParameters,
		String				headers )
	
		throws SearchException;
	
	public void
	search(
		SearchParameter[] 	searchParameters,
		int					max_matches,
		String				headers,
		ResultListener		listener );
	
	public String 
	getName();
	
	public long 
	getId();
	
	public long 
	getLastUpdated();
	
	public String 
	getIcon();

	public boolean
	isActive();
	
	public int
	getSelectionState();
	
	public void
	setSelectionState(
		int			state );
	
	public void
	checkSelectionStateRecorded();
		
	public int
	getSource();
	
	public void
	setSource(
		int		source );
	
	public Map 
	exportToBencodedMap() 
	
		throws IOException;
	
	public String
	exportToJSONString()
	
		throws IOException;
	
	public void
	exportToVuzeFile(
		File	target )
	
		throws IOException;
	
		/**
		 * Tests for sameness in terms of function (ignores id, selection state etc)
		 * @param other
		 * @return
		 */
	
	public boolean
	sameAs(
		Engine	other );
	
	public void
	delete();
	
	public String
	getString();
}
