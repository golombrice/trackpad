OBJS = xdo.o main.o 
CC = gcc
CFLAGS = -Wall -c -lX11
LFLAGS = -Wall -lX11 -lXtst -lXinerama

server : $(OBJS)
	$(CC) $(LFLAGS) $(OBJS) -o server

xdo.o : xdo.h xdo.c xdo_util.h xdo_version.h
	$(CC) $(CFLAGS) xdo.c

main.o: main.c
	$(CC) $(CFLAGS) main.c

clean:
	\rm *.o server
