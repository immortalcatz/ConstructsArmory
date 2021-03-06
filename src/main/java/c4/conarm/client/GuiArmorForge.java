package c4.conarm.client;

import c4.conarm.lib.ArmoryRegistry;
import c4.conarm.lib.ArmoryRegistryClient;
import c4.conarm.ConstructsArmory;
import c4.conarm.lib.armor.ArmorCore;
import c4.conarm.lib.client.ArmorBuildGuiInfo;
import c4.conarm.lib.modifiers.IArmorModifyable;
import c4.conarm.lib.tinkering.TinkersArmor;
import c4.conarm.armor.common.inventory.ContainerArmorForge;
import c4.conarm.armor.common.inventory.SlotArmorForgeIn;
import c4.conarm.armor.common.network.ArmorForgeSelectionPacket;
import c4.conarm.armor.common.network.ArmorForgeTextPacket;
import c4.conarm.armor.common.tileentities.TileArmorForge;
import com.google.common.collect.Lists;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.Level;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.Point;
import slimeknights.mantle.client.gui.GuiElement;
import slimeknights.mantle.client.gui.GuiElementScalable;
import slimeknights.mantle.client.gui.GuiModule;
import slimeknights.tconstruct.common.TinkerNetwork;
import slimeknights.tconstruct.library.TinkerRegistry;
import slimeknights.tconstruct.library.Util;
import slimeknights.tconstruct.library.client.Icons;
import slimeknights.tconstruct.library.modifiers.IModifier;
import slimeknights.tconstruct.library.modifiers.ModifierNBT;
import slimeknights.tconstruct.library.tinkering.IModifyable;
import slimeknights.tconstruct.library.tinkering.IToolStationDisplay;
import slimeknights.tconstruct.library.tinkering.PartMaterialType;
import slimeknights.tconstruct.library.tools.IToolPart;
import slimeknights.tconstruct.library.utils.TagUtil;
import slimeknights.tconstruct.tools.common.client.GuiTinkerStation;
import slimeknights.tconstruct.tools.common.client.module.GuiInfoPanel;
import slimeknights.tconstruct.tools.common.inventory.ContainerTinkerStation;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

/*This class is a re-implementation of the
GuiToolStation class from Tinkers' Construct
Tinkers' Construct is licensed under the MIT License
Find the source here: https://github.com/SlimeKnights/TinkersConstruct
 */
@SideOnly(Side.CLIENT)
public class GuiArmorForge extends GuiTinkerStation
{
    private static final ResourceLocation BACKGROUND = new ResourceLocation(ConstructsArmory.MODID, "textures/gui/armorforge.png");
    private static final ResourceLocation ICONS = new ResourceLocation(ConstructsArmory.MODID, "textures/gui/icons.png");

    private static final GuiElement TextFieldActive = new GuiElement(0, 210, 92, 12, 256, 256);
    private static final GuiElement ItemCover = new GuiElement(176, 18, 80, 64);
    private static final GuiElement SlotBackground = new GuiElement(176, 0, 18, 18);
    private static final GuiElement SlotBorder = new GuiElement(194, 0, 18, 18);

    private static final GuiElement SlotSpaceTop = new GuiElement(0, 174 + 2, 18, 2);
    private static final GuiElement SlotSpaceBottom = new GuiElement(0, 174, 18, 2);
    private static final GuiElement PanelSpaceL = new GuiElement(0, 174, 5, 4);
    private static final GuiElement PanelSpaceR = new GuiElement(9, 174, 9, 4);

    private static final GuiElement BeamLeft = new GuiElement(0, 180, 2, 7);
    private static final GuiElement BeamRight = new GuiElement(131, 180, 2, 7);
    private static final GuiElementScalable BeamCenter = new GuiElementScalable(2, 180, 129, 7);

    public static final int Column_Count = 5;
    private static final int Table_slot_count = 6;

