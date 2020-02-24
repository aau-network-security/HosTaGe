arm: CC = arm-linux-androideabi-gcc
x86: CC = i686-linux-android-gcc
mips: CC = mipsel-linux-android-gcc
CFLAGS = -Wall -g
LDFLAGS = -llog
SRC = bind.c
OBJ = $(SRC:.c=.o)
EXE = bind

arm x86 mips: $(SRC) $(EXE)

$(EXE): $(OBJ)
	$(CC) -o $@ $^ $(LDFLAGS)

%.o: %.c
	$(CC) -o $@ -c $< $(CFLAGS)

clean:
	rm -f *.o $(EXE)

install:
	adb push bind /data/local