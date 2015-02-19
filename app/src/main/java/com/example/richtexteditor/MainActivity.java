package com.example.richtexteditor;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.example.richtexteditor.OpenDraftDialog.OpenDraftDialogListener;
import com.example.richtexteditor.db.TextDataSource;

/** 
 * Starting point for Rich Text Editing experience for Android.
 * 
 * MyTextEditorFragment and MyHtml do most of the string parsing and editing logic.
 * 
 * Modeled after Android API Html class and Wordpress Android editor.
 */
public class MainActivity extends Activity implements FragmentActionListener {

  TextDataSource datasource;
  FragmentManager mFragmentManager;
  Fragment mFragment;
  String mSavedTextBlock = ""; // should come from server.
  String mSavedTextBlockId = "xyz"; // should come from server.

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    datasource = new TextDataSource(this);
    datasource.open();

    if (savedInstanceState != null) {
      return;
    }
    
    if (datasource.hasTextBlock(mSavedTextBlockId)) {
      OpenDraftDialog dialog = OpenDraftDialog.newInstance(getOpenDraftDialogCallbackManager());
      dialog.show(getFragmentManager(), "OpenDraftDialog");
    } else {
      mFragment = MyTextEditorFragment.getInstance(mSavedTextBlock, mSavedTextBlockId);
      mFragment.setRetainInstance(true);
      showFragment();
    }
  }

  @Override
  protected void onResume() {
    super.onResume();

    if (! datasource.isOpen()) {
      datasource.open();
    }
  }

  @Override
  protected void onPause() {
    super.onPause();

    datasource.close();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();

    datasource.close();
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);

    hideFragment();
    showFragment();
  }

  @Override
  public void cancelTextEditor() {
    hideFragment();
  }

  @Override
  public void sendEditedText(String editedTextBlock, String editedTextBlockId) {
    hideFragment();
    // Do something with the edited text, e.g. save to server...
    Log.i(null, editedTextBlockId + ": " + editedTextBlock);
  }

  @Override
  public void draftEditedText(String editedTextBlock, String editedTextBlockId) {
    hideFragment();

    if (datasource.addTextBlock(editedTextBlockId, editedTextBlock)) {
      Toast toast = Toast.makeText(this, R.string.toast_text_draft_save_ok, Toast.LENGTH_SHORT);
      toast.setGravity(Gravity.CENTER, 0, 200);
      toast.show();
    } else {
      Toast toast = Toast.makeText(this, R.string.toast_text_draft_save_nok, Toast.LENGTH_SHORT);
      toast.setGravity(Gravity.CENTER, 0, 200);
      toast.show();
    }
  }

  @Override
  public void onBackPressed() {
      super.onBackPressed();
    //  hideFragment();
  }

  private void showFragment() {
    if (! (mFragment.isAdded())) {
      mFragmentManager = getFragmentManager();
      FragmentTransaction fragTransac = mFragmentManager.beginTransaction();
      fragTransac.add(R.id.container, mFragment);
      fragTransac.show(mFragment);
      fragTransac.commit();
    }
  }

  private void hideFragment() {
    if ((mFragment != null) && (mFragment.isAdded())) {
      FragmentTransaction fragTransac = mFragmentManager.beginTransaction();
      fragTransac.remove(mFragment);
      fragTransac.commit();
      mFragmentManager.executePendingTransactions();
    }    
  }
  
  /**
   * Gets the open draft dialog callback manager.
   * 
   * @return open draft dialog callback manager
   */
  private OpenDraftDialogListener getOpenDraftDialogCallbackManager()
  {
    return new OpenDraftDialogListener()
    {
      @Override
      public void onOpenDraftClick(DialogFragment dialog)
      {
        mFragment = MyTextEditorFragment.getInstance(datasource.getTextBlock(mSavedTextBlockId), mSavedTextBlockId);
        mFragment.setRetainInstance(true);
        datasource.deleteTextBlock(mSavedTextBlockId);
        showFragment();
      }

      @Override
      public void onCancelClick(DialogFragment dialog)
      {
        mFragment = MyTextEditorFragment.getInstance(mSavedTextBlock, mSavedTextBlockId);
        mFragment.setRetainInstance(true);
        datasource.deleteTextBlock(mSavedTextBlockId);
        showFragment();
      }
    };
  }
}