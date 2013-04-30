/**
 * 
 */
package com.aelitis.azureus.ui.swt.columns.torrent;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Display;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.UrlUtils;
import org.gudy.azureus2.plugins.download.DownloadTypeIncomplete;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemFillListener;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.mainwindow.SWTThread;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter;
import org.gudy.azureus2.ui.swt.views.MyTorrentsView;
import org.gudy.azureus2.ui.swt.views.ViewUtils;
import org.gudy.azureus2.ui.swt.views.table.CoreTableColumnSWT;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener;

import com.aelitis.azureus.core.download.DownloadManagerEnhancer;
import com.aelitis.azureus.core.download.EnhancedDownloadManager;
import com.aelitis.azureus.ui.common.table.TableRowCore;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;
import com.aelitis.azureus.ui.swt.skin.SWTSkinFactory;
import com.aelitis.azureus.ui.swt.skin.SWTSkinProperties;
import com.aelitis.azureus.ui.swt.utils.FontUtils;

/**
 * @author TuxPaper
 * @created Jun 13, 2006
 *
 */
public class ColumnProgressETA
	extends CoreTableColumnSWT
	implements TableCellAddedListener, TableCellMouseListener,
	TableCellRefreshListener, TableCellSWTPaintListener
{
	public static final Class DATASOURCE_TYPE = DownloadTypeIncomplete.class;

	public static final String COLUMN_ID = "ProgressETA";

	private static final int borderWidth = 1;

	private static final int COLUMN_WIDTH = 200;

	public static final long SHOW_ETA_AFTER_MS = 30000;

	private final static Object CLICK_KEY = new Object();

	protected static final String CFG_SHOWETA = "ColumnProgressETA.showETA";

	protected static final String CFG_SHOWSPEED = "ColumnProgressETA.showSpeed";

	private static Font fontText = null;

	Display display;

	private Color cBGdl;

	private Color cBGcd;

	private Color cBorder;

	private Color cText;

	Color textColor;

	private Image imgBGTorrent;

	private Color cTextDrop;

	private ViewUtils.CustomDateFormat cdf;

	private ColumnTorrentFileProgress fileProgress;

	protected boolean showETA;

	protected boolean showSpeed;

	/**
	 * 
	 */
	public ColumnProgressETA(String sTableID) {
		super(DATASOURCE_TYPE, COLUMN_ID, ALIGN_CENTER, COLUMN_WIDTH, sTableID);
		addDataSourceType(DiskManagerFileInfo.class);
		initializeAsGraphic(COLUMN_WIDTH);
		setAlignment(ALIGN_LEAD);
		setMinWidth(COLUMN_WIDTH);

		display = SWTThread.getInstance().getDisplay();

		SWTSkinProperties skinProperties = SWTSkinFactory.getInstance().getSkinProperties();
		cBGdl = skinProperties.getColor("color.progress.bg.dl");
		if (cBGdl == null) {
			cBGdl = Colors.blues[Colors.BLUES_DARKEST];
		}
		cBGcd = skinProperties.getColor("color.progress.bg.cd");
		if (cBGcd == null) {
			cBGcd = Colors.green;
		}
		cBorder = skinProperties.getColor("color.progress.border");
		if (cBorder == null) {
			cBorder = Colors.grey;
		}
		cText = skinProperties.getColor("color.progress.text");
		if (cText == null) {
			cText = Colors.black;
		}
		cTextDrop = skinProperties.getColor("color.progress.text.drop");

		cdf = ViewUtils.addCustomDateFormat(this);

		ImageLoader imageLoader = ImageLoader.getInstance();
		imgBGTorrent = imageLoader.getImage("image.progress.bg.torrent");

		fileProgress = new ColumnTorrentFileProgress(display);

		TableContextMenuItem menuShowETA = addContextMenuItem(
				"ColumnProgressETA.showETA", MENU_STYLE_HEADER);
		menuShowETA.setStyle(TableContextMenuItem.STYLE_CHECK);
		menuShowETA.addFillListener(new MenuItemFillListener() {
			public void menuWillBeShown(MenuItem menu, Object data) {
				menu.setData(new Boolean(showETA));
			}
		});
		menuShowETA.addMultiListener(new MenuItemListener() {
			public void selected(MenuItem menu, Object target) {
				showETA = ((Boolean) menu.getData()).booleanValue();
				setUserData(CFG_SHOWETA, showETA ? 1 : 0);
			}
		});

		TableContextMenuItem menuShowSpeed = addContextMenuItem(
				"ColumnProgressETA.showSpeed", MENU_STYLE_HEADER);
		menuShowSpeed.setStyle(TableContextMenuItem.STYLE_CHECK);
		menuShowSpeed.addFillListener(new MenuItemFillListener() {
			public void menuWillBeShown(MenuItem menu, Object data) {
				menu.setData(new Boolean(showSpeed));
			}
		});
		menuShowSpeed.addMultiListener(new MenuItemListener() {
			public void selected(MenuItem menu, Object target) {
				showSpeed = ((Boolean) menu.getData()).booleanValue();
				setUserData(CFG_SHOWSPEED, showSpeed ? 1 : 0);
			}
		});

	}
	
	

	public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] {
			CAT_CONTENT,
			CAT_ESSENTIAL,
			CAT_TIME,
		});
		info.setProficiency(TableColumnInfo.PROFICIENCY_BEGINNER);
	}

	public void cellAdded(TableCell cell) {
		cell.setMarginHeight(3);
		cell.setMarginWidth(8);
	}

	public void cellMouseTrigger(TableCellMouseEvent event) {

		Object ds = event.cell.getDataSource();
		if (ds instanceof DiskManagerFileInfo) {
			fileProgress.fileInfoMouseTrigger(event);
			return;
		}

		DownloadManager dm = (DownloadManager) ds;
		if (dm == null) {
			return;
		}

		String clickable = (String) dm.getUserData(CLICK_KEY);

		if (clickable == null) {

			return;
		}

		event.skipCoreFunctionality = true;

		if (event.eventType == TableCellMouseEvent.EVENT_MOUSEUP) {

			String url = UrlUtils.getURL(clickable);

			if (url != null) {

				Utils.launch(url);
			}
		}
	}

	public void refresh(TableCell cell) {
		Object ds = cell.getDataSource();

		int percentDone = getPercentDone(ds);

		long sortValue = 0;

		if (ds instanceof DownloadManager) {
			DownloadManager dm = (DownloadManager) cell.getDataSource();

			long completedTime = dm.getDownloadState().getLongParameter(
					DownloadManagerState.PARAM_DOWNLOAD_COMPLETED_TIME);
			if (completedTime <= 0 || !dm.isDownloadComplete(false)) {
				sortValue = Long.MAX_VALUE - ((10000 + percentDone) << 2 + dm.getState());
			} else {
				sortValue = completedTime << 2 + dm.getState();
			}
		} else if (ds instanceof DiskManagerFileInfo) {
			DiskManagerFileInfo fileInfo = (DiskManagerFileInfo) ds;
			int st = fileInfo.getStorageType();
			if ((st == DiskManagerFileInfo.ST_COMPACT || st == DiskManagerFileInfo.ST_REORDER_COMPACT)
					&& fileInfo.isSkipped()) {
				sortValue = 1;
			} else if (fileInfo.isSkipped()) {
				sortValue = 2;
			} else if (fileInfo.getPriority() > 0) {

				int pri = fileInfo.getPriority();
				sortValue = 4;

				if (pri > 1) {
					sortValue += pri;
				}
			} else {
				sortValue = 3;
			}
			sortValue = (fileInfo.getDownloadManager().getState() * 10000)
					+ percentDone + sortValue;
		}

		long eta = showETA ? getETA(cell) : 0;
		long speed = showSpeed ? getSpeed(ds) : 0;

		//System.out.println("REFRESH " + sortValue + ";" + ds);
		boolean sortChanged = cell.setSortValue(sortValue);

		if (sortChanged) {
			UIFunctionsManagerSWT.getUIFunctionsSWT().refreshIconBar();
		}

		long lastETA = 0;
		long lastSpeed = 0;
		TableRow row = cell.getTableRow();
		if (row != null) {
			if (showETA) {
				Object data = row.getData("lastETA");
				if (data instanceof Number) {
					lastETA = ((Number) data).longValue();
				}
				row.setData("lastETA", new Long(eta));
			}
			if (showSpeed) {
				Object data = row.getData("lastSpeed");
				if (data instanceof Number) {
					lastSpeed = ((Number) data).longValue();
				}
				row.setData("lastSpeed", new Long(speed));
			}
		}

		if (!sortChanged && (lastETA != eta || lastSpeed != speed)) {
			cell.invalidate();
		}
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener#cellPaint(org.eclipse.swt.graphics.GC, org.gudy.azureus2.ui.swt.views.table.TableCellSWT)
	public void cellPaint(GC gc, TableCellSWT cell) {
		Object ds = cell.getDataSource();
		if (ds instanceof DiskManagerFileInfo) {
			fileProgress.fillInfoProgressETA(cell.getTableRowCore(), gc,
					(DiskManagerFileInfo) ds, cell.getBounds());
			return;
		}

		if (!(ds instanceof DownloadManager)) {
			return;
		}

		DownloadManager dm = (DownloadManager) cell.getDataSource();

		int percentDone = getPercentDone(ds);
		long eta = showETA ? getETA(cell) : 0;

		//Compute bounds ...
		int newWidth = cell.getWidth();
		if (newWidth <= 0) {
			return;
		}
		int newHeight = cell.getHeight();

		Color fgFirst = gc.getForeground();

		final Color fgOriginal = fgFirst;

		Rectangle cellBounds = cell.getBounds();

		int xStart = cellBounds.x;
		int yStart = cellBounds.y;

		int xRelProgressFillStart = borderWidth;
		int yRelProgressFillStart = borderWidth;
		int xRelProgressFillEnd = newWidth - xRelProgressFillStart - borderWidth;
		int yRelProgressFillEnd = yRelProgressFillStart + 13;
		boolean showSecondLine = yRelProgressFillEnd + 10 < newHeight;

		if (xRelProgressFillEnd < 10 || xRelProgressFillEnd < 10) {
			return;
		}
		String sStatusLine = null;

		// Draw Progress bar
		ImageLoader imageLoader = ImageLoader.getInstance();

		Rectangle boundsImgBG;

		if (!ImageLoader.isRealImage(imgBGTorrent)) {
			boundsImgBG = new Rectangle(0, 0, 0, 13);
		} else {
			boundsImgBG = imgBGTorrent.getBounds();
		}

		if (fontText == null) {
			fontText = FontUtils.getFontWithHeight(gc.getFont(), gc,
					boundsImgBG.height - 3);
		}

		if (!showSecondLine) {
			yRelProgressFillStart = (cellBounds.height / 2)
					- ((boundsImgBG.height) / 2);
		}

		yRelProgressFillEnd = yRelProgressFillStart + boundsImgBG.height;

		int progressWidth = newWidth - 2;
		gc.setForeground(cBorder);
		gc.drawRectangle(xStart + xRelProgressFillStart - 1, yStart
				+ yRelProgressFillStart - 1, progressWidth + 1, boundsImgBG.height + 1);

		int pctWidth = (int) (percentDone * (progressWidth) / 1000);
		gc.setBackground(percentDone == 1000 || dm.isDownloadComplete(false) ? cBGcd : cBGdl);
		gc.fillRectangle(xStart + xRelProgressFillStart, yStart
				+ yRelProgressFillStart, pctWidth, boundsImgBG.height);
		if (progressWidth > pctWidth) {
			gc.setBackground(Colors.white);
			gc.fillRectangle(xStart + xRelProgressFillStart + pctWidth, yStart
					+ yRelProgressFillStart, progressWidth - pctWidth, boundsImgBG.height);
		}

		if (boundsImgBG.width > 0) {
			gc.drawImage(imgBGTorrent, 0, 0, boundsImgBG.width, boundsImgBG.height,
					xStart + xRelProgressFillStart, yStart + yRelProgressFillStart,
					progressWidth, boundsImgBG.height);
		}

		if (sStatusLine == null) {
			if (dm.isUnauthorisedOnTracker()) {
				sStatusLine = dm.getTrackerStatus();
				// fgFirst = Colors.colorError;	pftt, no colours allowed apparently
			} else {
				if (showETA && eta > 0) {
					String sETA = ViewUtils.formatETA(eta,
							MyTorrentsView.progress_eta_absolute, cdf.getDateFormat());
					sStatusLine = MessageText.getString(
							"MyTorrents.column.ColumnProgressETA.2ndLine", new String[] {
								sETA
							});
				} else {
					sStatusLine = DisplayFormatters.formatDownloadStatus(dm).toUpperCase();
				}
			}

			int cursor_id;

			if (sStatusLine != null && sStatusLine.indexOf("http://") == -1) {

				dm.setUserData(CLICK_KEY, null);

				cursor_id = SWT.CURSOR_ARROW;

			} else {

				dm.setUserData(CLICK_KEY, sStatusLine);

				cursor_id = SWT.CURSOR_HAND;

				if (!cell.getTableRow().isSelected()) {

					fgFirst = Colors.blue;
				}
			}

			((TableCellSWT) cell).setCursorID(cursor_id);
		}

		gc.setTextAntialias(SWT.ON);
		gc.setFont(fontText);
		if (showSecondLine && sStatusLine != null) {
			gc.setForeground(fgFirst);
			boolean over = GCStringPrinter.printString(gc, sStatusLine,
					new Rectangle(cellBounds.x, yStart + yRelProgressFillEnd,
							cellBounds.width, newHeight - yRelProgressFillEnd), true,
					false, SWT.CENTER);
			cell.setToolTip(over ? sStatusLine : null);
			gc.setForeground(fgOriginal);
		}

		String sSpeed = "";
		if (showSpeed) {
			long lSpeed = getSpeed(ds);
			if (lSpeed > 0) {
				sSpeed = " ("
						+ DisplayFormatters.formatByteCountToKiBEtcPerSec(lSpeed, true)
						+ ")";
			}
		}

		String sPercent = DisplayFormatters.formatPercentFromThousands(percentDone);

		Rectangle area = new Rectangle(xStart + xRelProgressFillStart + 3, yStart
				+ yRelProgressFillStart, xRelProgressFillEnd - xRelProgressFillStart
				- 6, yRelProgressFillEnd - yRelProgressFillStart);
		GCStringPrinter sp = new GCStringPrinter(gc, sPercent + sSpeed, area, true,
				false, SWT.LEFT);
		if (cTextDrop != null) {
			area.x++;
			area.y++;
			gc.setForeground(cTextDrop);
			sp.printString();
			area.x--;
			area.y--;
		}
		gc.setForeground(cText);
		sp.printString();
		Point pctExtent = sp.getCalculatedSize();

		area.width -= (pctExtent.x + 3);
		area.x += (pctExtent.x + 3);

		if (!showSecondLine && sStatusLine != null) {
			boolean fit = GCStringPrinter.printString(gc, sStatusLine,
					area.intersection(cellBounds), true, false, SWT.RIGHT);
			cell.setToolTip(fit ? null : sStatusLine);
		}

		gc.setFont(null);
	}

	private int getPercentDone(Object ds) {
		if (ds instanceof DownloadManager) {
			return ((DownloadManager) ds).getStats().getDownloadCompleted(true);
		} else if (ds instanceof DiskManagerFileInfo) {
			DiskManagerFileInfo fileInfo = (DiskManagerFileInfo) ds;
			long length = fileInfo.getLength();
			if (length == 0) {
				return 1000;
			}
			return (int) (fileInfo.getDownloaded() * 1000 / length);
		}
		return 0;
	}

	private long getETA(TableCell cell) {
		Object ds = cell.getDataSource();
		if (ds instanceof DiskManagerFileInfo) {
			return 0;
		}
		DownloadManager dm = (DownloadManager) cell.getDataSource();

		long diff = SystemTime.getCurrentTime() - dm.getStats().getTimeStarted();
		if (diff > SHOW_ETA_AFTER_MS) {
			return dm.getStats().getETA();
		}
		return 0;
	}

	private int getState(TableCell cell) {
		DownloadManager dm = (DownloadManager) cell.getDataSource();
		if (dm == null) {
			return DownloadManager.STATE_ERROR;
		}
		return dm.getState();
	}

	private boolean isStopped(TableCell cell) {
		int state = getState(cell);
		return state == DownloadManager.STATE_QUEUED
				|| state == DownloadManager.STATE_STOPPED
				|| state == DownloadManager.STATE_STOPPING
				|| state == DownloadManager.STATE_ERROR;
	}

	private long getSpeed(Object ds) {
		if (!(ds instanceof DownloadManager)) {
			return 0;
		}

		return ((DownloadManager) ds).getStats().getDataReceiveRate();
	}

	public EnhancedDownloadManager getEDM(DownloadManager dm) {
		DownloadManagerEnhancer dmEnhancer = DownloadManagerEnhancer.getSingleton();
		if (dmEnhancer == null) {
			return null;
		}
		return dmEnhancer.getEnhancedDownload(dm);
	}

	private void log(TableCell cell, String s) {
		System.out.println(((TableRowCore) cell.getTableRow()).getIndex() + ":"
				+ System.currentTimeMillis() + ": " + s);
	}

	public void postConfigLoad() {
		super.postConfigLoad();

		Object oShowETA = getUserData(CFG_SHOWETA);
		if (oShowETA == null) {
			showETA = false; // we could read a global default from somewhere
		} else if (oShowETA instanceof Number) {
			showETA = ((Number) oShowETA).intValue() == 1;
		}

		Object oShowSpeed = getUserData(CFG_SHOWSPEED);
		if (oShowSpeed == null) {
			showSpeed = false; // we could read a global default from somewhere
		} else if (oShowSpeed instanceof Number) {
			showSpeed = ((Number) oShowSpeed).intValue() == 1;
		}

		cdf.update();
	}
}
