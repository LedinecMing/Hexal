package ram.talia.hexal.common.entities

import at.petrak.hexcasting.api.misc.FrozenColorizer
import at.petrak.hexcasting.api.misc.ManaConstants
import at.petrak.hexcasting.api.spell.SpellDatum
import at.petrak.hexcasting.api.spell.Widget
import com.mojang.datafixers.util.Either
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import ram.talia.hexal.api.plus

class WanderingWisp	(entityType: EntityType<out WanderingWisp>, world: Level) : BaseWisp(entityType, world) {

	var acceleration: Vec3
		get() = Vec3(entityData.get(ACCELERATION_X).toDouble(), entityData.get(ACCELERATION_Y).toDouble(), entityData.get(ACCELERATION_Z).toDouble())
		set(value) {
			entityData.set(ACCELERATION_X, value.x.toFloat())
			entityData.set(ACCELERATION_Y, value.y.toFloat())
			entityData.set(ACCELERATION_Z, value.z.toFloat())
		}

	override fun receiveIota(iota: SpellDatum<*>) { }

	override fun nextReceivedIota() = SpellDatum.make(Widget.NULL)

	override fun numRemainingIota() = 0

	override fun fightConsume(consumer: Either<BaseCastingWisp, ServerPlayer>) = false

	constructor(world: Level, pos: Vec3, media: Int) : this(HexalEntities.WANDERING_WISP, world) {
		setPos(pos)
		this.media = media
	}

	override fun tick() {
		super.tick()

		// make sure tick isn't called twice, since tick() is also called by castCallback to ensure wisps that need ticking don't actually get skipped on the tick that their
		// cast is successful. Not actually doing anything right now since tick() isn't currently being called twice.
//		if (lastTick == level.gameTime)
//			return
//		lastTick = level.gameTime

		// check if media is <= 0 ; destroy the wisp if it is, decrement the lifespan otherwise.
		if (media <= 0) {
			discard()
		}

//		HexalAPI.LOGGER.info("wisp $uuid ticked and ${if (scheduledCast) "does" else "doesn't"} have a cast scheduled.")

		if (!level.isClientSide)
			updateMedia()

		oldPos = position()

		move()

		if (level.isClientSide) {
			val colouriser = FrozenColorizer.fromNBT(entityData.get(COLOURISER))
			playWispParticles(colouriser)
			playTrailParticles(colouriser)
			playLinkParticles(colouriser)
		}
	}

	fun updateMedia() {
		media -= ManaConstants.DUST_UNIT/4
	}

	fun move() {

		val endPos = position() + deltaMovement

		//TODO: reflect* of blocks on collision, set deltaMovement to 0 and reflect acceleration
		setPos(endPos)

		deltaMovement += acceleration
		acceleration += Vec3(random.nextDouble(-0.05, 0.05), random.nextDouble(-0.05, 0.05), random.nextDouble(-0.05, 0.05))
		acceleration = Vec3(acceleration.x.coerceIn(-0.5, 0.5), acceleration.y.coerceIn(-0.5, 0.5), acceleration.z.coerceIn(-0.5, 0.5))
	}

	override fun readAdditionalSaveData(compound: CompoundTag) {
		super.readAdditionalSaveData(compound)

		entityData.set(ACCELERATION_X, compound.getFloat(TAG_ACCELERATION_X))
		entityData.set(ACCELERATION_Y, compound.getFloat(TAG_ACCELERATION_Y))
		entityData.set(ACCELERATION_Z, compound.getFloat(TAG_ACCELERATION_Z))
	}

	override fun addAdditionalSaveData(compound: CompoundTag) {
		super.addAdditionalSaveData(compound)

		compound.putFloat(TAG_ACCELERATION_X, entityData.get(ACCELERATION_X))
		compound.putFloat(TAG_ACCELERATION_Y, entityData.get(ACCELERATION_Y))
		compound.putFloat(TAG_ACCELERATION_Z, entityData.get(ACCELERATION_Z))
	}

	override fun defineSynchedData() {
		super.defineSynchedData()

		entityData.define(ACCELERATION_X, 0f)
		entityData.define(ACCELERATION_Y, 0f)
		entityData.define(ACCELERATION_Z, 0f)
	}

	companion object {
		@JvmStatic
		val ACCELERATION_X: EntityDataAccessor<Float> = SynchedEntityData.defineId(WanderingWisp::class.java, EntityDataSerializers.FLOAT)
		@JvmStatic
		val ACCELERATION_Y: EntityDataAccessor<Float> = SynchedEntityData.defineId(WanderingWisp::class.java, EntityDataSerializers.FLOAT)
		@JvmStatic
		val ACCELERATION_Z: EntityDataAccessor<Float> = SynchedEntityData.defineId(WanderingWisp::class.java, EntityDataSerializers.FLOAT)

		const val TAG_ACCELERATION_X = "acceleration_x"
		const val TAG_ACCELERATION_Y = "acceleration_y"
		const val TAG_ACCELERATION_Z = "acceleration_z"
	}
}