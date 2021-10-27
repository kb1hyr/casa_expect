# casa_expect

This software is pretty cable company specific.  It goes through provisioned modems, logs into a CMTS and queries it to find the make and model of each cable modem, which is then stored in a TAB-delimited file for easy importation into a spreadsheet program.

The provisioning system for cable modems is based on ISC DHCP, and all cable modems are provisioned by including their MAC address in a file that corresponds to their provisioned speed and other characteristics. An entry for an indivividual modem in this file looks like this:
    host comB81619F9F299 {
        hardware ethernet B8:16:19:F9:F2:99;
        }

This software reads a copy of those files, separates out the MAC addresses, and stores them in an array.  Then it logs into the CMTS to get the information it needs.

CMTS stands for Cable Modem Termination System.  It is a specialized switch/router combo that interfaces cable modems to a network.  When properly configured, cable modems give a lot of info to the CMTS, and this information can be queried in a variety of ways.  For what is needed here, the query is:
    show cable modem <MAC Address> verbose | include sysDescr 

This software uses telnet because the access into the CMTS is on a small (5 devices) separate and secure network.  It should be obvious that if this is being done across anything else, ssh should be used instead.  


