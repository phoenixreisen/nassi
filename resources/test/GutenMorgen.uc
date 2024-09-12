Der Wecker klingelt.

SWITCH Wochentag pruefen {

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

    IF Lust auf Kaffee {
      Kaffe kochen
      Tasse holen

      UNTIL Keine Lust mehr auf Kaffee {
	    Tasse mit Kaffe befuellen
        Tasse Kaffee trinken
      }
    }

    Duschen gehen

    """Zahnpflege:
    - Zahnseide verwenden
    - Zaehne gruendlich putzen
    - Zahnzwischenraumbuerstchen benutzen
    - Mundspuelung"""
  }
}

