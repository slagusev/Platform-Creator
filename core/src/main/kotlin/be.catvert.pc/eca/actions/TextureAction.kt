package be.catvert.pc.eca.actions

import be.catvert.pc.eca.Entity
import be.catvert.pc.eca.components.RequiredComponent
import be.catvert.pc.eca.components.graphics.TextureComponent
import be.catvert.pc.eca.containers.EntityContainer
import be.catvert.pc.eca.containers.Level
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.ui.Description
import be.catvert.pc.ui.UIImpl
import be.catvert.pc.utility.Constants
import com.fasterxml.jackson.annotation.JsonCreator
import imgui.ImGui
import imgui.functionalProgramming

/**
 * Action permettant de changer la texture en cour d'une entité
 */
@RequiredComponent(TextureComponent::class)
@Description("Permet de changer la texture actuelle d'une entité")
class TextureAction(var textureIndex: Int) : Action(), UIImpl {
    @JsonCreator private constructor() : this(-1)

    override fun invoke(entity: Entity, container: EntityContainer) {
        entity.getCurrentState().getComponent<TextureComponent>()?.also {
            it.currentIndex = textureIndex
        }
    }

    override fun insertUI(label: String, entity: Entity, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
        with(ImGui) {
            val textureData = entity.getCurrentState().getComponent<TextureComponent>()?.data ?: arrayListOf()

            functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
                combo("texture", ::textureIndex, textureData.map { it.name })
            }
        }
    }

    override fun toString(): String = super.toString() + " - index : $textureIndex"
}