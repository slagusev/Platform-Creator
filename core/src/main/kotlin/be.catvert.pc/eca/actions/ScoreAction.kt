package be.catvert.pc.eca.actions


import be.catvert.pc.eca.Entity
import be.catvert.pc.eca.containers.Level
import be.catvert.pc.ui.Description
import be.catvert.pc.ui.UI
import be.catvert.pc.utility.cast
import com.fasterxml.jackson.annotation.JsonCreator

/**
 * Action permettant d'ajouter des points de score au joueur
 */
@Description("Permet d'ajouter des points de score au joueur")
class ScoreAction(@UI(max = 100f) var points: Int) : Action() {
    @JsonCreator private constructor() : this(0)

    override fun invoke(entity: Entity) {
        entity.container.cast<Level>()?.apply {
            scorePoints += points
        }
    }

    override fun toString() = super.toString() + " - +$points"
}