package me.odinclient.features.impl.skyblock

import me.odinmain.events.impl.RenderChestEvent
import me.odinmain.features.Category
import me.odinmain.features.Module
import me.odinmain.features.settings.impl.BooleanSetting
import me.odinmain.features.settings.impl.ColorSetting
import me.odinmain.features.settings.impl.SelectorSetting
import me.odinmain.utils.VecUtils.toAABB
import me.odinmain.utils.equalsOneOf
import me.odinmain.utils.render.Color
import me.odinmain.utils.render.world.RenderUtils
import me.odinmain.utils.skyblock.LocationUtils
import me.odinmain.utils.skyblock.dungeon.DungeonUtils
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.init.Blocks
import net.minecraft.tileentity.TileEntityChest
import net.minecraft.util.BlockPos
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.opengl.GL11

object ChestEsp : Module(
    name = "Chest Esp",
    category = Category.RENDER,
    description = "Renders chams or the bounding box for all chests"
) {

    private val onlyDungeon: Boolean by BooleanSetting(name = "Only Dungeon")
    private val onlyCH: Boolean by BooleanSetting(name = "Only Crystal Hollows")
    private val hideClicked: Boolean by BooleanSetting(name = "Hide Clicked")
    private val renderMode: Int by SelectorSetting(name = "Render Mode", "Chams", arrayListOf("Chams", "Outline"))
    private val color: Color by ColorSetting(name = "Color", default = Color.RED, allowAlpha = true)

    private val chests = mutableSetOf<BlockPos>()

    init {
        onWorldLoad { chests.clear() }
    }

    @SubscribeEvent
    fun onInteract(event: PlayerInteractEvent) {
        if (event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK || !mc.theWorld.getBlockState(event.pos).block.equalsOneOf(
                Blocks.chest,
                Blocks.trapped_chest
            )
        ) return
        chests.add(event.pos)
    }

    @SubscribeEvent
    fun onRenderChest(event: RenderChestEvent.Pre) {
        if (renderMode != 0 || event.chest != mc.theWorld.getTileEntity(event.chest.pos)) return
        if (hideClicked && chests.contains(event.chest.pos)) return
        if ((onlyDungeon && DungeonUtils.inDungeons) || (onlyCH && LocationUtils.currentArea == "Crystal Hollows") || (!onlyDungeon && !onlyCH)) {
            GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL)
            GlStateManager.color(1f, 1f, 1f, color.alpha)
            GlStateManager.enablePolygonOffset()
            GlStateManager.doPolygonOffset(1f, -1000000f)
        }

    }

    @SubscribeEvent
    fun onRenderChest(event: RenderChestEvent.Post) {
        if (renderMode != 0 || event.chest != mc.theWorld.getTileEntity(event.chest.pos)) return
        if (hideClicked && chests.contains(event.chest.pos)) return
        if ((onlyDungeon && DungeonUtils.inDungeons) || (onlyCH && LocationUtils.currentArea == "Crystal Hollows") || (!onlyDungeon && !onlyCH)) {
            GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL)
            GlStateManager.doPolygonOffset(1f, 1000000f)
            GlStateManager.disablePolygonOffset()
        }
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorldLastEvent) {
        if (renderMode != 1 || (onlyDungeon && !DungeonUtils.inDungeons) && !(onlyCH && LocationUtils.currentArea !== "Crystal Hollows")) return
        val chests = mc.theWorld.loadedTileEntityList.filterIsInstance(TileEntityChest::class.java)
        chests.forEach {
            if (hideClicked && this.chests.contains(it.pos)) return
            RenderUtils.drawCustomBox(it.pos.toAABB(), color, 2f, phase = true)
        }
    }

}