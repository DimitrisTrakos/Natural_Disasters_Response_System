# Natural Disaster Response System 🌲🔥 

This project simulates the response to natural disasters, specifically the spread of fire in a forested area. The program uses a **multi-agent** system (simulated using Jade and Java) to model the spread of fire, the layout of the forest, and various utils functions to manage forest and fire behavior.

---

## Table of Contents

- [Project Overview](#project-overview)
- [Folder Structure](#folder-structure)
- [How to Run](#how-to-run)

---

## Project Overview

This project simulates the spread of fire in a forest grid, considering forest clusters and random fire ignition points. It aims to demonstrate how fire can spread over time in a random forest environment.

The program:

- **Generates a forest** with trees placed randomly or in clusters.
- **Simulates fire** that spreads over the forest grid, affecting neighboring forest cells.
- Uses utils classes to manage the forest grid and fire behavior.

---

## Folder Structure

```bash
src/
├── main/
│   └── Main.java            # Main simulation logic
├── mapGrid/
│   ├── MapGrid.java         # MapGrid class handling grid layout
│   └── GridCell.java        # GridCell class that represents individual cells on the map
└── utils/
    ├── FireUtils.java       # Util class for fire-related methods
    └── ForestUtils.java     # Util class for forest-related methods
```

---

## How to Run

1. **Prerequisites**: Ensure you have [Java 8+](https://www.oracle.com/java/technologies/javase-jdk8-downloads.html) installed on your machine.

2. **Clone the repository**:
   ```bash
   git clone git@github.com:DimitrisTrakos/Natural_Disasters_Response_System.git
   cd Natural_Disasters_Response_System
   ```
3. **Execute/Run**:
    ```bash
    javac src/main/Main.java src/mapGrid/*.java src/utils/*.java -d out/
    java -cp out/ main.Main
    ```
    Or just run it via Run button on your IDE  😊 😊 🚀 

