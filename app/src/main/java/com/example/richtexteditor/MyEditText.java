package com.example.richtexteditor;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import android.widget.EditText;

/**
 * EditText extension.
 */
public class MyEditText extends EditText {

  private OnSelectionChangedListener onSelectionChangedListener;

  /**
   * Instantiates a new MyEditText.
   * 
   * @param context
   */
  public MyEditText(Context context) {
    super(context);
  }

  /**
   * Instantiates a new MyEditText.
   * 
   * @param context
   * @param attrs
   */
  public MyEditText(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  /**
   * Instantiates a new MyEditText.
   * 
   * @param context
   * @param attrs
   * @param defStyle
   */
  public MyEditText(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  @Override
  protected void onSelectionChanged(int selStart, int selEnd) {
    if (onSelectionChangedListener != null)
      onSelectionChangedListener.onSelectionChanged();
  }

  /**
   * Sets the on selection changed listener.
   * 
   * @param listener
   */
  public void setOnSelectionChangedListener(OnSelectionChangedListener listener) {
    onSelectionChangedListener = listener;
  }

  /**
   * The listener interface for receiving onSelectionChanged events. Class that
   * is interested in processing a onSelectionChanged event implements this
   * interface, and the object created with that class is registered with a
   * component using the component's
   * <code>addOnSelectionChangedListener<code> method.
   * 
   * When onSelectionChanged event occurs, that object's appropriate method is invoked.
   * 
   * @see OnSelectionChangedEvent
   */
  public interface OnSelectionChangedListener {
    public abstract void onSelectionChanged();
  }

  @Override
  public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
    return new MyInputConnection(super.onCreateInputConnection(outAttrs), true);
  }

  /**
   * Confirmed working Build.VERSION_CODES.JELLY_BEAN (16), should older as well, but not newer.
   */
  private class MyInputConnection extends InputConnectionWrapper {

    /**
     * Instantiates a new input connection.
     * 
     * @param target
     * @param mutable
     */
    public MyInputConnection(InputConnection target, boolean mutable) {
      super(target, mutable);
    }

    @Override
    public boolean sendKeyEvent(KeyEvent event) {
      if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
        MyTextEditorFragment.setIsEnter();
      }
      if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
        MyTextEditorFragment.setIsBackspace();
      }
      return super.sendKeyEvent(event);
    }
  }
}