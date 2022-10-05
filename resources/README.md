# Cloud Computing II

---
## 1) Einleitung

- Um das Programm auszuführen, wird ein Java Runtime Environment der Version 17 oder höher benötigt.
- Das Programm läuft unter Java, sollte daher *theoretisch* auf jedem System laufen. <br>
Entwickelt wurde es jedoch unter Linux, daher wird die Funktionalität nur dafür garantiert.
- Es wird S3 und EC2 von Amazon verwendet.<br>
Auch wenn S3 nicht unbedingt nötig ist (siehe [scp](https://de.wikipedia.org/wiki/Secure_Copy)), wird es hier aus Übungsgründen trotzdem verwendet.

---
## 2) Programm ausführen
Um das Programm auszuführen, wird folgender Befehl benutzt:<br>
`java -jar cloudcomputing-0.1.0.jar ./resources/programm.sh`

### Beschreibung der Argumente:
1. `java` = Der Befehl, um Java-Programme auszuführen
2. `-jar` = Spezifiert den Dateityp des auszuführenden Programms als `.jar`-Datei
3. `cloudcomputing-0.1.0.jar` = Angabe der Programmdatei
4. `.resources/programm.sh` = Der Pfad zu dem Simulationsprogramm, welches in AWS ausgeführt werden soll. Dort muss der Nutzer sein Programm angeben

### 3) Aufgetretene Probleme

1. **Problem:** <br>
Java SDK an sich sehr gut, jedoch nur um Ressourcen zu verwalten, zu erstellen oder zu löschen. Eine einfache, direkte Verbindung ist nicht verfügbar.<br>
**Lösung:** <br>
Verwendung von SSH über Bash oder mittels SSH-Client. (siehe [JSch](http://www.jcraft.com/jsch/))
2. **Problem:** <br>
Nach der Initialisierung der EC2-Instanzen ist keine sofortige Verbindung über SCP/SSH möglich.<br>
**Lösung:** <br>
Sleep oder Timeout von ca. 10s ist ausreichend Zeit für die EC2-Instanz.
3. **Problem:** <br>
A<br>
**Lösung:** <br>
B<br>
