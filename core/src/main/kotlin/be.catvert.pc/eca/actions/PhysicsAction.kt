package be.catvert.pc.eca.actions


import be.catvert.pc.eca.Entity
import be.catvert.pc.eca.components.RequiredComponent
import be.catvert.pc.eca.components.logics.PhysicsComponent
import be.catvert.pc.eca.containers.EntityContainer
import be.catvert.pc.ui.Description
import be.catvert.pc.ui.UI
import com.fasterxml.jackson.annotation.JsonCreator

/**
 * Action permettant d'appliquer une action physique sur une entité ayant le component PhysicsComponent
 * @see PhysicsComponent
 */
@RequiredComponent(PhysicsComponent::class)
@Description("Permet d'effectuer une action physique sur une entité")
class PhysicsAction(@UI var physicsAction: PhysicsActions) : Action() {
    @JsonCreator private constructor() : this(PhysicsActions.MOVE_LEFT)

    /**
     * Représente les différentes actions physiques possible.
     */
    enum class PhysicsActions {
        MOVE_LEFT, MOVE_RIGHT, MOVE_UP, MOVE_DOWN, JUMP, FORCE_JUMP;

        override fun toString() = when (this) {
            PhysicsAction.PhysicsActions.MOVE_LEFT -> "Déplacer à gauche"
            PhysicsAction.PhysicsActions.MOVE_RIGHT -> "Déplacer à droite"
            PhysicsAction.PhysicsActions.MOVE_UP -> "Déplacer vers le haut"
            PhysicsAction.PhysicsActions.MOVE_DOWN -> "Déplacer vers le bas"
            PhysicsAction.PhysicsActions.JUMP -> "Sauter"
            PhysicsAction.PhysicsActions.FORCE_JUMP -> "Forcer le saut"
        }
    }

    override fun invoke(entity: Entity, container: EntityContainer) {
        entity.getCurrentState().getComponent<PhysicsComponent>()?.physicsActions?.add(physicsAction)
    }

    override fun toString() = super.toString() + " - $physicsAction"
}