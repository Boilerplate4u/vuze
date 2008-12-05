/**
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.ui.swt.views.skin;

import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.views.tableitems.pieces.ReservedByItem;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.cnetwork.ContentNetwork;
import com.aelitis.azureus.core.cnetwork.ContentNetworkManager;
import com.aelitis.azureus.core.cnetwork.ContentNetworkManagerFactory;
import com.aelitis.azureus.core.messenger.ClientMessageContext;
import com.aelitis.azureus.core.messenger.config.PlatformConfigMessenger;
import com.aelitis.azureus.ui.skin.SkinConstants;
import com.aelitis.azureus.ui.swt.browser.BrowserContext;
import com.aelitis.azureus.ui.swt.browser.listener.TorrentListener;
import com.aelitis.azureus.ui.swt.browser.listener.VuzeListener;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.ui.swt.utils.ContentNetworkUI;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.*;
import com.aelitis.azureus.util.ConstantsV3;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginManager;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.menus.MenuManager;
import org.gudy.azureus2.plugins.ui.sidebar.SideBarVitalityImage;

/**
 * @author TuxPaper
 * @created Sep 30, 2006
 *
 */
public class Browse
	extends SkinView implements SideBarCloseListener
{
	private SWTSkinObjectBrowser browserSkinObject;

	public SWTSkinObjectBrowser getBrowserSkinObject() {
		return browserSkinObject;
	}

	private SWTSkin skin;

	private SWTSkinObject soMain;

	private SideBarVitalityImage vitalityImage;

	private ContentNetwork contentNetwork;

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.swt.views.SkinView#showSupport(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	 */
	public Object skinObjectInitialShow(SWTSkinObject skinObject, Object params) {
		this.soMain = skinObject;
		skin = skinObject.getSkin();
		Object creationParams = skinObject.getData("CreationParams");
		
		if (creationParams instanceof ContentNetwork) {
			contentNetwork = (ContentNetwork) creationParams;
		} else {
			contentNetwork = ConstantsV3.DEFAULT_CONTENT_NETWORK; 
		}

		
		browserSkinObject = SWTSkinUtils.findBrowserSO(soMain);
		
		final SideBar sidebar = (SideBar) SkinViewManager.getByClass(SideBar.class);
		if (sidebar != null) {
			final SideBarEntrySWT entry = sidebar.getSideBarEntry(this);
			if (entry != null) {
				
				entry.addListener(this);
				
				vitalityImage = entry.addVitalityImage("image.sidebar.vitality.dots");
				vitalityImage.setVisible(false);
				
				sidebar.addListener(new SideBarListener() {
					long lastSelect = 0;
					public void sidebarItemSelected(SideBarEntrySWT newSideBarEntry,
							SideBarEntrySWT oldSideBarEntry) {
						if (entry == newSideBarEntry) {
							if (entry == oldSideBarEntry) {
								if (lastSelect < SystemTime.getOffsetTime(-1000)) {
									if (browserSkinObject != null) {
										browserSkinObject.restart();
									}
								}
							} else {
								lastSelect = SystemTime.getCurrentTime();
							}
						}
					}
				});
			}
		}

		browserSkinObject.addListener(new BrowserContext.loadingListener() {
			public void browserLoadingChanged(boolean loading, String url) {
				if (vitalityImage != null) {
					vitalityImage.setVisible(loading);
				}
			}
		});

		createBrowseArea(browserSkinObject);
		
		if (org.gudy.azureus2.core3.util.Constants.isCVSVersion()) {
  		PluginManager pm = AzureusCoreFactory.getSingleton().getPluginManager();
  		PluginInterface pi = pm.getDefaultPluginInterface();
  		UIManager uim = pi.getUIManager();
  		MenuManager menuManager = uim.getMenuManager();
  		
  		String menuID = "sidebar." + ContentNetworkUI.getTarget(contentNetwork); 
  		
  		MenuItem menuItem = menuManager.addMenuItem(menuID, "Button.reset");
  		menuItem.addListener(new MenuItemListener() {
  			public void selected(MenuItem menu, Object target) {
  				browserSkinObject.restart();
  			}
  		});
  		
  		menuItem = menuManager.addMenuItem(menuID, "Tux RPC Test");
  		menuItem.addListener(new MenuItemListener() {
  			public void selected(MenuItem menu, Object target) {
  	  		browserSkinObject.setURL("c:\\test\\BrowserMessaging.html");
  			}
  		});
  		
  		if (contentNetwork != ConstantsV3.DEFAULT_CONTENT_NETWORK) {
    		menuItem = menuManager.addMenuItem(menuID, "Remove HD Network");
    		menuItem.addListener(new MenuItemListener() {
    			public void selected(MenuItem menu, Object target) {
    				if (sidebar != null) {
    					final SideBarEntrySWT entry = sidebar.getSideBarEntry(Browse.this);
    					if (entry != null) {
    						entry.removeListener(Browse.this);
    					}
    					sidebar.closeSideBar(ContentNetworkUI.getTarget(contentNetwork));
    				}
    				contentNetwork.remove();
    			}
    		});
  		}
  		
		}

		return null;
	}
	
	private void createBrowseArea(SWTSkinObjectBrowser browserSkinObject) {
		this.browserSkinObject = browserSkinObject;

		browserSkinObject.setURL(contentNetwork.getServiceURL(ContentNetwork.SERVICE_BIG_BROWSE));
	}

	public void sidebarClosed(SideBarEntrySWT entry) {
		
		Utils.openMessageBox(
				null,
				SWT.OK,
				"You closed a sidebar entry",
				"OMG OMG! Did you know you just closed a sidebar entry?\n\nYou can get it back by asking the frog gods");
	}
}
