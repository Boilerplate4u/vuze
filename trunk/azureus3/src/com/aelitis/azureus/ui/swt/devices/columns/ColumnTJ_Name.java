/**
 * Created on Feb 26, 2009
 *
 * Copyright 2008 Vuze, Inc.  All rights reserved.
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA 
 */

package com.aelitis.azureus.ui.swt.devices.columns;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.ui.swt.debug.ObfusticateCellText;

import com.aelitis.azureus.core.devices.TranscodeFile;
import com.aelitis.azureus.core.devices.TranscodeJob;
import com.aelitis.azureus.util.DataSourceUtils;

import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadException;
import org.gudy.azureus2.plugins.ui.tables.*;

/**
 * @author TuxPaper
 * @created Feb 26, 2009
 *
 */
public class ColumnTJ_Name
	implements TableCellRefreshListener, ObfusticateCellText,
	TableCellDisposeListener
{
	public static final String COLUMN_ID = "transcode_name";

	/**
	 * 
	 * @param sTableID
	 */
	public ColumnTJ_Name(TableColumn column) {
		column.initialize(TableColumn.ALIGN_LEAD, TableColumn.POSITION_LAST, 250);
		column.addListeners(this);
		column.setObfustication(true);
		column.setRefreshInterval(TableColumn.INTERVAL_GRAPHIC);
		column.setType(TableColumn.TYPE_TEXT_ONLY);
		column.setMinWidth(100);
	}

	public void refresh(TableCell cell) {
		TranscodeFile tf = (TranscodeFile) cell.getDataSource();
		if (tf == null) {
			return;
		}
		TranscodeJob job = tf.getJob();

		String text;
		
		if (job == null) {
			try{
				DiskManagerFileInfo sourceFile = tf.getSourceFile();
				
				try {
					Download download = sourceFile.getDownload();
					if (download == null) {
						text = sourceFile.getFile().getName();
					} else {
						text = download.getName();
						DiskManagerFileInfo[] fileInfo = download.getDiskManagerFileInfo();
						if (fileInfo.length > 1) {
							text += ": " + sourceFile.getFile().getName();
						}
					}
				} catch (DownloadException e) {
					text = sourceFile.getFile().getName();
				}
			
			}catch( Throwable e ){
				// most likely been recently deleted, stick with existing text
				return;
			}
		} else {
			text = job.getName();
		}

		cell.setText(text);
	}

	public String getObfusticatedText(TableCell cell) {
		String name = null;
		DownloadManager dm = DataSourceUtils.getDM(cell.getDataSource());
		if (dm != null) {
			name = dm.toString();
			int i = name.indexOf('#');
			if (i > 0) {
				name = name.substring(i + 1);
			}
		}

		if (name == null)
			name = "";
		return name;
	}

	public void dispose(TableCell cell) {

	}
}
