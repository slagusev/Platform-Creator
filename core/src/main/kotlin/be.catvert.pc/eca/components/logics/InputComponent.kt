package be.catvert.pc.eca.components.logics

import be.catvert.pc.eca.Entity
import be.catvert.pc.eca.actions.Action
import be.catvert.pc.eca.actions.EmptyAction
import be.catvert.pc.eca.components.Component
import be.catvert.pc.eca.containers.Level
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.ui.*
import be.catvert.pc.utility.Updeatable
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.fasterxml.jackson.annotation.JsonCreator
import imgui.ImGui
import imgui.functionalProgramming

@Description("Ajoute la possibilité de déclencher une action sur une entité quand une touche est pressée")
class InputComponent(var inputs: ArrayList<InputData>) : Component(), Updeatable, UIImpl, UITextImpl {
    constructor(vararg inputs: InputData) : this(arrayListOf(*inputs))
    @JsonCreator private constructor() : this(arrayListOf())

    data class InputData(var key: Int = Input.Keys.UNKNOWN, var pressed: Boolean = true, @UI var action: Action = EmptyAction()) : UIImpl {
        override fun insertUI(label: String, entity: Entity, level: Level, editorUI: EditorScene.EditorUI) {
            with(ImGui) {
                checkbox("", ::pressed)
                if (ImGui.isItemHovered()) {
                    functionalProgramming.withTooltip {
                        text("Pressé en continu")
                    }
                }
                sameLine(0f, style.itemInnerSpacing.x)
                ImGuiHelper.gdxKey(::key)
            }
        }

        fun update(entity: Entity) {
            if (entity.container != null) {
                if (pressed) {
                    if (Gdx.input.isKeyPressed(key))
                        action(entity, entity.container!!)
                } else {
                    if (Gdx.input.isKeyJustPressed(key))
                        action(entity, entity.container!!)
                }
            }
        }
    }

    override fun update() {
        inputs.forEach {
            it.update(entity)
        }
    }

    override fun insertUI(label: String, entity: Entity, level: Level, editorUI: EditorScene.EditorUI) {
        ImGuiHelper.addImguiWidgetsArray("inputs", inputs, { item -> Input.Keys.toString(item.key) }, { InputData() }, entity, level, editorUI)
    }

    override fun insertText() {
        inputs.forEach {
            ImGuiHelper.textColored(Color.RED, "<-->")
            ImGuiHelper.textPropertyColored(Color.ORANGE, "key :", Input.Keys.toString(it.key))
            ImGuiHelper.textPropertyColored(Color.ORANGE, "action :", it.action)
            ImGuiHelper.textPropertyColored(Color.ORANGE, "pressed :", it.pressed)
            ImGui.sameLine()
            ImGuiHelper.textColored(Color.ORANGE, if (it.pressed) Gdx.input.isKeyJustPressed(it.key) else Gdx.input.isKeyPressed(it.key))
            ImGuiHelper.textColored(Color.RED, "<-->")
        }
    }
}