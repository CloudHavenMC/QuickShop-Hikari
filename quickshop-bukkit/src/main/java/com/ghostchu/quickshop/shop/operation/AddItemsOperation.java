package com.ghostchu.quickshop.shop.operation;

import com.ghostchu.quickshop.api.inventory.InventoryWrapper;
import com.ghostchu.quickshop.api.operation.Operation;
import com.ghostchu.quickshop.util.Util;
import com.ghostchu.quickshop.util.logger.Log;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Operation to add items
 */
public class AddItemsOperation implements Operation {

  private final List<ItemStack> items;
  private final InventoryWrapper inv;
  private boolean committed;
  private boolean rollback;
  private ItemStack[] snapshot;


  /**
   * Constructor.
   *
   * @param items  the items to add
   * @param inv    The {@link InventoryWrapper} to add to
   */
  public AddItemsOperation(@NotNull final List<ItemStack> items, @NotNull final InventoryWrapper inv) {

    this.items = items.stream().map(ItemStack::clone).toList();
    this.inv = inv;
  }

  @Override
  public boolean commit() {

    committed = true;
    this.snapshot = inv.createSnapshot();
    for (ItemStack target : this.items) {
      int lastRemains = -1;
      int remains = target.getAmount();
      int itemMaxStackSize = Util.getItemMaxStackSize(target.getType());

      while(remains > 0) {
        final int stackSize = Math.min(remains, itemMaxStackSize);
        target.setAmount(stackSize);
        Log.debug("Committing add item operation, remains: " + remains + ", stackSize: " + stackSize + ", target: " + target);
        final Map<Integer, ItemStack> notSaved = inv.addItem(target);
        if(notSaved.isEmpty()) {
          remains -= stackSize;
        } else {
          remains -= stackSize - notSaved.entrySet().iterator().next().getValue().getAmount();
        }
        if(remains == lastRemains) {
          return false;
        }
        lastRemains = remains;
      }
    }
    return true;
  }

  @Override
  public boolean isCommitted() {

    return this.committed;
  }

  @Override
  public boolean isRollback() {

    return this.rollback;
  }

  @Override
  public boolean rollback() {

    rollback = true;
    return inv.restoreSnapshot(this.snapshot);
  }

}
