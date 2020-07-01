# FTP-SSH-JAVA

This program acts as a server/client network program to establish a persistent FTP connection over TCP with a simplifed SSH encryption where both parties have keys.\
The program is implemented in Java 8.

notes:
1- The program can work as the client or the server\
2- The client can send files or request files from the server\
2- Files names can be relative or absolute\
2- Maximum file name length is 255 characters and only English names are supported\
3- The server can handle multiple simultaneous connections using threading, each associated with a unique ID\
4- Error handling has been implemented (wrong values, input mismatch, non-existing files, abruptly closing the session...)\
5- Before sending a file, the program will encrypt it using AES algorithem with 256 key and secure RNG for IV


Packet types:\
PUT packet:\
\[1b:type,1b:pathSize,nb:pathName,4b:fileSize,16b:IV,{mb:file}]\
GET packet:\
\[1b:type,1b:pathSize,nb:pathName]\
GET found reply:\
\[1b:response,4b:fileSize,16b:IV,{mb:file}]\
GET not found reply\
\[1b:response]

Packet types headers:\
0x00: PUT\
0x01: GET

Response header:\
0x00: File doesn't exist on server\
0x01: File exist on server


Mahmoud Habboush
