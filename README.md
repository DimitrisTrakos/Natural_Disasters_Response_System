# Natural Disaster Response System 🌲🔥🚁🚒🏠

This project simulates the response to natural disasters, specifically the spread of fire in a forested area. The program uses a **multi-agent** system (simulated using Jade and Java) to model the spread of fire, the layout of the forest, and various utils functions to manage forest and fire behavior.

---

## Table of Contents

- [System Overview](#system-overview)
- [Agents](#agents)
  - [Drone Agent](#drone-agent-)
  - [Firefighter Agent](#firefighter-agent-)
  - [Homeowner Agent](#homeowner-agent-)
  - [DataCenter Agent](#datacenter-agent-)
- [Folder Structure](#folder-structure)
- [How to Run](#how-to-run)

---

## System Overview

This simulation models an intelligent wildfire response system featuring:

- Dynamic fire propagation algorithms
- Priority-based emergency response
- A* pathfinding for optimal navigation
- Real-time monitoring and reporting
- Centralized command and control

## Agents

### Drone Agent 🚁

**Role:** 
Aerial Surveillance Unit

**Behaviors:**
- Systematic grid scanning pattern
- Fire detection in 3x3 vision radius
- Duplicate report prevention
- Provides real-time fire detection updates
- Reports fire locations and types (house/forest) to DataCenter

### Firefighter Agent 🚒

**Role:** 
Fire Suppression Unit

**Behaviors:**
- Priority-based target acquisition
- A* pathfinding navigation
- Fire extinguishing protocol
- Base return when mission complete
- Pauses after extinguishing before next assignment
- Receives target coordinates from DataCenter

### Homeowner Agent 🏠

**Role:** 
Residential Monitor

**Behaviors:**
- 24/7 property monitoring
- Immediate fire alerts (1x1 area)
- Critical incident reporting
- Emergency priority signaling

### DataCenter Agent 🖥️

**Role:** 
Command Center

**Behaviors:**
- Fire priority triage:
  1. House fires 🔥🏠 (Critical)
  2. Near-house fires 🔥 (High)
  3. Forest fires 🌲 (Standard)
- Duplicate report handling
- Resource allocation
- Mission status monitoring
- Receives and processes reports from all agents
- Removes extinguished fires from tracking
- Directs firefighter to return to base when fires are cleared

---

## Folder Structure

```bash
src/
├── agents/
│   ├── DataCenterAgent.java    # Central coordination
│   ├── DroneAgent.java         # Aerial surveillance
│   ├── FirefighterAgent.java   # Fire suppression
│   └── HomeOwnerAgent.java     # Residential monitoring
├── main/
│   └── Main.java               # Simulation entry point
├── mapGrid/
│   ├── MapGrid.java            # Grid management
│   └── GridCell.java           # Individual cell representation
└── utils/
    ├── FireUtils.java          # Fire behavior utilities
    ├── ForestUtils.java        # Forest generation utilities
    └── AStarPathfinder.java    # Pathfinding algorithm
    └── HouseUtils.java         # House generation utilities
```

---

## How to Run

1. **Prerequisites**: Ensure you have:
   [Java 8+](https://www.oracle.com/java/technologies/javase-jdk8-downloads.html)
   [JADE](https://img.shields.io/badge/Framework-JADE-green)
2. **Clone the repository**:
   ```bash
   git clone git@github.com:DimitrisTrakos/Natural_Disasters_Response_System.git
   cd Natural_Disasters_Response_System
   ```
3. **Execute/Run**:
   ```bash
   chmod +x compile_run.sh
   ./compile_run
   ```
   Or just run it via Run button on your IDE  😊 😊 🚀
