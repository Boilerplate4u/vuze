/*
 * Copyright (C) 2004 Aelitis SARL, All rights Reserved
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
 * AELITIS, SARL au capital de 30,000 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
 
package org.gudy.azureus2.ui.swt.views.tableitems.mytracker;


import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.tracker.host.*;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

/**
 *
 * @author TuxPaper
 * @since 2.0.8.5
 */
public class PassiveItem
       extends CoreTableColumn 
       implements TableCellRefreshListener
{
  /** Default Constructor */
  public PassiveItem() {
    super("passive", ALIGN_TRAIL, POSITION_LAST, 60, TableManager.TABLE_MYTRACKER);
  }

  public void refresh(TableCell cell) {
	  
    TRHostTorrent item = (TRHostTorrent)cell.getDataSource();
	
    String status_text = "";
    
    if( item != null ) {
  
     if( !cell.setSortValue( item.isPassive()?1:0 ) && cell.isValid() ) {
        return;
     }
	  
     if ( item.isPassive() ){
	     status_text = MessageText.getString( "Button.yes" );
     }else{
		 status_text = MessageText.getString( "Button.no" );
     }

    }
    
    cell.setText( status_text );
  }

}
