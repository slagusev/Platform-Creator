package be.catvert.pc.ui

import be.catvert.pc.PCGame
import be.catvert.pc.eca.Entity
import be.catvert.pc.eca.EntityID
import be.catvert.pc.eca.EntityTag
import be.catvert.pc.eca.Prefab
import be.catvert.pc.eca.actions.Action
import be.catvert.pc.eca.actions.Actions
import be.catvert.pc.eca.components.RequiredComponent
import be.catvert.pc.eca.components.graphics.TextureComponent
import be.catvert.pc.eca.containers.Level
import be.catvert.pc.factories.PrefabFactory
import be.catvert.pc.i18n.MenusText
import be.catvert.pc.managers.ResourcesManager
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.utility.*
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Color
import com.esotericsoftware.reflectasm.ClassAccess
import glm_.func.common.clamp
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import uno.kotlin.isPrintable
import kotlin.math.roundToInt
import kotlin.reflect.KMutableProperty0

/**
 * Classe utilitaire utilisée dans la création de l'interface graphique.
 */
object ImGuiHelper {
    class Item<T>(var obj: T) {
        inline fun <reified T : Any> cast() = this as Item<T>
    }

    private fun getSettingsBtnIconHandle(): Int = ResourcesManager.getTexture(Constants.uiDirPath.child("settings.png")).textureObjectHandle
    private fun getFavBtnIconHandle(): Int = ResourcesManager.getTexture(Constants.uiDirPath.child("fav.png")).textureObjectHandle
    private fun getTickButtonSound(): Sound? = ResourcesManager.getSound(Constants.gameDirPath.child("tick.mp3"))

    private val hoveredTickButtons = mutableSetOf<Int>()

    private val searchBarBuffers = mutableMapOf<String, Item<String>>()

    private val uiCache = AnnotationCache(UI::class.java)
    private val descriptionCache = AnnotationCache(Description::class.java)
    private val requiredComponentCache = AnnotationCache(RequiredComponent::class.java)

    inline fun <T : Any> addImguiWidgetsArray(label: String, array: ArrayList<T>, itemLabel: (item: T) -> String, createItem: () -> T, entity: Entity, level: Level, editorUI: EditorScene.EditorUI, itemUI: UI = UIFactory.empty, endBlock: () -> Unit = {}) {
        addImguiWidgetsArray(label, array, itemLabel, createItem, {
            addImguiWidget(itemLabel(it.obj), it, entity, level, itemUI, editorUI)
        }, endBlock)
    }

    inline fun <T : Any> addImguiWidgetsArray(label: String, array: ArrayList<T>, itemLabel: (item: T) -> String, createItem: () -> T, itemBlock: (item: Item<T>) -> Unit, endBlock: () -> Unit = {}) {
        with(ImGui) {
            var removeItem: T? = null

            array.forEachIndexed { index, it ->
                pushId("remove $index")
                if (button("Suppr.")) {
                    removeItem = it
                }
                popId()

                sameLine()

                val item = Item(it)
                functionalProgramming.withId("collapse $index") {
                    if (collapsingHeader("")) {
                        sameLine(0f, style.itemInnerSpacing.x)
                        text(itemLabel(item.obj))

                        functionalProgramming.withIndent {
                            functionalProgramming.withId("array item $index") {
                                itemBlock(item)
                            }
                        }
                    } else {
                        sameLine(0f, style.itemInnerSpacing.x)
                        text(itemLabel(item.obj))
                    }

                    array[index] = item.obj
                }
            }

            if (removeItem != null)
                array.remove(removeItem!!)

            functionalProgramming.withId("$label add btn") {
                if (button("Ajouter", Vec2(if (array.isEmpty()) Constants.defaultWidgetsWidth else -1f, 0))) {
                    array.add(createItem())
                }
            }

            endBlock()
        }
    }

