package ganymedes01.etfuturum.inventory;

import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.lib.Reference;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

/**
 * Port of the modern {@code GrindstoneMenu}. The 1.7.10/inventory API is close enough to the modern
 * one that the logic can stay almost line for line identical.
 */
public class ContainerGrindstone extends Container {

	private final World world;
	private final int x, y, z;
	private final InventoryCrafting inputSlots = new InventoryCrafting(this, 1, 2);
	private final InventoryCraftResult resultSlots = new InventoryCraftResult();

	public ContainerGrindstone(InventoryPlayer inventory, World world, int x, int y, int z) {
		this.world = world;
		this.x = x;
		this.y = y;
		this.z = z;

		addSlotToContainer(new Slot(inputSlots, 0, 49, 19) {
			@Override
			public boolean isItemValid(ItemStack stack) {
				return canBePutInGrindstone(stack);
			}
		});
		addSlotToContainer(new Slot(inputSlots, 1, 49, 40) {
			@Override
			public boolean isItemValid(ItemStack stack) {
				return canBePutInGrindstone(stack);
			}
		});
		addSlotToContainer(new SlotGrindstoneResult(this, 2, 129, 34));

		for (int row = 0; row < 3; ++row) {
			for (int column = 0; column < 9; ++column) {
				addSlotToContainer(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
			}
		}
		for (int column = 0; column < 9; ++column) {
			addSlotToContainer(new Slot(inventory, column, 8 + column * 18, 142));
		}
	}

	private static boolean canBePutInGrindstone(ItemStack stack) {
		return stack != null && (stack.isItemStackDamageable() || stack.getItem() == Items.enchanted_book || stack.isItemEnchanted());
	}

	@Override
	public void onCraftMatrixChanged(IInventory inventory) {
		super.onCraftMatrixChanged(inventory);
		if (inventory == inputSlots) {
			createResult();
		}
	}

	private void createResult() {
		ItemStack first = inputSlots.getStackInSlot(0);
		ItemStack second = inputSlots.getStackInSlot(1);
		boolean any = first != null || second != null;
		boolean both = first != null && second != null;

		if (!any) {
			resultSlots.setInventorySlotContents(0, null);
			return;
		}

		boolean anyUnenchanted = first != null && first.getItem() != Items.enchanted_book && !first.isItemEnchanted()
				|| second != null && second.getItem() != Items.enchanted_book && !second.isItemEnchanted();
		if (first != null && first.stackSize > 1 || second != null && second.stackSize > 1 || !both && anyUnenchanted) {
			resultSlots.setInventorySlotContents(0, null);
			return;
		}

		int count = 1;
		int damage;
		ItemStack base;

		if (both) {
			if (first.getItem() != second.getItem()) {
				resultSlots.setInventorySlotContents(0, null);
				return;
			}

			Item item = first.getItem();
			int firstRemaining = item.getMaxDamage() - first.getItemDamage();
			int secondRemaining = item.getMaxDamage() - second.getItemDamage();
			int repaired = firstRemaining + secondRemaining + item.getMaxDamage() * 5 / 100;
			damage = Math.max(item.getMaxDamage() - repaired, 0);
			base = mergeEnchants(first, second);

			if (!first.isItemStackDamageable()) {
				if (!ItemStack.areItemStacksEqual(first, second)) {
					resultSlots.setInventorySlotContents(0, null);
					return;
				}
				count = 2;
			}
		} else {
			boolean isFirst = first != null;
			damage = isFirst ? first.getItemDamage() : second.getItemDamage();
			base = isFirst ? first : second;
		}

		resultSlots.setInventorySlotContents(0, removeNonCurses(base, damage, count));
	}

	private static ItemStack mergeEnchants(ItemStack first, ItemStack second) {
		ItemStack result = first.copy();
		for (Map.Entry<Integer, Integer> entry : getEnchantments(second).entrySet()) {
			Enchantment enchantment = Enchantment.enchantmentsList[entry.getKey()];
			if (enchantment != null && (!isCurse(enchantment) || EnchantmentHelper.getEnchantmentLevel(enchantment.effectId, result) == 0)) {
				result.addEnchantment(enchantment, entry.getValue());
			}
		}
		return result;
	}

	private static ItemStack removeNonCurses(ItemStack stack, int damage, int count) {
		ItemStack result = stack.copy();
		result.stackSize = count;
		stripEnchantments(result);
		if (damage > 0) {
			result.setItemDamage(damage);
		} else if (result.hasTagCompound()) {
			result.getTagCompound().removeTag("Damage");
		}

		Map<Integer, Integer> curses = new HashMap<>();
		for (Map.Entry<Integer, Integer> entry : getEnchantments(stack).entrySet()) {
			Enchantment enchantment = Enchantment.enchantmentsList[entry.getKey()];
			if (enchantment != null && isCurse(enchantment)) {
				curses.put(entry.getKey(), entry.getValue());
			}
		}
		if (!curses.isEmpty()) {
			setEnchantments(result, curses);
		}

		result.setRepairCost(0);
		if (result.getItem() == Items.enchanted_book && curses.isEmpty()) {
			ItemStack book = new ItemStack(Items.book);
			if (stack.hasDisplayName()) {
				book.setStackDisplayName(stack.getDisplayName());
			}
			result = book;
		}
		return result;
	}

	private static void stripEnchantments(ItemStack stack) {
		if (stack.hasTagCompound()) {
			stack.getTagCompound().removeTag("ench");
			stack.getTagCompound().removeTag("StoredEnchantments");
			if (stack.getTagCompound().hasNoTags()) {
				stack.setTagCompound(null);
			}
		}
	}

	private static Map<Integer, Integer> getEnchantments(ItemStack stack) {
		Map<Integer, Integer> enchantments = new HashMap<>();
		if (stack == null) {
			return enchantments;
		}
		if (stack.getItem() == Items.enchanted_book) {
			NBTTagList list = Items.enchanted_book.func_92110_g(stack);
			if (list != null) {
				for (int i = 0; i < list.tagCount(); ++i) {
					NBTTagCompound tag = list.getCompoundTagAt(i);
					enchantments.put((int) tag.getShort("id"), (int) tag.getShort("lvl"));
				}
			}
		} else {
			enchantments.putAll(EnchantmentHelper.getEnchantments(stack));
		}
		return enchantments;
	}

	private static void setEnchantments(ItemStack stack, Map<Integer, Integer> enchantments) {
		if (stack.getItem() == Items.enchanted_book) {
			NBTTagList list = new NBTTagList();
			for (Map.Entry<Integer, Integer> entry : enchantments.entrySet()) {
				NBTTagCompound tag = new NBTTagCompound();
				tag.setShort("id", (short) (int) entry.getKey());
				tag.setShort("lvl", (short) (int) entry.getValue());
				list.appendTag(tag);
			}
			NBTTagCompound stackTag = stack.getTagCompound();
			if (stackTag == null) {
				stackTag = new NBTTagCompound();
				stack.setTagCompound(stackTag);
			}
			stackTag.setTag("StoredEnchantments", list);
		} else {
			EnchantmentHelper.setEnchantments(enchantments, stack);
		}
	}

	/**
	 * 1.7.10 (and Et Futurum Requiem) never add the vanilla curse enchantments, so nothing is a curse
	 * here. The checks are kept so the code still matches modern behaviour if a mod adds one.
	 */
	private static boolean isCurse(Enchantment enchantment) {
		return false;
	}

	private int getExperienceFromItem(ItemStack stack) {
		int experience = 0;
		for (Map.Entry<Integer, Integer> entry : getEnchantments(stack).entrySet()) {
			Enchantment enchantment = Enchantment.enchantmentsList[entry.getKey()];
			if (enchantment != null && !isCurse(enchantment)) {
				experience += enchantment.getMinEnchantability(entry.getValue());
			}
		}
		return experience;
	}

	/**
	 * Called by the result slot when the player takes the output. Mirrors the modern grindstone:
	 * award a random amount of experience between ceil(total / 2) and total, play the use sound and
	 * consume both input items.
	 */
	private void onResultTaken() {
		if (world.isRemote) {
			return;
		}

		int total = getExperienceFromItem(inputSlots.getStackInSlot(0)) + getExperienceFromItem(inputSlots.getStackInSlot(1));
		if (total > 0) {
			int half = (int) Math.ceil(total / 2.0D);
			int amount = half + world.rand.nextInt(half);
			while (amount > 0) {
				int split = EntityXPOrb.getXPSplit(amount);
				amount -= split;
				world.spawnEntityInWorld(new EntityXPOrb(world, x + 0.5D, y + 0.5D, z + 0.5D, split));
			}
		}

		world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, Reference.MCAssetVer + ":block.grindstone.use", 1.0F, world.rand.nextFloat() * 0.1F + 0.9F);
		inputSlots.setInventorySlotContents(0, null);
		inputSlots.setInventorySlotContents(1, null);
	}

