package com.example.richtexteditor;

import com.example.richtexteditor.R;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.Layout;
import android.text.Layout.Alignment;
import android.text.Selection;
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.style.AlignmentSpan;
import android.text.style.BulletSpan;
import android.text.style.CharacterStyle;
import android.text.style.LeadingMarginSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.text.style.UnderlineSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView.BufferType;
import android.widget.ToggleButton;

import com.example.richtexteditor.MyHtml.MyArrayList;

/**
 * Rich text editor Fragment.
 */
public class MyTextEditorFragment extends Fragment
implements OnClickListener, TextWatcher, MyEditText.OnSelectionChangedListener
{
  private static MyTextEditorFragment textEditor = null;
  private static Bundle bundle = null;
  private MyEditText mEditText;
  private int mLastPosition = -1;
  private ToggleButton mTitleToggleButton, mBulletListToggleButton, mBoldToggleButton, mItalicsToggleButton, mUnderlineToggleButton, mStrikethroughSpanToggleButton, mLinkToggleButton, mBrushToggleButton, mAlignRightToggleButton, mAlignCenterToggleButton, mAlignLeftToggleButton;
  private static boolean mIsBackspace, mIsEnter, mWasEnter = false;
  protected int mSelectionStart, mSelectionEnd;
  private final static String TEXT_BLOCK_TAG = "MyTextEditorFragment.TEXT_BLOCK";
  private final static String TEXT_BLOCK_ID_TAG = "MyTextEditorFragment.TEXT_BLOCK_ID";
  private final String EOL = System.getProperty("line.separator");
  private static final float H2 = 1.4f;
  private static final int LIST_INDENT = 7;
  private MyArrayList htmlList = new MyArrayList();
  private static final boolean IS_JELLY_BEAN_MR1_OR_OLDER = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1;
  private static long mStartTime;
//  private static final long TIMEOUT = 2100000;
  private static final long TIMEOUT = 1000; // set this to some meaningful once save to server is available.
  public static final int EDIT_LINKS_DIALOG_FRAGMENT = 1;
  public final static String EDIT_LINKS_TAG = "MyTextEditorFragment.LINK";

  //Convenience variable to disable buggiest feature of this code.
  private boolean disableLists = true;

  /**
   * Setter for Enter key (if older than IS_JELLY_BEAN_MR1).
   */
  public static void setIsEnter () {
    mIsEnter = true;
  }

  /**
   * Setter for backspace key (if older than IS_JELLY_BEAN_MR1).
   */
  public static void setIsBackspace () {
    mIsBackspace = true;
    mIsEnter = false;
    mWasEnter = false;
  }

  /**
   * Entry point for starting this fragment. Call e.g.:
   * 
   * @param textBlock
   * @param textBlockId
   * @param orientationChange
   * @return single instance of TextEditorFragment
   */
  public static MyTextEditorFragment getInstance(String textBlock, String textBlockId) {

    if (textEditor == null) {
      textEditor = new MyTextEditorFragment();
      bundle = new Bundle();
      textEditor.setArguments(bundle);
    }

    bundle.putCharSequence(TEXT_BLOCK_TAG, MyHtml.fromHtml(textBlock));
    bundle.putString(TEXT_BLOCK_ID_TAG, textBlockId);

    mStartTime = System.currentTimeMillis();

    return textEditor;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);

    if (container == null) return null;

    View layout = (View) inflater.inflate(R.layout.text_editor_layout, container, false);

    getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

    setSaveTextButtonClickListener(layout);

    mEditText = (MyEditText) layout.findViewById(R.id.te_editor);
    mEditText.setOnSelectionChangedListener(this);
    mEditText.addTextChangedListener(this);

    mTitleToggleButton = (ToggleButton) layout.findViewById(R.id.te_toggleTitle);
    mTitleToggleButton.setOnClickListener(this);
    mBoldToggleButton = (ToggleButton) layout.findViewById(R.id.te_toggleBold);
    mBoldToggleButton.setOnClickListener(this);
    mItalicsToggleButton = (ToggleButton) layout.findViewById(R.id.te_toggleItalics);
    mItalicsToggleButton.setOnClickListener(this);
    mUnderlineToggleButton = (ToggleButton) layout.findViewById(R.id.te_toggleUnderline);
    mUnderlineToggleButton.setOnClickListener(this);
    mStrikethroughSpanToggleButton = (ToggleButton) layout.findViewById(R.id.te_toggleStrikeThrough);
    mStrikethroughSpanToggleButton.setOnClickListener(this);
    mLinkToggleButton = (ToggleButton) layout.findViewById(R.id.te_toggleLinks);
    mLinkToggleButton.setOnClickListener(this);
    mAlignRightToggleButton = (ToggleButton) layout.findViewById(R.id.te_toggleAlignRight);
    mAlignRightToggleButton.setOnClickListener(this);
    mAlignCenterToggleButton = (ToggleButton) layout.findViewById(R.id.te_toggleAlignCenter);
    mAlignCenterToggleButton.setOnClickListener(this);
    mAlignLeftToggleButton = (ToggleButton) layout.findViewById(R.id.te_toggleAlignLeft);
    mAlignLeftToggleButton.setOnClickListener(this);
    mBrushToggleButton = (ToggleButton) layout.findViewById(R.id.te_toggleBrush);
    mBrushToggleButton.setOnClickListener(this);
    mBrushToggleButton.setChecked(false);

    mBulletListToggleButton = (ToggleButton) layout.findViewById(R.id.te_toggleBulletList);

    if (disableLists) {
      mBulletListToggleButton.setVisibility(View.GONE);
    } else {
      mBulletListToggleButton.setOnClickListener(this);
    }

    return layout;
  }

  @Override
  public void onResume() {
    super.onResume();

    try {
      mEditText.setText(getArguments().getCharSequence(TEXT_BLOCK_TAG), BufferType.SPANNABLE);
    } catch(NullPointerException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void onPause() {
    super.onPause();

    try {
      bundle.putCharSequence(TEXT_BLOCK_TAG, mEditText.getText());
    } catch(NullPointerException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void onClick(View v) {
    int id = v.getId();
    if (id == R.id.te_toggleTitle) {
      formatBtnClick(mTitleToggleButton, "h2");
    } else if (id == R.id.te_toggleBold) {
      formatBtnClick(mBoldToggleButton, "b");
    } else if (id == R.id.te_toggleItalics) {
      formatBtnClick(mItalicsToggleButton, "i");
    } else if (id == R.id.te_toggleUnderline) {
      formatBtnClick(mUnderlineToggleButton, "u");
    } else if (id == R.id.te_toggleStrikeThrough) {
      formatBtnClick(mStrikethroughSpanToggleButton, "st");
    } else if (id == R.id.te_toggleBulletList) {
      formatBtnClick(mBulletListToggleButton, "ul");
    } else if (id == R.id.te_toggleLinks) {
      formatBtnClick(mLinkToggleButton, "link");
    } else if (id == R.id.te_toggleAlignRight) {
      formatBtnClick(mAlignRightToggleButton, "aRight");
    } else if (id == R.id.te_toggleAlignCenter) {
      formatBtnClick(mAlignCenterToggleButton, "aCenter");
    } else if (id == R.id.te_toggleAlignLeft) {
      formatBtnClick(mAlignLeftToggleButton, "aLeft");
    } else if (id == R.id.te_toggleBrush) {
      formatBtnClick(mBrushToggleButton, "brush");
      mBrushToggleButton.setChecked(false);
    }
  }

  @Override
  public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    if (!mEditText.hasFocus()) {
      mIsBackspace = false;
      return;
    }

    if (IS_JELLY_BEAN_MR1_OR_OLDER) {
      if ((count - after == 1) || (s.length() == 0)) { // wont work with selections.
        mIsBackspace = true;
        mIsEnter = false;
        mWasEnter = false;
      } else {
        mIsBackspace = false;
      }
    }
  }

  @Override
  public void onTextChanged(CharSequence s, int start, int before, int count) {
    if (!mEditText.hasFocus()) {
      mIsEnter = false;
      return;
    }

    if ((IS_JELLY_BEAN_MR1_OR_OLDER) && (s.length()>0 && s.subSequence(s.length()-1, s.length()).toString().equalsIgnoreCase("\n"))) {
      mIsEnter = true;
    }
  }

  @Override
  public void afterTextChanged(Editable s) {

    try {
      if (!mEditText.hasFocus()) {
        mIsEnter = false;
        mWasEnter = false;
        mIsBackspace = false;
        return;
      }

      if (mIsBackspace) {
        if (mBulletListToggleButton.isChecked()) {
          if (mSelectionEnd > mSelectionStart) {
            BulletSpan[] bs = s.getSpans(mSelectionStart, mSelectionEnd, BulletSpan.class);
            s.removeSpan(bs[0]);
            mIsBackspace = false;
            return;
          }
        }
      }

      int selectionStart = Selection.getSelectionStart(mEditText.getText());
      if ((mIsBackspace && selectionStart != 1) || (mLastPosition == selectionStart)) {
        mIsBackspace = false;
        return;
      }

      if (selectionStart < 0) {
        selectionStart = 0;
      }
      mLastPosition = selectionStart;
      int textEnd = s.length();

      if (selectionStart > 0) {
        if (mSelectionStart > selectionStart) {
          mSelectionStart = selectionStart - 1;
        }
        mSelectionEnd = selectionStart;
        boolean exists = false;

        if (mTitleToggleButton.isChecked()) {

          RelativeSizeSpan[] rs = s.getSpans(mSelectionStart, mSelectionEnd, RelativeSizeSpan.class);
          exists = false;
          for (int i = 0; i < rs.length; i++) {
            exists = true;
          }

          if (mIsEnter) {
            mTitleToggleButton.setChecked(false);
          } else if (!exists) {
            s.setSpan(new RelativeSizeSpan(H2), mSelectionStart, mSelectionEnd, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            s.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), mSelectionStart, mSelectionEnd, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
          }
        }

        if (mBoldToggleButton.isChecked()) {
          StyleSpan[] ss = s.getSpans(mSelectionStart, mSelectionEnd, StyleSpan.class);
          exists = false;
          for (int i = 0; i < ss.length; i++) {
            if (ss[i].getStyle() == android.graphics.Typeface.BOLD) {
              exists = true;
            }
          }
          if (!exists) {
            s.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), mSelectionStart, mSelectionEnd, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
          }
        }

        if (mItalicsToggleButton.isChecked()) {
          StyleSpan[] ss = s.getSpans(mSelectionStart, mSelectionEnd, StyleSpan.class);
          exists = false;
          for (int i = 0; i < ss.length; i++) {
            if (ss[i].getStyle() == android.graphics.Typeface.ITALIC) {
              exists = true;
            }
          }
          if (!exists) {
            s.setSpan(new StyleSpan(android.graphics.Typeface.ITALIC), mSelectionStart, mSelectionEnd, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
          }
        }

        if (mUnderlineToggleButton.isChecked()) {
          UnderlineSpan[] us = s.getSpans(mSelectionStart, mSelectionEnd, UnderlineSpan.class);
          exists = false;
          for (int i = 0; i < us.length; i++) {
            exists = true;
          }
          if (!exists)
            s.setSpan(new UnderlineSpan(), mSelectionStart, mSelectionEnd, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        }

        if (mStrikethroughSpanToggleButton.isChecked()) {
          StrikethroughSpan[] st = s.getSpans(mSelectionStart, mSelectionEnd, StrikethroughSpan.class);
          exists = false;
          for (int i = 0; i < st.length; i++) {
            exists = true;
          }
          if (!exists)
            s.setSpan(new StrikethroughSpan(), mSelectionStart, mSelectionEnd, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        }

        if (mBulletListToggleButton.isChecked()) {
          if (mIsEnter) {

            if (mSelectionEnd == textEnd) {
            } else {
              for (int i = mSelectionEnd; i <= textEnd; i++) {
                if (i == textEnd) {
                  mSelectionEnd = textEnd;
                  break;
                } else if (s.charAt(i) == '\n') {
                  mSelectionEnd = i;
                  break;
                }
              }
            }

            if (mSelectionStart == 0) {
            } else {
              for (int i = mSelectionStart-1; i >= 0; i--) {
                if (i == 0) {
                  mSelectionStart = 0;
                  break;
                } else if (s.charAt(i) == '\n') {
                  mSelectionStart = i+1;
                  break;
                }
              }
            }

            BulletSpan[] bs = s.getSpans(selectionStart, mSelectionEnd, BulletSpan.class);
            for (int i = 0; i < bs.length; i++) {
              s.removeSpan(bs[i]);
            }
            if (! mWasEnter) {
              s.setSpan(new BulletSpan(LIST_INDENT), selectionStart, mSelectionEnd, Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
            }

            BulletSpan[] bs2 = s.getSpans(mSelectionStart, selectionStart, BulletSpan.class);
            for (int i = 0; i < bs2.length; i++) {
              s.removeSpan(bs2[i]);
            }
            if (! mWasEnter) {
              s.setSpan(new BulletSpan(LIST_INDENT), mSelectionStart, selectionStart, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

          } else {

            BulletSpan[] bs = s.getSpans(mSelectionStart, mSelectionEnd, BulletSpan.class);
            exists = false;
            for (int i = 0; i < bs.length; i++) {
              exists = true;
            }
            if (!exists) {
              s.setSpan(new BulletSpan(LIST_INDENT), mSelectionStart, mSelectionEnd, Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
            }
          }
        }


        if (mIsEnter) {
          mWasEnter = true;
        } else {
          mWasEnter = false;
        }
        mIsEnter = false;
        mIsBackspace = false;
      }
    } catch (Exception e) {
      e.printStackTrace(); // DEBUG
    }
  }

  /**
   * Called before afterTextChanged, unless its backspace,
   * in which case selection parameters from here are still valid.
   */
  @Override
  public void onSelectionChanged() {
    if (!mEditText.isFocused()) {
      titleToggles(false, false);
      bodyToggles(false, false);
      listToggles(false, false);
      alignToggles(false, false);
      return;
    } else {
      titleToggles(true, false);
      bodyToggles(true, false);
      listToggles(true, false);
      alignToggles(true, false);
    }

    final Spannable s = mEditText.getText();
    mSelectionStart = mEditText.getSelectionStart();
    mSelectionEnd = mEditText.getSelectionEnd();
    Object[] spans = s.getSpans(mEditText.getSelectionStart(), mEditText.getSelectionEnd(), Object.class);
    boolean isTitle = false;

    for (Object span : spans) {
      if (span instanceof RelativeSizeSpan) {
        mTitleToggleButton.setChecked(true);

        bodyToggles(false, false);
        listToggles(false, false);

        isTitle = true;
      }
    }

    for (Object span : spans) {
      if (span instanceof AlignmentSpan) {
        alignToggles(true, false);
        Layout.Alignment align = ((AlignmentSpan) span).getAlignment();

        if (align == Layout.Alignment.ALIGN_OPPOSITE) {
          mAlignRightToggleButton.setChecked(true);
          mAlignCenterToggleButton.setChecked(false);
          mAlignLeftToggleButton.setChecked(false);
        } else if (align == Layout.Alignment.ALIGN_CENTER) {
          mAlignRightToggleButton.setChecked(false);
          mAlignCenterToggleButton.setChecked(true);
          mAlignLeftToggleButton.setChecked(false);
        } else {
          mAlignRightToggleButton.setChecked(false);
          mAlignCenterToggleButton.setChecked(false);
          mAlignLeftToggleButton.setChecked(true);
        }

        if (isTitle) {
          bodyToggles(false, false);
        } else {
          bodyToggles(true, false);
        }

        listToggles(false, false);
        if (isTitle) return; else break;
      }
    }

    for (Object span : spans) {
      if (span instanceof BulletSpan) {
        mBulletListToggleButton.setChecked(true);
        titleToggles(false, false);
        alignToggles(false, false);
        break;
      } else if (span instanceof LeadingMarginSpan) {
        mBulletListToggleButton.setChecked(false);
        mBulletListToggleButton.setEnabled(false);
        titleToggles(false, false);
        alignToggles(false, false);
        break;
      }
    }

    for (Object span : spans) {

      if (span instanceof StyleSpan) {
        StyleSpan ss = (StyleSpan) span;
        if (ss.getStyle() == android.graphics.Typeface.BOLD) {
          mBoldToggleButton.setChecked(true);
        }
        if (ss.getStyle() == android.graphics.Typeface.ITALIC) {
          mItalicsToggleButton.setChecked(true);
        }
      }
      if (span instanceof URLSpan) {
        mLinkToggleButton.setChecked(true);
      }
      if (span instanceof UnderlineSpan) {
        mUnderlineToggleButton.setChecked(true);
      }
      if (span instanceof StrikethroughSpan) {
        mStrikethroughSpanToggleButton.setChecked(true);
      }
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch(requestCode) {
    case EDIT_LINKS_DIALOG_FRAGMENT:
      Editable str = mEditText.getText();
      str.setSpan(new URLSpan(data.getStringExtra(EDIT_LINKS_TAG)), mSelectionStart, mSelectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
      mLinkToggleButton = (ToggleButton) getActivity().findViewById(R.id.te_toggleLinks);
      mLinkToggleButton.setChecked(true);
      break;
    }
  }

  /**
   * Title toggle buttons.
   */
  private void titleToggles (boolean enabled, boolean checked) {
    mTitleToggleButton.setEnabled(enabled);
    mTitleToggleButton.setChecked(checked);
  }

  /**
   * List toggle buttons.
   */
  private void listToggles (boolean enabled, boolean checked) {
    mBulletListToggleButton.setEnabled(enabled);
    mBulletListToggleButton.setChecked(checked);
  }

  /**
   * Body toggle buttons.
   */
  private void bodyToggles (boolean enabled, boolean checked) {
    mBoldToggleButton.setEnabled(enabled);
    mBoldToggleButton.setChecked(checked);
    mItalicsToggleButton.setEnabled(enabled);
    mItalicsToggleButton.setChecked(checked);
    mLinkToggleButton.setEnabled(enabled);
    mLinkToggleButton.setChecked(checked);
    mUnderlineToggleButton.setEnabled(enabled);
    mUnderlineToggleButton.setChecked(checked);
    mStrikethroughSpanToggleButton.setEnabled(enabled);
    mStrikethroughSpanToggleButton.setChecked(checked);
  }

  /**
   * Align toggle buttons. Change status, depending where user has moved cursor.
   */
  private void alignToggles (boolean enabled, boolean checked) {
    mAlignRightToggleButton.setEnabled(enabled);
    mAlignRightToggleButton.setChecked(checked);
    mAlignCenterToggleButton.setEnabled(enabled);
    mAlignCenterToggleButton.setChecked(checked);
    mAlignLeftToggleButton.setEnabled(enabled);
    mAlignLeftToggleButton.setChecked(checked);
  }

  /**
   * Format btn click.
   * 
   * @param toggleButton
   * @param tag
   */
  private void formatBtnClick(ToggleButton toggleButton, String tag) {
    try {
      Editable str = mEditText.getText();
      int textEnd = str.length();

      if (!(mEditText.isFocused()) && (tag.equals("brush"))) {
        CharacterStyle[] cs = str.getSpans(0, textEnd, CharacterStyle.class);
        for (int i = 0; i < cs.length; i++) {
          str.removeSpan(cs[i]);
        }

        LeadingMarginSpan[] lms = str.getSpans(0, textEnd, LeadingMarginSpan.class);
        for (int i = 0; i < lms.length; i++) {
          str.removeSpan(lms[i]);
        }

        AlignmentSpan[] as = str.getSpans(0, textEnd, AlignmentSpan.class);
        for (int i = 0; i < as.length; i++) {
          str.removeSpan(as[i]);
        }
        return;
      }

      mSelectionStart = mEditText.getSelectionStart();
      mSelectionEnd = mEditText.getSelectionEnd();

      if (mSelectionStart > mSelectionEnd) {
        int temp = mSelectionEnd;
        mSelectionEnd = mSelectionStart;
        mSelectionStart = temp;
      }

      if (mSelectionEnd > mSelectionStart) {
        if (tag.equals("h2")) {

          RelativeSizeSpan[] rs = str.getSpans(mSelectionStart, mSelectionEnd, RelativeSizeSpan.class);
          boolean exists = false;
          for (int i = 0; i < rs.length; i++) {
            str.removeSpan(rs[i]);
            exists = true;
          }

          if (exists) {
            StyleSpan[] ss = str.getSpans(mSelectionStart, mSelectionEnd, StyleSpan.class);
            for (int i = 0; i < ss.length; i++) {
              if (ss[i].getStyle() == android.graphics.Typeface.BOLD) {
                str.removeSpan(ss[i]);
              }
            }
          }

          if (!exists) {
            CharacterStyle[] cs = str.getSpans(mSelectionStart, mSelectionEnd, CharacterStyle.class);
            for (int i = 0; i < cs.length; i++) {
              str.removeSpan(cs[i]);
            }

            str.setSpan(new RelativeSizeSpan(H2), mSelectionStart, mSelectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            str.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), mSelectionStart, mSelectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            if (mSelectionEnd != textEnd) {
              if (((str.charAt(mSelectionEnd)) == ' ') || ((str.charAt(mSelectionEnd) == '\n'))) {
                str.replace(mSelectionEnd, mSelectionEnd+1, EOL);
              } else {
                str.insert(mSelectionEnd, EOL);
              }
            }

            if (mSelectionStart != 0) {
              if (((str.charAt(mSelectionStart-1)) == ' ') || ((str.charAt(mSelectionStart-1) == '\n'))) {
                str.replace(mSelectionStart-1, mSelectionStart, EOL);
              } else {
                str.insert(mSelectionStart, EOL);
              }
            }

            toggleButton.setChecked(true);
          } else {
            toggleButton.setChecked(false);
          }
          mEditText.clearFocus();

        } else if (tag.equals("aRight")) {

          AlignmentSpan[] as = str.getSpans(mSelectionStart, mSelectionEnd, AlignmentSpan.class);
          boolean exists = false;
          for (int i = 0; i < as.length; i++) {
            Layout.Alignment align = ((AlignmentSpan) as[i]).getAlignment();
            if (align == Layout.Alignment.ALIGN_OPPOSITE) {
              exists = true;
            }
            str.removeSpan(as[i]);
          }

          if (!exists) {

            if (mSelectionEnd == textEnd) {
            } else {
              for (int i = mSelectionEnd; i <= textEnd; i++) {
                if (i == textEnd) {
                  mSelectionEnd = textEnd;
                  break;
                } else if (str.charAt(i) == '\n') {
                  mSelectionEnd = i;
                  break;
                }
              }
            }

            if (mSelectionStart == 0) {
            } else {
              for (int i = mSelectionStart; i >= 0; i--) {
                if (i == 0) {
                  mSelectionStart = 0;
                  break;
                } else if (str.charAt(i) == '\n') {
                  mSelectionStart = i;
                  break;
                }
              }
            }

            str.setSpan(new AlignmentSpan.Standard(Alignment.ALIGN_OPPOSITE), mSelectionStart, mSelectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            mAlignRightToggleButton.setChecked(true);
            mAlignCenterToggleButton.setChecked(false);
            mAlignLeftToggleButton.setChecked(false);

            mBulletListToggleButton.setEnabled(false);
          } else {
            mAlignRightToggleButton.setChecked(false);
            mAlignCenterToggleButton.setChecked(false);
            mAlignLeftToggleButton.setChecked(false);

            mBulletListToggleButton.setEnabled(true);
          }

        } else if (tag.equals("aCenter")) {

          AlignmentSpan[] as = str.getSpans(mSelectionStart, mSelectionEnd, AlignmentSpan.class);
          boolean exists = false;
          for (int i = 0; i < as.length; i++) {
            Layout.Alignment align = ((AlignmentSpan) as[i]).getAlignment();
            if (align == Layout.Alignment.ALIGN_CENTER) {
              exists = true;
            }
            str.removeSpan(as[i]);
          }

          if (!exists) {

            if (mSelectionEnd == textEnd) {
            } else {
              for (int i = mSelectionEnd; i <= textEnd; i++) {
                if (i == textEnd) {
                  mSelectionEnd = textEnd;
                  break;
                } else if (str.charAt(i) == '\n') {
                  mSelectionEnd = i;
                  break;
                }
              }
            }

            if (mSelectionStart == 0) {
            } else {
              for (int i = mSelectionStart; i >= 0; i--) {
                if (i == 0) {
                  mSelectionStart = 0;
                  break;
                } else if (str.charAt(i) == '\n') {
                  mSelectionStart = i;
                  break;
                }
              }
            }

            str.setSpan(new AlignmentSpan.Standard(Alignment.ALIGN_CENTER), mSelectionStart, mSelectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            mAlignRightToggleButton.setChecked(false);
            mAlignCenterToggleButton.setChecked(true);
            mAlignLeftToggleButton.setChecked(false);

            mBulletListToggleButton.setEnabled(false);
          } else {
            mAlignRightToggleButton.setChecked(false);
            mAlignCenterToggleButton.setChecked(false);
            mAlignLeftToggleButton.setChecked(false);

            mBulletListToggleButton.setEnabled(true);
          }

        } else if (tag.equals("aLeft")) {

          AlignmentSpan[] as = str.getSpans(mSelectionStart, mSelectionEnd, AlignmentSpan.class);
          boolean exists = false;
          for (int i = 0; i < as.length; i++) {
            Layout.Alignment align = ((AlignmentSpan) as[i]).getAlignment();
            if (align == Layout.Alignment.ALIGN_NORMAL) {
              exists = true;
            }
            str.removeSpan(as[i]);
          }

          if (!exists) {

            if (mSelectionEnd == textEnd) {
            } else {
              for (int i = mSelectionEnd; i <= textEnd; i++) {
                if (i == textEnd) {
                  mSelectionEnd = textEnd;
                  break;
                } else if (str.charAt(i) == '\n') {
                  mSelectionEnd = i;
                  break;
                }
              }
            }

            if (mSelectionStart == 0) {
            } else {
              for (int i = mSelectionStart; i >= 0; i--) {
                if (i == 0) {
                  mSelectionStart = 0;
                  break;
                } else if (str.charAt(i) == '\n') {
                  mSelectionStart = i;
                  break;
                }
              }
            }

            str.setSpan(new AlignmentSpan.Standard(Alignment.ALIGN_NORMAL), mSelectionStart, mSelectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            mAlignRightToggleButton.setChecked(false);
            mAlignCenterToggleButton.setChecked(false);
            mAlignLeftToggleButton.setChecked(true);

            mBulletListToggleButton.setEnabled(false);
          } else {
            mAlignRightToggleButton.setChecked(false);
            mAlignCenterToggleButton.setChecked(false);
            mAlignLeftToggleButton.setChecked(false);

            mBulletListToggleButton.setEnabled(true);
          }

        } else if (tag.equals("b")) {
          StyleSpan[] ss = str.getSpans(mSelectionStart, mSelectionEnd, StyleSpan.class);
          boolean exists = false;
          for (int i = 0; i < ss.length; i++) {

            if (ss[i].getStyle() == android.graphics.Typeface.BOLD) {
              str.removeSpan(ss[i]);
              exists = true;
            }
          }

          if (!exists) {
            str.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), mSelectionStart, mSelectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            toggleButton.setChecked(true);
          } else {
            toggleButton.setChecked(false);
          }

        } else if (tag.equals("i")) {
          StyleSpan[] ss = str.getSpans(mSelectionStart, mSelectionEnd, StyleSpan.class);
          boolean exists = false;
          for (int i = 0; i < ss.length; i++) {
            if (ss[i].getStyle() == android.graphics.Typeface.ITALIC) {
              str.removeSpan(ss[i]);
              exists = true;
            }
          }

          if (!exists) {
            str.setSpan(new StyleSpan(android.graphics.Typeface.ITALIC), mSelectionStart, mSelectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            toggleButton.setChecked(true);
          } else {
            toggleButton.setChecked(false);
          }
        } else if (tag.equals("u")) {

          UnderlineSpan[] us = str.getSpans(mSelectionStart, mSelectionEnd, UnderlineSpan.class);

          boolean exists = false;
          for (int i = 0; i < us.length; i++) {
            str.removeSpan(us[i]);
            exists = true;
          }

          if (!exists) {
            str.setSpan(new UnderlineSpan(), mSelectionStart, mSelectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            toggleButton.setChecked(true);
          } else {
            toggleButton.setChecked(false);
          }
        } else if (tag.equals("st")) {

          StrikethroughSpan[] st = str.getSpans(mSelectionStart, mSelectionEnd, StrikethroughSpan.class);

          boolean exists = false;
          for (int i = 0; i < st.length; i++) {
            str.removeSpan(st[i]);
            exists = true;
          }

          if (!exists) {
            str.setSpan(new StrikethroughSpan(), mSelectionStart, mSelectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            toggleButton.setChecked(true);
          } else {
            toggleButton.setChecked(false);
          }

        } else if (tag.equals("ul")) {

          BulletSpan[] bs = str.getSpans(mSelectionStart, mSelectionEnd, BulletSpan.class);

          boolean exists = false;
          for (int i = 0; i < bs.length; i++) {
            str.removeSpan(bs[i]);
            exists = true;
          }

          if (!exists) {
            if (mSelectionEnd < textEnd) {
              for (int i = mSelectionEnd; i <= textEnd; i++) {
                if (str.charAt(i) == '\n') {
                  mSelectionEnd = i;
                  break;
                }
              }
            } else {
              mSelectionEnd = textEnd;
            }

            if (mSelectionStart > 0) {
              for (int i = mSelectionStart; i >= 0; i--) {
                if ((str.charAt(i) == '\n') || (i == 0)) {
                  mSelectionStart = i;
                  break;
                }
              }
            } else {
              mSelectionStart = 0;
            }

            if (htmlList.isSet()) htmlList.clear();
            htmlList.setType(MyHtml.BULLET_SPAN_TAG);
            MyArrayList.Row htmlListRow;
            int rowStart = mSelectionStart, nextRowStart = rowStart, rowEnd = 0;
            for (int i = mSelectionStart; i <= mSelectionEnd; i++) {
              if (i == textEnd) {
                htmlList.addRow(rowStart, textEnd);
                break;
              } else if (str.charAt(i) == '\n') {

                if (i == mSelectionEnd) {
                  rowEnd = i;
                } else {
                  nextRowStart = i+1;
                  rowEnd = i;
                }

                if (rowStart < rowEnd) {
                  htmlList.addRow(rowStart, rowEnd);
                }
                rowStart = nextRowStart;
              }
            }

            for(int i = 0; i < htmlList.listRows(); i++) {
              htmlListRow = htmlList.getRow(i);
              str.setSpan(new BulletSpan(LIST_INDENT), htmlListRow.start, htmlListRow.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            toggleButton.setChecked(true);

            mAlignRightToggleButton.setEnabled(false);
            mAlignCenterToggleButton.setEnabled(false);
            mAlignLeftToggleButton.setEnabled(false);
            mTitleToggleButton.setEnabled(false);
          } else {
            toggleButton.setChecked(false);

            mAlignRightToggleButton.setEnabled(true);
            mAlignCenterToggleButton.setEnabled(true);
            mAlignLeftToggleButton.setEnabled(true);
            mTitleToggleButton.setEnabled(true);
          }
          htmlList.clear();

        } else if (tag.equals("link")) {

          URLSpan[] us = str.getSpans(mSelectionStart, mSelectionEnd, URLSpan.class);
          URLSpan u = null;
          boolean exists = false;
          for (int i = 0; i < us.length; i++) {
            exists = true;
            u = us[i];
          }

          if (exists) {
            toggleButton.setChecked(true);
            FragmentManager fm = getFragmentManager();
            EditLinksDialog eld = EditLinksDialog.getInstance(u.getURL());
            eld.setTargetFragment(this, EDIT_LINKS_DIALOG_FRAGMENT);
            eld.show(fm, "edit_links_layout");
          } else {
            FragmentManager fm = getFragmentManager();
            EditLinksDialog eld = EditLinksDialog.getInstance("");
            eld.setTargetFragment(this, EDIT_LINKS_DIALOG_FRAGMENT);
            eld.show(fm, "edit_links_layout");
          }

          // --> go wait for results in onActivityResult

        } else if (tag.equals("brush")) {
          CharacterStyle[] cs = str.getSpans(mSelectionStart, mSelectionEnd, CharacterStyle.class);
          for (int i = 0; i < cs.length; i++) {
            str.removeSpan(cs[i]);
          }

          LeadingMarginSpan[] lms = str.getSpans(mSelectionStart, mSelectionEnd, LeadingMarginSpan.class);
          for (int i = 0; i < lms.length; i++) {
            str.removeSpan(lms[i]);
          }

          AlignmentSpan[] as = str.getSpans(mSelectionStart, mSelectionEnd, AlignmentSpan.class);
          for (int i = 0; i < as.length; i++) {
            str.removeSpan(as[i]);
          }

          mEditText.clearFocus();
        }
      } else if (toggleButton.isChecked()) {

        if (tag.equals("h2")) {
          if ((mSelectionStart == textEnd) || (str.charAt(mSelectionStart) == '\n')) {
            mSelectionEnd = mSelectionStart;
            mSelectionStart--;
          } else {
            mSelectionEnd = mSelectionStart+1;
          }

          RelativeSizeSpan[] rs = str.getSpans(mSelectionStart, mSelectionEnd, RelativeSizeSpan.class);
          boolean exists = false;
          for (int i = 0; i < rs.length; i++) {
            str.removeSpan(rs[i]);
            exists = true;
          }

          if (exists) {
            StyleSpan[] ss = str.getSpans(mSelectionStart, mSelectionEnd, StyleSpan.class);
            for (int i = 0; i < ss.length; i++) {
              if (ss[i].getStyle() == android.graphics.Typeface.BOLD) {
                str.removeSpan(ss[i]);
              }
            }
          }

          if (!exists) {
            if (mSelectionEnd == textEnd) {
            } else {
              for (int i = mSelectionEnd; i <= textEnd; i++) {
                if (i == textEnd) {
                  mSelectionEnd = textEnd;
                  break;
                } else if ((str.charAt(i) == '\n') || (str.charAt(i) == ' ')) {
                  mSelectionEnd = i;
                  break;
                }
              }
            }

            if (mSelectionStart == 0) {
            } else {
              for (int i = mSelectionStart; i >= 0; i--) {
                if (i == 0) {
                  mSelectionStart = 0;
                  break;
                } else if ((str.charAt(i) == '\n') || (str.charAt(i) == ' ')) {
                  mSelectionStart = i;
                  break;
                }
              }
            }

            CharacterStyle[] cs = str.getSpans(mSelectionStart, mSelectionEnd, CharacterStyle.class);
            for (int i = 0; i < cs.length; i++) {
              str.removeSpan(cs[i]);
            }

            str.setSpan(new RelativeSizeSpan(H2), mSelectionStart, mSelectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            str.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), mSelectionStart, mSelectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            if (mSelectionEnd != textEnd) {
              if (((str.charAt(mSelectionEnd)) == ' ') || ((str.charAt(mSelectionEnd) == '\n'))) {
                str.replace(mSelectionEnd, mSelectionEnd+1, EOL);
              } else {
                str.insert(mSelectionEnd, EOL);
              }
            }

            if (mSelectionStart != 0) {
              if (((str.charAt(mSelectionStart-1)) == ' ') || ((str.charAt(mSelectionStart-1) == '\n'))) {
                str.replace(mSelectionStart-1, mSelectionStart, EOL);
              } else {
                str.insert(mSelectionStart, EOL);
              }
            }

            listToggles(false, false);
            bodyToggles(false, false);
            toggleButton.setChecked(true);
          } else {
            toggleButton.setChecked(false);
          }
          mEditText.clearFocus();

        } else if (tag.equals("aRight")) {

          AlignmentSpan[] as = str.getSpans(mSelectionStart, mSelectionEnd, AlignmentSpan.class);
          boolean exists = false;
          for (int i = 0; i < as.length; i++) {
            Layout.Alignment align = ((AlignmentSpan) as[i]).getAlignment();
            if (align == Layout.Alignment.ALIGN_OPPOSITE) {
              exists = true;
            }
            str.removeSpan(as[i]);
          }

          if (!exists) {

            if (mSelectionEnd == textEnd) {
            } else {
              for (int i = mSelectionEnd; i <= textEnd; i++) {
                if (i == textEnd) {
                  mSelectionEnd = textEnd;
                  break;
                } else if (str.charAt(i) == '\n') {
                  mSelectionEnd = i;
                  break;
                }
              }
              for (int i = mSelectionStart; i >= 0; i--) {
                if (i == 0) {
                  mSelectionStart = 0;
                  break;
                } else if (str.charAt(i) == '\n') {
                  mSelectionStart = i;
                  break;
                }
              }
            }

            str.setSpan(new AlignmentSpan.Standard(Alignment.ALIGN_OPPOSITE), mSelectionStart, mSelectionEnd, Spannable.SPAN_MARK_MARK);
            mAlignRightToggleButton.setChecked(true);
            mAlignCenterToggleButton.setChecked(false);
            mAlignLeftToggleButton.setChecked(false);

            mBulletListToggleButton.setEnabled(false);
          } else {
            mAlignRightToggleButton.setChecked(false);
            mAlignCenterToggleButton.setChecked(false);
            mAlignLeftToggleButton.setChecked(false);

            mBulletListToggleButton.setEnabled(true);
          }

        } else if (tag.equals("aCenter")) {

          AlignmentSpan[] as = str.getSpans(mSelectionStart, mSelectionEnd, AlignmentSpan.class);
          boolean exists = false;
          for (int i = 0; i < as.length; i++) {
            Layout.Alignment align = ((AlignmentSpan) as[i]).getAlignment();
            if (align == Layout.Alignment.ALIGN_CENTER) {
              exists = true;
            }
            str.removeSpan(as[i]);
          }

          if (!exists) {

            if (mSelectionEnd == textEnd) {
            } else {
              for (int i = mSelectionEnd; i <= textEnd; i++) {
                if (i == textEnd) {
                  mSelectionEnd = textEnd;
                  break;
                } else if (str.charAt(i) == '\n') {
                  mSelectionEnd = i;
                  break;
                }
              }
              for (int i = mSelectionStart; i >= 0; i--) {
                if (i == 0) {
                  mSelectionStart = 0;
                  break;
                } else if (str.charAt(i) == '\n') {
                  mSelectionStart = i;
                  break;
                }
              }
            }

            str.setSpan(new AlignmentSpan.Standard(Alignment.ALIGN_CENTER), mSelectionStart, mSelectionEnd, Spannable.SPAN_MARK_MARK);
            mAlignRightToggleButton.setChecked(false);
            mAlignCenterToggleButton.setChecked(true);
            mAlignLeftToggleButton.setChecked(false);

            mBulletListToggleButton.setEnabled(false);
          } else {
            mAlignRightToggleButton.setChecked(false);
            mAlignCenterToggleButton.setChecked(false);
            mAlignLeftToggleButton.setChecked(false);

            mBulletListToggleButton.setEnabled(true);
          }

        } else if (tag.equals("aLeft")) {

          AlignmentSpan[] as = str.getSpans(mSelectionStart, mSelectionEnd, AlignmentSpan.class);
          boolean exists = false;
          for (int i = 0; i < as.length; i++) {
            Layout.Alignment align = ((AlignmentSpan) as[i]).getAlignment();
            if (align == Layout.Alignment.ALIGN_NORMAL) {
              exists = true;
            }
            str.removeSpan(as[i]);
          }

          if (!exists) {

            if (mSelectionEnd == textEnd) {
            } else {
              for (int i = mSelectionEnd; i <= textEnd; i++) {
                if (i == textEnd) {
                  mSelectionEnd = textEnd;
                  break;
                } else if (str.charAt(i) == '\n') {
                  mSelectionEnd = i;
                  break;
                }
              }
              for (int i = mSelectionStart; i >= 0; i--) {
                if (i == 0) {
                  mSelectionStart = 0;
                  break;
                } else if (str.charAt(i) == '\n') {
                  mSelectionStart = i;
                  break;
                }
              }
            }

            str.setSpan(new AlignmentSpan.Standard(Alignment.ALIGN_NORMAL), mSelectionStart, mSelectionEnd, Spannable.SPAN_MARK_MARK);
            mAlignRightToggleButton.setChecked(false);
            mAlignCenterToggleButton.setChecked(false);
            mAlignLeftToggleButton.setChecked(true);

            mBulletListToggleButton.setEnabled(false);
          } else {
            mAlignRightToggleButton.setChecked(false);
            mAlignCenterToggleButton.setChecked(false);
            mAlignLeftToggleButton.setChecked(false);

            mBulletListToggleButton.setEnabled(true);
          }

        } else if (tag.equals("ul")) {

          if ((mSelectionStart == textEnd) || (str.charAt(mSelectionStart) == '\n')) {
            mSelectionEnd = mSelectionStart;
            mSelectionStart--;
          } else {
            mSelectionEnd = mSelectionStart+1;
          }
          BulletSpan[] bs = str.getSpans(mSelectionStart, mSelectionEnd, BulletSpan.class);

          boolean exists = false;
          for (int i = 0; i < bs.length; i++) {
            str.removeSpan(bs[i]);
            exists = true;
          }

          if (!exists) {
            if (mSelectionEnd == textEnd) {
            } else {
              for (int i = mSelectionEnd; i <= textEnd; i++) {
                if (i == textEnd) {
                  mSelectionEnd = textEnd;
                  break;
                } else if (str.charAt(i) == '\n') {
                  mSelectionEnd = i;
                  break;
                }
              }
            }

            if (mSelectionStart == 0) {
            } else {
              for (int i = mSelectionStart; i >= 0; i--) {
                if (i == 0) {
                  mSelectionStart = 0;
                  break;
                } else if (str.charAt(i) == '\n') {
                  mSelectionStart = i+1;
                  break;
                }
              }
            }

            str.setSpan(new BulletSpan(LIST_INDENT), mSelectionStart, mSelectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            toggleButton.setChecked(true);

            mAlignRightToggleButton.setEnabled(false);
            mAlignCenterToggleButton.setEnabled(false);
            mAlignLeftToggleButton.setEnabled(false);
            mTitleToggleButton.setEnabled(false);
          } else {
            toggleButton.setChecked(false);

            mAlignRightToggleButton.setEnabled(true);
            mAlignCenterToggleButton.setEnabled(true);
            mAlignLeftToggleButton.setEnabled(true);
            mTitleToggleButton.setEnabled(true);
          }
          htmlList.clear();

        } else if (tag.equals("brush")) {

          if ((mSelectionStart == textEnd) || (str.charAt(mSelectionStart) == '\n')) {
            mSelectionEnd = mSelectionStart;
            mSelectionStart--;
          } else {
            mSelectionEnd = mSelectionStart+1;
          }

          if (mSelectionEnd == textEnd) {
          } else {
            for (int i = mSelectionEnd; i <= textEnd; i++) {
              if (i == textEnd) {
                mSelectionEnd = textEnd;
                break;
              } else if ((str.charAt(i) == '\n') || (str.charAt(i) == ' ')) {
                mSelectionEnd = i;
                break;
              }
            }
          }

          if (mSelectionStart == 0) {
          } else {
            for (int i = mSelectionStart; i >= 0; i--) {
              if (i == 0) {
                mSelectionStart = 0;
                break;
              } else if ((str.charAt(i) == '\n') || (str.charAt(i) == ' ')) {
                mSelectionStart = i;
                break;
              }
            }
          }

          CharacterStyle[] cs = str.getSpans(mSelectionStart, mSelectionEnd, CharacterStyle.class);
          for (int i = 0; i < cs.length; i++) {
            str.removeSpan(cs[i]);
          }

          LeadingMarginSpan[] lms = str.getSpans(mSelectionStart, mSelectionEnd, LeadingMarginSpan.class);
          for (int i = 0; i < lms.length; i++) {
            str.removeSpan(lms[i]);
          }

          AlignmentSpan[] as = str.getSpans(mSelectionStart, mSelectionEnd, AlignmentSpan.class);
          for (int i = 0; i < as.length; i++) {
            str.removeSpan(as[i]);
          }

          toggleButton.setChecked(false);
          mEditText.clearFocus();
        }
      } else if (!toggleButton.isChecked()) {

        if (tag.equals("h2")) {
          RelativeSizeSpan[] rs = str.getSpans(mSelectionStart, mSelectionEnd, RelativeSizeSpan.class);
          for (int i = 0; i < rs.length; i++) {
            str.removeSpan(rs[i]);
          }

          StyleSpan[] ss = str.getSpans(mSelectionStart, mSelectionEnd, StyleSpan.class);
          for (int i = 0; i < ss.length; i++) {
            if (ss[i].getStyle() == android.graphics.Typeface.BOLD) {
              str.removeSpan(ss[i]);
            }
          }
          listToggles(true, false);
          bodyToggles(true, false);

        } else if ((tag.equals("aRight")) || (tag.equals("aCenter")) || (tag.equals("aLeft"))) {

          AlignmentSpan[] as = str.getSpans(mSelectionStart, mSelectionEnd, AlignmentSpan.class);
          for (int i = 0; i < as.length; i++) {
            str.removeSpan(as[i]);
            mAlignRightToggleButton.setChecked(false);
            mAlignCenterToggleButton.setChecked(false);
            mAlignLeftToggleButton.setChecked(false);
          }
          titleToggles(true, false);
          listToggles(true, false);

        } else if (tag.equals("ul")) {
          BulletSpan[] bs = str.getSpans(mSelectionStart, mSelectionEnd, BulletSpan.class);

          for (int i = 0; i < bs.length; i++) {
            str.removeSpan(bs[i]);
          }
          titleToggles(true, false);
          alignToggles(true, false);

        }
        if (tag.equals("b") || tag.equals("i")) {

          StyleSpan[] ss = str.getSpans(mSelectionStart - 1, mSelectionStart, StyleSpan.class);

          for (int i = 0; i < ss.length; i++) {
            int tagStart = str.getSpanStart(ss[i]);
            int tagEnd = str.getSpanEnd(ss[i]);
            if (ss[i].getStyle() == android.graphics.Typeface.BOLD && tag.equals("b")) {
              tagStart = str.getSpanStart(ss[i]);
              tagEnd = str.getSpanEnd(ss[i]);
              str.removeSpan(ss[i]);
              str.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), tagStart, tagEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            if (ss[i].getStyle() == android.graphics.Typeface.ITALIC && tag.equals("i")) {
              tagStart = str.getSpanStart(ss[i]);
              tagEnd = str.getSpanEnd(ss[i]);
              str.removeSpan(ss[i]);
              str.setSpan(new StyleSpan(android.graphics.Typeface.ITALIC), tagStart, tagEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
          }
        } else if (tag.equals("u")) {
          UnderlineSpan[] us = str.getSpans(mSelectionStart - 1, mSelectionStart, UnderlineSpan.class);
          for (int i = 0; i < us.length; i++) {
            int tagStart = str.getSpanStart(us[i]);
            int tagEnd = str.getSpanEnd(us[i]);
            str.removeSpan(us[i]);
            str.setSpan(new UnderlineSpan(), tagStart, tagEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
          }
        } else if (tag.equals("st")) {
          StrikethroughSpan[] st = str.getSpans(mSelectionStart - 1, mSelectionStart, StrikethroughSpan.class);
          for (int i = 0; i < st.length; i++) {
            int tagStart = str.getSpanStart(st[i]);
            int tagEnd = str.getSpanEnd(st[i]);
            str.removeSpan(st[i]);
            str.setSpan(new UnderlineSpan(), tagStart, tagEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
          }
        } else if (tag.equals("link")) {

          URLSpan[] us = str.getSpans(mSelectionStart-1, mSelectionEnd, URLSpan.class);
          URLSpan u = null;
          boolean exists = false;
          for (int i = 0; i < us.length; i++) {
            mSelectionStart = str.getSpanStart(us[i]);
            mSelectionEnd = str.getSpanEnd(us[i]);
            exists = true;
            u = us[i];
          }

          if (exists) {
            toggleButton.setChecked(true);
            FragmentManager fm = getFragmentManager();
            EditLinksDialog eld = EditLinksDialog.getInstance(u.getURL());
            eld.setTargetFragment(this, EDIT_LINKS_DIALOG_FRAGMENT);
            eld.show(fm, "edit_links_layout");
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace(); // DEBUG
    }
  }

  /**
   * Callback for the Apply Button.
   * 
   * @param layout
   *          new save text button click listener
   */
  private void setSaveTextButtonClickListener(View layout)
  {
    Button selectTextButton = (Button) layout.findViewById(R.id.editor_saveText);

    selectTextButton.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v)
      {
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        String s = MyHtml.toHtml(mEditText.getText());
        FragmentActionListener listener = ((FragmentActionListener) getActivity());
        if (s == null) {
          listener.cancelTextEditor();
        }

        long elapsedTime = System.currentTimeMillis() - mStartTime;
        if (elapsedTime > TIMEOUT) {
          listener.draftEditedText(s, getArguments().getString(TEXT_BLOCK_ID_TAG));
        } else {
          listener.sendEditedText(s, getArguments().getString(TEXT_BLOCK_ID_TAG));
        }
      }
    });
  }
}