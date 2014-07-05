pconf
=====

configuration parser

usage example 
java -jar pconf.jar pyang 
        -yang C:\Users\wenjians.AD4\Documents\GitHub\pconf\parameters 
        -yin C:\Users\wenjians.AD4\Documents\GitHub\pconf\parameters\yin
        -pdd C:\Users\wenjians.AD4\Documents\GitHub\pconf\parameters\export\pdd.xls
        -cliboard scm
        -clixml C:\Users\wenjians.AD4\Documents\GitHub\pconf\parameters\syslog\gfi-syslog.xml
        -clitree C:\Users\wenjians.AD4\Documents\GitHub\pconf\parameters\export
        -cliexport C:\Users\wenjians.AD4\Documents\GitHub\pconf\parameters\export

Linux: 
java -jar pconf.jar -pyang -yang /mnt/hgfs/pconf/parameters -yin /mnt/hgfs/pconf/parameters/yin -pdd /mnt/hgfs/pconf/parameters/export/pdd.xls


pyang -f tree /mnt/hgfs/pconf/parameters/ip/gw-ipng-filter.yang  -p /mnt/hgfs/pconf/parameters/external -p /mnt/hgfs/pconf/parameters/extensions -p /mnt/hgfs/pconf/parameters/common -p /mnt/hgfs/pconf/parameters/ip
module: gw-ipng-filter
   +--rw state?   enumeration
   +--rw rules* [name]
      +--rw name              string
      +--rw sate?             enumeration
      +--rw action?           enumeration
      +--rw protocol?         enumeration
      +--rw application?      enumeration
      +--rw type?             enumeration
      +--rw group-id?         string
      +--rw position?         string
      +--rw direction?        enumeration
      +--rw local-address?    string
      +--rw remote-address?   string
      +--rw local-port?       string
      +--rw behavior?         enumeration
      +--rw log?              enumeration
diag@ubuntu:/mnt/hgfs/pconf$ 

pyang -f tree /mnt/hgfs/pconf/parameters/realm/gw-ip-realm.yang  -p /mnt/hgfs/pconf/parameters/external -p /mnt/hgfs/pconf/parameters/extensions -p /mnt/hgfs/pconf/parameters/common -p /mnt/hgfs/pconf/parameters/realm
module: gw-ip-realm
   +--ro version?          uint32
   +--rw realm-table* [name]
   |  +--rw name              h248:iprealm-name
   |  +--rw ip-if* [ip]
   |  |  +--rw ip      gwip:ip-address
   |  |  +--rw vlan?   gwip:vlan-id
   |  +--rw type?             enumeration
   |  +--rw media-profile?    string
   |  +--rw admin-state?      admin-state
   |  +--rw oper-down?        enumeration
   |  +--ro oper-state?       string
   |  +--rw service-change?   enumeration
   |  +--rw vmg-id*           h248:vmg-id
   |  +--rw cp-monitoring
   |     +--rw ip-address?      gwip:ip-address
   |     +--rw admin-state?     admin-state
   |     +--rw depend-port?     gwlpa:eth-lpa
   |     +--ro oper-state?      string
   |     +--ro suspend-state?   string
   +--rw cp-rx-interval?   uint32
   +--rw cp-tx-interval?   uint32
   +--rw cp-delay?         uint32
diag@ubuntu:/mnt/hgfs/pconf$ 