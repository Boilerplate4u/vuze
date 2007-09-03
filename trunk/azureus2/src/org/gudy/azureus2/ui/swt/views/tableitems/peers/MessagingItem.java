/*
 * Copyright (C) 2005, 2006 Aelitis SAS, All rights Reserved
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
 *
 * AELITIS, SAS au capital de 46,603.30 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
 
package org.gudy.azureus2.ui.swt.views.tableitems.peers;

import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

/**
 * 
 */
public class MessagingItem
       extends CoreTableColumn 
       implements TableCellRefreshListener
{
  /** Default Constructor */
  public MessagingItem() {
    super("Messaging", ALIGN_CENTER, POSITION_INVISIBLE, 20, TableManager.TABLE_TORRENT_PEERS);
    setRefreshInterval(INTERVAL_LIVE);
  }

  public void refresh(TableCell cell) {
    PEPeer peer = (PEPeer)cell.getDataSource();
    int value = (peer == null) ? PEPeer.MESSAGING_BT_ONLY : peer.handshakedMessaging();

    if (!cell.setSortValue(value) && cell.isValid())
      return;
    
    String text;
    
    switch (value) {
    case PEPeer.MESSAGING_BT_ONLY:
		text = "Bt";		
		break;
	case PEPeer.MESSAGING_LTEP:
		text = "Lt";		
		break;
	case PEPeer.MESSAGING_AZMP:
		text = "Az";		
		break;
	case PEPeer.MESSAGING_EXTERN:
		text = "Plugin";		
		break;
	default:
		text = "";
		break;
	}
    
    cell.setText(text);
  }
}
