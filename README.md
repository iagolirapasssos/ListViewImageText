# ListViewImageText
This extension lets you show a scrollable list with an image, a title, and a subtitle for each item. You can set default images, change text styles, and react to clicks

---

## 1. Getting Started

1. **Import the AIX**: In Kodular Designer, go to **Extensions → Import Extension** and select `ListViewImageText.aix`.
2. **Add to Screen**: Drag **ListViewImageText** from the Extensions palette onto your screen.
3. **Anchor**: Use the `AnchorTo` block to place it into any layout (e.g., a VerticalArrangement).

```blocks
// Place list into VerticalArrangement1
ListViewImageText.AnchorTo(VerticalArrangement1)
```

---

## 2. Basic Blocks

### Set Default Image

Choose a URL to use whenever an item has no image.

```blocks
ListViewImageText.GenericImageUrl("https://example.com/default.png")
```

### Add One Item

If `imageUrl` is empty, it uses your default image.

```blocks
ListViewImageText.AddItem(
  imageUrl: "",
  title: "Hello",
  subtitle: "No image provided"
)
```

### Add Many Items at Once

Prepare a list of sublists: `[ ["Title1","Sub1","Url1"], ["Title2","Sub2","Url2"] ]`

```blocks
defaultsList = make a list
add items to list defaultsList
  make a list "Item A" "Description A" ""
  make a list "Item B" "Description B" "https://.../img.png"
ListViewImageText.AddItemFromList(defaultsList)
```

### Update or Remove

```blocks
// Update item at position 2:
ListViewImageText.UpdateItem(
  index: 2,
  imageUrl: "https://.../new.png",
  title: "Updated",
  subtitle: "New subtitle"
)

// Remove item at position 1:
ListViewImageText.RemoveItem(1)

// Clear all items:
ListViewImageText.ClearList()
```

---

## 3. Search Filter

Show only items whose title or subtitle contains `query`:

```blocks
ListViewImageText.Search("hello")
// To show all again:
ListViewImageText.Search("")
```

---

## 4. Styling Text

Change fonts, sizes, and colors for title and subtitle separately.

```blocks
// Title style
ListViewImageText.TitleFontTypeface("Serif")
ListViewImageText.TitleFontSize(18)
ListViewImageText.TitleFontBold(true)
ListViewImageText.TitleFontItalic(false)
ListViewImageText.TitleTextColor(0xFF0000FF)  // Blue

// Subtitle style
ListViewImageText.SubtitleFontTypeface("Monospace")
ListViewImageText.SubtitleFontSize(14)
ListViewImageText.SubtitleFontBold(false)
ListViewImageText.SubtitleFontItalic(true)
ListViewImageText.SubtitleTextColor(0xFF888888)  // Gray
```

---

## 5. Events

Handle taps on items to know which one was clicked.

```blocks
when ListViewImageText.Click do
  // position, title, subtitle, imageUrl are provided
  show alert join "You clicked: " title

when ListViewImageText.LongClick do
  show alert join "Long press: " subtitle
```

---

Enjoy making rich lists with images and text—no coding needed!
Feel free to ask questions in the Kodular community if you get stuck.
