Der Wecker klingelt.

SWITCH Wochentag pruefen {

  CASE Samstag oder Sonntag? {
    Wecker ausschalten
    Umdrehen
    Weiterschlafen
  }

  DEFAULT Wertktag {

    WHILE solange ich noch min 5 Minuten Zeit habe {
      Snooze Taste des Weckers druecken 
      bisschen weiter doesen...
    }

    Wecker ausmachen
    Aufstehen

    IF Lust auf Kaffee? {
      Kaffe kochen
      Tasse holen

      UNTIL Keine Lust mehr auf Kaffee {
	    Tasse mit Kaffe befuellen
        Tasse Kaffee trinken
      }
    }
    ELSE {
        Weiter mit Schritt [#duschen](#duschen).
    }

    Zaehne putzen

    #duschen Duschen gehen

  }
}