    protected GuiElement buttonDecorationTop = SlotSpaceTop;
    protected GuiElement buttonDecorationBot = SlotSpaceBottom;
    protected GuiElement panelDecorationL = PanelSpaceL;
    protected GuiElement panelDecorationR = PanelSpaceR;

    protected GuiElement beamL = new GuiElement(0, 0, 0, 0);
    protected GuiElement beamR = new GuiElement(0, 0, 0, 0);
    protected GuiElementScalable beamC = new GuiElementScalable(0, 0, 0, 0);

    protected GuiButtonsArmorForge buttons;
    protected int activeSlots;

    public GuiTextField textField;

    protected GuiInfoPanel armorInfo;
    protected GuiInfoPanel traitInfo;

    public ArmorBuildGuiInfo currentInfo = GuiButtonArmorRepair.info;

    private static final Field GUI_TOP = ReflectionHelper.findField(GuiContainer.class, "guiTop", "field_147009_r");
    private static final Field BUTTON_LIST = ReflectionHelper.findField(GuiScreen.class, "buttonList", "field_146292_n");

    public GuiArmorForge(InventoryPlayer playerInv, World world, BlockPos pos, TileArmorForge tile) {
        super(world, pos, (ContainerTinkerStation) tile.createContainer(playerInv, world, pos));

        buttons = new GuiButtonsArmorForge(this, inventorySlots);
        this.addModule(buttons);
        armorInfo = new GuiInfoPanel(this, inventorySlots);
        this.addModule(armorInfo);
        traitInfo = new GuiInfoPanel(this, inventorySlots);
        this.addModule(traitInfo);

        armorInfo.yOffset = 5;
        traitInfo.yOffset = armorInfo.getYSize() + 9;

        this.ySize = 174;

        metal();
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);

        this.guiTop += 4;
        this.cornerY += 4;

        textField = new GuiTextField(0, fontRenderer, cornerX + 80, cornerY + 7, 82, 12);
        textField.setEnableBackgroundDrawing(false);
        textField.setMaxStringLength(40);

        buttons.xOffset = -2;
        buttons.yOffset = beamC.h + buttonDecorationTop.h;
        armorInfo.xOffset = 2;
        armorInfo.yOffset = beamC.h + panelDecorationL.h;
        traitInfo.xOffset = armorInfo.xOffset;
        traitInfo.yOffset = armorInfo.yOffset + armorInfo.getYSize() + 4;

        for(GuiModule module : modules) {
            try {
                GUI_TOP.setInt(module, module.getGuiTop() + 4);
            } catch (IllegalAccessException e) {
                ConstructsArmory.logger.error("Failed to set guiTop");
            }
        }

