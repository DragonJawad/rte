package com.example.richtexteditor;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Stack;

import org.ccil.cowan.tagsoup.HTMLSchema;
import org.ccil.cowan.tagsoup.Parser;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.example.richtexteditor.HtmlToSpannedConverter.List;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.Layout;
import android.text.Layout.Alignment;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.AlignmentSpan;
import android.text.style.BulletSpan;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.ParagraphStyle;
import android.text.style.QuoteSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.SubscriptSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.TextAppearanceSpan;
import android.text.style.TypefaceSpan;
import android.text.style.URLSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;

/**
 * HTML renderer class.
 * 
 * File is from https://android.googlesource.com/platform/frameworks/base
 * git tag android-4.3_r3.1
 */
public class MyHtml {

  /**
   * Instantiates a new MyHtml.
   */
  public MyHtml() { }

  public final static String BULLET_SPAN_TAG = "ul";
  private static MyArrayList htmlList = new MyArrayList();

  private static class HtmlParser {
    private static final HTMLSchema schema = new HTMLSchema();
  }

  public static Spanned fromHtml(String htmlSource) {
    Parser parser = new Parser();
    try {
      parser.setProperty(Parser.schemaProperty, HtmlParser.schema);
    } catch (org.xml.sax.SAXNotRecognizedException e) {
      throw new RuntimeException(e);
    } catch (org.xml.sax.SAXNotSupportedException e) {
      throw new RuntimeException(e);
    }

    List list = new List();
    HtmlToSpannedConverter converter = new HtmlToSpannedConverter(htmlSource, list, parser);

    return converter.convert();
  }

