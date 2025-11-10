// android/src/main/java/lhacenmed/modules/dropdownmenu/DropdownMenuModule.kt
package lhacenmed.modules.dropdownmenu

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.Base64
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import me.saket.cascade.CascadePopupMenu
import android.os.Handler
import android.os.Looper

class DropdownMenuModule : Module() {
  private val mainHandler = Handler(Looper.getMainLooper())

  @RequiresApi(Build.VERSION_CODES.P)
  override fun definition() = ModuleDefinition {
    Name("DropdownMenu")
    Events("onItemSelected")
    AsyncFunction("showMenu") { anchorTag: Int, menuSpec: Map<String, Any?> ->
      val anchor = appContext.findView(anchorTag) as? View ?: return@AsyncFunction
      runOnUiThread {
        showCascadeFromAnchor(anchor, menuSpec) { payload ->
          sendEvent("onItemSelected", payload)
        }
      }
    }
  }

  private fun runOnUiThread(block: () -> Unit) {
    if (Looper.myLooper() == Looper.getMainLooper()) block() else mainHandler.post(block)
  }

  @RequiresApi(Build.VERSION_CODES.P)
  private fun showCascadeFromAnchor(
    anchor: View,
    menuSpec: Map<String, Any?>,
    onSelect: (Map<String, Any?>) -> Unit,
  ) {
    val popup = try {
      val styler = createStylerFromSpec(menuSpec)
      CascadePopupMenu(anchor.context, anchor, styler = styler)
    } catch (e: Exception) {
      // Fallback: show menu without custom styling if anything fails
      CascadePopupMenu(anchor.context, anchor)
    }

    val groups = (menuSpec["groups"] as? List<*>)?.filterIsInstance<Map<String, Any?>>() ?: emptyList()
    if (groups.isNotEmpty()) {
      popup.menu.setGroupDividerEnabled(true)
      groups.forEachIndexed { groupIndex, group ->
        val items = (group["items"] as? List<*>)?.filterIsInstance<Map<String, Any?>>() ?: emptyList()
        buildMenuGroup(popup.menu, groupIndex, items, mutableListOf(), anchor) { id, title, path ->
          onSelect(mapOf("id" to id, "title" to title, "path" to path))
        }
      }
    }
    popup.show()
  }

  @RequiresApi(Build.VERSION_CODES.P)
  private fun createStylerFromSpec(menuSpec: Map<String, Any?>): CascadePopupMenu.Styler {
    val backgroundColor = menuSpec["backgroundColor"] as? String
    val cornerRadius = (menuSpec["cornerRadius"] as? Number)?.toFloat() ?: 0f

    return if (backgroundColor != null || cornerRadius > 0) {
      val drawable = createBackgroundDrawable(backgroundColor, cornerRadius)
      CascadePopupMenu.Styler(background = { drawable })
    } else {
      CascadePopupMenu.Styler()
    }
  }

  private fun createBackgroundDrawable(
    backgroundColor: String?,
    cornerRadius: Float
  ): Drawable {
    val color = try {
      backgroundColor?.toColorInt() ?: Color.WHITE
    } catch (e: Exception) {
      Color.WHITE // Safe fallback
    }

    return if (cornerRadius > 0) {
      GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setCornerRadius(cornerRadius)
        setColor(color)
      }
    } else {
      color.toDrawable()
    }
  }

  @RequiresApi(Build.VERSION_CODES.P)
  private fun buildMenuGroup(
    menu: Menu,
    groupId: Int,
    items: List<Map<String, Any?>>,
    path: MutableList<String>,
    anchor: View,
    onSelect: (id: String?, title: String, path: List<String>) -> Unit,
  ) {
    items.forEach { item ->
      val title = (item["title"] as? String) ?: return@forEach
      val id = item["id"] as? String
      val enabled = (item["enabled"] as? Boolean) ?: true
      val children = (item["items"] as? List<*>)?.filterIsInstance<Map<String, Any?>>()

      if (!children.isNullOrEmpty()) {
        val sub = menu.addSubMenu(groupId, Menu.NONE, Menu.NONE, title)
        (item["icon"] as? Map<*, *>)?.let { setIconFromSpec(anchor, sub.item, it) }
        path.add(title)
        buildSubMenu(sub, children, path, anchor, onSelect)
        path.removeLastOrNull()
      } else {
        val mi = menu.add(groupId, id?.hashCode() ?: 0, Menu.NONE, title)
        mi.isEnabled = enabled
        (item["icon"] as? Map<*, *>)?.let { setIconFromSpec(anchor, mi, it) }
        mi.setOnMenuItemClickListener {
          path.add(title)
          onSelect(id, title, path.toList())
          path.removeLastOrNull()
          true
        }
      }
    }
  }

  @RequiresApi(Build.VERSION_CODES.P)
  private fun buildSubMenu(
    sub: android.view.SubMenu,
    items: List<Map<String, Any?>>,
    path: MutableList<String>,
    anchor: View,
    onSelect: (id: String?, title: String, path: List<String>) -> Unit,
  ) {
    items.forEach { item ->
      val title = (item["title"] as? String) ?: return@forEach
      val id = item["id"] as? String
      val enabled = (item["enabled"] as? Boolean) ?: true
      val children = (item["items"] as? List<*>)?.filterIsInstance<Map<String, Any?>>()

      if (!children.isNullOrEmpty()) {
        val nested = sub.addSubMenu(title)
        (item["icon"] as? Map<*, *>)?.let { setIconFromSpec(anchor, nested.item, it) }
        path.add(title)
        buildSubMenu(nested, children, path, anchor, onSelect)
        path.removeLastOrNull()
      } else {
        val mi = sub.add(Menu.NONE, id?.hashCode() ?: 0, Menu.NONE, title)
        mi.isEnabled = enabled
        (item["icon"] as? Map<*, *>)?.let { setIconFromSpec(anchor, mi, it) }
        mi.setOnMenuItemClickListener {
          path.add(title)
          onSelect(id, title, path.toList())
          path.removeLastOrNull()
          true
        }
      }
    }
  }

  @SuppressLint("DiscouragedApi")
  private fun setIconFromSpec(anchor: View, mi: MenuItem, iconSpec: Map<*, *>) {
    val resName = iconSpec["resource"] as? String
    val base64 = iconSpec["base64"] as? String
    if (!resName.isNullOrBlank()) {
      var resId = anchor.resources.getIdentifier(resName, "drawable", anchor.context.packageName)
      if (resId == 0) {
        resId = anchor.resources.getIdentifier(resName, "drawable", "android")
      }
      if (resId != 0) {
        mi.icon = ContextCompat.getDrawable(anchor.context, resId)
        return
      }
    }
    if (!base64.isNullOrBlank()) {
      try {
        val clean = base64.substringAfter(",", base64)
        val bytes = Base64.decode(clean, Base64.DEFAULT)
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        mi.icon = bmp.toDrawable(anchor.resources)
        return
      } catch (_: Throwable) {}
    }
    // fallback to a default built-in icon
    mi.icon = ContextCompat.getDrawable(anchor.context, android.R.drawable.ic_menu_view)
  }

}
