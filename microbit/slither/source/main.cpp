#include "SlitherClient.h"

/**
 * The main program entry point.
 */
int main() {
    // Create and run the slither game client.
    auto *client = new slither::SlitherClient();
    client->run();

    // Exit the fiber.
    release_fiber();
}
