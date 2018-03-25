cmake_minimum_required(VERSION 3.10)
project(display)

set(CMAKE_CXX_STANDARD 11)

file(GLOB_RECURSE sources
        display/source/*.h
        display/source/*.cpp)

file(GLOB_RECURSE project
        .gitignore
        README.md
        display/.gitignore
        display/.yotta.json
        display/source/config.json
        display/LICENSE
        display/Makefile
        display/config.json
        display/module.json)

include_directories(display/yotta_modules/microbit/inc
        display/yotta_modules/microbit-dal/inc/bluetooth
        display/yotta_modules/microbit-dal/inc/core
        display/yotta_modules/microbit-dal/inc/drivers
        display/yotta_modules/microbit-dal/inc/platform
        display/yotta_modules/microbit-dal/inc/types
        display/yotta_modules/ble
        display/yotta_modules/mbed-classic/api)

add_executable(display ${sources} display/source/SlitherClient.cpp display/source/SlitherClient.h)
add_executable(dummy ${project})