	public boolean hasInput() {
		return inputSlots.getStackInSlot(0) != null || inputSlots.getStackInSlot(1) != null;
	}

	public boolean hasResult() {
		return resultSlots.getStackInSlot(0) != null;
	}

	@Override
	public boolean canInteractWith(EntityPlayer player) {
		return world.getBlock(x, y, z) == ModBlocks.GRINDSTONE.get() && player.getDistanceSq(x + 0.5D, y + 0.5D, z + 0.5D) <= 64.0D;
	}

	@Override
	public void onContainerClosed(EntityPlayer player) {
		super.onContainerClosed(player);
		if (!world.isRemote) {
			for (int i = 0; i < inputSlots.getSizeInventory(); ++i) {
				ItemStack stack = inputSlots.getStackInSlotOnClosing(i);
				if (stack != null) {
					player.dropPlayerItemWithRandomChoice(stack, false);
				}
			}
		}
	}

	@Override
	public ItemStack transferStackInSlot(EntityPlayer player, int index) {
		ItemStack result = null;
		Slot slot = (Slot) inventorySlots.get(index);

		if (slot != null && slot.getHasStack()) {
			ItemStack stack = slot.getStack();
			result = stack.copy();

			if (index == 2) {
				if (!mergeItemStack(stack, 3, 39, true)) {
					return null;
				}
				slot.onSlotChange(stack, result);
			} else if (index != 0 && index != 1) {
				if (inputSlots.getStackInSlot(0) != null && inputSlots.getStackInSlot(1) != null) {
					if (index >= 3 && index < 30) {
						if (!mergeItemStack(stack, 30, 39, false)) {
							return null;
						}
					} else if (index >= 30 && index < 39 && !mergeItemStack(stack, 3, 30, false)) {
						return null;
					}
				} else if (!mergeItemStack(stack, 0, 2, false)) {
					return null;
				}
			} else if (!mergeItemStack(stack, 3, 39, false)) {
				return null;
			}

			if (stack.stackSize == 0) {
				slot.putStack(null);
			} else {
				slot.onSlotChanged();
			}

			if (stack.stackSize == result.stackSize) {
				return null;
			}
			slot.onPickupFromSlot(player, stack);
		}

		return result;
	}

	private static class SlotGrindstoneResult extends Slot {

		private final ContainerGrindstone container;

		SlotGrindstoneResult(ContainerGrindstone container, int index, int x, int y) {
			super(container.resultSlots, 0, x, y);
			this.container = container;
		}

		@Override
		public boolean isItemValid(ItemStack stack) {
			return false;
		}

		@Override
		public void onPickupFromSlot(EntityPlayer player, ItemStack stack) {
			container.onResultTaken();
			super.onPickupFromSlot(player, stack);
		}
	}
}
