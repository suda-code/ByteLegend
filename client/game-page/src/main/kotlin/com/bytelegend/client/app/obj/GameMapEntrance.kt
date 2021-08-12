@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.bytelegend.client.app.obj

import com.bytelegend.app.client.api.GameScene
import com.bytelegend.app.client.api.GameSceneAware
import com.bytelegend.app.shared.GridCoordinate
import com.bytelegend.app.shared.INVISIBLE_OBJECT_LAYER
import com.bytelegend.app.shared.PixelCoordinate
import com.bytelegend.app.shared.objects.CoordinateAware
import com.bytelegend.app.shared.objects.GameObject
import com.bytelegend.app.shared.objects.GameObjectRole
import com.bytelegend.client.app.engine.Game
import com.bytelegend.client.app.web.WebSocketClient
import com.bytelegend.client.utils.jsObjectBackedSetOf
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class GameMapEntrance(
    override val id: String,
    override val gameScene: GameScene,
    override val gridCoordinate: GridCoordinate,
    val destMapId: String,
    private val backEntrancePointId: String,
    private val roadSign: Boolean,
    private val webSocketClient: WebSocketClient
) : GameObject, GameSceneAware, CoordinateAware {
    override val layer: Int = INVISIBLE_OBJECT_LAYER
    override val roles: Set<String> = jsObjectBackedSetOf(GameObjectRole.MapEntrance, GameObjectRole.CoordinateAware).apply {
        if (roadSign) {
            add(GameObjectRole.HasFloatingTitle.toString())
        }
    }
    override val pixelCoordinate: PixelCoordinate = gridCoordinate * gameScene.map.tileSize

    @Suppress("UnsafeCastFromDynamic")
    override fun onTouch(obj: GameObject) {
        if (gameRuntime.hero != null && gameRuntime.hero!!.id == obj.id) {
            val heroId = gameRuntime.hero!!.id
            GlobalScope.launch {
                webSocketClient.leaveScene(gameScene.map.id, destMapId)
            }

            gameRuntime.sceneContainer.loadScene(destMapId) { oldScene, newScene ->
                val oldHero = oldScene!!.objects.getById<HeroCharacter>(heroId)

                val newHero = HeroCharacter(newScene, oldHero.player.asDynamic()).apply { init() }
                newHero.direction = oldHero.direction
                val newMapEntrance = newScene.objects.getPointById(backEntrancePointId)
                newHero.pixelCoordinate = newMapEntrance * newScene.map.tileSize
                gameRuntime.heroPlayer.map = newScene.map.id
                gameRuntime.heroPlayer.x = newMapEntrance.x
                gameRuntime.heroPlayer.y = newMapEntrance.y

                oldHero.close()
                gameRuntime.unsafeCast<Game>()._hero = newHero

                GlobalScope.launch {
                    webSocketClient.enterScene(oldScene.map.id, newScene.map.id)
                }
            }
        } else {
            gameRuntime.activeScene.objects.getById<CharacterSprite>(obj.id).close()
        }
    }
}