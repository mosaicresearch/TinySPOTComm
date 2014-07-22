#ifndef TINYSPOTCOMM_H
#define TINYSPOTCOMM_H

enum
{
    TIMER_PERIOD_MILLI = 500,
    AM_SPOTCOMM = 0x41
};

typedef nx_struct
{
    nx_uint8_t ident;
    nx_uint16_t counter;
} TinySPOTPacket;


#endif