  /**
   * From spanned to Html.
   */
  public static String toHtml(Spanned text) {
    try {
      StringBuilder out = new StringBuilder();
      withinHtml(out, text);
      return out.toString();
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Within html.
   * 
   * @param out
   * @param text
   */
  private static void withinHtml(StringBuilder out, Spanned text) {
    int len = text.length();
    htmlList.clear();

    int paraEnd;
    for (int i = 0; i < text.length(); i = paraEnd) {
      paraEnd = text.nextSpanTransition(i, len, ParagraphStyle.class);

      ParagraphStyle[] ps = text.getSpans(i, paraEnd, ParagraphStyle.class);
      String elements = " ";
      boolean needDiv = false;

      for(int x = 0; x < ps.length; x++) {
        if (ps[x] instanceof AlignmentSpan) {
          Layout.Alignment align = ((AlignmentSpan) ps[x]).getAlignment();
          needDiv = true;
          if (align == Layout.Alignment.ALIGN_OPPOSITE) {
            elements = "align=\"right\" " + elements;
          } else if (align == Layout.Alignment.ALIGN_CENTER) {
            elements = "align=\"center\" " + elements;
          }
        }
      }

      if (needDiv) {
        out.append("<div ").append(elements).append(">");
      }

      int next;
      for (int j = i; j < paraEnd; j = next) {
        next = TextUtils.indexOf(text, '\n', j, paraEnd);
        if (next < 0) {
          next = paraEnd;
        }

        int nl = 0;

        while (next < paraEnd && text.charAt(next) == '\n') {
          nl++;
          next++;
        }

        boolean header = false;
        CharacterStyle[] cs = text.getSpans(j, next - nl, CharacterStyle.class);
        for (int k = 0; k < cs.length; k++) {
          if (cs[k] instanceof RelativeSizeSpan) {
            header = true;
          }
        }

        if (header) {
          withinHeader(out, text, j, next - nl, nl, next == paraEnd);
        } else {
          withinParagraph(out, text, j, next - nl, nl, next == paraEnd);
        }
      }

      if (needDiv) {
        out.append("</div>");
      }
    }

    // list is last element of text.
    if (htmlList.isSet()) {
      withinList(out, text);
      htmlList.clear();
    }
  }

  /**
   * Within header.
   * 
   * @param out
   * @param text
   * @param start
   * @param end
   * @param nl
   * @param last
   */
  private static void withinHeader(StringBuilder out, Spanned text, int start, int end, int nl, boolean last) {
    if (htmlList.isSet()) {
      withinList(out, text);
      htmlList.clear();
    }

    out.append("<h2>");
    applyStyles(out, text, start, end, nl, last, true);
    out.append("</h2>");
  }

  /**
   * Within paragraph.
   * 
   * @param out
   * @param text
   * @param start
   * @param end
   * @param nl
   * @param last
   */
  private static void withinParagraph(StringBuilder out, Spanned text, int start, int end, int nl, boolean last) {
    ParagraphStyle[] paraStyle = text.getSpans(start, end, ParagraphStyle.class);
    for(int l = 0; l < paraStyle.length; l++) {
      if (paraStyle[l] instanceof LeadingMarginSpan) {
        if (htmlList.isSet()) {
          if (paraStyle[l] instanceof QuoteSpan) {
          } else if (paraStyle[l] instanceof BulletSpan) {
            htmlList.addRow(start, end);
          } else {
            htmlList.addRow(start, end);
          }
        } else {
          if (paraStyle[l] instanceof QuoteSpan) {
          } else if (paraStyle[l] instanceof BulletSpan) {
            htmlList.setType(BULLET_SPAN_TAG);
            htmlList.addRow(start, end);
          }
        }
        return;
      }
    }

    if (htmlList.isSet()) {
      withinList(out, text);
      htmlList.clear();
    }

    out.append("<p>");
    applyStyles(out, text, start, end, nl, last, false);
    out.append("</p>");
  }

  /**
   * Within list.
   * 
   * @param out
   * @param text
   */
  private static void withinList(StringBuilder out, Spanned text) {
    MyArrayList.Row htmlListRow;

    if (htmlList.getType() == BULLET_SPAN_TAG) {
      out.append("<p><ul>");
      for(int m = 0; m < htmlList.listRows(); m++) {
        htmlListRow = htmlList.getRow(m);
        if (htmlListRow.start < htmlListRow.end) {
          out.append("<li>");
          applyStyles(out, text, htmlListRow.start, htmlListRow.end, 0, false, false);
          out.append("</li>");
        }
      }
      out.append("</ul></div></p>");
    }
  }

  /**
   * Apply styles.
   * 
   * @param out
   * @param text
   * @param start
   * @param end
   * @param nl
   * @param last
   * @param header
   */
  private static void applyStyles(StringBuilder out, Spanned text, int start, int end, int nl, boolean last, boolean header) {
    int next;
    for (int i = start; i < end; i = next) {
      next = text.nextSpanTransition(i, end, CharacterStyle.class);
      CharacterStyle[] style = text.getSpans(i, next,
          CharacterStyle.class);

      if (!header) {
        for (int j = 0; j < style.length; j++) {
          if (style[j] instanceof StyleSpan) {
            int s = ((StyleSpan) style[j]).getStyle();

            if ((s & Typeface.BOLD) != 0) {
              out.append("<strong>");
            }
            if ((s & Typeface.ITALIC) != 0) {
              out.append("<i>");
            }
          }
          if (style[j] instanceof TypefaceSpan) {
            String s = ((TypefaceSpan) style[j]).getFamily();

            if (s.equals("monospace")) {
              out.append("<tt>");
            }
          }
          if (style[j] instanceof SuperscriptSpan) {
            out.append("<sup>");
          }
          if (style[j] instanceof SubscriptSpan) {
            out.append("<sub>");
          }
          if (style[j] instanceof UnderlineSpan) {
            out.append("<u>");
          }
          if (style[j] instanceof StrikethroughSpan) {
            out.append("<strike>");
          }
          if (style[j] instanceof URLSpan) {
            out.append("<a href=\"");
            out.append(((URLSpan) style[j]).getURL());
            out.append("\">");
          }
          if (style[j] instanceof AbsoluteSizeSpan) {
            out.append("<font size =\"");
            out.append(((AbsoluteSizeSpan) style[j]).getSize() / 6);
            out.append("\">");
          }
          if (style[j] instanceof ForegroundColorSpan) {
            out.append("<font color =\"#");
            String color = Integer.toHexString(((ForegroundColorSpan)
                style[j]).getForegroundColor() + 0x01000000);
            while (color.length() < 6) {
              color = "0" + color;
            }
            out.append(color);
            out.append("\">");
          }
        }
      }

      constructChars(out, text, i, next);

      if (!header) {

        for (int j = style.length - 1; j >= 0; j--) {
          if (style[j] instanceof ForegroundColorSpan) {
            out.append("</font>");
          }
          if (style[j] instanceof AbsoluteSizeSpan) {
            out.append("</font>");
          }
          if (style[j] instanceof URLSpan) {
            out.append("</a>");
          }
          if (style[j] instanceof StrikethroughSpan) {
            out.append("</strike>");
          }
          if (style[j] instanceof UnderlineSpan) {
            out.append("</u>");
          }
          if (style[j] instanceof SubscriptSpan) {
            out.append("</sub>");
          }
          if (style[j] instanceof SuperscriptSpan) {
            out.append("</sup>");
          }
          if (style[j] instanceof TypefaceSpan) {
            String s = ((TypefaceSpan) style[j]).getFamily();

            if (s.equals("monospace")) {
              out.append("</tt>");
            }
          }
          if (style[j] instanceof StyleSpan) {
            int s = ((StyleSpan) style[j]).getStyle();

            if ((s & Typeface.BOLD) != 0) {
              out.append("</strong>");
            }
            if ((s & Typeface.ITALIC) != 0) {
              out.append("</i>");
            }
          }
        }
      }
    }

    if (!header) {
      for (int i = 1; i < nl; i++) {
        out.append("<br>");
      }

    }
  }

  /**
   * If character issues, check http://www.ascii.cl/htmlcodes.htm
   * 
   * @param out
   * @param text
   * @param start
   * @param end
   */
  private static void constructChars(StringBuilder out, CharSequence text, int start, int end) {
    for (int i = start; i < end; i++) {
      char c = text.charAt(i);

      if (c == '<') {
        out.append("&lt;");
      } else if (c == '>') {
        out.append("&gt;");
      } else if (c == '&') {
        out.append("&amp;");
      } else if (c == '\'') {
        out.append("&#39;");
      } else if (c > 0x7E || c < ' ') {
        out.append("&#").append((int) c).append(";");
      } else if (c == ' ') {
        while (i + 1 < end && text.charAt(i + 1) == ' ') {
          out.append("&nbsp;");
          i++;
        }
        out.append(' ');
      } else {
        out.append(c);
      }
    }
  }

  /**
   * Class List.
   */
  public static class MyArrayList {
    private String type = "";
    private ArrayList<Row> al = new ArrayList<Row>();

    /**
     * Set type of HtmlList
     * 
     * @param s
     */
    public void setType (String s) {
      this.type = s;
    }

    /**
     * Is type set.
     * 
     * @return true, if is sets the
     */
    public boolean isSet() {
      if (this.type.isEmpty()) {
        return false;
      } else {
        return true;
      }
    }

    /**
     * Get type of HtmlList.
     * 
     * @return type
     */
    public String getType () {
      return this.type;
    }

    /**
     * Add new row to list.
     * 
     * @param start
     * @param end
     */
    public void addRow (int start, int end) {
      al.add(new Row(start, end));
    }

    /**
     * Replace last row in list.
     * 
     * @param start
     * @param end
     */
    public void replaceLastRow (int start, int end) {
      al.set(listRows(), new Row(start, end));
    }

    /**
     * Number of rows in list.
     * 
     * @return int
     */
    public int listRows () {
      return al.size();
    }

    /**
     * Return ListRow.
     * 
     * @param i
     * @return row
     */
    public Row getRow (int i) {
      return al.get(i);
    }

    /**
     * Clear the list.
     */
    public void clear () {
      this.type = "";
      this.al.clear();
    }

    /**
     * Class Row.
     */
    public class Row {
      public int start;
      public int end;

      /**
       * Instantiates a new row.
       * 
       * @param rowStart
       * @param rowEnd
       */
      public Row (int rowStart, int rowEnd) {
        this.start = rowStart;
        this.end = rowEnd;
      }    
    }
  }
}

class HtmlToSpannedConverter implements ContentHandler {

  private static final float[] HEADER_SIZES = { 1.5f, 1.4f, 1.3f, 1.2f, 1.1f, 1f, };

  private String mSource;
  private XMLReader mReader;
  private SpannableStringBuilder mSpannableStringBuilder;
  private List mListHandler;

  AlignRight ar = new AlignRight();
  AlignCenter ac = new AlignCenter();
  AlignLeft al = new AlignLeft();

  public HtmlToSpannedConverter(String source, List list, Parser parser) {
    mSource = source;
    mSpannableStringBuilder = new SpannableStringBuilder();
    mListHandler = list;
    mReader = parser;
  }

  public Spanned convert() {
    mReader.setContentHandler(this);
    try {
      mReader.parse(new InputSource(new StringReader(mSource)));
    } catch (IOException e) {
      throw new RuntimeException(e);
    } catch (SAXException e) {
      throw new RuntimeException(e);
    }

    Object[] obj = mSpannableStringBuilder.getSpans(0, mSpannableStringBuilder.length(), ParagraphStyle.class);
    for (int i = 0; i < obj.length; i++) {
      int start = mSpannableStringBuilder.getSpanStart(obj[i]);
      int end = mSpannableStringBuilder.getSpanEnd(obj[i]);

      if (end - 2 >= 0) {
        if (mSpannableStringBuilder.charAt(end - 1) == '\n' &&
            mSpannableStringBuilder.charAt(end - 2) == '\n') {
          end--;
        }
      }

      if (end == start) {
        mSpannableStringBuilder.removeSpan(obj[i]);
      } else {
        mSpannableStringBuilder.setSpan(obj[i], start, end, Spannable.SPAN_PARAGRAPH);
      }
    }

    return mSpannableStringBuilder;
  }

  private void handleStartTag(String tag, Attributes attributes) {
    if (tag.equalsIgnoreCase("br")) {
    } else if (tag.equalsIgnoreCase("p")) {
      handleP(mSpannableStringBuilder);
    } else if (tag.equalsIgnoreCase("div")) {
      handleP(mSpannableStringBuilder);
      if (attributes.getValue(0) != null) {
        if (attributes.getValue(0).equalsIgnoreCase("right")) {
          ar.set(true);
          start(mSpannableStringBuilder, ar);
        } else if (attributes.getValue(0).equalsIgnoreCase("center")) {
          ac.set(true);
          start(mSpannableStringBuilder, ac);
        } else {
          al.set(true);
          start(mSpannableStringBuilder, al);
        }
      }
    } else if (tag.equalsIgnoreCase("strong")) {
      start(mSpannableStringBuilder, new Bold());
    } else if (tag.equalsIgnoreCase("b")) {
      start(mSpannableStringBuilder, new Bold());
    } else if (tag.equalsIgnoreCase("em")) {
      start(mSpannableStringBuilder, new Italic());
    } else if (tag.equalsIgnoreCase("cite")) {
      start(mSpannableStringBuilder, new Italic());
    } else if (tag.equalsIgnoreCase("dfn")) {
      start(mSpannableStringBuilder, new Italic());
    } else if (tag.equalsIgnoreCase("i")) {
      start(mSpannableStringBuilder, new Italic());
    } else if (tag.equalsIgnoreCase("big")) {
      start(mSpannableStringBuilder, new Big());
    } else if (tag.equalsIgnoreCase("small")) {
      start(mSpannableStringBuilder, new Small());
    } else if (tag.equalsIgnoreCase("font")) {
      startFont(mSpannableStringBuilder, attributes);
    } else if (tag.equalsIgnoreCase("blockquote")) {
      handleP(mSpannableStringBuilder);
      start(mSpannableStringBuilder, new Blockquote());
    } else if (tag.equalsIgnoreCase("tt")) {
      start(mSpannableStringBuilder, new Monospace());
    } else if (tag.equalsIgnoreCase("a")) {
      startA(mSpannableStringBuilder, attributes);
    } else if (tag.equalsIgnoreCase("u")) {
      start(mSpannableStringBuilder, new Underline());
    } else if (tag.equalsIgnoreCase("strike")) {
      start(mSpannableStringBuilder, new Strikethrough());
    } else if (tag.equalsIgnoreCase("sup")) {
      start(mSpannableStringBuilder, new Super());
    } else if (tag.equalsIgnoreCase("sub")) {
      start(mSpannableStringBuilder, new Sub());
    } else if (tag.length() == 2 &&
        Character.toLowerCase(tag.charAt(0)) == 'h' &&
        tag.charAt(1) >= '1' && tag.charAt(1) <= '6') {
      handleP(mSpannableStringBuilder);
      start(mSpannableStringBuilder, new Header(tag.charAt(1) - '1'));
    } else if (mListHandler != null) {
      mListHandler.handleTag(true, tag, mSpannableStringBuilder, mReader);
    }
  }

  private void handleEndTag(String tag) {
    if (tag.equalsIgnoreCase("br")) {
      handleBr(mSpannableStringBuilder);
    } else if (tag.equalsIgnoreCase("p")) {
      handleP(mSpannableStringBuilder);
    } else if (tag.equalsIgnoreCase("div")) {
      handleP(mSpannableStringBuilder);
      if (ar.isSet()) {
        end(mSpannableStringBuilder, AlignRight.class, new AlignmentSpan.Standard(Alignment.ALIGN_OPPOSITE));
      } else if (ac.isSet()) {
        end(mSpannableStringBuilder, AlignCenter.class, new AlignmentSpan.Standard(Alignment.ALIGN_CENTER));
      } else if (al.isSet()) {
        end(mSpannableStringBuilder, AlignLeft.class, new AlignmentSpan.Standard(Alignment.ALIGN_NORMAL));
      }
      ar.set(false);
      ac.set(false);
      al.set(false);
    } else if (tag.equalsIgnoreCase("strong")) {
      end(mSpannableStringBuilder, Bold.class, new StyleSpan(Typeface.BOLD));
    } else if (tag.equalsIgnoreCase("b")) {
      end(mSpannableStringBuilder, Bold.class, new StyleSpan(Typeface.BOLD));
    } else if (tag.equalsIgnoreCase("em")) {
      end(mSpannableStringBuilder, Italic.class, new StyleSpan(Typeface.ITALIC));
    } else if (tag.equalsIgnoreCase("cite")) {
      end(mSpannableStringBuilder, Italic.class, new StyleSpan(Typeface.ITALIC));
    } else if (tag.equalsIgnoreCase("dfn")) {
      end(mSpannableStringBuilder, Italic.class, new StyleSpan(Typeface.ITALIC));
    } else if (tag.equalsIgnoreCase("i")) {
      end(mSpannableStringBuilder, Italic.class, new StyleSpan(Typeface.ITALIC));
    } else if (tag.equalsIgnoreCase("big")) {
      end(mSpannableStringBuilder, Big.class, new RelativeSizeSpan(1.25f));
    } else if (tag.equalsIgnoreCase("small")) {
      end(mSpannableStringBuilder, Small.class, new RelativeSizeSpan(0.8f));
    } else if (tag.equalsIgnoreCase("font")) {
      endFont(mSpannableStringBuilder);
    } else if (tag.equalsIgnoreCase("blockquote")) {
      handleP(mSpannableStringBuilder);
      end(mSpannableStringBuilder, Blockquote.class, new QuoteSpan());
    } else if (tag.equalsIgnoreCase("tt")) {
      end(mSpannableStringBuilder, Monospace.class,
          new TypefaceSpan("monospace"));
    } else if (tag.equalsIgnoreCase("a")) {
      endA(mSpannableStringBuilder);
    } else if (tag.equalsIgnoreCase("u")) {
      end(mSpannableStringBuilder, Underline.class, new UnderlineSpan());
    } else if (tag.equalsIgnoreCase("strike")) {
      end(mSpannableStringBuilder, Strikethrough.class, new StrikethroughSpan());
    } else if (tag.equalsIgnoreCase("sup")) {
      end(mSpannableStringBuilder, Super.class, new SuperscriptSpan());
    } else if (tag.equalsIgnoreCase("sub")) {
      end(mSpannableStringBuilder, Sub.class, new SubscriptSpan());
    } else if (tag.length() == 2 &&
        Character.toLowerCase(tag.charAt(0)) == 'h' &&
        tag.charAt(1) >= '1' && tag.charAt(1) <= '6') {
      handleP(mSpannableStringBuilder);
      endHeader(mSpannableStringBuilder);
    } else if (mListHandler != null) {
      mListHandler.handleTag(false, tag, mSpannableStringBuilder, mReader);
    }
  }

  private static void handleP(SpannableStringBuilder text) {
    int len = text.length();

    if (len >= 1 && text.charAt(len - 1) == '\n') {
      if (len >= 2 && text.charAt(len - 2) == '\n') {
        return;
      }

      text.append("\n");
      return;
    }

    if (len != 0) {
      text.append("\n\n");
    }
  }

  private static void handleBr(SpannableStringBuilder text) {
    text.append("\n");
  }

  private static Object getLast(Spanned text, Class<?> kind) {
    /*
     * This knows that the last returned object from getSpans()
     * will be the most recently added.
     */
    Object[] objs = text.getSpans(0, text.length(), kind);

    if (objs.length == 0) {
      return null;
    } else {
      return objs[objs.length - 1];
    }
  }

  private static void start(SpannableStringBuilder text, Object mark) {
    int len = text.length();
    text.setSpan(mark, len, len, Spannable.SPAN_MARK_MARK);
  }

  private static void end(SpannableStringBuilder text, Class<?> kind, Object repl) {
    int len = text.length();
    Object obj = getLast(text, kind);
    int where = text.getSpanStart(obj);

    text.removeSpan(obj);

    if (where != len) {
      text.setSpan(repl, where, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
  }

  private static void startFont(SpannableStringBuilder text, Attributes attributes) {
    String color = attributes.getValue("", "color");
    String face = attributes.getValue("", "face");

    int len = text.length();
    text.setSpan(new Font(color, face), len, len, Spannable.SPAN_MARK_MARK);
  }

  private static void endFont(SpannableStringBuilder text) {
    int len = text.length();
    Object obj = getLast(text, Font.class);
    int where = text.getSpanStart(obj);

    text.removeSpan(obj);

    if (where != len) {
      Font f = (Font) obj;

      if (!TextUtils.isEmpty(f.mColor)) {
        if (f.mColor.startsWith("@")) {
          Resources res = Resources.getSystem();
          String name = f.mColor.substring(1);
          int colorRes = res.getIdentifier(name, "color", "android");
          if (colorRes != 0) {
            ColorStateList colors = res.getColorStateList(colorRes);
            text.setSpan(new TextAppearanceSpan(null, 0, 0, colors, null),
                where, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
          }
        }/* else {
          int c = Color.getHtmlColor(f.mColor);
          if (c != -1) {
            text.setSpan(new ForegroundColorSpan(c | 0xFF000000),
                where, len,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
          }
        }*/
      }

      if (f.mFace != null) {
        text.setSpan(new TypefaceSpan(f.mFace), where, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
      }
    }
  }

  private static void startA(SpannableStringBuilder text, Attributes attributes) {
    String href = attributes.getValue("", "href");

    int len = text.length();
    text.setSpan(new Href(href), len, len, Spannable.SPAN_MARK_MARK);
  }

  private static void endA(SpannableStringBuilder text) {
    int len = text.length();
    Object obj = getLast(text, Href.class);
    int where = text.getSpanStart(obj);

    text.removeSpan(obj);

    if (where != len) {
      Href h = (Href) obj;

      if (h.mHref != null) {
        text.setSpan(new URLSpan(h.mHref), where, len,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
      }
    }
  }

  private static void endHeader(SpannableStringBuilder text) {
    int len = text.length();
    Object obj = getLast(text, Header.class);

    int where = text.getSpanStart(obj);

    text.removeSpan(obj);

    // Back off not to change only the text, not the blank line.
    while (len > where && text.charAt(len - 1) == '\n') {
      len--;
    }

    if (where != len) {
      Header h = (Header) obj;

      text.setSpan(new RelativeSizeSpan(HEADER_SIZES[h.mLevel]), where, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
      text.setSpan(new StyleSpan(Typeface.BOLD), where, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
  }

  public void setDocumentLocator(Locator locator) { }

  public void startDocument() throws SAXException { }

  public void endDocument() throws SAXException { }

  public void startPrefixMapping(String prefix, String uri) throws SAXException { }

  public void endPrefixMapping(String prefix) throws SAXException { }

  public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
    handleStartTag(localName, attributes);
  }

  public void endElement(String uri, String localName, String qName) throws SAXException {
    handleEndTag(localName);
  }

  public void characters(char ch[], int start, int length) throws SAXException {
    StringBuilder sb = new StringBuilder();

    /*
     * Ignore whitespace that immediately follows other whitespace;
     * newlines count as spaces.
     */
    for (int i = 0; i < length; i++) {
      char c = ch[i + start];

      if (c == ' ' || c == '\n') {
        char pred;
        int len = sb.length();

        if (len == 0) {
          len = mSpannableStringBuilder.length();

          if (len == 0) {
            pred = '\n';
          } else {
            pred = mSpannableStringBuilder.charAt(len - 1);
          }
        } else {
          pred = sb.charAt(len - 1);
        }

        if (pred != ' ' && pred != '\n') {
          sb.append(' ');
        }
      } else {
        sb.append(c);
      }
    }

    mSpannableStringBuilder.append(sb);
  }

  public void ignorableWhitespace(char ch[], int start, int length) throws SAXException { }
  public void processingInstruction(String target, String data) throws SAXException { }
  public void skippedEntity(String name) throws SAXException { }

  private static class Bold { }
  private static class Italic { }
  private static class Underline { }
  private static class Strikethrough { }
  private static class Big { }
  private static class Small { }
  private static class Monospace { }
  private static class Blockquote { }
  private static class AlignRight {
    private boolean set = false;
    protected void set (boolean b) {
      this.set = b;
    }
    protected boolean isSet() {
      return this.set;
    }
  }
  private static class AlignCenter {
    private boolean set = false;
    protected void set (boolean b) {
      this.set = b;
    }
    protected boolean isSet() {
      return this.set;
    }
  }
  private static class AlignLeft {
    private boolean set = false;
    protected void set (boolean b) {
      this.set = b;
    }
    protected boolean isSet() {
      return this.set;
    }
  }
  private static class Super { }
  private static class Sub { }

  private static class Font {
    public String mColor;
    public String mFace;

    public Font(String color, String face) {
      mColor = color;
      mFace = face;
    }
  }

  private static class Href {
    public String mHref;

    public Href(String href) {
      mHref = href;
    }
  }

  private static class Header {
    private int mLevel;

    public Header(int level) {
      mLevel = level;
    }
  }

  /**
   * Fail-back class for special tags.
   */
  public static class List {
  
    private Stack<String> lists = new Stack<String>();
    private static final int LIST_INDENT = 7;
  
    public void handleTag(boolean opening, String tag, Editable output, XMLReader xmlReader) {
      if (tag.equalsIgnoreCase("ul")) {
        if (opening) {
          lists.push(tag);
        } else {
          lists.pop();
        }
      }
      else if (tag.equalsIgnoreCase("li")) {
        if (opening) {
          if (output.length() > 0 && output.charAt(output.length()-1) != '\n') {
            output.append("\n");
          }
          String parentList = lists.peek();

          if (parentList.equalsIgnoreCase("ul")) {
            start(output, new Ul());
          }
        } else {
          if (lists.peek().equalsIgnoreCase("ul")) {
            if (output.length() > 0 && output.charAt(output.length()-1) != '\n' ) {
              output.append("\n");
            }
  
            end(output, Ul.class, new BulletSpan(LIST_INDENT));
          }
        }
      } else {
        if (opening) Log.d("TagHandler", "Found an unsupported tag " + tag);
      }
    }
  
    /**
     * Start.
     * 
     * @param text
     * @param mark
     */
    private void start(Editable text, Object mark) {
      int len = text.length();
      text.setSpan(mark, len, len, Spanned.SPAN_MARK_MARK);
    }
  
    /**
     * End.
     * 
     * @param text
     * @param kind
     * @param replaces
     */
    private void end(Editable text, Class<?> kind, Object... replaces) {
      int len = text.length();
      Object obj = getLast(text, kind);
      int where = text.getSpanStart(obj);
      text.removeSpan(obj);
      if (where != len) {
        for (Object replace: replaces) {
          text.setSpan(replace, where, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
      }
      return;
    }
  
    /**
     * Gets the last.
     * 
     * @param text
     * @param kind
     * @return last
     */
    private Object getLast(Spanned text, Class<?> kind) {
      Object[] objs = text.getSpans(0, text.length(), kind);
      if (objs.length == 0) {
        return null;
      }
      return objs[objs.length - 1];
    }
  
    /**
     * Class Ul.
     */
    private class Ul { }
  }
}