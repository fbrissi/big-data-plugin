/*! ******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2017 by Hitachi Vantara : http://www.pentaho.com
 *
 *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package org.pentaho.big.data.kettle.plugins.formats.impl.parquet.input;


import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.pentaho.big.data.api.initializer.ClusterInitializationException;
import org.pentaho.big.data.kettle.plugins.formats.FormatInputOutputField;
import org.pentaho.big.data.kettle.plugins.formats.impl.parquet.BaseParquetStepDialog;
import org.pentaho.big.data.kettle.plugins.formats.parquet.input.ParquetInputField;
import org.pentaho.big.data.kettle.plugins.formats.parquet.input.ParquetInputMetaBase;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.row.value.ValueMetaFactory;
import org.pentaho.di.core.util.Utils;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.TransPreviewFactory;
import org.pentaho.di.ui.core.dialog.EnterNumberDialog;
import org.pentaho.di.ui.core.dialog.EnterTextDialog;
import org.pentaho.di.ui.core.dialog.PreviewRowsDialog;
import org.pentaho.di.ui.core.gui.GUIResource;
import org.pentaho.di.ui.core.widget.ColumnInfo;
import org.pentaho.di.ui.core.widget.ColumnsResizer;
import org.pentaho.di.ui.core.widget.TableView;
import org.pentaho.di.ui.trans.dialog.TransPreviewProgressDialog;
import org.pentaho.hadoop.shim.api.format.IParquetInputField;
import org.pentaho.hadoop.shim.api.format.SchemaDescription;

public class ParquetInputDialog extends BaseParquetStepDialog<ParquetInputMeta> {

  private static final Class<?> PKG = ParquetInputMeta.class;

  private static final int DIALOG_WIDTH = 526;

  private static final int DIALOG_HEIGHT = 506;

  private static final int PARQUET_PATH_COLUMN_INDEX = 1;

  private static final int FIELD_NAME_COLUMN_INDEX = 2;

  private static final int FIELD_TYPE_COLUMN_INDEX = 3;

  private TableView wInputFields;

  public ParquetInputDialog( Shell parent, Object in, TransMeta transMeta, String sname ) {
    super( parent, (ParquetInputMeta) in, transMeta, sname );
  }

  @Override
  protected Control createAfterFile( Composite shell ) {

    Button wGetFields = new Button( shell, SWT.PUSH );
    wGetFields.setText( BaseMessages.getString( PKG, "ParquetInputDialog.Fields.Get" ) );
    wGetFields.addListener( SWT.Selection, event -> {
      try {
        getFields( );
      } catch ( ClusterInitializationException ex ) {
        if ( !BaseParquetStepDialog.checkForNonActiveShim( ex ) ) {
          throw new RuntimeException( ex );
        }
      } catch ( Exception ex ) {
        throw new RuntimeException( ex );
      }
    } );
    props.setLook( wGetFields );
    new FD( wGetFields ).bottom( 100, 0 ).right( 100, 0 ).apply();

    Label wlFields = new Label( shell, SWT.RIGHT );
    wlFields.setText( BaseMessages.getString( PKG, "ParquetInputDialog.Fields.Label" ) );
    props.setLook( wlFields );
    new FD( wlFields ).left( 0, 0 ).top( 0, FIELDS_SEP ).apply();
    List<ParquetInputField> fields = meta.getInputFields();
    int nrRows = fields == null ? 0 : fields.size();
    ColumnInfo[] parameterColumns = new ColumnInfo[] {
      new ColumnInfo( BaseMessages.getString( PKG, "ParquetInputDialog.Fields.column.AvroPath" ),
          ColumnInfo.COLUMN_TYPE_TEXT, false, false ),
      new ColumnInfo( BaseMessages.getString( PKG, "ParquetInputDialog.Fields.column.Name" ),
          ColumnInfo.COLUMN_TYPE_TEXT, false, false ),
      new ColumnInfo( BaseMessages.getString( PKG, "ParquetInputDialog.Fields.column.Type" ),
          ColumnInfo.COLUMN_TYPE_CCOMBO, ValueMetaFactory.getValueMetaNames() ) };
    wInputFields =
        new TableView( transMeta, shell, SWT.FULL_SELECTION | SWT.SINGLE | SWT.BORDER | SWT.NO_SCROLL | SWT.V_SCROLL,
            parameterColumns, nrRows, lsMod, props );
    props.setLook( wInputFields );
    new FD( wInputFields ).left( 0, 0 ).right( 100, 0 ).top( wlFields, FIELD_LABEL_SEP )
      .bottom( wGetFields, -FIELDS_SEP ).apply();

    for ( ColumnInfo col : parameterColumns ) {
      col.setAutoResize( false );
    }
    ColumnsResizer resizer = new ColumnsResizer( 0, 50, 25, 25 );
    wInputFields.getTable().addListener( SWT.Resize, resizer );
    setTruncatedColumn( wInputFields.getTable(), 1 );
    if ( !Const.isWindows() ) {
      addColumnTooltip( wInputFields.getTable(), 1 );
    }
    return wGetFields;
  }

  /**
   * Read the data from the meta object and show it in this dialog.
   */
  @Override
  protected void getData( ParquetInputMeta meta ) {
    if ( meta.inputFiles.fileName.length > 0 ) {
      wPath.setText( meta.inputFiles.fileName[0] );
    }
    int nrFields = meta.getInputFields().size();
    for ( int i = 0; i < nrFields; i++ ) {
      TableItem item = null;
      if ( i < wInputFields.table.getItemCount() ) {
        item = wInputFields.table.getItem( i );
      } else {
        item = new TableItem( wInputFields.table, SWT.NONE );
      }

      ParquetInputField inputField = meta.getInputFields().get( i );
      if ( inputField.getFormatFieldName() != null ) {
        item.setText( PARQUET_PATH_COLUMN_INDEX, inputField.getFormatFieldName() );
      }
      if ( inputField.getPentahoFieldName() != null ) {
        item.setText( FIELD_NAME_COLUMN_INDEX, inputField.getPentahoFieldName() );
      }
      item.setText( FIELD_TYPE_COLUMN_INDEX, inputField.getTypeDesc() );
    }
  }

  protected void getFields() throws Exception {
    List<? extends IParquetInputField> schema =
        ParquetInput.retrieveSchema( meta.namedClusterServiceLocator, meta.getNamedCluster(), wPath.getText().trim() );

    if ( schema.isEmpty() ) {
      return;
    }
    Set<String> existKeys = new TreeSet<>();
    if ( wInputFields.table.getItemCount() > 0 ) {
      MessageDialog dialog = getFieldsChoiceDialog( shell, schema.size() );
      int idx = dialog.open();
      int choice = idx & 0xFF;
      switch ( choice ) {
        case 3:
        case 255:
          return; // cancel
        case 2:
          wInputFields.table.removeAll();
          break;
        case 0:
          for ( int i = 0; i < wInputFields.table.getItemCount(); i++ ) {
            TableItem tableItem = wInputFields.table.getItem( i );
            String key = tableItem.getText( FIELD_NAME_COLUMN_INDEX );
            if ( !Utils.isEmpty( key ) ) {
              existKeys.add( key );
            }
          }
          break;
      }
    }
    for ( IParquetInputField f : schema ) {
      if ( existKeys.contains( f.getFormatFieldName() ) ) {
        continue;
      }
      TableItem item = new TableItem( wInputFields.table, SWT.NONE );

      item.setText( PARQUET_PATH_COLUMN_INDEX, f.getFormatFieldName() );
      item.setText( FIELD_NAME_COLUMN_INDEX, f.getPentahoFieldName() );
      item.setText( FIELD_TYPE_COLUMN_INDEX, ValueMetaFactory.getValueMetaName( f.getParquetType().getPdiType() ) );
    }

    meta.setChanged();
  }

  /**
   * Fill meta object from UI options.
   */
  @Override
  protected void getInfo( ParquetInputMeta meta, boolean preview ) {
    String filePath = wPath.getText();
    if ( filePath != null && !filePath.isEmpty() ) {
      meta.allocateFiles( 1 );
      meta.inputFiles.fileName[0] = wPath.getText().trim();
    }
    int nrFields = wInputFields.nrNonEmpty();
    meta.setInputFields( new ArrayList<>(  )  );
    for ( int i = 0; i < nrFields; i++ ) {
      TableItem item = wInputFields.getNonEmpty( i );
      ParquetInputField field = new ParquetInputField();
      field.setFormatFieldName( item.getText( PARQUET_PATH_COLUMN_INDEX ) );
      field.setPentahoFieldName( item.getText( FIELD_NAME_COLUMN_INDEX ) );
      field.setPentahoType( ValueMetaFactory.getIdForValueMeta( item.getText( FIELD_TYPE_COLUMN_INDEX ) ) );
      meta.getInputFields().add( field );
    }
  }

  @Override
  protected int getWidth() {
    return DIALOG_WIDTH;
  }

  @Override
  protected int getHeight() {
    return DIALOG_HEIGHT;
  }

  @Override
  protected String getStepTitle() {
    return BaseMessages.getString( PKG, "ParquetInputDialog.Shell.Title" );
  }

  @Override
  protected Listener getPreview() {
    // Preview the data
    return event -> {
      // Create the XML input step
      ParquetInputMetaBase oneMeta = (ParquetInputMeta) meta.clone();
      oneMeta.allocateFiles( 1 );
      oneMeta.inputFiles.fileName[0] = wPath.getText().trim();

      try {
        List<? extends IParquetInputField> schema =
            ParquetInput.retrieveSchema( meta.namedClusterServiceLocator, meta.getNamedCluster(),
              oneMeta.inputFiles.fileName[0] );
        meta.setInputFields( new ArrayList<>(  ) );
        for ( IParquetInputField f : schema ) {
          ParquetInputField fo = new ParquetInputField();
          fo.setFormatFieldName( f.getFormatFieldName() );
          fo.setFormatFieldName( f.getFormatFieldName() );
          fo.setPentahoType( f.getPentahoType() );
          meta.getInputFields().add( fo );
        }
      } catch ( Exception ex ) {
        throw new RuntimeException( ex );
      }

      TransMeta previewMeta =
          TransPreviewFactory.generatePreviewTransformation( transMeta, oneMeta, wStepname.getText() );

      EnterNumberDialog numberDialog =
          new EnterNumberDialog( shell, props.getDefaultPreviewSize(),
              BaseMessages.getString( PKG, "ParquetInputDialog.PreviewSize.DialogTitle" ),
              BaseMessages.getString( PKG, "ParquetInputDialog.PreviewSize.DialogMessage" ) );
      int previewSize = numberDialog.open();
      if ( previewSize > 0 ) {
        TransPreviewProgressDialog progressDialog =
            new TransPreviewProgressDialog( shell, previewMeta, new String[] {
              wStepname.getText() },
                new int[] {
                  previewSize } );
        progressDialog.open();

        Trans trans = progressDialog.getTrans();
        String loggingText = progressDialog.getLoggingText();

        if ( !progressDialog.isCancelled() ) {
          if ( trans.getResult() != null && trans.getResult().getNrErrors() > 0 ) {
            EnterTextDialog etd =
                new EnterTextDialog( shell, BaseMessages.getString( PKG, "System.Dialog.PreviewError.Title" ),
                    BaseMessages.getString( PKG, "System.Dialog.PreviewError.Message" ), loggingText, true );
            etd.setReadOnly();
            etd.open();
          }
        }

        PreviewRowsDialog prd =
            new PreviewRowsDialog( shell, transMeta, SWT.NONE, wStepname.getText(),
                progressDialog.getPreviewRowsMeta( wStepname.getText() ),
                progressDialog.getPreviewRows( wStepname.getText() ), loggingText );
        prd.open();
      }
    };
  }

  static MessageDialog getFieldsChoiceDialog( Shell shell, int newFields ) {
    MessageDialog messageDialog =
      new MessageDialog( shell,
        BaseMessages.getString( PKG, "ParquetInput.GetFieldsChoice.Title" ), // "Warning!"
        null,
        BaseMessages.getString( PKG, "ParquetInput.GetFieldsChoice.Message", "" + newFields ),
        MessageDialog.WARNING, new String[] {
        BaseMessages.getString( PKG, "ParquetInput.GetFieldsChoice.AddNew" ),
        BaseMessages.getString( PKG, "ParquetInput.GetFieldsChoice.Add" ),
        BaseMessages.getString( PKG, "ParquetInput.GetFieldsChoice.ClearAndAdd" ),
        BaseMessages.getString( PKG, "ParquetInput.GetFieldsChoice.Cancel" ), }, 0 ) {

        public void create() {
          super.create();
          getShell().setBackground( GUIResource.getInstance().getColorWhite() );
        }

        protected Control createMessageArea( Composite composite ) {
          Control control = super.createMessageArea( composite );
          imageLabel.setBackground( GUIResource.getInstance().getColorWhite() );
          messageLabel.setBackground( GUIResource.getInstance().getColorWhite() );
          return control;
        }

        protected Control createDialogArea( Composite parent ) {
          Control control = super.createDialogArea( parent );
          control.setBackground( GUIResource.getInstance().getColorWhite() );
          return control;
        }

        protected Control createButtonBar( Composite parent ) {
          Control control = super.createButtonBar( parent );
          control.setBackground( GUIResource.getInstance().getColorWhite() );
          return control;
        }
      };
    MessageDialog.setDefaultImage( GUIResource.getInstance().getImageSpoon() );
    return messageDialog;
  }
}
