package com.devikapps.vaikaparts.model.classifier;

import lombok.Getter;

@Getter
public enum PartCategory {
  // Engine & Performance
  ENGINE_PART("Pièce moteur"),
  TURBO_COMPRESSOR("Turbo/Compresseur"),
  COOLING_SYSTEM("Système de refroidissement"),
  FUEL_SYSTEM("Système d'alimentation"),
  AIR_FILTER("Filtre à air"),
  OIL_FILTER("Filtre à huile"),
  EXHAUST_PIPE("Échappement"),
  CATALYTIC_CONVERTER("Pot catalytique"),

  // Transmission & Drive
  CLUTCH_GEARBOX("Embrayage/Boîte de vitesses"),
  TRANSMISSION("Transmission"),
  DRIVESHAFT("Arbre de transmission"),
  DIFFERENTIAL("Différentiel"),

  // Electrical & Electronics
  STARTER("Démarreur"),
  ALTERNATOR("Alternateur"),
  BATTERY("Batterie"),
  IGNITION_SYSTEM("Système d'allumage"),
  SPARK_PLUGS("Bougies d'allumage"),
  WIRING_HARNESS("Faisceau électrique"),
  FUSES_RELAYS("Fusibles/Relais"),
  SENSORS_AND_PROBES("Capteurs et sondes"),
  ECU_COMPUTER("Calculateur/ECU"),

  // Suspension & Steering
  DIRECTION_SUSPENSION("Direction/Suspension"),
  SHOCK_ABSORBERS("Amortisseurs"),
  SPRINGS("Ressorts"),
  CONTROL_ARMS("Bras de suspension"),
  BALL_JOINTS("Rotules"),
  STEERING_RACK("Crémaillère de direction"),
  POWER_STEERING_PUMP("Pompe de direction assistée"),

  // Braking
  BRAKING("Freinage"),
  BRAKE_PADS("Plaquettes de frein"),
  BRAKE_DISCS("Disques de frein"),
  BRAKE_DRUMS("Tambours de frein"),
  BRAKE_CALIPERS("Étriers de frein"),
  MASTER_CYLINDER("Maître-cylindre"),
  ABS_SYSTEM("Système ABS"),

  // Wheels & Tires
  TIRE("Pneu"),
  WHEELS_RIMS("Jantes"),
  WHEEL_BEARINGS("Roulements de roue"),

  // Lighting
  HEADLIGHTS("Phares avant"),
  TAIL_LIGHTS("Feux arrière"),
  FOG_LIGHTS("Antibrouillards"),
  INDICATORS("Clignotants"),
  INTERIOR_LIGHTS("Éclairage intérieur"),

  // Body & Exterior
  BODY("Carrosserie"),
  BUMPERS("Pare-chocs"),
  DOORS("Portes"),
  FENDERS("Ailes"),
  HOOD_BONNET("Capot"),
  TRUNK_TAILGATE("Coffre/Hayon"),
  MIRRORS("Rétroviseurs"),
  WINDSHIELD_WINDOWS("Pare-brise/Vitres"),
  WIPERS("Essuie-glaces"),
  GRILLE("Calandre"),

  // Interior
  PASSENGER_COMPARTMENT("Habitacle"),
  SEATS("Sièges"),
  DASHBOARD("Tableau de bord"),
  STEERING_WHEEL("Volant"),
  PEDALS("Pédales"),
  CARPETS_MATS("Tapis/Moquettes"),

  // Climate Control
  AC_HEATER("Climatisation/Chauffage"),
  RADIATOR("Radiateur"),
  THERMOSTAT("Thermostat"),
  COOLING_FAN("Ventilateur de refroidissement"),
  AC_COMPRESSOR("Compresseur de climatisation"),

  // Fluids & Maintenance
  ENGINE_OIL("Huile moteur"),
  BRAKE_FLUID("Liquide de frein"),
  COOLANT("Liquide de refroidissement"),
  TRANSMISSION_FLUID("Huile de transmission"),

  // Distribution & Timing
  DISTRIBUTION("Distribution"),
  TIMING_BELT("Courroie de distribution"),
  TIMING_CHAIN("Chaîne de distribution"),

  // Other
  ACCESSORIES("Accessoires"),
  UNIVERSAL_PARTS("Pièces universelles"),
  OTHER("Autre");

  private final String frenchName;

  PartCategory(String frenchName) {
    this.frenchName = frenchName;
  }
}
