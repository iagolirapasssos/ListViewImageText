package com.bosonshiggs.listviewimagetext;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.text.Spanned;
import androidx.core.text.HtmlCompat;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.runtime.Component;
import com.google.appinventor.components.runtime.AndroidViewComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.Form;
import com.google.appinventor.components.runtime.util.YailList;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ListViewImageText
 *
 * A dynamic ListView component showing an image and two text labels.
 * Call AnchorTo() to place it into a layout.
 */
@DesignerComponent(
    version = 1,
    versionName = "1.0",
    description = "List View Image and Text dynamic component for App Inventor/Kodular. Call AnchorTo() to place it.",
    iconName = "icon.png"
)
public class ListViewImageText extends AndroidViewComponent
    implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

  private final Form form;
  private final Context context;
  private final ListView listView;
  private final Adapter adapter;
  private final List<Item> items = new ArrayList<>();
  private final List<Item> originalItems = new ArrayList<>();
  private final Map<String, Drawable> imageCache = new HashMap<>();
  private final ExecutorService executor = Executors.newFixedThreadPool(4);
  private final Handler mainHandler = new Handler(Looper.getMainLooper());

  // Appearance properties
  private int bgColor = Color.TRANSPARENT;
  private int dividerColor = Color.parseColor("#80FFFFFF");
  private int dividerHeight = 1;
  private int imageSide = 1; // 1=left, 2=right
  private int itemSize = 1;    // 1=small, 2=medium, 3=large

  // Title properties
  private String titleFontTypeface = Component.TYPEFACE_DEFAULT;
  private boolean titleFontBold;
  private boolean titleFontItalic;
  private float titleTextSize = 16f;
  private int titleColor = Color.BLACK;
  private boolean titleHtml;

  // Subtitle properties
  private String subtitleFontTypeface = Component.TYPEFACE_DEFAULT;
  private boolean subtitleFontBold;
  private boolean subtitleFontItalic;
  private float subtitleTextSize = 14f;
  private int subtitleColor = 0xFF444444;
  private boolean subtitleHtml;


  private String genericImageUrl = "https://cdn-icons-png.flaticon.com/512/7500/7500224.png";


  public ListViewImageText(ComponentContainer container) {
    super(container);
    form = container.$form();
    context = container.$context();
    listView = new ListView(context);
    listView.setBackgroundColor(bgColor);
    listView.setDivider(new ColorDrawable(dividerColor));
    listView.setDividerHeight(dividerHeight);
    listView.setOnItemClickListener(this);
    listView.setOnItemLongClickListener(this);
    adapter = new Adapter();
    listView.setAdapter(adapter);
    container.$add(this);
  }

  @Override
  public View getView() {
    return listView;
  }

  @SimpleEvent(description = "Fired when an item is clicked")
  public void Click(int position, String title, String subtitle, String image) {
    EventDispatcher.dispatchEvent(this, "Click", position, title, subtitle, image);
  }

  @SimpleEvent(description = "Fired when an item is long-clicked")
  public void LongClick(int position, String title, String subtitle, String image) {
    EventDispatcher.dispatchEvent(this, "LongClick", position, title, subtitle, image);
  }

  @SimpleFunction(description = "Anchors this component into the given container")
  public void AnchorTo(ComponentContainer container) {
    ViewGroup parent = (ViewGroup) listView.getParent();
    if (parent != null) parent.removeView(listView);
    container.$add(this);
  }

  // Data methods
  @SimpleFunction(description = "Adds an item with optional image URL, uses generic image if empty.")
  public void AddItem(String image, String title, String subtitle) {
    String img = (image == null || image.trim().isEmpty()) ? genericImageUrl : image;
    Item item = new Item(img, title, subtitle);
    items.add(item);
    originalItems.add(item);
    adapter.notifyDataSetChanged();
  }

  @SimpleFunction(description = "Adds multiple items from a YailList of [title, subtitle, url], uses generic image if empty.")
  public void AddItemFromList(YailList list) {
    originalItems.clear();
    for (Object o : list.toArray()) {
      if (o instanceof YailList) {
        YailList triple = (YailList) o;
        String t = triple.getString(0);
        String s = triple.getString(1);
        String u = triple.getString(2);
        String img = (u == null || u.trim().isEmpty()) ? genericImageUrl : u;
        Item item = new Item(img, t, s);
        originalItems.add(item);
      }
    }
    items.clear();
    items.addAll(originalItems);
    adapter.notifyDataSetChanged();
  }

  // Property to set generic image URL
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING, defaultValue = "")
  @SimpleProperty(description = "Sets the default generic image URL for empty image entries.")
  public void GenericImageUrl(String url) {
    this.genericImageUrl = url;
    adapter.notifyDataSetChanged();
  }
  @SimpleProperty(description = "Returns the current generic image URL.")
  public String GenericImageUrl() { return genericImageUrl; }

  @SimpleFunction(description = "Clears all items and cache")
  public void ClearList() {
    items.clear();
    originalItems.clear();
    imageCache.clear();
    adapter.notifyDataSetChanged();
  }

  @SimpleFunction(description = "Removes item at the specified position")
  public void RemoveItem(int position) {
    if (position >= 0 && position < items.size()) {
      Item removed = items.remove(position);
      originalItems.remove(removed);
      adapter.notifyDataSetChanged();
    }
  }

  @SimpleFunction(description = "Updates existing item, uses generic image if URL empty.")
  public void UpdateItem(int position, String image, String title, String subtitle) {
    if (position < 0 || position >= items.size()) return;
    String img = (image == null || image.trim().isEmpty()) ? genericImageUrl : image;
    Item it = items.get(position);
    it.image = img;
    it.title = title;
    it.subtitle = subtitle;
    adapter.notifyDataSetChanged();
  }

  @SimpleFunction(description = "Filters list by query (case-insensitive)")
  public void Search(String query) {
    items.clear();
    if (query == null || query.trim().isEmpty()) {
      items.addAll(originalItems);
    } else {
      String q = query.toLowerCase(Locale.getDefault());
      for (Item it : originalItems) {
        if (it.title.toLowerCase(Locale.getDefault()).contains(q)
            || it.subtitle.toLowerCase(Locale.getDefault()).contains(q)) {
          items.add(it);
        }
      }
    }
    adapter.notifyDataSetChanged();
  }

  @SimpleProperty(description = "Divider color of the list")
  public void DividerColor(int color) {
    dividerColor = color;
    listView.setDivider(new ColorDrawable(color));
    adapter.notifyDataSetChanged();
  }

  @SimpleProperty(description = "Divider height in pixels")
  public void DividerHeight(int height) {
    dividerHeight = height;
    listView.setDividerHeight(height);
    adapter.notifyDataSetChanged();
  }

  @SimpleProperty(description = "Sets image side: 1=Left,2=Right")
  public void ImageSide(int side) {
    imageSide = side;
    adapter.notifyDataSetChanged();
  }

  @SimpleProperty(description = "Item size: 1=Normal,2=Small,3=Large")
  public void ItemSize(int size) {
    itemSize = size;
    adapter.notifyDataSetChanged();
  }

  @SimpleProperty(description = "Title text color")
  public void TitleColor(int color) {
    titleColor = color;
    adapter.notifyDataSetChanged();
  }

  @SimpleProperty(description = "Subtitle text color")
  public void SubtitleColor(int color) {
    subtitleColor = color;
    adapter.notifyDataSetChanged();
  }

  // Designer-visible only properties
  @DesignerProperty(
      editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN,
      defaultValue = "false")
  @SimpleProperty(description = "Title displays HTML")
  public void TitleHTML(boolean html) {
    titleHtml = html;
    adapter.notifyDataSetChanged();
  }

  @SimpleProperty(userVisible = false)
  public boolean TitleHTML() {
    return titleHtml;
  }

  @DesignerProperty(
      editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN,
      defaultValue = "false")
  @SimpleProperty(description = "Subtitle displays HTML")
  public void SubtitleHTML(boolean html) {
    subtitleHtml = html;
    adapter.notifyDataSetChanged();
  }

  @SimpleProperty(userVisible = false)
  public boolean SubtitleHTML() {
    return subtitleHtml;
  }

  // --- BackgroundColor ---
  @DesignerProperty(
      editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR,
      defaultValue = Component.DEFAULT_VALUE_COLOR_NONE + ""
  )
  @SimpleProperty(description = "Background color of the list (ARGB)")
  public void BackgroundColor(int argb) {
    this.bgColor = argb;
    listView.setBackgroundColor(argb);
    adapter.notifyDataSetChanged();
  }

  @SimpleProperty(userVisible = false)
  public int BackgroundColor() {
    return this.bgColor;
  }

  // --- Title Font Bold ---
  @DesignerProperty(
      editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN,
      defaultValue = "False"
  )
  @SimpleProperty(description = "Define se o título (Title) ficará em negrito.")
  public void TitleFontBold(boolean bold) {
    this.titleFontBold = bold;
    adapter.notifyDataSetChanged();
  }

  @SimpleProperty(description = "Retorna se o título (Title) está em negrito.")
  public boolean TitleFontBold() {
    return titleFontBold;
  }

  // --- Subtitle Font Bold ---
  @DesignerProperty(
      editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN,
      defaultValue = "False"
  )
  @SimpleProperty(description = "Define se o subtítulo (SubTitle) ficará em negrito.")
  public void SubtitleFontBold(boolean bold) {
    this.subtitleFontBold = bold;
    adapter.notifyDataSetChanged();
  }

  @SimpleProperty(description = "Retorna se o subtítulo (SubTitle) está em negrito.")
  public boolean SubtitleFontBold() {
    return subtitleFontBold;
  }

  // --- Title Font Italic ---
  @DesignerProperty(
      editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN,
      defaultValue = "False"
  )
  @SimpleProperty(description = "Define se o título (Title) ficará em itálico.")
  public void TitleFontItalic(boolean italic) {
    this.titleFontItalic = italic;
    adapter.notifyDataSetChanged();
  }

  @SimpleProperty(description = "Retorna se o título (Title) está em itálico.")
  public boolean TitleFontItalic() {
    return titleFontItalic;
  }

  // --- Subtitle Font Italic ---
  @DesignerProperty(
      editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN,
      defaultValue = "False"
  )
  @SimpleProperty(description = "Define se o subtítulo (SubTitle) ficará em itálico.")
  public void SubtitleFontItalic(boolean italic) {
    this.subtitleFontItalic = italic;
    adapter.notifyDataSetChanged();
  }

  @SimpleProperty(description = "Retorna se o subtítulo (SubTitle) está em itálico.")
  public boolean SubtitleFontItalic() {
    return subtitleFontItalic;
  }

  @DesignerProperty(
      editorType = PropertyTypeConstants.PROPERTY_TYPE_FLOAT,
      defaultValue = "16.0"
  )
  @SimpleProperty(description = "Define o tamanho da fonte do título (Title) em sp.")
  public void TitleFontSize(float size) {
    this.titleTextSize = size;
    adapter.notifyDataSetChanged();
  }
  @SimpleProperty(description = "Retorna o tamanho da fonte do título (Title).")
  public float TitleFontSize() { return titleTextSize; }

  @DesignerProperty(
      editorType = PropertyTypeConstants.PROPERTY_TYPE_FLOAT,
      defaultValue = "14.0"
  )
  @SimpleProperty(description = "Define o tamanho da fonte do subtítulo (SubTitle) em sp.")
  public void SubtitleFontSize(float size) {
    this.subtitleTextSize = size;
    adapter.notifyDataSetChanged();
  }
  @SimpleProperty(description = "Retorna o tamanho da fonte do subtítulo (SubTitle).")
  public float SubtitleFontSize() { return subtitleTextSize; }

  // Style controls for Subtitle
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_TYPEFACE, defaultValue = Component.TYPEFACE_DEFAULT)
  @SimpleProperty(description = "Define a fonte (typeface) do subtítulo (SubTitle).")
  public void SubtitleFontTypeface(String typeface) {
    this.subtitleFontTypeface = typeface;
    adapter.notifyDataSetChanged();
  }
  @SimpleProperty(description = "Retorna a fonte atual do subtítulo (SubTitle).")
  public String SubtitleFontTypeface() { return subtitleFontTypeface; }

  // Style controls for Title
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_TYPEFACE, defaultValue = Component.TYPEFACE_DEFAULT)
  @SimpleProperty(description = "Define a fonte (typeface) do título (Title).")
  public void TitleFontTypeface(String typeface) {
    this.titleFontTypeface = typeface;
    adapter.notifyDataSetChanged();
  }
  @SimpleProperty(description = "Retorna a fonte atual do título (Title).")
  public String TitleFontTypeface() { return titleFontTypeface; }

  // --- TextColor ---
  @DesignerProperty(
      editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR,
      defaultValue = Component.DEFAULT_VALUE_COLOR_BLACK
  )
  @SimpleProperty
  public void TitleTextColor(int argb) {
    this.titleColor = argb;
    adapter.notifyDataSetChanged();
  }

  @SimpleProperty(userVisible = false)
  public int TitleTextColor() {
    return this.titleColor;
  }

  @DesignerProperty(
      editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR,
      defaultValue = Component.DEFAULT_VALUE_COLOR_BLACK
  )
  @SimpleProperty
  public void SubTitleTextColor(int argb) {
    this.subtitleColor = argb;
    adapter.notifyDataSetChanged();
  }

  @SimpleProperty(userVisible = false)
  public int SubTitleTextColor() {
    return this.subtitleColor;
  }


@Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    Item it = items.get(position);
    Click(position, it.title, it.subtitle, it.image);
  }

  @Override
  public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
    Item it = items.get(position);
    LongClick(position, it.title, it.subtitle, it.image);
    return true;
  }

  private boolean isValidUrl(String url) {
    try {
      new URL(url);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

    private Typeface getMappedTypeface(int idx) {
    switch (idx) {
      case 1:
        return Typeface.SERIF;
      case 2:
        return Typeface.SANS_SERIF;
      case 3:
        return Typeface.MONOSPACE;
      default:
        return Typeface.DEFAULT;
    }
  }


  private Typeface resolveTypeface(String typeface) {
  switch (typeface) {
      case Component.TYPEFACE_SERIF:
        return Typeface.SERIF;
      case Component.TYPEFACE_SANSSERIF:
        return Typeface.SANS_SERIF;
      case Component.TYPEFACE_MONOSPACE:
        return Typeface.MONOSPACE;
      default:
        return Typeface.DEFAULT;
    }
  }

  private class Adapter extends BaseAdapter {
    @Override
    public int getCount() {
      return items.size();
    }

    @Override
    public Object getItem(int position) {
      return items.get(position);
    }

    @Override
    public long getItemId(int position) {
      return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      Item it = items.get(position);
      LinearLayout layout = new LinearLayout(context);
      layout.setOrientation(LinearLayout.HORIZONTAL);
      layout.setLayoutParams(new ListView.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.WRAP_CONTENT));
      layout.setBackgroundColor(bgColor);

      ImageView iv = new ImageView(context);
      int sz = (itemSize == 1 ? 64 : (itemSize == 2 ? 48 : 96));
      LinearLayout.LayoutParams ivp = new LinearLayout.LayoutParams(sz, sz);
      ivp.gravity = Gravity.CENTER_VERTICAL;
      if (imageSide == 1) layout.addView(iv, ivp);

      LinearLayout txt = new LinearLayout(context);
      txt.setOrientation(LinearLayout.VERTICAL);
      txt.setLayoutParams(new LinearLayout.LayoutParams(
          0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
      int pad = (int)(8 * context.getResources().getDisplayMetrics().density);
      txt.setPadding(pad, pad/2, pad, pad/2);

      TextView tvT = new TextView(context);
      if (titleHtml) tvT.setText(HtmlCompat.fromHtml(it.title, HtmlCompat.FROM_HTML_MODE_LEGACY));
      else tvT.setText(it.title);
      tvT.setTextSize(titleTextSize);
      int ts = (titleFontBold?Typeface.BOLD:0)|(titleFontItalic?Typeface.ITALIC:0);
      tvT.setTypeface(Typeface.create(resolveTypeface(titleFontTypeface), ts));
      tvT.setTextColor(titleColor);
      txt.addView(tvT);

      TextView tvS = new TextView(context);
      if (subtitleHtml) tvS.setText(HtmlCompat.fromHtml(it.subtitle, HtmlCompat.FROM_HTML_MODE_LEGACY));
      else tvS.setText(it.subtitle);
      tvS.setTextSize(subtitleTextSize);
      int ss = (subtitleFontBold?Typeface.BOLD:0)|(subtitleFontItalic?Typeface.ITALIC:0);
      tvS.setTypeface(Typeface.create(resolveTypeface(subtitleFontTypeface), ss));
      tvS.setTextColor(subtitleColor);
      txt.addView(tvS);

      if (imageSide == 2) layout.addView(iv, ivp);
      layout.addView(txt);

      if (imageCache.containsKey(it.image)) {
        iv.setImageDrawable(imageCache.get(it.image));
      } else {
        executor.submit(() -> {
          try {
            HttpURLConnection conn =
                (HttpURLConnection) new URL(it.image).openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            InputStream is = conn.getInputStream();
            Drawable d = Drawable.createFromStream(is, null);
            is.close();
            imageCache.put(it.image, d);
            mainHandler.post(() -> iv.setImageDrawable(d));
          } catch (Exception ignored) {
          }
        });
      }

      return layout;
    }
  }

  private static class Item {
    String image;
    String title;
    String subtitle;
    Item(String image, String title, String subtitle) {
      this.image = image;
      this.title = title;
      this.subtitle = subtitle;
    }
  }
}