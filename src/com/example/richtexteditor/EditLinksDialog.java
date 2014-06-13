package com.example.richtexteditor;

import com.example.richtexteditor.R;
import android.app.Activity;
import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TextView.OnEditorActionListener;

/**
 * Class EditLinksDialog.
 */
public class EditLinksDialog extends DialogFragment implements OnEditorActionListener {

  private static EditLinksDialog linkEditor = null;
  private EditText mEditUrl;
  private EditText mEditEmail;
  private static String mLink = "";

  /**
   * Instantiates a new edits the links dialog.
   */
  public EditLinksDialog() {
  }

  /**
   * Gets the single instance of EditLinksDialog.
   * 
   * @param link
   * @return single instance of EditLinksDialog
   */
  public static EditLinksDialog getInstance(String link) {
    if (linkEditor != null) linkEditor = null;
    linkEditor = new EditLinksDialog();

    if (! link.isEmpty()) {
      mLink = link;
    } else {
      mLink = "";
    }
    return linkEditor;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.edit_links_layout, container);
    mEditUrl = (EditText) view.findViewById(R.id.te_link_url);
    mEditEmail = (EditText) view.findViewById(R.id.te_link_email);
    getDialog().setTitle(R.string.te_enter_title);
    getDialog().getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    mEditUrl.setOnEditorActionListener(this);
    mEditEmail.setOnEditorActionListener(this);

    if (mLink.startsWith("mailto:")) { // limitation in Patterns.EMAIL_ADDRESS.matcher.
      mLink = mLink.replaceFirst("mailto:", "");
    }

    if (!(mLink.isEmpty())) {
      if (Patterns.WEB_URL.matcher(mLink).matches()) {
        mEditUrl.setText(mLink, TextView.BufferType.NORMAL);
      } else if (Patterns.EMAIL_ADDRESS.matcher(mLink).matches()) {
        mEditEmail.setText(mLink, TextView.BufferType.NORMAL);
      }
    }

    return view;
  }

  @Override
  public boolean onEditorAction(TextView tv, int actionId, KeyEvent event) {
    if (EditorInfo.IME_ACTION_DONE == actionId) {
      if (mEditUrl.isFocused()) {
        String url = mEditUrl.getText().toString();
        if ((! url.isEmpty()) && (Patterns.WEB_URL.matcher(url).matches())) {
          if ((! url.startsWith("http:")) && (! url.startsWith("https:"))) {
            url = "http://" + url;
          }
          Intent i = getActivity().getIntent();
          i.putExtra(MyTextEditorFragment.EDIT_LINKS_TAG, url);
          getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, i);
          this.dismiss();
          return true;
        } else {
          Toast toast = Toast.makeText(getActivity(), R.string.toast_te_invalid_url, Toast.LENGTH_LONG);
          toast.setGravity(Gravity.CENTER, 0, 200);
          toast.show();
        }
      } else if (mEditEmail.isFocused()) {
        String email = mEditEmail.getText().toString();
        if ((! email.isEmpty()) && (Patterns.EMAIL_ADDRESS.matcher(email).matches())) {
          if (! email.startsWith("mailto:")) {
            email = "mailto:" + email;
          }
          Intent i = getActivity().getIntent();
          i.putExtra(MyTextEditorFragment.EDIT_LINKS_TAG, email);
          getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, i);
          this.dismiss();
          return true;
        } else {
          Toast toast = Toast.makeText(getActivity(), R.string.toast_te_invalid_email, Toast.LENGTH_LONG);
          toast.setGravity(Gravity.CENTER, 0, 200);
          toast.show();
        }
      }
    }
    return false;
  }
}