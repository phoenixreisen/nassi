Der Wecker klingelt.
Der Tag faengt an

SWITCH checkDay: Wochentag pruefen {

  CASE Samstag oder Sonntag? {
    Wecker ausschalten
    Umdrehen
    Weiterschlafen
  }

  DEFAULT Wertktag {

    WHILE solange ich noch min 5 Minuten Zeit habe {
      Wecker ausmachen
      kleines bisschen weiter doesen...
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


    """Zahnpflege:
    - Zahnseide verwenden
    - Zaehne gruendlich putzen
    - Zahnzwischenraumbuerstchen benutzen
    - Mundspuelung"""
  }
}
