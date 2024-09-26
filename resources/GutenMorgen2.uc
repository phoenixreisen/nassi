Der Wecker klingelt.

SWITCH #WT-pruefen Wochentag pruefen [#Zaehne](#Zaehne) {

  CASE Samstag oder Sonntag? {
    Wecker ausschalten
    Umdrehen
    Weiterschlafen
  }

  DEFAULT Wertktag {

    WHILE solange ich noch min 5 Minuten Zeit habe {
      Wecker ausmachen
      bisschen weiter doesen...
    }

    Wecker ausmachen
    Aufstehen

    IF #Lust Lust auf Kaffee {
      Kaffe kochen
      Tasse holen

      UNTIL Keine Lust mehr auf Kaffee {
	    Tasse mit Kaffe befuellen
        Tasse Kaffee trinken
      }
    }

    Duschen gehen

    
    """ #Zaehne 
    Zahnpflege:
    - Zahnseide verwenden
    - Zaehne gruendlich putzen
    - Zahnzwischenraumbuerstchen benutzen
    - Mundspuelung"""
  }
}

