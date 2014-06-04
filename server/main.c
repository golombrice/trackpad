/*
The server for 'Mouse for Linux'.

Receives UDP packets from the client and processes the simple commands 
in order to operate on the mouse or keyboard on top of X11.
Uses xdotool to implement the mouse/keyboard operation. 
See COPYRIGHT_xdo and http://www.semicomplete.com/projects/xdotool/ for details.

04.06.2014
Petri Helin
http://www.cs.tut.fi/~helinp/android-trackpad.shtml
*/
#include<stdio.h> 
#include<string.h>
#include<stdlib.h> 
#include<arpa/inet.h>
#include<sys/socket.h>
#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include "xdo.h"

#define BUFLEN 100  //Max length of buffer

void process_command(char* pch, xdo_t* xdo);

int main(int argc, char** argv)
{

   // parse input arguments
   int verbose = 0;
   int port = 32000;
   int c = 0;

   while ((c = getopt (argc, argv, "p:v")) != -1) {
      switch (c)
      {
         case 'v':
            verbose = 1;
            break;
         case 'p':
            port = atoi(optarg);
            break;
         case '?':
            if (optopt == 'p')
               fprintf (stderr, "Option -%c requires an argument.\n", optopt);
            else
               fprintf (stderr,
                     "Unknown option character `\\x%x'.\n",
                     optopt);
            return 1;
         default:
            break;
      }
   }

   // create a UDP socket
   struct sockaddr_in si_me;
   struct sockaddr_in si_other;

   int s = 0; 
   socklen_t slen = sizeof(si_other);
   int recv_len = 0;
   char buf[BUFLEN];

   if ((s=socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP)) == -1)
   {
      perror("Socket connection failed.");
      return 1;
   }

   memset((char *) &si_me, 0, sizeof(si_me));

   si_me.sin_family = AF_INET;
   si_me.sin_port = htons(port);
   si_me.sin_addr.s_addr = htonl(INADDR_ANY);

   // bind socket to port
   if( bind(s, (struct sockaddr*)&si_me, sizeof(si_me) ) == -1)
   {
      perror("Binding to port failed.");
      return 1;
   }

   // create a object for controlling x events
   xdo_t* xdo = xdo_new(NULL);

   //keep listening for data
   while(1)
   {
      if( verbose == 1 ) {
         printf("Waiting for data...");
      }

      //try to receive some data, this is a blocking call
      if ((recv_len = recvfrom(s, buf, BUFLEN, 0, (struct sockaddr *) &si_other, &slen)) == -1)
      {
         perror("Receive from client failed.");
         return 1;
      }

      if( verbose == 1 ) {
         printf("Received packet from %s:%d\n", inet_ntoa(si_other.sin_addr), ntohs(si_other.sin_port));
         printf("Data: %s\n" , buf);
      }

      char* pch = strtok(buf," ");

      process_command(pch, xdo);

      memset(buf, 0, BUFLEN);
   }

   close(s);
   return 0;
}

void process_command(char* pch, xdo_t* xdo) {
   if(strcmp(pch, "click") == 0) {
      xdo_click(xdo, CURRENTWINDOW, 1);
   }
   if(strcmp(pch, "clicksec") == 0) {
      xdo_click(xdo, CURRENTWINDOW, 3);
   }
   if(strcmp(pch, "scroll") == 0) {
      pch = strtok(NULL, " ");
      int y = atoi(pch);
      int i = 0;
      if( y > 0 ) {
         for(i = 0; i < y; i=i+5 ) {
            xdo_click(xdo, CURRENTWINDOW, 4);
         }
      } else {
         for(i = 0; i < -y; i=i+5 ) {
            xdo_click(xdo, CURRENTWINDOW, 5);
         }
      }
   }
   if(strcmp(pch, "buttondown") == 0 ) {
      xdo_mousedown(xdo, CURRENTWINDOW, 1);
   }
   if(strcmp(pch, "buttonup") == 0 ) {
      xdo_mouseup(xdo, CURRENTWINDOW, 1);
   }
   if(strcmp(pch, "bdmove") == 0 ) {
      xdo_mousedown(xdo, CURRENTWINDOW, 1);
      pch = strtok(NULL, " ");
      int x = atoi(pch);
      pch = strtok(NULL, " ");
      int y = atoi(pch);
      xdo_mousemove_relative(xdo, -x, -y);
      xdo_mouseup(xdo, CURRENTWINDOW, 1);
   }
   if(strcmp(pch, "move") == 0 ) {
      pch = strtok(NULL, " ");
      int x = atoi(pch);
      pch = strtok(NULL, " ");
      int y = atoi(pch);
      xdo_mousemove_relative(xdo, -x, -y);
   }
   if(strcmp(pch, "text") == 0 ) {
      if( pch != NULL ) {
         pch = strtok(NULL, " ");
         xdo_type(xdo, CURRENTWINDOW, pch, 12000);
         xdo_keysequence(xdo, CURRENTWINDOW, "Return", 12000);
      }
   }
   if(strcmp(pch, "char") == 0 ) {
      if( pch != NULL ) {
         pch = pch+5;
         if( pch[0] == 8) {
            xdo_keysequence(xdo, CURRENTWINDOW, "BackSpace", 12000);
         }
         if( pch[0] == 10) {
            xdo_keysequence(xdo, CURRENTWINDOW, "Return", 12000);
         }
         if( pch[0] > 10 ) {
            xdo_type(xdo, CURRENTWINDOW, pch, 12000);
         }

      }
   }
}