    fun <T : Any> addImguiWidget(label: String, item: Item<T>, entity: Entity, level: Level, UI: UI, editorUI: EditorScene.EditorUI) {
        val value = item.obj

        with(ReflectionUtility) {
            with(ImGui) {
                when (value) {
                    is Action -> {
                        action(label, item.cast(), entity, level, editorUI)
                    }
                    is UIImpl -> {
                        insertUIFields(value, entity, level, editorUI)
                    }
                    is EntityID -> {
                        entity(value, level, editorUI)
                    }
                    is Boolean -> {
                        checkbox(label, item.cast<Boolean>()::obj)
                    }
                    is Int -> {
                        functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
                            val item = item.cast<Int>()
                            inputInt(label, item::obj)
                            item.obj = item.obj.clamp(UI.min.roundToInt(), UI.max.roundToInt())
                        }
                    }
                    is Float -> {
                        functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
                            sliderFloat(label, item.cast<Float>()::obj, UI.min, UI.max, "%.1f")
                        }
                    }
                    is Prefab -> {
                        prefab(item.cast(), level, label)
                    }
                    is Size -> {
                        size(item.cast(), Size(UI.min.roundToInt(), UI.min.roundToInt()), Size(UI.max.roundToInt(), UI.max.roundToInt()))
                    }
                    is String -> {
                        when (UI.customType) {
                            CustomType.DEFAULT -> {
                                inputText(label, item.cast())
                            }
                            CustomType.TAG_STRING -> {
                                entityTag(item.cast(), level)
                            }
                        }
                    }
                    is Enum<*> -> {
                        enum(label, item.cast())
                    }
                    else -> {
                        insertUIFields(item.obj, entity, level, editorUI)
                    }
                }

                if (!UI.description.isBlank() && isItemHovered()) {
                    functionalProgramming.withTooltip {
                        text(UI.description)
                    }
                }
            }
        }
    }

    fun inputText(label: String, text: Item<String>, width: Float = Constants.defaultWidgetsWidth) {
        val buf = text.obj.toCharArray(CharArray(32))
        functionalProgramming.withItemWidth(width) {
            if (ImGui.inputText(label, buf))
                text.obj = String(buf.filter { it.isPrintable }.toCharArray())
        }
    }

    fun inputText(label: String, value: KMutableProperty0<String>, width: Float = Constants.defaultWidgetsWidth) {
        val item = Item(value())
        inputText(label, item, width)
        value.set(item.obj)
    }

    fun action(label: String, action: KMutableProperty0<Action>, entity: Entity, level: Level, editorUI: EditorScene.EditorUI) {
        val item = Item(action())
        action(label, item, entity, level, editorUI)
        action.set(item.obj)
    }

    fun action(label: String, action: Item<Action>, entity: Entity, level: Level, editorUI: EditorScene.EditorUI) {
        with(ImGui) {
            val index = intArrayOf(Actions.values().indexOfFirst { it.action.isInstance(action.obj) })

            functionalProgramming.withId("prop $label") {

                val actionClass = Actions.values()[index[0]].action.java

                val requiredComponent = requiredComponentCache.getClassAnnotations(actionClass).firstOrNull()
                val incorrectAction = let {
                    requiredComponent?.component?.forEach {
                        if (!entity.getStateOrDefault(editorUI.entityCurrentStateIndex).hasComponent(it))
                            return@let true
                    }
                    false
                }

                if (comboWithSettingsButton(label, index, Actions.values().map { it.name }, {
                            if (incorrectAction) {
                                textColored(Color.RED, "\\!/ Il manque le component :")
                                text("${requiredComponent!!.component.map { it.simpleName }}")
                            }
                            insertUIFields(action.obj, entity, level, editorUI)
                        }, searchBar = true)) {
                    action.obj = ReflectionUtility.createInstance(Actions.values()[index[0]].action.java)
                }

                val description = descriptionCache.getClassAnnotations(actionClass).firstOrNull()

                if (description != null) {
                    sameLine(0f, style.itemInnerSpacing.x)
                    text("(?)")

                    if (isItemHovered()) {
                        functionalProgramming.withTooltip {
                            text(description.description)
                        }
                    }
                }
            }
        }
    }

    inline fun comboWithSettingsButton(label: String, currentItem: IntArray, items: List<String>, popupBlock: () -> Unit, settingsBtnDisabled: Boolean = false, onSettingsBtnDisabled: () -> Unit = {}, searchBar: Boolean = false): Boolean {
        val popupTitle = "popup settings $label"

        var comboChanged = false

        with(ImGui) {
            functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth - g.fontSize - g.style.itemInnerSpacing.x * 3f) {
                functionalProgramming.withId(label) {
                    if (searchBar) {
                        if (searchCombo("", currentItem, items))
                            comboChanged = true
                    } else if (combo("", currentItem, items))
                        comboChanged = true
                }
            }

            sameLine(0f, style.itemInnerSpacing.x)

            pushItemFlag(ItemFlag.Disabled.i, settingsBtnDisabled)

            if (settingsButton())
                openPopup(popupTitle)

            popItemFlag()

            if (settingsBtnDisabled)
                onSettingsBtnDisabled()

            sameLine(0f, style.itemInnerSpacing.x)

            text(label)

            functionalProgramming.popup(popupTitle) {
                popupBlock()
            }
        }

        return comboChanged
    }

    inline fun comboWithSettingsButton(label: String, currentItem: KMutableProperty0<Int>, items: List<String>, popupBlock: () -> Unit, settingsBtnDisabled: Boolean = false, onSettingsBtnDisabled: () -> Unit = {}, searchBar: Boolean = false) {
        val item = intArrayOf(currentItem.get())
        comboWithSettingsButton(label, item, items, popupBlock, settingsBtnDisabled, onSettingsBtnDisabled, searchBar)
        currentItem.set(item[0])
    }

    fun searchCombo(label: String, currentItem: IntArray, items: List<String>, selectableHovered: (index: Int) -> Unit = {}): Boolean {
        var changed = false

        with(ImGui) {
            if (beginCombo(label, items.elementAtOrNull(currentItem[0]))) {
                cursorPosX += style.itemInnerSpacing.x
                val buf = searchBarBuffers.getOrPut(label) { Item("") }
                inputText("", buf)
                cursorPosY += style.itemInnerSpacing.y

                separator()

                for (i in 0 until items.size) {
                    if (!items[i].startsWith(buf.obj, true))
                        continue
                    pushId(i)
                    val itemSelected = i == currentItem[0]
                    val itemText = items.getOrElse(i, { "*Unknown item*" })
                    if (selectable(itemText, itemSelected)) {
                        currentItem[0] = i
                        changed = true
                    }
                    if (itemSelected) setItemDefaultFocus()

                    if (isItemHovered())
                        selectableHovered(i)

                    popId()
                }

                endCombo()
            }
        }
        return changed
    }

    fun settingsButton(size: Vec2 = Vec2(g.fontSize)) = ImGui.imageButton(getSettingsBtnIconHandle(), size, uv1 = Vec2(1))
    fun favButton(size: Vec2 = Vec2(g.fontSize), tintColor: Vec4 = Vec4(1)) = ImGui.imageButton(getFavBtnIconHandle(), size, uv1 = Vec2(1), tintCol = tintColor)

    fun entityTag(tag: Item<EntityTag>, level: Level, label: String = "tag") {
        val selectedIndex = intArrayOf(level.tags.indexOfFirst { it == tag.obj })
        functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
            if (ImGui.combo(label, selectedIndex, level.tags)) {
                tag.obj = level.tags[selectedIndex[0]]
            }
        }
    }

    fun entity(entityID: EntityID, level: Level, editorUI: EditorScene.EditorUI, label: String = "entité") {
        val favTitle = "set entity fav"

        with(ImGui) {
            if (button("Sélect. $label", Vec2(Constants.defaultWidgetsWidth - g.fontSize - g.style.itemInnerSpacing.x * 3f, 0))) {
                editorUI.editorMode = EditorScene.EditorUI.EditorMode.SELECT_ENTITY
                editorUI.onSelectEntity.register(true) {
                    if (it != null)
                        entityID.ID = it.id()
                }
            }

            if (isItemHovered()) {
                functionalProgramming.withTooltip {
                    val entity = entityID.entity(level)
                    if (entity == null)
                        textColored(Color.RED, "aucune entité sélectionnée")
                    else
                        textPropertyColored(Color.ORANGE, "entité actuelle :", entity.name + " #${entity.id()}")
                }
            }

            sameLine(0f, style.itemInnerSpacing.x)

            if (favButton())
                openPopup(favTitle)

            val fav = favoritesPopup(favTitle, level)

            if (fav != null) {
                entityID.ID = fav.id()
            }
        }
    }

    private val favoritesIndex = mutableMapOf<String, IntArray>()
    fun favoritesPopup(id: String, level: Level): Entity? {
        var entity: Entity? = null
        val favoris = level.favoris

        with(ImGui) {
            functionalProgramming.popup(id) {
                if (favoris.isNotEmpty()) {
                    functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
                        val favorisEntities = favoris.mapNotNull { level.findEntityByID(it) }
                        searchCombo("favoris", favoritesIndex.getOrPut(id, { intArrayOf(0) }), favorisEntities.map { it.name }, {
                            texturePreviewTooltip(favorisEntities[it])
                        })
                    }

                    if (button("Sélectionner", Vec2(-1, 0))) {
                        entity = level.findEntityByID(favoris[favoritesIndex[id]!![0]])
                        closeCurrentPopup()
                    }
                } else {
                    text("Aucun favoris n'est présent dans votre collection.")
                }
            }
        }

        return entity
    }

    fun texturePreviewTooltip(entity: Entity) {
        val texture = entity.getCurrentState().getComponent<TextureComponent>()
        if (texture != null) {
            functionalProgramming.withStyleColor(Col.PopupBg, ImGui.getStyleColorVec4(Col.PopupBg).apply { a = 0.5f }) {
                functionalProgramming.withTooltip {
                    val region = texture.groups[texture.currentIndex].currentFrame().getTextureRegion()
                    ImGui.image(region.texture.textureObjectHandle, Vec2(entity.box.width, entity.box.height), Vec2(region.u, region.v), Vec2(region.u2, region.v2))
                }
            }
        }
    }

    fun prefab(prefab: Item<Prefab>, level: Level, label: String = "prefab") {
        val prefabs = level.resourcesPrefabs() + PrefabFactory.values().map { it.prefab }

        val selectedIndex = intArrayOf(prefabs.indexOfFirst { it.name == prefab.obj.name })
        functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
            if (ImGui.combo(label, selectedIndex, prefabs.map { it.name })) {
                prefab.obj = prefabs[selectedIndex[0]]
            }
        }
    }

    fun point(point: KMutableProperty0<Point>, minPoint: Point, maxPoint: Point, editorUI: EditorScene.EditorUI) {
        with(ImGui) {
            val pos = floatArrayOf(point.get().x, point.get().y)
            functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth - g.fontSize - g.style.itemInnerSpacing.x * 3f) {
                if (inputFloat2("", pos,"%f")) {
                    val x = pos[0]
                    val y = pos[1]

                    if (x >= minPoint.x && x <= maxPoint.x && y >= minPoint.y && y <= maxPoint.y)
                        point.set(Point(x, y))
                }
            }

            sameLine(0f, 0f)

            if (settingsButton()) {
                editorUI.editorMode = EditorScene.EditorUI.EditorMode.SELECT_POINT
                editorUI.onSelectPoint.register(true) {
                    point.set(Point(it.x.clamp(minPoint.x, maxPoint.x), it.y.clamp(minPoint.y, maxPoint.y)))
                }
            }

            if (isItemHovered()) {
                functionalProgramming.withTooltip {
                    text("Sélectionner un point")
                }
            }

            sameLine(0f, style.itemInnerSpacing.x)
            text("position")
        }
    }

    fun size(size: KMutableProperty0<Size>, minSize: Size, maxSize: Size) {
        val item = Item(size.get())
        size(item, minSize, maxSize)
        size.set(item.obj)
    }

    fun size(size: Item<Size>, minSize: Size, maxSize: Size) {
        val sizeArr = intArrayOf(size.obj.width, size.obj.height)
        functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
            if (ImGui.inputInt2("taille", sizeArr, 0)) {
                val width = sizeArr[0]
                val height = sizeArr[1]

                if (width >= minSize.width && width <= maxSize.width && height >= minSize.height && height <= maxSize.height)
                    size.obj = Size(width, height)
            }
        }
    }

    private val keys = mutableMapOf<Int, Boolean>()
    fun gdxKey(key: KMutableProperty0<Int>) {
        val keyInt = key.get()
        with(ImGui) {
            if (button(if (keys[keyInt] == true) MenusText.MM_SETTINGS_PRESSKEY() else Input.Keys.toString(keyInt), Vec2(-1, 0))) {
                if (!keys.containsKey(keyInt) || keys[keyInt] == false) {
                    keys.forEach {
                        keys[it.key] = false
                        PCInputProcessor.keyDownSignal.clear()
                    }

                    keys[keyInt] = true

                    PCInputProcessor.keyDownSignal.register(true) {
                        key.set(it)
                        keys[keyInt] = false
                    }
                }
            }
        }
    }

    inline fun enumWithSettingsButton(label: String, enum: Item<Enum<*>>, popupBlock: () -> Unit, settingsBtnDisabled: Boolean = false, onSettingsBtnDisabled: () -> Unit = {}) {
        val enumConstants = enum.obj.javaClass.enumConstants
        val selectedIndex = intArrayOf(enumConstants.indexOfFirst { it == enum.obj })

        if (comboWithSettingsButton(label, selectedIndex, enumConstants.map { (it as Enum<*>).toString() }, popupBlock, settingsBtnDisabled, onSettingsBtnDisabled))
            enum.obj = enumConstants[selectedIndex[0]]
    }

    fun enum(label: String, enum: Item<Enum<*>>) {
        val enumConstants = enum.obj.javaClass.enumConstants
        val selectedIndex = intArrayOf(enumConstants.indexOfFirst { it == enum.obj })

        functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
            if (ImGui.combo(label, selectedIndex, enumConstants.map { (it as Enum<*>).toString() })) {
                enum.obj = enumConstants[selectedIndex[0]]
            }
        }
    }

    inline fun withCenteredWindow(name: String, open: KMutableProperty0<Boolean>? = null, size: Vec2, flags: Int = 0, centerCond: Cond = Cond.Once, block: () -> Unit) {
        ImGui.setNextWindowSize(size, centerCond)
        ImGui.setNextWindowPos(Vec2(Gdx.graphics.width / 2f - size.x / 2f, Gdx.graphics.height / 2f - size.y / 2f), centerCond)
        functionalProgramming.withWindow(name, open, flags) {
            block()
        }
    }

    fun textColored(color: Color, content: Any) {
        ImGui.textColored(Vec4(color.r, color.g, color.b, 1f), content.toString())
    }

    fun textPropertyColored(color: Color, propertyName: String, content: Any) {
        textColored(color, propertyName)
        ImGui.sameLine()

        content.cast<UITextImpl>()?.insertText() ?: ImGui.text(content.toString())
    }

    fun centeredTextColored(containerWidth: Float, color: Color, content: Any) {
        ImGui.cursorPosX += containerWidth / 2 - ImGui.calcTextSize(content.toString()).x / 2
        textColored(color, content)
    }

    fun centeredTextPropertyColored(containerWidth: Float, color: Color, propertyName: String, content: Any) {
        ImGui.cursorPosX += containerWidth / 2f - ImGui.calcTextSize(propertyName + content.toString()).x / 2f - 0.5f
        textColored(color, propertyName)
        ImGui.sameLine(0f, 1f)
        ImGui.text(content.toString())
    }

    fun tickSoundButton(label: String, size: Vec2): Boolean {
        val pressed = ImGui.button(label, size)

        val id = ImGui.currentWindow.dc.lastItemId

        if (ImGui.isItemHovered()) {
            if (!hoveredTickButtons.contains(id)) {
                getTickButtonSound()?.play(PCGame.soundVolume)
                hoveredTickButtons.add(id)
            }
        } else hoveredTickButtons.remove(id)

        return pressed
    }

    inline fun withMenuButtonsStyle(block: () -> Unit) {
        with(ImGui) {
            pushStyleColor(Col.WindowBg, ImGui.getStyleColorVec4(Col.WindowBg).apply { a = 0f })
            pushStyleColor(Col.Border, ImGui.getStyleColorVec4(Col.Border).apply { a = 0f })
            pushStyleColor(Col.Button, Vec4.fromColor(29, 114, 249, 255))
            pushStyleColor(Col.ButtonHovered, Vec4.fromColor(22, 85, 186, 255))
            pushStyleColor(Col.ButtonActive, Vec4.fromColor(22, 85, 186, 255))
            pushStyleColor(Col.Text, Vec4(1))
            pushStyleVar(StyleVar.FrameRounding, 15f)
            pushStyleVar(StyleVar.FramePadding, Vec2(10f))
            pushFont(PCGame.imguiBigFont)

            block()

            ImGui.popStyleColor(6)
            ImGui.popStyleVar(2)
            popFont()
        }
    }

    fun insertUIFields(instance: Any, entity: Entity, level: Level, editorUI: EditorScene.EditorUI) {
        if (instance is UIImpl) {
            instance.insertUI(ReflectionUtility.simpleNameOf(instance), entity, level, editorUI)
        }

        val access = ClassAccess.access(instance.javaClass)

        uiCache.getFieldsAnnotations(instance.javaClass).forEach { name, ui ->
            val item = Item(access.get<Any>(instance, name))
            addImguiWidget(if (ui.customName.isBlank()) name else ui.customName, item, entity, level, ui, editorUI)
            access.set<Any>(instance, name, item.obj)
        }
    }

    fun insertUITextFields(instance: Any) {
        with(ImGui) {
            instance.cast<UITextImpl>()?.insertText()
                    ?: if (instance.toString().isNotBlank()) text(instance.toString())

            val access = ClassAccess.access(instance.javaClass)

            uiCache.getFieldsAnnotations(instance.javaClass).forEach { name, ui ->
                val value = access.get<Any>(instance, name)

                textPropertyColored(Color.ORANGE, "${if (ui.customName.isBlank()) name else ui.customName} :", value)
            }
        }
    }
}