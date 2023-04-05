package hellfirepvp.modularmachinery.common.integration.crafttweaker.command;

import crafttweaker.CraftTweakerAPI;
import crafttweaker.runtime.ScriptLoader;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import stanhebben.zenscript.ZenModule;
import youyihj.zenutils.ZenUtils;
import youyihj.zenutils.api.reload.ScriptReloadEvent;

import static crafttweaker.mc1120.commands.SpecialMessagesChat.getNormalMessage;
import static youyihj.zenutils.impl.reload.ReloadCommand.RELOADABLE_LOADER;

/**
 * Similar to /ct reload, but can be executed by the server.
 */
public class CommandCTReload extends CommandBase {
    @Override
    public String getName() {
        return "mm-reload";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "command.modularmachinery.reload";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        if (server.isDedicatedServer()) {
            sender.sendMessage(getNormalMessage(TextFormatting.RED + "DedicatedServer detected! May be cause issues!"));
        }

        sender.sendMessage(getNormalMessage(TextFormatting.AQUA + "Beginning reload scripts"));
        sender.sendMessage(getNormalMessage("Only scripts that marked " + TextFormatting.GRAY + "#loader reloadable " + TextFormatting.RESET + "can be reloaded."));
        sender.sendMessage(getNormalMessage(TextFormatting.YELLOW + "Most recipe modifications are not reloadable, they will be ignored."));
        ZenUtils.tweaker.freezeActionApplying();
        ZenModule.loadedClasses.clear();
        ZenUtils.crafttweakerLogger.clear();
        MinecraftForge.EVENT_BUS.post(new ScriptReloadEvent.Pre(sender));
        ScriptLoader loader = RELOADABLE_LOADER.get();
        loader.setLoaderStage(ScriptLoader.LoaderStage.NOT_LOADED);
        CraftTweakerAPI.tweaker.loadScript(false, loader);
        if (loader.getLoaderStage() == ScriptLoader.LoaderStage.ERROR) {
            sender.sendMessage(getNormalMessage(TextFormatting.DARK_RED + "Failed to reload scripts"));
        } else {
            sender.sendMessage(getNormalMessage("Reloaded successfully"));
        }
        MinecraftForge.EVENT_BUS.post(new ScriptReloadEvent.Post(sender));
    }
}
