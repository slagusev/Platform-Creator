package be.catvert.pc.eca.components.logics

import be.catvert.pc.eca.Entity
import be.catvert.pc.eca.actions.Action
import be.catvert.pc.eca.actions.EmptyAction
import be.catvert.pc.eca.components.Component
import be.catvert.pc.eca.containers.Level
import be.catvert.pc.scenes.EditorScene
import be.catvert.pc.ui.Description
import be.catvert.pc.ui.UI
import be.catvert.pc.ui.UIImpl
import be.catvert.pc.utility.Constants
import be.catvert.pc.utility.Timer
import be.catvert.pc.utility.Updeatable
import com.fasterxml.jackson.annotation.JsonCreator
import imgui.ImGui
import imgui.functionalProgramming

@Description("Permet d'effectuer une action tout les x secondes")
class TimerComponent(interval: Float, @UI var action: Action) : Component(), Updeatable, UIImpl {
    @JsonCreator private constructor() : this(1f, EmptyAction())

    var interval = interval
        set(value) {
            field = value
            timer.interval = value
        }

    private val timer = Timer(interval).apply {
        onIncrement.register {
            if(entity.container != null)
                action(entity, entity.container!!)
        }
    }

    override fun update() {
        timer.update()
    }

    override fun insertUI(label: String, entity: Entity, level: Level, editorSceneUI: EditorScene.EditorSceneUI) {
        functionalProgramming.withItemWidth(Constants.defaultWidgetsWidth) {
            ImGui.sliderFloat("interval", ::interval, 0f, 10f)
        }
    }

    override fun toString() = "interval : $interval"
}