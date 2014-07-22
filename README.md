TinySPOTComm
============

TinySPOTComm consists of a number of tweaks to the 'blue' release of the Sun SPOT SDK
to allow for basic interoperability with TinyOS Motes. This README file describes how to setup
the TinySPOTComm library. For more information about the TinySPOTComm project we refer to the following 
paper:

Van Den Akker, D., Smolderen, K., De Cleyn, P., Braem, B., & Blondia, C. (2010). TinySPOTComm: Facilitating 
communication over IEEE 802.15. 4 between Sun SPOTs and TinyOS-based motes. In Sensor Applications, 
Experimentation, and Logistics (pp. 177-194). Springer Berlin Heidelberg.
http://dx.doi.org/10.1007/978-3-642-11870-8_12

Installation
============

The installation of TinySPOTComm is very straightforward:

1) Make sure that you have an integral backup of your SunSPOT distributing. During the installation of TinySPOTComm
some libraries of the SunSPOT SDK will be altered

2) Checkout the TinySPOTComm repository from GitHub
    https://github.com/mosaicresearch/TinySPOTComm.git

3) Run 'ant prepare' from the directory where you downloaded TinySPOTComm. The ant script will now
   update your SunSPOT SDK files and create a new library.
   
4) Run 'ant flashlibrary' for all your SunSPOTs which need to use TinySPOTComm

Demo
=====

Sample code for the TinySPOTComm project can be found in the 'demo' directory.
The 'demo' uses one SunSPOT as 'server' node and one or two SunSPOTs/TinyOS-motes as 'client nodes'.
Code for both the server and client are provided for the SunSPOT platform. For the TinyOS platform, only the client code is provided

In order to run the demo you must first deploy the server code onto a SunSPOT and the client code onto at least one SunSPOT or TinyOS-mote.
After the code has been deployed and the nodes have been rebooted, the client nodes broadcast packets containing an incrementing counter.
When these packets are received by the server the counter contained in the packet is displayed in (4bits) binary using the leds of the SunSPOT.
By pressing the switch under the leds indicating the counter of a specific node, the server can be instructed to start or stop unicasting packets 
to the node in question. When the server is unicasting packets to a specific node, you should see a binary counter on the client node.


Final notes
===========

- Please note that TinySPOTComm does NOT provide muli-hop support for communication with TinyOS nodes.

- This version of TinySPOTComm only supports the BLUE release of the SunSPOT SDK
