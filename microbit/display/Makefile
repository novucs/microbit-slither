# Edit this line to where your micro:bit device is mounted:
MICROBIT_PATH = /run/media/billy/MICROBIT

# Uncomment the line below if you are using the supplied Ubuntu VM.
#MICROBIT_PATH = /media/student/MICROBIT

SOURCE_FILES += \
    source/main.cpp \
    source/MessageService.cpp \
    source/MessageService.h \
    source/MoveService.cpp \
    source/MoveService.h \
    source/SlitherClient.cpp \
    source/SlitherClient.h

OUTPUT_FILE = build/bbc-microbit-classic-gcc/source/microbit-display-combined.hex

run: ${OUTPUT_FILE}
	cp ${OUTPUT_FILE} ${MICROBIT_PATH}
	cp ${OUTPUT_FILE} ${MICROBIT_PATH}1

0: ${OUTPUT_FILE}
	cp ${OUTPUT_FILE} ${MICROBIT_PATH}

1: ${OUTPUT_FILE}
	cp ${OUTPUT_FILE} ${MICROBIT_PATH}1

${OUTPUT_FILE}: ${SOURCE_FILES}
	yt build
