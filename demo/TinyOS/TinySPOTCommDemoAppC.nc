#include <Timer.h>
#include "TinySPOTComm.h"

configuration TinySPOTCommDemoAppC
{}
implementation
{
    components MainC;
    components LedsC;
    components TinySPOTCommDemoC as App;
    components new TimerMilliC() as Timer0;
    
    components ActiveMessageC;
    components new AMSenderC(AM_SPOTCOMM);
    components new AMReceiverC(AM_SPOTCOMM);
    
    components CC2420ControlC;
    
    App.Boot -> MainC.Boot;
    App.Leds -> LedsC.Leds;
    App.Timer0 -> Timer0;
    
    App.Packet -> AMSenderC.Packet;
    App.AMPacket -> AMSenderC.AMPacket;
    App.AMSend -> AMSenderC.AMSend;
    App.AMControl -> ActiveMessageC;
    App.Receive -> AMReceiverC;
    
    App.CC2420Config -> CC2420ControlC.CC2420Config;
    App.CC2420Power -> CC2420ControlC.CC2420Power;
    App.ReadRssi -> CC2420ControlC.ReadRssi;
    App.Resource -> CC2420ControlC.Resource;
    
}