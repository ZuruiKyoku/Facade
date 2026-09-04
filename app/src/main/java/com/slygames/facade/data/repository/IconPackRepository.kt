package com.slygames.facade.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.XmlResourceParser
import android.graphics.drawable.Drawable
import android.util.Log
import com.slygames.facade.data.model.IconPackInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.xmlpull.v1.XmlPullParser
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Icon pack parsing engine: discovers installed icon packs (any app
 * exposing the community-standard `com.novalauncher.THEME` /
 * `org.adw.launcher.THEMES` intent actions) and parses their
 * `res/xml/appfilter.xml` into an [IconPackInfo] Facade can use to override
 * [com.slygames.facade.data.model.AppItem.icon] in the app drawer and
 * workspace.
 *
 * appfilter.xml format (shared by Nova/ADW/Apex-compatible packs):
 * ```
 * <resources>
 *   <item component="ComponentInfo{com.example/com.example.MainActivity}" drawable="ic_example" />
 *   <iconback img1="iconback" />
 *   <iconupon img1="iconupon" />
 *   <iconmask img1="iconmask" />
 *   <scale factor="1.0" />
 * </resources>
 * ```
 */
@Singleton
class IconPackRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _activePack = MutableStateFlow<IconPackInfo?>(null)
    val activePack: StateFlow<IconPackInfo?> = _activePack.asStateFlow()

    /** Every installed package that advertises itself as an icon pack via a themed launcher intent. */
    fun discoverInstalledIconPacks(): List<String> {
        val actions = listOf(
            "com.novalauncher.THEME",
            "org.adw.launcher.THEMES",
            "org.adw.launcher.icons.ACTION_PICK_ICON"
        )
        val packageManager = context.packageManager
        return actions
            .flatMap { action ->
                val intent = android.content.Intent(action)
                packageManager.queryIntentActivities(intent, 0)
            }
            .map { it.activityInfo.packageName }
            .distinct()
    }

    suspend fun loadIconPack(packageName: String): IconPackInfo? = try {
        val resources = context.packageManager.getResourcesForApplication(packageName)
        val xmlResId = resources.getIdentifier("appfilter", "xml", packageName)
        if (xmlResId == 0) {
            Log.w(TAG, "$packageName has no res/xml/appfilter.xml")
            return null
        }

        val parser = resources.getXml(xmlResId)
        val componentMap = mutableMapOf<String, String>()
        val backNames = mutableListOf<String>()
        var uponName: String? = null
        var maskName: String? = null
        var scale = 1f

        try {
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "item" -> parseItemTag(parser)?.let { (component, drawable) -> componentMap[component] = drawable }
                        "iconback" -> collectImgAttrs(parser, backNames)
                        "iconupon" -> uponName = uponName ?: firstImgAttr(parser)
                        "iconmask" -> maskName = maskName ?: firstImgAttr(parser)
                        "scale" -> scale = parser.getAttributeValue(null, "factor")?.toFloatOrNull() ?: scale
                    }
                }
                eventType = parser.next()
            }
        } finally {
            parser.close()
        }

        val label = context.packageManager.getApplicationLabel(
            context.packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        ).toString()

        IconPackInfo(
            packageName = packageName,
            displayName = label,
            componentToDrawable = componentMap,
            iconBackNames = backNames,
            iconUponName = uponName,
            iconMaskName = maskName,
            iconScale = scale
        )
    } catch (e: Exception) {
        Log.w(TAG, "Failed to parse icon pack $packageName", e)
        null
    }

    suspend fun setActivePack(packageName: String?) {
        _activePack.value = packageName?.let { loadIconPack(it) }
    }

    /** Resolves the themed [Drawable] for [componentKey] ("pkg/activity") from the currently active pack, if any. */
    fun resolveIconDrawable(componentKey: String): Drawable? {
        val pack = _activePack.value ?: return null
        val drawableName = pack.drawableNameFor(componentKey) ?: return null
        return try {
            val resources = context.packageManager.getResourcesForApplication(pack.packageName)
            val resId = resources.getIdentifier(drawableName, "drawable", pack.packageName)
            if (resId == 0) null else androidx.core.content.res.ResourcesCompat.getDrawable(resources, resId, null)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to resolve $drawableName from ${pack.packageName}", e)
            null
        }
    }

    private fun parseItemTag(xml: XmlResourceParser): Pair<String, String>? {
        val componentAttr = xml.getAttributeValue(null, "component") ?: return null
        val drawableAttr = xml.getAttributeValue(null, "drawable") ?: return null
        // componentAttr looks like "ComponentInfo{pkg/activity}"
        val componentKey = componentAttr.removePrefix("ComponentInfo{").removeSuffix("}")
        return componentKey to drawableAttr
    }

    private fun collectImgAttrs(xml: XmlResourceParser, into: MutableList<String>) {
        for (i in 0 until xml.attributeCount) {
            val attrName = xml.getAttributeName(i)
            if (attrName.startsWith("img")) into += xml.getAttributeValue(i)
        }
    }

    private fun firstImgAttr(xml: XmlResourceParser): String? =
        (0 until xml.attributeCount)
            .map { xml.getAttributeName(it) to xml.getAttributeValue(it) }
            .firstOrNull { it.first.startsWith("img") }
            ?.second

    private companion object {
        const val TAG = "IconPackRepository"
    }
}
