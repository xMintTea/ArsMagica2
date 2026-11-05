package am2.spell.modifiers;

import am2.api.spell.component.interfaces.ISpellModifier;
import am2.api.spell.enums.SpellModifiers;
import am2.items.ItemsCommonProxy;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.EnumSet;

public class Lunar implements ISpellModifier{

	@Override
	public int getID(){
		return 10;
	}

	@Override
	public EnumSet<SpellModifiers> getAspectsModified(){
		return EnumSet.of(SpellModifiers.RANGE, SpellModifiers.RADIUS, SpellModifiers.DAMAGE, SpellModifiers.DURATION, SpellModifiers.HEALING);
	}

	@SuppressWarnings("incomplete-switch")
	@Override
	public float getModifier(SpellModifiers type, EntityLivingBase caster, Entity target, World world, byte[] metadata){
		switch (type){
		case RANGE:
			return modifyValueOnLunarCycle(world, 3f);
		case RADIUS:
			return modifyValueOnLunarCycle(world, 3);
		case DAMAGE:
			return modifyValueOnTime(world, 2.4f);
		case DURATION:
			return modifyValueOnTime(world, 5f);
		case HEALING:
			return modifyValueOnTime(world, 2f);
		}
		return 1.0f;
	}

	private float modifyValueOnTime(World world, float value){
		long x = world.provider.getWorldTime() % 24000;
        float moonPhase = world.provider.getMoonPhase(world.getWorldInfo().getWorldTime());
        float phaseMult = (float)(0.5 + 1.5 * Math.abs(4 - moonPhase) / 4.0) ;
		float multiplierFromTime = (float)(1 - 0.5 * Math.cos((2 * Math.PI * (x - 6000)) / 24000));

        return value * multiplierFromTime * phaseMult;
	}

	private float modifyValueOnLunarCycle(World world, float value){
		long boundedTime = world.provider.getWorldTime() % 24000;
		if (boundedTime > 12500 && boundedTime < 23500){
			return value + Math.abs(4 - world.provider.getMoonPhase(world.getWorldInfo().getWorldTime()));
		}
		return Math.abs(value - 1);
	}

	@Override
	public Object[] getRecipeItems(){
		return new Object[]{
				new ItemStack(ItemsCommonProxy.essence, 1, ItemsCommonProxy.essence.META_NATURE),
				new ItemStack(ItemsCommonProxy.itemOre, 1, ItemsCommonProxy.itemOre.META_MOONSTONE),
				Items.clock
		};
	}

	@Override
	public float getManaCostMultiplier(ItemStack spellStack, int stage, int quantity){
		return 4.0f * quantity;
	}

	@Override
	public byte[] getModifierMetadata(ItemStack[] matchedRecipe){
		return null;
	}
}
