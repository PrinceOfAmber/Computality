package dan200.computercraft.core.apis;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.core.computer.IComputerEnvironment;
import dan200.computercraft.core.terminal.Terminal;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import org.luaj.vm2.ast.Str;

import java.util.List;

public class MinecraftAPI implements ILuaAPI {
    private Terminal m_terminal;
    private IComputerEnvironment m_environment;

    public MinecraftAPI(IAPIEnvironment _environment) {
        m_terminal = _environment.getTerminal();
        m_environment = _environment.getComputerEnvironment();
    }

    @Override
    public String[] getNames() {
        return new String[]{
                "minecraft",
                "mc"
        };
    }

    @Override
    public void startup() {

    }

    @Override
    public void advance(double _dt) {

    }

    @Override
    public void shutdown() {

    }

    @Override
    public String[] getMethodNames() {
        return new String[]
                {
                        "getVersion",
                        "isModLoaded",
                        "getLoadedModIds",
                        "getLoadedModNames",
                };
    }

    @Override
    public Object[] callMethod(ILuaContext context, int method, Object[] arguments) throws LuaException, InterruptedException {
        switch (method) {
            case 0:
                return new Object[]{Loader.MC_VERSION};
            case 1:
                if (arguments.length < 1 || !(arguments[0] instanceof String))
                    throw new LuaException("Expected String");
                return new Object[]{Loader.isModLoaded(arguments[0].toString())};
            case 2:
                List<ModContainer> containers = Loader.instance().getActiveModList();
                String[] strings = new String[containers.size()];
                for (int i = 0; i < containers.size(); i++) {
                    ModContainer container = containers.get(i);
                    strings[i]=container.getModId();
                }
                return strings;
            case 3:
                List<ModContainer> mods = Loader.instance().getActiveModList();
                String[] modNames = new String[mods.size()];
                for (int i = 0; i < mods.size(); i++) {
                    ModContainer container = mods.get(i);
                    modNames[i]=container.getName();
                }
                return modNames;
        }
        return new Object[0];
    }
}