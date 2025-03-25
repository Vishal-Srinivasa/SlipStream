#include "controller.h"
#include <iostream>

int main() {
    try {
        
        crow::SimpleApp app;
        std::cout << "Creating controller..." << std::endl;
        // SlipStreamController controller(app);
        
        std::cout << "Starting web server on port 8080..." << std::endl;
        app.port(8080).multithreaded().run();
        
        return 0;
    } catch (const std::exception& e) {
        std::cerr << "Fatal error: " << e.what() << std::endl;
        return 1;
    }
}

