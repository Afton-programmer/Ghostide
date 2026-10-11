package ir.hanzodev1375.ghostide.models;

import android.graphics.drawable.Drawable;

public class ToolbarModel {
  private int icon;
  private final Drawable drawable;
  private String tag;
  private boolean showVisblityItem;
  private boolean visible = true;

  public ToolbarModel(int icon, String tag, boolean showVisblityItem) {
    this.icon = icon;
    this.tag = tag;
    this.showVisblityItem = showVisblityItem;
    this.drawable = null;
  }

  public ToolbarModel(int icon, String tag) {
    this.icon = icon;
    this.tag = tag;
    this.showVisblityItem = true;
    this.drawable = null;
  }

  public ToolbarModel(int icon, String tag, boolean showVisblityItem, boolean visible) {
    this.icon = icon;
    this.tag = tag;
    this.showVisblityItem = showVisblityItem;
    this.visible = visible;
    this.drawable = null;
  }

  public ToolbarModel(Drawable drawable, String tag) {
    this.drawable = drawable;
    this.tag = tag;
    this.showVisblityItem = true;
    this.icon = 0;
  }

  public ToolbarModel(Drawable drawable, String tag, boolean showVisblityItem) {
    this.drawable = drawable;
    this.tag = tag;
    this.showVisblityItem = showVisblityItem;
    this.icon = 0;
  }

  public int getIcon() {
    return this.icon;
  }

  public void setIcon(int icon) {
    this.icon = icon;
  }

  public Drawable getDrawable() {
    return this.drawable;
  }

  public String getTag() {
    return this.tag;
  }

  public void setTag(String tag) {
    this.tag = tag;
  }

  public boolean isShowVisblityItem() {
    return this.showVisblityItem;
  }

  public void setShowVisblityItem(boolean showVisblityItem) {
    this.showVisblityItem = showVisblityItem;
  }

  public boolean isVisible() {
    return this.visible;
  }

  public void setVisible(boolean visible) {
    this.visible = visible;
  }
}