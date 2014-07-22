#include <Timer.h>
#include "TinySPOTComm.h"

#define BROADCAST_IDENTIFIER 0x01
#define UNICAST_IDENTIFIER 0x02

module TinySPOTCommDemoC
{
    uses interface Boot;
    uses interface Leds;
    uses interface Timer<TMilli> as Timer0;
    
    uses interface Packet;
    uses interface AMPacket;
    uses interface AMSend;
    uses interface SplitControl as AMControl;
    uses interface Receive;
    
    uses interface CC2420Config;
    uses interface CC2420Power;
    uses interface Read<uint16_t> as ReadRssi;
    uses interface Resource;
}
implementation
{
    uint16_t counter = 0;
    am_addr_t destination = 0;
    bool busy = FALSE;
    message_t pkt;
    
    event void Boot.booted()
    {
	call CC2420Config.setChannel(26);
	call CC2420Config.setPanAddr(3);
	call AMControl.start();
    }
    event void AMControl.startDone(error_t err)
    {
	if (err != SUCCESS)
	    call AMControl.start();
    }
    event void AMControl.stopDone(error_t){}
    
    event void Timer0.fired()
    {
	counter++;
	if(!busy)
	{
	    TinySPOTPacket* btr_pkt = (TinySPOTPacket*)(call Packet.getPayload(&pkt, 0 ));
	    btr_pkt->ident = UNICAST_IDENTIFIER;
	    btr_pkt->counter = counter;
	    if (call AMSend.send(destination,&pkt,sizeof(TinySPOTPacket)) == SUCCESS)
	    {
		busy = TRUE;
	    }
	}
    }
    event void AMSend.sendDone(message_t* msg, error_t error)
    {
        if (&pkt == msg)
	{
	    busy = FALSE;
	}
    }
    
    event message_t* Receive.receive(message_t* msg, void* payload, uint8_t len)
    {
	TinySPOTPacket* btr_pkt = (TinySPOTPacket*)payload;
	//received broadcast packet and no destination known
	if (btr_pkt->ident == BROADCAST_IDENTIFIER && destination == 0)
	{
	    destination = call AMPacket.source(msg);
	    call Timer0.startPeriodic(TIMER_PERIOD_MILLI);
	    
	}
	//UGLY hack: in broadcast packets the counter field actually doesn't exist
	//so we make sure we only read it from genuine unicast packets
	else if (btr_pkt->ident == UNICAST_IDENTIFIER)
	{
	    call Leds.set(btr_pkt->counter);
	}

	return msg;
    }
    //CC2420Stuff
    event void CC2420Config.syncDone(error_t err) {}
    async event void CC2420Power.startOscillatorDone() {}
    async event void CC2420Power.startVRegDone() {}
    event void ReadRssi.readDone(error_t result, uint16_t val) {}
    event void Resource.granted() {}
}