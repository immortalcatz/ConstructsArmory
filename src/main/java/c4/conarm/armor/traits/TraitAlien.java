package c4.conarm.armor.traits;

import c4.conarm.lib.armor.ArmorNBT;
import c4.conarm.armor.ArmorTagUtil;
import c4.conarm.lib.materials.CoreMaterialStats;
import c4.conarm.lib.materials.PlatesMaterialStats;
import com.google.common.collect.ImmutableList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import slimeknights.tconstruct.library.utils.TagUtil;

import java.util.List;

public class TraitAlien extends TraitProgressiveArmorStats {

    protected static final int TICK_PER_STAT = 72;

    protected static final int DURABILITY_STEP = 1;
    protected static final float TOUGHNESS_STEP = 0.005F;
    protected static final float ARMOR_STEP = 0.007F;

    public TraitAlien() {
        super("alien_armor", TextFormatting.YELLOW);
    }

    @Override
    public void applyEffect(NBTTagCompound rootCompound, NBTTagCompound modifierTag) {

        if (!hasPool(rootCompound)) {

            StatNBT data = new StatNBT();

            int statPoints = 800;
            for (; statPoints > 0; statPoints--) {
                switch (random.nextInt(3)) {
                    case 0:
                        data.durability += DURABILITY_STEP;
                        break;
                    case 1:
                        data.toughness += TOUGHNESS_STEP;
                        break;
                    case 2:
                        data.armor += ARMOR_STEP;
                        break;
                }
            }

            setPool(rootCompound, data);
        }

        super.applyEffect(rootCompound, modifierTag);
    }

    @Override
    public void onArmorTick(ItemStack armor, World world, EntityPlayer player){

        if (player.world.isRemote) {
            return;
        }

        if (player.ticksExisted % TICK_PER_STAT > 0) {
            return;
        }

        NBTTagCompound root = TagUtil.getTagSafe(armor);
        StatNBT pool = getPool(root);
        StatNBT distributed = getBonus(root);
        ArmorNBT data = ArmorTagUtil.getArmorStats(armor);

        //Armor
        if (player.ticksExisted % (TICK_PER_STAT * 3) == 0) {
            if (distributed.armor < pool.armor) {
                data.armor += ARMOR_STEP;
                distributed.armor += ARMOR_STEP;
            }
        }

        //Toughness
        else if (player.ticksExisted % (TICK_PER_STAT * 2) == 0) {
            if (distributed.toughness < pool.toughness) {
                data.toughness += TOUGHNESS_STEP;
                distributed.toughness += TOUGHNESS_STEP;
            }
        }
        //Durability
        else {
            if (distributed.durability < pool.durability) {
                data.durability += DURABILITY_STEP;
                distributed.durability += DURABILITY_STEP;
            }
        }

        TagUtil.setToolTag(root, data.get());
        setBonus(root, distributed);
    }

    @Override
    public List<String> getExtraInfo(ItemStack tool, NBTTagCompound modifierTag) {
        StatNBT pool = getBonus(TagUtil.getTagSafe(tool));

        return ImmutableList.of(CoreMaterialStats.formatDurability(pool.durability),
                CoreMaterialStats.formatArmor(pool.armor),
                PlatesMaterialStats.formatToughness(pool.toughness));
    }
}
