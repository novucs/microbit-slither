cmake_minimum_required(VERSION 3.10)
project(slither)

set(CMAKE_CXX_STANDARD 11)

file(GLOB_RECURSE sources
        slither/source/*.h
        slither/source/*.cpp)

file(GLOB_RECURSE project
        .gitignore
        README.md
        slither/.gitignore
        slither/.yotta.json
        slither/source/config.json
        slither/LICENSE
        slither/Makefile
        slither/config.json
        slither/module.json)

include_directories(slither/yotta_modules/microbit/inc
        slither/yotta_modules/microbit-dal/inc/bluetooth
        slither/yotta_modules/microbit-dal/inc/core
        slither/yotta_modules/microbit-dal/inc/drivers
        slither/yotta_modules/microbit-dal/inc/platform
        slither/yotta_modules/microbit-dal/inc/types
        slither/yotta_modules/ble
        slither/yotta_modules/mbed-classic/api)

add_executable(slither ${sources})
add_executable(dummy ${project})
