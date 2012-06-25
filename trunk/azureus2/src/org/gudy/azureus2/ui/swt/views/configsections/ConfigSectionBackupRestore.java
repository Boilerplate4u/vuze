/*
 * File    : ConfigPanel*.java
 * Created : 11 mar. 2004
 * By      : TuxPaper
 * 
 * Copyright (C) 2004, 2005, 2006 Aelitis SAS, All rights Reserved
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

package org.gudy.azureus2.ui.swt.views.configsections;

import java.io.File;

import org.eclipse.swt.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.TextViewerWindow;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.config.BooleanParameter;
import org.gudy.azureus2.ui.swt.config.ChangeSelectionActionPerformer;
import org.gudy.azureus2.ui.swt.config.IntParameter;
import org.gudy.azureus2.ui.swt.config.StringParameter;
import org.gudy.azureus2.ui.swt.plugins.*;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.backup.BackupManager;
import com.aelitis.azureus.core.backup.BackupManagerFactory;
import com.aelitis.azureus.ui.UserPrompterResultListener;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;


public class ConfigSectionBackupRestore implements UISWTConfigSection {

	private final static String LBLKEY_PREFIX = "ConfigView.label.";

	public String configSectionGetParentSection() {
		return ConfigSection.SECTION_ROOT;
	}

	public String configSectionGetName() {
		return "backuprestore";
	}

	public void configSectionSave() {
	}

	public void configSectionDelete() {
		ImageLoader imageLoader = ImageLoader.getInstance();
		imageLoader.releaseImage("openFolderButton");
	}
	
	public int maxUserMode() {
		return 0;
	}


	public Composite 
	configSectionCreate(
		final Composite parent) 
	{
		ImageLoader imageLoader = ImageLoader.getInstance();
		Image imgOpenFolder = imageLoader.getImage("openFolderButton");
		
		GridData gridData;
		GridLayout layout;

		final Composite cBR = new Composite( parent, SWT.NULL );

		gridData = new GridData(GridData.VERTICAL_ALIGN_FILL |  GridData.HORIZONTAL_ALIGN_FILL);
		cBR.setLayoutData(gridData);
		layout = new GridLayout();
		layout.numColumns = 1;
		cBR.setLayout(layout);
				
	    Label	info_label = new Label( cBR, SWT.WRAP );
	    Messages.setLanguageText( info_label, "ConfigView.section.br.overview" );
	    gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL );
	    gridData.horizontalSpan = 1;
	    
	    info_label.setLayoutData( gridData );
	    
	    final BackupManager	backup_manager = BackupManagerFactory.getManager( AzureusCoreFactory.getSingleton());
	    
	    	// backup
	    
		Group gBackup = new Group(cBR, SWT.NULL);
		Messages.setLanguageText(gBackup, "br.backup");
		layout = new GridLayout(2, false);
		gBackup.setLayout(layout);
		gBackup.setLayoutData(new GridData( GridData.FILL_HORIZONTAL ));
	    
	    Label backup_manual_label = new Label(gBackup, SWT.NULL );
	    Messages.setLanguageText(backup_manual_label, "br.backup.manual.info");

	    Button backup_button = new Button(gBackup, SWT.PUSH);
	    Messages.setLanguageText(backup_button, "br.backup");
	    
	    backup_button.addListener(SWT.Selection, 
	    		new Listener() 
				{
			        public void 
					handleEvent(Event event) 
			        {
			        	String	def_dir = COConfigurationManager.getStringParameter( "br.backup.folder.default" );
			        	
						DirectoryDialog dialog = new DirectoryDialog(parent.getShell(),	SWT.APPLICATION_MODAL);
						
						if ( def_dir != null ){
							dialog.setFilterPath( def_dir );
						}
						
						dialog.setMessage(MessageText.getString("br.backup.folder.info"));
						dialog.setText(MessageText.getString("br.backup.folder.title"));
						
						String path = dialog.open();
						
						if ( path != null ){
																
							COConfigurationManager.setParameter( "br.backup.folder.default", path );
								
							runBackup( backup_manager, path );
						}
			        }
				});
	    
		final BooleanParameter auto_backup_enable = new BooleanParameter( gBackup, "br.backup.auto.enable", "br.backup.auto.enable" );
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		auto_backup_enable.setLayoutData( gridData );

		Composite gDefaultDir = new Composite(gBackup, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 3;
		layout.marginHeight = 2;
		gDefaultDir.setLayout(layout);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 2;
		gDefaultDir.setLayoutData(gridData);
		
		Label lblDefaultDir = new Label(gDefaultDir, SWT.NONE);
		Messages.setLanguageText(lblDefaultDir,	"ConfigView.section.file.defaultdir.ask");
		lblDefaultDir.setLayoutData(new GridData());


		gridData = new GridData(GridData.FILL_HORIZONTAL);
		final StringParameter pathParameter = new StringParameter(gDefaultDir, "br.backup.auto.dir", "" );
		pathParameter.setLayoutData(gridData);

		if ( pathParameter.getValue().length() == 0 ){
		   	String	def_dir = COConfigurationManager.getStringParameter( "br.backup.folder.default" );

			pathParameter.setValue( def_dir );
		}
		
		Button browse = new Button(gDefaultDir, SWT.PUSH);
		browse.setImage(imgOpenFolder);
		imgOpenFolder.setBackground(browse.getBackground());
		browse.setToolTipText(MessageText.getString("ConfigView.button.browse"));

		browse.addListener(SWT.Selection, new Listener() {
			/* (non-Javadoc)
			 * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
			 */
			public void handleEvent(Event event) {
				DirectoryDialog dialog = new DirectoryDialog(parent.getShell(),
						SWT.APPLICATION_MODAL);
				dialog.setFilterPath(pathParameter.getValue());
				dialog.setMessage(MessageText.getString("br.backup.auto.dir.select"));
				dialog.setText(MessageText.getString("ConfigView.section.file.defaultdir.ask"));
				String path = dialog.open();
				if (path != null) {
					pathParameter.setValue(path);
					
					COConfigurationManager.setParameter( "br.backup.folder.default", path );
				}
			}
		});
		
		
		Label lbl_backup_days = new Label(gDefaultDir, SWT.NULL);
		Messages.setLanguageText(lbl_backup_days, "br.backup.auto.everydays" );

		IntParameter backup_everydays = new IntParameter( gDefaultDir, "br.backup.auto.everydays", 1, Integer.MAX_VALUE );
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		backup_everydays.setLayoutData( gridData );
		
		Label lbl_backup_retain = new Label(gDefaultDir, SWT.NULL);
		Messages.setLanguageText(lbl_backup_retain, "br.backup.auto.retain" );

		IntParameter backup_retain = new IntParameter( gDefaultDir, "br.backup.auto.retain", 1, Integer.MAX_VALUE );
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		backup_retain.setLayoutData( gridData );
		
	    Label backup_auto_label = new Label(gDefaultDir, SWT.NULL );
	    Messages.setLanguageText(backup_auto_label, "br.backup.auto.now");

	    Button backup_auto_button = new Button(gDefaultDir, SWT.PUSH);
	    Messages.setLanguageText(backup_auto_button, "br.test");
	    
	    backup_auto_button.addListener(SWT.Selection, 
	    		new Listener() 
				{
			        public void 
					handleEvent(Event event) 
			        {
			        	runBackup( backup_manager, null );
			        }
				});
	    
		auto_backup_enable.setAdditionalActionPerformer(
			new ChangeSelectionActionPerformer( lblDefaultDir ));
					
		auto_backup_enable.setAdditionalActionPerformer(
				new ChangeSelectionActionPerformer( pathParameter ));

		auto_backup_enable.setAdditionalActionPerformer(
				new ChangeSelectionActionPerformer( browse ));

		auto_backup_enable.setAdditionalActionPerformer(
				new ChangeSelectionActionPerformer( lbl_backup_days ));

		auto_backup_enable.setAdditionalActionPerformer(
				new ChangeSelectionActionPerformer( backup_everydays ));

		auto_backup_enable.setAdditionalActionPerformer(
				new ChangeSelectionActionPerformer( lbl_backup_retain ));

		auto_backup_enable.setAdditionalActionPerformer(
				new ChangeSelectionActionPerformer( backup_retain ));

		auto_backup_enable.setAdditionalActionPerformer(
				new ChangeSelectionActionPerformer( backup_auto_label ));

		auto_backup_enable.setAdditionalActionPerformer(
				new ChangeSelectionActionPerformer( backup_auto_button ));

		
	    	// restore
	    
		Group gRestore = new Group(cBR, SWT.NULL);
		Messages.setLanguageText(gRestore, "br.restore");
		layout = new GridLayout(2, false);
		gRestore.setLayout(layout);
		gRestore.setLayoutData(new GridData( GridData.FILL_HORIZONTAL ));
		
	    Label restore_label = new Label(gRestore, SWT.NULL );
	    Messages.setLanguageText(restore_label, "br.restore.info");

	    Button restore_button = new Button(gRestore, SWT.PUSH);
	    Messages.setLanguageText(restore_button, "br.restore");

	    restore_button.addListener(SWT.Selection, 
	    		new Listener() 
				{
			        public void 
					handleEvent(Event event) 
			        {
			        	String	def_dir = COConfigurationManager.getStringParameter( "br.backup.folder.default" );
			        	
						DirectoryDialog dialog = new DirectoryDialog(parent.getShell(),	SWT.APPLICATION_MODAL );
						
						if ( def_dir != null ){
							dialog.setFilterPath( def_dir );
						}
						
						dialog.setMessage(MessageText.getString("br.restore.folder.info"));
						
						dialog.setText(MessageText.getString("br.restore.folder.title"));
						
						final String path = dialog.open();
						
						if ( path != null ){

				        	MessageBoxShell mb = new MessageBoxShell(
				        			SWT.ICON_WARNING | SWT.OK | SWT.CANCEL,
				        			MessageText.getString("br.restore.warning.title"),
				        			MessageText.getString("br.restore.warning.info"));
				        	
				        	mb.setDefaultButtonUsingStyle(SWT.CANCEL);
				        	mb.setParent(parent.getShell());
	
				        	mb.open(new UserPrompterResultListener() {
										public void prompterClosed(int returnVal) {
											if (returnVal != SWT.OK) {
												return;
											}
	
											final TextViewerWindow viewer = 
												new TextViewerWindow(
														MessageText.getString( "br.backup.progress" ),
														null, "", true, true );
															
											viewer.setEditable( false );

											viewer.setOKEnabled( false );
											
											backup_manager.restore(
												new File( path ),
												new BackupManager.BackupListener()
												{
													public boolean
													reportProgress(
														String		str )
													{
														return( append( str, false ));
													}
													
													public void
													reportComplete()
													{
														append( "Restore Complete!", true );	
														
														Utils.execSWTThread(
															new AERunnable() 
															{
																public void 
																runSupport() 
																{
																	MessageBoxShell mb = new MessageBoxShell( 
													        				SWT.ICON_INFORMATION | SWT.OK,
													        				MessageText.getString( "ConfigView.section.security.restart.title" ),
													        				MessageText.getString( "ConfigView.section.security.restart.msg" ));
												      				mb.setParent(parent.getShell());
												        			mb.open(
												        				new UserPrompterResultListener() 
												        				{
																			public void 
																			prompterClosed(
																				int returnVal) 
																			{		
																        		UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
																        		
																            	if ( uiFunctions != null ){
																            		
																            		uiFunctions.dispose(true, false);
																            	}
																			}
												        				});
																}
															});
													}
													
													public void
													reportError(
														Throwable 	error )
													{
														append( "Restore Failed: " + Debug.getNestedExceptionMessage( error ), true );
													}
													
													private boolean
													append(
														final String		str,
														final boolean		complete )
													{	
														if ( viewer.isDisposed()){
															
															return( false );
														}
														
														Utils.execSWTThread(
															new AERunnable() 
															{
																public void 
																runSupport() 
																{
																	if ( str.endsWith( "..." )){
																		
																		viewer.append( str );
																		
																	}else{
																	
																		viewer.append( str + "\r\n" );
																	}
																	
																	if ( complete ){
																		
																		viewer.setOKEnabled( true );
																	}
																}
															});
														
														return( true );
													}													
												});
											
											viewer.goModal();
												
										}
									});
						}
			        }
			    });
	    
		return( cBR );
	}
	
	private void
	runBackup(
		BackupManager	backup_manager,
		String			path )
		
	{
		final TextViewerWindow viewer = 
			new TextViewerWindow(
					MessageText.getString( "br.backup.progress" ),
					null, "", true, true );
						
		viewer.setEditable( false );
		
		viewer.setOKEnabled( false );
		
		BackupManager.BackupListener	listener = 
			new BackupManager.BackupListener()
			{
				public boolean
				reportProgress(
					String		str )
				{
					return( append( str, false ));
				}
				
				public void
				reportComplete()
				{
					append( "Backup Complete!", true );

				}
				
				public void
				reportError(
					Throwable 	error )
				{
					append( "Backup Failed: " + Debug.getNestedExceptionMessage( error ), true );
				}
				
				private boolean
				append(
					final String		str,
					final boolean		complete )
				{	
					if ( viewer.isDisposed()){
						
						return( false );
					}
					
					Utils.execSWTThread(
						new AERunnable() 
						{
							public void 
							runSupport() 
							{
								if ( str.endsWith( "..." )){
									
									viewer.append( str );
									
								}else{
								
									viewer.append( str + "\r\n" );
								}
								
								if ( complete ){
									
									viewer.setOKEnabled( true );
								}
							}
						});
					
					return( true );
				}
			};
			
		if ( path == null ){
			
			backup_manager.runAutoBackup( listener );
			
		}else{
		
			backup_manager.backup( new File( path ), listener );
		}
		
		viewer.goModal();
	}
}
