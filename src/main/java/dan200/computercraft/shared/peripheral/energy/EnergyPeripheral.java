package dan200.computercraft.shared.peripheral.energy;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraftforge.energy.IEnergyStorage;

public class EnergyPeripheral implements IPeripheral {
    public EnergyPeripheral(IEnergyStorage energyStorage) {
        this.energyStorage = energyStorage;
    }

    private final IEnergyStorage energyStorage;

    @Override
    public String getType() {
        return "energy";
    }

    @Override
    public String[] getMethodNames() {
        return new String[]{
                "getEnergyStored",
                "getMaxEnergyStored",
                "canExtract",
                "canReceive"
        };
    }

    @Override
    public Object[] callMethod(IComputerAccess computer, ILuaContext context, int method, Object[] arguments) throws LuaException, InterruptedException {
        switch (method) {
            case 0:
                return new Object[]{energyStorage.getEnergyStored()};
            case 1:
                return new Object[]{energyStorage.getMaxEnergyStored()};
            case 2:
                return new Object[]{energyStorage.canExtract()};
            case 3:
                return new Object[]{energyStorage.canReceive()};
        }
        return new Object[0];
    }

    @Override
    public void attach(IComputerAccess computer) {

    }

    @Override
    public void detach(IComputerAccess computer) {

    }

    @Override
    public boolean equals(IPeripheral other) {
        return (other != null && other.getClass() == this.getClass());
    }
}