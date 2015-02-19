package com.example.richtexteditor.db;

/**
 * Class TextBlocks. Java representation of table in DB.
 */
public class TextBlocks {
  private int id;
  private long blockTime;
  private String blockId;
  private String blockContent;
  private int blockVersion;

  /**
   * Gets the id.
   * 
   * @return id
   */
  public int getId() {
    return id;
  }

  /**
   * Sets the id.
   * 
   * @param i
   *          new id
   */
  public void setId(int i) {
    this.id = i;
  }

  
  /**
   * Gets the time.
   * 
   * @return time
   */
  public long getTime() {
    return blockTime;
  }

  /**
   * Sets the time.
   * 
   * @param t
   *          new time
   */
  public void setTime(long t) {
    this.blockTime = t;
  }
  
  /**
   * Gets the block id.
   * 
   * @return block id
   */
  public String getBlockId() {
    return blockId;
  }

  /**
   * Sets the block id.
   * 
   * @param s
   *          new block id
   */
  public void setBlockId(String s) {
    this.blockId = s;
  }

  /**
   * Gets the block content.
   * 
   * @return block content
   */
  public String getBlockContent() {
    return blockContent;
  }

  /**
   * Sets the block content.
   * 
   * @param s
   *          new block content
   */
  public void setBlockContent(String s) {
    this.blockContent = s;
  }

  /**
   * Gets the block version.
   * 
   * @return block version
   */
  public int getBlockVersion() {
    return blockVersion;
  }

  /**
   * Sets the block version.
   * 
   * @param i
   *          new block version
   */
  public void setBlockVersion(int i) {
    this.blockVersion = i;
  }
}