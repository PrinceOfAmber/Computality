package dan200.computercraft.shared.peripheral.energy;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralProvider;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.energy.CapabilityEnergy;

public class EnergyPeripheralProvider implements IPeripheralProvider {
    @Override
    public IPeripheral getPeripheral(World world, BlockPos pos, EnumFacing side) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile == null)
            return null;
        if (!tile.hasCapability(CapabilityEnergy.ENERGY, side.getOpposite()))
            return null;
        return new EnergyPeripheral(tile.getCapability(CapabilityEnergy.ENERGY, side.getOpposite()));
    }
}
