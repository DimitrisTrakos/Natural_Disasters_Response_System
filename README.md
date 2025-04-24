Natural Disaster Simulation - Fire Spread and Forest Management

This project simulates the response to natural disasters, specifically the spread of fire in a forested area. The program uses a **multi-agent** system (simulated using Jade and Java to model the spread of fire, the layout of the forest, and various helper functions to manage forest and fire behavior.

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

src/
├── main/
│   └── Main.java                # Main simulation logic
├── mapGrid/
│   ├── MapGrid.java             # MapGrid class handling grid layout
│   └── GridCell.java            # GridCell class that represents individual cells on the map
└── utils/
├── FireUtils.java           # Util class for fire-related methods
└── ForestUtils.java         # Util class for forest-related methods
