Das System ermittelt die gewuenschten PrintJobs.

EXCEPTION 1: Nicht alle PrintJobs gehoeren zur gleichen Buchung.
EXCEPTION 2: Nicht alle PrintJobs befinden sich im erwarteten Status.
EXCEPTION 3: Nicht alle PrintJobs haben die gleiche Anzahl an Mappen.

IF PrintJobs sollen sortiert werden {
  """
  Die PrintJobs in folgender Reihenfolge sortieren:
  1. Reiseplaene              (Dokument-Typ 4)
  2. Einschiffungshinweise    (Dokument-Typ 2)
  3. Allgemeine Informationen (Dokument-Typ 3)
  4. Ausflugsprogramme        (Dokument-Typ 5)
  """
}

"""
Die Liste der zu druckenden Dateien ermitteln: 

- Zuerst kommen die Deckblaetter bzw. Anschreiben der PrintJobs.
- Anschliessend werden die generierten Dokumente, sowie die Anlagen der
  PrintJobs so sortiert, dass sie nach dem Ausdruck in die Mappen gepackt
  werden koennen: 
"""


kommen alle Dokumente fuer die erste Mappe


FOR Das System ermittelt fuer jede Mappe, die zu druckenden Dokumente {
  FOR Das System ermittelt die generierten Dokumente jedes PrintJobs {
    IF Teilnehmerspezifischer PrintJob {
        
    }
    ELSE {

    }
  }
} 

Zu diesem Zweck muessen die D
Die zu druckenden Dokumente aller PrintJobs in die richtige Reihenfolge bringen.  
    1.  Zuerst kommen die Deckblaetter bzw. der Anschreiben der PrintJobs
    2.  Jetzt kommen alle Dokumente fuer die erste Mappe (d.h. beginnend mit
        dem ersten PrintJob der Sortierung: 

        2.a PrintJob ist teilnehmerspeziefisch

        zuerst alle generierten Dokumente des
        ersten PrintJobs danach alle 

Pruefen, ob alle zu druckenden Dateien vorhanden sind;
Das "Gedruckt-Am", sowie den "Status" jedes PrintJobs aktualisieren.
Eintrag in der Protokoll-Tabelle erzeugen.
