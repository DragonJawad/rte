package com.example.richtexteditor;

/**
 * Interface actions implemented by MainActivity.
 */
public interface FragmentActionListener {
  public void cancelTextEditor();
  public void sendEditedText(String editedTextBlock, String editedTextBlockId);
  public void draftEditedText(String editedTextBlock, String editedTextBlockId);
}