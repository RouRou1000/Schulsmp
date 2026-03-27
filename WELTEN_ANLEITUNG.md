# 🌍 Random Teleport (RTP)

Mit `/rtp` kannst du dich an eine zufällige Position in einer Welt teleportieren.

---

## 🎮 Verwendung

```bash
/rtp              # Zeigt alle verfügbaren Welten
/rtp <weltname>   # Teleportiert zu einer zufälligen Position
```

### Beispiele
```bash
/rtp world           # Random TP in die Hauptwelt
/rtp world_nether    # Random TP in den Nether
/rtp world_the_end   # Random TP ins End
/rtp world_farm      # Random TP in die Farm-Welt
```

---

## ✨ Features

- 🎲 **Zufällige Position** im Radius von 5000 Blöcken
- 🛡️ **Sichere Landung** - prüft auf festen Boden
- 🔥 **Nether-Support** - findet freie Räume unter der Decke
- ⚡ **Teleport-Effekte** - Portal-Partikel und Sound

---

## 📋 Welten anzeigen

Gib einfach `/rtp` ohne Parameter ein, um alle geladenen Welten zu sehen:

```
🌍 world
🔥 world_nether
⭐ world_the_end
```

---

## 💡 Tipps

- Die Position ist mindestens **100 Blöcke** vom Welt-Spawn entfernt
- Im **Nether** wird zwischen Y 30-100 nach freiem Raum gesucht
- Falls keine sichere Position gefunden wird, versuche es erneut

---

## 🔧 Für Server-Admins

Der RTP-Befehl nutzt automatisch alle geladenen Welten. Keine Config nötig!

Erstelle neue Welten mit Multiverse:
```bash
/mv create world_farm normal    # Neue Overworld
/mv create world_nether nether  # Nether
/mv create world_the_end end    # End
```

---

**Viel Spaß beim Erkunden! 🎉**

### PvP-Einstellungen (mit Multiverse)
```bash
# PvP in Farm-Welt deaktivieren
/mv modify set pvp false world_farm

# PvP im Nether aktivieren
/mv modify set pvp true world_nether
```

### Spielmodus pro Welt
```bash
# Farm-Welt im Survival
/mv modify set mode survival world_farm
```

---

## 🛡️ Zusätzliche Plugins (Optional)

| Plugin | Nutzen |
|--------|--------|
| **WorldGuard** | Regionen & Schutz |
| **EssentialsX** | Erweiterte Warps |
| **WorldBorder** | Präzise Grenzen |
| **CoreProtect** | Block-Logging |

---

## ❓ Fehlerbehebung

### "Welt nicht gefunden"
- Prüfe den exakten Namen in `config.yml`
- Groß-/Kleinschreibung beachten!
- Server-Konsole auf Fehler prüfen

### Welt lädt nicht
- Prüfe, ob die Welt im Server-Ordner existiert
- Bei Multiverse: `/mv list` zeigt alle Welten

### Spieler spawnen falsch
- `/setworldspawn` in jeder Welt ausführen
- Multiverse: `/mv setspawn`

---

**Viel Spaß beim Einrichten! 🎉**
