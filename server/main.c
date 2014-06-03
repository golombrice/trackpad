#include<stdio.h> //printf
#include<string.h> //memset
#include<stdlib.h> //exit(0);
#include<arpa/inet.h>
#include<sys/socket.h>
#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include "xdo.h"
 
#define BUFLEN 100  //Max length of buffer
#define PORT 32000 //The port on which to listen for incoming data

// void movepointer(int x, int y)
// {
//    Display *display;
//    Window window;
//    XWindowAttributes w_attr, r_attr;
//    char *w_str;
// 
//    display = XOpenDisplay("");
//    w_str = (char *)getenv("WINDOWID");
//    window = atoi(w_str);
// 
//    XGetWindowAttributes(display, window, &w_attr);
//    XGetWindowAttributes(display, w_attr.root, &r_attr);
// 
//    Window root_return;
//    Window child_return;
//    int root_x_return;
//    int root_y_return;
//    int win_x_return;
//    int win_y_return;
//    unsigned int mask_return;
// 
//    XQueryPointer(display, window, &root_return, &child_return, &root_x_return, &root_y_return, 
//                               &win_x_return, &win_y_return, &mask_return);
//    printf("%d %d\n", root_x_return, root_y_return);
// 
//    XWarpPointer(display, None, w_attr.root, 0, 0, 0, 0,
//          root_x_return-x, root_y_return-y);
// 
//  //  XWarpPointer(display, None, w_attr.root, 0, 0, 0, 0,
//  //        w_attr.x + (w_attr.width / 2), 
//  //        w_attr.y + (w_attr.height / 2));
// 
//    XFlush(display);
//    XCloseDisplay(display);
// }
// 
// // Simulate mouse click
//    void
// click (Display *display, int button)
// {
//    // Create and setting up the event
//    XEvent event;
//    memset (&event, 0, sizeof (event));
//    event.xbutton.button = button;
//    event.xbutton.same_screen = True;
//    event.xbutton.subwindow = DefaultRootWindow (display);
//    while (event.xbutton.subwindow)
//    {
//       event.xbutton.window = event.xbutton.subwindow;
//       XQueryPointer (display, event.xbutton.window,
//             &event.xbutton.root, &event.xbutton.subwindow,
//             &event.xbutton.x_root, &event.xbutton.y_root,
//             &event.xbutton.x, &event.xbutton.y,
//             &event.xbutton.state);
//    }
//    // Press
//    event.type = ButtonPress;
//    if (XSendEvent (display, PointerWindow, True, ButtonPressMask, &event) == 0)
//       fprintf (stderr, "Error to send the event!\n");
//    XFlush (display);
//    usleep (1);
//    // Release
//    event.type = ButtonRelease;
//    if (XSendEvent (display, PointerWindow, True, ButtonReleaseMask, &event) == 0)
//       fprintf (stderr, "Error to send the event!\n");
//    XFlush (display);
//    usleep (1);
// }
//  
void die(char *s)
{
   perror(s);
   exit(1);
}
 
int main(void)
{
    struct sockaddr_in si_me;
    struct sockaddr_in si_other;
     
    int s = 0; 
    socklen_t slen = sizeof(si_other);
    int recv_len = 0;
    char buf[BUFLEN];
     
    //create a UDP socket
    if ((s=socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP)) == -1)
    {
        die("socket");
    }
     
    // zero out the structure
    memset((char *) &si_me, 0, sizeof(si_me));
     
    si_me.sin_family = AF_INET;
    si_me.sin_port = htons(PORT);
    si_me.sin_addr.s_addr = htonl(INADDR_ANY);
     
    //bind socket to port
    if( bind(s , (struct sockaddr*)&si_me, sizeof(si_me) ) == -1)
    {
        die("bind");
    }

    xdo_t* xdo = xdo_new(NULL);
     
    //keep listening for data
    while(1)
    {
        printf("Waiting for data...");
        fflush(stdout);
         
        //try to receive some data, this is a blocking call
        if ((recv_len = recvfrom(s, buf, BUFLEN, 0, (struct sockaddr *) &si_other, &slen)) == -1)
        {
            die("recvfrom()");
        }
         
        //print details of the client/peer and the data received
        printf("Received packet from %s:%d\n", inet_ntoa(si_other.sin_addr), ntohs(si_other.sin_port));
        printf("Data: %s\n" , buf);

        char* pch = strtok(buf," ");

        if(strcmp(pch, "click") == 0) {
           xdo_click(xdo, CURRENTWINDOW, 1);
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
              //xdo_(xdo, -x, -y);
              xdo_type(xdo, CURRENTWINDOW, pch, 12000);
              xdo_keysequence(xdo, CURRENTWINDOW, "Return", 12000);
           }
        }

        memset(buf, 0, BUFLEN);
    }
 
    close(s);
    return 0;
}
