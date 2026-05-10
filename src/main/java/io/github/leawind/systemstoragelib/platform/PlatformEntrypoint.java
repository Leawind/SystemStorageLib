package io.github.leawind.systemstoragelib.platform;
/*? if fabric {*/

import net.fabricmc.api.ModInitializer;
/*?}*/

/*? if forge {*/
/*import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
*/
/*?}*/

/*? if neoforge {*/
/*import net.neoforged.fml.ModContainer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
*/
/*?}*/

/*? if forgeLike {*/
/*@Mod("system_storage_lib")
public class PlatformEntrypoint {*/
/*?}*/

/*? if fabric {*/
public class PlatformEntrypoint implements ModInitializer {
  /*?}*/
  
  /*? if forge {*/
  /*public PlatformEntrypoint(final FMLJavaModLoadingContext context) {}*/
  /*?}*/
  
  /*? if neoforge {*/
  /*public PlatformEntrypoint(IEventBus modEventBus, ModContainer modContainer) {}*/
  /*?}*/
  
  /*? if fabric {*/
  @Override
  public void onInitialize() {}
  /*?}*/
}