        updateGUI();
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
    }

    public Set<ArmorCore> getBuildableItems() {
        return ArmoryRegistry.getArmorForgeCrafting();
    }

    public void onArmorSelection(ArmorBuildGuiInfo info) {
        activeSlots = Math.min(info.positions.size(), Table_slot_count);
        currentInfo = info;

        ArmorCore armor = null;

        if(info.armor.getItem() instanceof ArmorCore) {
            armor = (ArmorCore) info.armor.getItem();
        }

        ((ContainerArmorForge) inventorySlots).setArmorSelection(armor, activeSlots);
        TinkerNetwork.sendToServer(new ArmorForgeSelectionPacket(armor, activeSlots));
        updateGUI();
    }

    public void onArmorSelectionPacket(ArmorForgeSelectionPacket packet) {
        ArmorBuildGuiInfo info = ArmoryRegistryClient.getArmorBuildInfoForArmor(packet.armor);
        if(info == null) {
            info = GuiButtonArmorRepair.info;
        }
        activeSlots = packet.activeSlots;
        currentInfo = info;

        buttons.setSelectedButtonByArmor(currentInfo.armor);

        updateGUI();
    }

    public void updateGUI() {
        int i;
        for(i = 0; i < activeSlots; i++) {
            Point point = currentInfo.positions.get(i);

            Slot slot = inventorySlots.getSlot(i);
            slot.xPos = point.getX();
            slot.yPos = point.getY();
        }

        int stillFilled = 0;
        for(; i < Table_slot_count; i++) {
            Slot slot = inventorySlots.getSlot(i);

            if(slot.getHasStack()) {
                slot.xPos = 87 + 20 * stillFilled;
                slot.yPos = 62;
                stillFilled++;
            }
            else {
                slot.xPos = 0;
                slot.yPos = 0;
            }
        }

        updateDisplay();
    }

    @Override
    public void updateDisplay() {
        ContainerArmorForge container = (ContainerArmorForge) inventorySlots;
        ItemStack armorStack = container.getResult();
        if(armorStack.isEmpty()) {
            armorStack = inventorySlots.getSlot(0).getStack();
        }

        if(armorStack.getItem() instanceof IArmorModifyable) {
            if(armorStack.getItem() instanceof IToolStationDisplay) {
                IToolStationDisplay armor = (IToolStationDisplay) armorStack.getItem();
                armorInfo.setCaption(armor.getLocalizedName());
                armorInfo.setText(armor.getInformation(armorStack));
            }
            else {
                armorInfo.setCaption(armorStack.getDisplayName());
                armorInfo.setText();
            }

            traitInfo.setCaption(Util.translate("gui.toolstation.traits"));

            List<String> mods = Lists.newLinkedList();
            List<String> tips = Lists.newLinkedList();
            NBTTagList tagList = TagUtil.getModifiersTagList(armorStack);
            for(int i = 0; i < tagList.tagCount(); i++) {
                NBTTagCompound tag = tagList.getCompoundTagAt(i);
                ModifierNBT data = ModifierNBT.readTag(tag);

                IModifier modifier = ArmoryRegistry.getArmorModifier(data.identifier);
                if (modifier == null) {
                    modifier = TinkerRegistry.getModifier(data.identifier);
                }
                if (modifier == null || modifier.isHidden()) {
                    continue;
                }

                mods.add(data.getColorString() + modifier.getTooltip(tag, true));
                tips.add(data.getColorString() + modifier.getLocalizedDesc());
            }

            if(mods.isEmpty()) {
                mods.add(Util.translate("gui.toolstation.noTraits"));
            }

            traitInfo.setText(mods, tips);
        }
        else if(currentInfo.armor.isEmpty()) {
            armorInfo.setCaption(Util.translate("gui.toolstation.repair"));
            armorInfo.setText();

            traitInfo.setCaption(null);
            String c = TextFormatting.DARK_GRAY.toString();
            String[] art = new String[]{
                    c + "",
                    c + "",
                    c + "       .",
                    c + "     /( _________",
                    c + "     |  >:=========`",
                    c + "     )(  ",
                    c + "     \"\""
            };
            traitInfo.setText(art);
        }
        else {
            ArmorCore armor = (ArmorCore) currentInfo.armor.getItem();
            armorInfo.setCaption(armor.getLocalizedToolName());
            armorInfo.setText(armor.getLocalizedDescription());

            List<String> text = Lists.newLinkedList();
            List<PartMaterialType> pms = armor.getRequiredComponents();
            for(int i = 0; i < pms.size(); i++) {
                PartMaterialType pmt = pms.get(i);
                StringBuilder sb = new StringBuilder();

                ItemStack slotStack = container.getSlot(i).getStack();
                if(!pmt.isValid(slotStack)) {
                    sb.append(TextFormatting.RED);

                    if(slotStack.getItem() instanceof IToolPart) {
                        if(pmt.isValidItem((IToolPart) slotStack.getItem())) {
                            warning(Util.translate("gui.error.wrong_material_part"));
                        }
                    }
                }

                sb.append(" * ");
                for(IToolPart part : pmt.getPossibleParts()) {
                    if(part instanceof Item) {
                        sb.append(((Item) part).getItemStackDisplayName(new ItemStack((Item) part)));
                        sb.append("/");
                    }
                }
                sb.deleteCharAt(sb.length() - 1);
                text.add(sb.toString());
            }
            traitInfo.setCaption(Util.translate("gui.toolstation.components"));
            traitInfo.setText(text.toArray(new String[text.size()]));
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        textField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if(!textField.isFocused()) {
            super.keyTyped(typedChar, keyCode);
        }
        else {
            if(keyCode == 1) {
                this.mc.player.closeScreen();
            }

            textField.textboxKeyTyped(typedChar, keyCode);
            TinkerNetwork.sendToServer(new ArmorForgeTextPacket(textField.getText()));
            ((ContainerArmorForge) container).setArmorName(textField.getText());
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        textField.updateCursorCounter();
    }

    @Override
    public void drawSlot(Slot slotIn) {
        if(slotIn instanceof SlotArmorForgeIn && ((SlotArmorForgeIn) slotIn).isDormant() && !slotIn.getHasStack()) {
            return;
        }

        super.drawSlot(slotIn);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        drawBackground(BACKGROUND);

        if(textField.isFocused()) {
            TextFieldActive.draw(cornerX + 78, cornerY + 6);
        }

        textField.drawTextBox();

        int x = 0;
        int y = 0;

        final float scale = 3.7f;
        final float xOff = 10f;
        final float yOff = 22f;
        GlStateManager.translate(xOff, yOff, 0);
        GlStateManager.scale(scale, scale, 1.0f);
        {
            int logoX = (int) (this.cornerX / scale);
            int logoY = (int) (this.cornerY / scale);

            if(currentInfo != null) {
                if(!currentInfo.armor.isEmpty()) {
                    itemRender.renderItemIntoGUI(currentInfo.armor, logoX, logoY);
                }
                else if(currentInfo == GuiButtonArmorRepair.info) {
                    this.mc.getTextureManager().bindTexture(Icons.ICON);
                    Icons.ICON_Anvil.draw(logoX, logoY);
                }
            }
        }
        GlStateManager.scale(1f / scale, 1f / scale, 1.0f);
        GlStateManager.translate(-xOff, -yOff, 0);

        this.mc.getTextureManager().bindTexture(BACKGROUND);

        GlStateManager.enableBlend();
        GlStateManager.enableAlpha();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableDepth();

        GlStateManager.color(1.0f, 1.0f, 1.0f, 0.82f);
        ItemCover.draw(this.cornerX + 7, this.cornerY + 18);

        GlStateManager.color(1.0f, 1.0f, 1.0f, 0.28f);
        for(int i = 0; i < activeSlots; i++) {
            Slot slot = inventorySlots.getSlot(i);
            SlotBackground.draw(x + this.cornerX + slot.xPos - 1, y + this.cornerY + slot.yPos - 1);
        }

        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        for(int i = 0; i < Table_slot_count; i++) {
            Slot slot = inventorySlots.getSlot(i);
            if(slot instanceof SlotArmorForgeIn && (!((SlotArmorForgeIn) slot).isDormant() || slot.getHasStack())) {
                SlotBorder.draw(
                        x + this.cornerX + slot.xPos - 1, y + this.cornerY + slot.yPos - 1);
            }
        }

        this.mc.getTextureManager().bindTexture(Icons.ICON);

        if(currentInfo == GuiButtonArmorRepair.info) {
            drawRepairSlotIcons();
        }
        else if(currentInfo.armor.getItem() instanceof TinkersArmor) {
            for(int i = 0; i < activeSlots; i++) {
                Slot slot = inventorySlots.getSlot(i);
                if(!(slot instanceof SlotArmorForgeIn)) {
                    continue;
                }

                ItemStack stack = ((SlotArmorForgeIn) slot).icon;
                if(stack == null) {
                    continue;
                }

                itemRender.renderItemIntoGUI(stack,
                        x + this.cornerX + slot.xPos,
                        y + this.cornerY + slot.yPos);
            }
        }

        this.mc.getTextureManager().bindTexture(BACKGROUND);
        x = buttons.getGuiLeft() - beamL.w;
        y = cornerY;
        x += beamL.draw(x, y);
        x += beamC.drawScaledX(x, y, buttons.getXSize());
        beamR.draw(x, y);

        x = armorInfo.getGuiLeft() - beamL.w;
        x += beamL.draw(x, y);
        x += beamC.drawScaledX(x, y, armorInfo.getXSize());
        beamR.draw(x, y);

        List<GuiButton> buttonList = Lists.newLinkedList();
        try {
            buttonList = (List<GuiButton>) BUTTON_LIST.get(buttons);
        } catch (IllegalAccessException e) {
            ConstructsArmory.logger.log(Level.ERROR, "Failed to initialize buttonList");
        }
        for(Object o : buttonList) {
            GuiButton button = (GuiButton) o;

            buttonDecorationTop.draw(button.x, button.y - buttonDecorationTop.h);
            if(button.id < buttonList.size() - Column_Count) {
                buttonDecorationBot.draw(button.x, button.y + button.height);
            }
        }

        panelDecorationL.draw(armorInfo.getGuiLeft() + 5, armorInfo.getGuiTop() - panelDecorationL.h);
        panelDecorationR.draw(armorInfo.guiRight() - 5 - panelDecorationR.w, armorInfo.getGuiTop() - panelDecorationR.h);
        panelDecorationL.draw(traitInfo.getGuiLeft() + 5, traitInfo.getGuiTop() - panelDecorationL.h);
        panelDecorationR.draw(traitInfo.guiRight() - 5 - panelDecorationR.w, traitInfo.getGuiTop() - panelDecorationR.h);

        GlStateManager.enableDepth();

        super.drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);
    }

    protected void drawRepairSlotIcons() {
        for(int i = 0; i < activeSlots; i++) {
            drawRepairSlotIcon(i);
        }
    }

    protected void drawRepairSlotIcon(int i) {
        GuiElement icon = null;
        Slot slot = inventorySlots.getSlot(i);

        if(slot.getHasStack()) {
            return;
        }

        if(i == 0) {
            icon = Icons.ICON_Pickaxe;
        }
        else if(i == 1) {
            icon = Icons.ICON_Dust;
        }
        else if(i == 2) {
            icon = Icons.ICON_Lapis;
        }
        else if(i == 3) {
            icon = Icons.ICON_Ingot;
        }
        else if(i == 4) {
            icon = Icons.ICON_Gem;
        }
        else if(i == 5) {
            icon = Icons.ICON_Quartz;
        }

        if(icon != null) {
            drawIconEmpty(slot, icon);
        }
    }

    @Override
    protected void drawIcon(Slot slot, GuiElement element) {
        this.mc.getTextureManager().bindTexture(ICONS);
        element.draw(slot.xPos + this.cornerX - 1, slot.yPos + this.cornerY - 1);
    }

    protected void metal() {
        armorInfo.metal();
        traitInfo.metal();

        buttonDecorationTop = SlotSpaceTop.shift(SlotSpaceTop.w * 2, 0);
        buttonDecorationBot = SlotSpaceBottom.shift(SlotSpaceBottom.w * 2, 0);
        panelDecorationL = PanelSpaceL.shift(18 * 2, 0);
        panelDecorationR = PanelSpaceR.shift(18 * 2, 0);

        buttons.metal();

        beamL = BeamLeft.shift(0, BeamLeft.h);
        beamR = BeamRight.shift(0, BeamRight.h);
        beamC = BeamCenter.shift(0, BeamCenter.h);
    }

    @Override
    public void error(String message) {
        armorInfo.setCaption(Util.translate("gui.error"));
        armorInfo.setText(message);
        traitInfo.setCaption(null);
        traitInfo.setText();
    }

    @Override
    public void warning(String message) {
        armorInfo.setCaption(Util.translate("gui.warning"));
        armorInfo.setText(message);
        traitInfo.setCaption(null);
        traitInfo.setText();
    }
}