package PacketListeners;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;


// logger class to make sure that packets are being sent properly.
public class PacketEventsListener implements PacketListener {
    @Override
    public void onPacketSend (PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.ENTITY_ANIMATION) {
          //  FluixonBossPlugin.print("Punch Animation Sent to Client.");
        }
        if (event.getPacketType() == PacketType.Play.Server.ENTITY_HEAD_LOOK) {
            //FluixonBossPlugin.print("Look Animation Sent to Client");
        }
    }
}
