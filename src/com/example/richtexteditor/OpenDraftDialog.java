/* ***************************************
 * Copyright 2013 Simple Different Co Ltd.
 * 
 *          All Rights Reserved 
 *************************************** */
package com.example.richtexteditor;

import com.example.richtexteditor.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

/**
 * Class OpenDraftDialog.
 */
public class OpenDraftDialog extends DialogFragment
{
  /**
   * The listener interface for receiving openDraftDialog events. Class that is
   * interested in processing a openDraftDialog event implements this interface,
   * and the object created with that class is registered with a component using
   * the component's <code>addOpenDraftDialogListener<code> method. When
   * openDraftDialog event occurs, that object's appropriate
   * method is invoked.
   * 
   * @see OpenDraftDialogEvent
   */
  public interface OpenDraftDialogListener
  {
    /**
     * Open draft click.
     * 
     * @param dialog
     *          the dialog
     */
    public void onOpenDraftClick(DialogFragment dialog);
    /**
     * Cancel click.
     * 
     * @param dialog
     */
    public void onCancelClick(DialogFragment dialog);
  }

  private OpenDraftDialogListener mListener;

  /**
   * New instance.
   * 
   * @param listener
   * @return open draft dialog
   */
  public static OpenDraftDialog newInstance(OpenDraftDialogListener listener)
  {
    OpenDraftDialog dialog = new OpenDraftDialog();
    dialog.mListener = listener;

    return dialog;
  }

  /* (non-Javadoc)
   * @see android.app.DialogFragment#onCreateDialog(android.os.Bundle)
   */
  public Dialog onCreateDialog(Bundle savedInstanceState)
  {
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

    StringBuilder messageBuilder = new StringBuilder(getActivity().getResources().getString(R.string.te_draft_dialog_main));

    builder.setPositiveButton(R.string.gen_yes, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id)
      {
        mListener.onOpenDraftClick(OpenDraftDialog.this);
      }
    });

    builder.setNegativeButton(R.string.gen_no, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id)
      {
        mListener.onCancelClick(OpenDraftDialog.this);
      }
    });

    builder.setMessage(messageBuilder.toString());

    return builder.create();
  }
}