package fr.zeevoker2vex.radio.client;

import fr.nathanael2611.modularvoicechat.api.VoiceKeyEvent;
import fr.nathanael2611.modularvoicechat.api.VoicePlayEvent;
import fr.nathanael2611.modularvoicechat.client.gui.GuiConfig;
import fr.nathanael2611.modularvoicechat.client.voice.audio.MicroManager;
import fr.nathanael2611.modularvoicechat.util.AudioUtil;
import fr.zeevoker2vex.radio.common.CommonProxy;
import fr.zeevoker2vex.radio.common.RadioAddon;
import fr.zeevoker2vex.radio.common.items.RadioItem;
import fr.zeevoker2vex.radio.common.network.NetworkHandler;
import fr.zeevoker2vex.radio.common.network.server.PlayerSpeakingOnRadioPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import uk.me.berndporr.iirj.ChebyshevI;

public class ClientProxy extends CommonProxy {

    public static final KeyBinding SPEAK_ON_RADIO = new KeyBinding("key." + RadioAddon.MOD_ID + ".speakonradio", Keyboard.KEY_X, "key.categories." + RadioAddon.MOD_ID);

    public static boolean speaking = false;
    private Minecraft mc;

    public ChebyshevI chebyshevFilter;
    public int gainRadio = 5;

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);

        mc = Minecraft.getMinecraft();

        ClientRegistry.registerKeyBinding(SPEAK_ON_RADIO);

        MinecraftForge.EVENT_BUS.register(this);

        chebyshevFilter = new ChebyshevI();
        chebyshevFilter.bandPass(2, 48000, 2000, 1200, 1);
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
    }

    @SubscribeEvent
    public void onVoiceKey(VoiceKeyEvent event) {
        if(SPEAK_ON_RADIO.isKeyDown()) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onVoicePlayEvent(VoicePlayEvent event) {
        if(event.getProperties().getBooleanValue("isRadio")) {
            short[] samples = AudioUtil.bytesToShorts(event.getAudioSamples());
            for (int i = 0; i < samples.length; i++) {
                samples[i] = (short)(chebyshevFilter.filter((samples[i])) * gainRadio);
            }
            event.setAudioSamples(AudioUtil.shortsToBytes(samples));
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if(event.phase == TickEvent.Phase.START){
            if(!GuiConfig.audioTesting){
                if(GameSettings.isKeyDown(SPEAK_ON_RADIO) && mc.player != null && RadioItem.isPlayerHeldActiveRadio(mc.player)){
                    if(MicroManager.isRunning() && !MicroManager.getHandler().isSending()){
                        if(!speaking) NetworkHandler.getInstance().getNetwork().sendToServer(new PlayerSpeakingOnRadioPacket(true));
                        MicroManager.getHandler().start();
                    }
                }else {
                    if(MicroManager.isRunning() && MicroManager.getHandler().isSending()){
                        if(speaking) NetworkHandler.getInstance().getNetwork().sendToServer(new PlayerSpeakingOnRadioPacket(false));
                    }
                }
            }
        }
    }
}