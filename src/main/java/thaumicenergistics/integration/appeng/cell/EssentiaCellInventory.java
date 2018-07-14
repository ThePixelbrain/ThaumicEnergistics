package thaumicenergistics.integration.appeng.cell;

import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.api.EssentiaStack;
import thaumicenergistics.api.storage.IAEEssentiaStack;
import thaumicenergistics.api.storage.IEssentiaStorageChannel;
import thaumicenergistics.integration.appeng.AEEssentiaStack;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.exceptions.AppEngException;
import appeng.api.implementations.items.IStorageCell;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.ISaveProvider;
import appeng.api.storage.IStorageChannel;
import appeng.me.storage.AbstractCellInventory;

/**
 * FIXME: Don't use any core AE2 core, AKA remove AbstractCellInventory
 * @author BrockWS
 */
public class EssentiaCellInventory extends AbstractCellInventory<IAEEssentiaStack> {

    protected EssentiaCellInventory(final NBTTagCompound data, final ISaveProvider container) {
        super(data, container, 8);
    }

    private EssentiaCellInventory(final ItemStack o, final ISaveProvider container) throws AppEngException {
        super(o, container, 8);
    }

    // *******************
    // Based on ItemCellInventory from Applied Energistics 2
    // *******************

    public static IMEInventoryHandler getCell(final ItemStack s, final ISaveProvider c) {
        try {
            return new EssentiaCellInventoryHandler(new EssentiaCellInventory(s, c));
        } catch (AppEngException e) {
            return null;
        }
    }

    private static boolean isStorageCell(ItemStack stack) {
        if (stack == null)
            return false;

        Item item = stack.getItem();
        if (item instanceof IStorageCell)
            return !((IStorageCell) item).storableInStorageCell();

        return false;
    }

    public static boolean isCell(ItemStack stack) {
        if (stack == null)
            return false;

        Item item = stack.getItem();
        if (item instanceof IStorageCell) {
            if (((IStorageCell) item).getChannel() == AEApi.instance().storage().getStorageChannel(IEssentiaStorageChannel.class))
                return ((IStorageCell) item).isStorageCell(stack);
        }

        return false;
    }

    @Override
    protected void loadCellItem(NBTTagCompound tag, int stackSize) {
        EssentiaStack stack = EssentiaStack.readFromNBT(tag);
        // TODO: Probably should catch errors and warn when failing to load it
        if (stack == null) {
            ThaumicEnergistics.LOG.warn("Failed to load EssentiaStack " + tag.toString());
            return;
        }
        stack.setAmount(stackSize);
        if (stack.getAmount() > 0) {
            this.cellItems.add(this.getChannel().createStack(stack));
        }
    }

    @Override
    public IAEEssentiaStack injectItems(IAEEssentiaStack input, Actionable mode, IActionSource src) {
        if (input == null)
            return null;
        if (input.getStackSize() == 0)
            return null;
        if (this.cellType.isBlackListed(this.i, input))
            return input;
        EssentiaStack essentiaStack = input.getStack();

        IAEEssentiaStack a = this.getCellItems().findPrecise(input);
        if (a != null) {
            long remainingItemSlots = this.getRemainingItemCount();
            if (remainingItemSlots <= 0) {
                return input;
            }
            if (input.getStackSize() > remainingItemSlots) {
                IAEEssentiaStack b = input.copy();
                b.setStackSize(b.getStackSize() - remainingItemSlots);
                if (mode == Actionable.MODULATE) {
                    a.setStackSize(a.getStackSize() + remainingItemSlots);
                    this.updateItemCount(remainingItemSlots);
                    this.saveChanges();
                }
                return b;
            } else {
                if (mode == Actionable.MODULATE) {
                    a.setStackSize(a.getStackSize() + input.getStackSize());
                    this.updateItemCount(input.getStackSize());
                    this.saveChanges();
                }
            }
        }

        if (this.canHoldNewItem()) {
            int remainingCount = (int) (this.getRemainingItemCount() - this.getBytesPerType() * itemsPerByte);
            if (remainingCount > 0) {
                if (input.getStackSize() > remainingCount) {
                    EssentiaStack toReturn = essentiaStack.copy();
                    toReturn.setAmount(essentiaStack.getAmount() - remainingCount);
                    if (mode == Actionable.MODULATE) {
                        EssentiaStack toWrite = essentiaStack.copy();
                        toWrite.setAmount(remainingCount);

                        this.cellItems.add(AEEssentiaStack.fromEssentiaStack(toWrite));
                        this.updateItemCount(toWrite.getAmount());
                        this.saveChanges();
                    }
                    return AEEssentiaStack.fromEssentiaStack(toReturn);
                }

                if (mode == Actionable.MODULATE) {
                    this.updateItemCount(input.getStackSize());
                    this.cellItems.add(input);
                    this.saveChanges();
                }

                return null;
            }
        }
        return input;
    }

    @Override
    public IAEEssentiaStack extractItems(IAEEssentiaStack request, Actionable mode, IActionSource src) {
        if (request == null)
            return null;

        long size = Math.min(Integer.MAX_VALUE, request.getStackSize());
        IAEEssentiaStack result = null;
        IAEEssentiaStack a = this.getCellItems().findPrecise(request);
        if (a != null) {
            result = a.copy();
            if (a.getStackSize() <= size) {
                result.setStackSize(a.getStackSize());
                if (mode == Actionable.MODULATE) {
                    a.setStackSize(0);
                    this.updateItemCount(-a.getStackSize());
                    this.saveChanges();
                }
            } else {
                result.setStackSize(size);
                if (mode == Actionable.MODULATE) {
                    a.setStackSize(a.getStackSize() - size);
                    this.updateItemCount(-size);
                    this.saveChanges();
                }
            }
        }
        return result;
    }

    @Override
    public IStorageChannel<IAEEssentiaStack> getChannel() {
        return AEApi.instance().storage().getStorageChannel(IEssentiaStorageChannel.class);
    }
}