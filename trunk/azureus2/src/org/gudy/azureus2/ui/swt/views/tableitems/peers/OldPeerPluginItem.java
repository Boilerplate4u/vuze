/*
 * File    : OldPeerPluginItem.java
 * Created : 24 nov. 2003
 * By      : Olivier
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
 
package org.gudy.azureus2.ui.swt.views.tableitems.peers;

import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.plugins.peers.*;
import org.gudy.azureus2.plugins.ui.tables.peers.*;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.pluginsimpl.local.peers.*;
import org.gudy.azureus2.ui.swt.views.table.impl.TableColumnImpl;

/** Cell/Factory to support old style Plugin columns
 *
 * @author Olivier
 * @author TuxPaper (2004/Apr/17: modified to TableCellAdapter)
 */
/** Link the old PluginMyTorrentsItemFactory to the new generic stuff */
public class OldPeerPluginItem
       extends TableColumnImpl
       implements TableCellAddedListener
{
  private PluginPeerItemFactory oldFactory;
  private String oldFactoryType;

  public OldPeerPluginItem(String sTableID, String sCellName, 
                           PluginPeerItemFactory item) {
    super(sTableID, sCellName);
    oldFactory = item;
    oldFactoryType = oldFactory.getType();
    addCellAddedListener(this);
    setRefreshInterval(TableColumn.INTERVAL_LIVE);
  }
  
  public void cellAdded(TableCell cell) {
    new Cell(cell);
  }

  private class Cell
          implements TableCellRefreshListener, PeerTableItem
  {
    PluginPeerItem pluginItem;
    TableCell cell;
  
    public Cell(TableCell item) {
      cell = item;
      pluginItem = OldPeerPluginItem.this.oldFactory.getInstance(this);

      // listener is disposed of in core when cell is removed
      cell.addRefreshListener(this);
    }
    
    public Peer getPeer() {
      return (Peer)cell.getDataSource();
    }
    
    public void refresh(TableCell cell) {
      try {
        if (cell.isShown()) {
          pluginItem.refresh();

          if (oldFactoryType.equals(PluginPeerItemFactory.TYPE_STRING))
            cell.setSortValue(pluginItem.getStringValue());
          else
            cell.setSortValue(pluginItem.getIntValue());
        }
      } catch(Throwable e) {
        LGLogger.log(LGLogger.ERROR,"Plugin in PeersView generated an exception : " + e );
        e.printStackTrace();
      }
    }
    
    public boolean setText(String s) {
      return cell.setText(s);
    }
  }
}
