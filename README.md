# 🍩 DonutCore (Deutsch)

Ein umfangreiches Economy- und PvP-Plugin für **Spigot/Paper 1.21.5** im Stil von Donut SMP.

---

## ✨ Features

### 🎮 Hauptmenü (`/menu`)
- **Zentrales GUI**: Zugang zu allen Features über ein übersichtliches Menü
- **Spieler-Info**: Geld, Shards, Kills, Deaths auf einen Blick
- **Schnellzugriff**: Alle Shops, Auktionshaus, Orders und Kisten

### 💰 Economy-System
- **Geld & Shards**: Duale Währung (Geld für allgemeinen Handel, Shards für PvP-Shop)
- **Worth-System**: 80+ Items mit vordefinierten Werten
  - Alle Erze, Barren, Edelsteine
  - Mob-Drops, Nahrung, Spezialitems
  - Multiplier für benannte Items
  - Multiplier für Verzauberungen
- **Befehle**: `/sell`, `/sell hand`, `/pay`, `/balance`

### ⚔️ PvP-Progression
- **Kills & Deaths Tracking**: Statistiken werden gespeichert
- **Shards durch Kills**: Verdiene Shards für jeden Kill
- **K/D-Ratio**: Automatische Berechnung

### 🎰 Kisten-System (Vollständig UI-basiert!)
- **Tiers**: Basic, Rare, Legendary mit unterschiedlichen Enchant-Chancen
- **Garantierte Items**: Bestimmte Items bei jedem Öffnen
- **Pools & Bundles**: Gewichtete Zufallsbelohnungen
- **Animierte Öffnung**: Spannendes visuelles Erlebnis
- **Keys kaufen**: Mit Geld ODER Shards direkt in der GUI!
  - Basic Key: $100 oder 50 Shards
  - Rare Key: $500 oder 250 Shards
  - Legendary Key: $2000 oder 1000 Shards
- **Admin-Befehle**: `/crateadmin give/givekey/addguaranteed/settier/test`

### 🛒 Shops
- **Donut Shop**: Kaufe Items für Geld
- **Slay Shop**: Kaufe PvP-Items für Shards (20+ Items!)
- **Schöne GUIs**: Mit Kategorien, Borders und Navigation

### 📦 Auktionshaus (100% UI!)
- **Eigene Items verkaufen**: Direkt in der GUI - Item halten, Button klicken, Preis im Chat eingeben
- **Durchsuchen & Kaufen**: Linksklick zum Kaufen
- **Meine Auktionen**: Eigene Auktionen verwalten, Rechtsklick zum Zurückziehen
- **Worth-Integration**: Zeigt Item-Wert an

### 📋 Orders-System (100% UI!)
- **Aufträge erstellen**: Direkt in der GUI - Item halten, Button klicken, Menge und Preis im Chat
- **Aufträge erfüllen**: Linksklick mit passenden Items im Inventar
- **Meine Orders**: Eigene Orders verwalten, Rechtsklick zum Stornieren (Geld wird erstattet)

### 🌍 Teleport-System
- **Random Teleport**: `/rtp <weltname>` - Teleportiere zu einer zufälligen sicheren Position
- **Dynamisch**: Funktioniert mit allen geladenen Welten
- **Keine Config nötig**: Erkennt automatisch verfügbare Welten

### 📊 Sidebar/Scoreboard
- Zeigt in Echtzeit: Geld, Shards, Kills, Deaths, K/D

---

## 📥 Installation

### Voraussetzungen
- Java 17 oder höher
- Spigot/Paper Server 1.21.5

### Build
```bash
# Mit Maven Wrapper (empfohlen)
./mvnw clean package -DskipTests

# Oder mit globalem Maven
mvn clean package -DskipTests
```

### Installation
1. Kopiere `target/donut-core-1.0.0.jar` nach `plugins/`
2. Starte den Server
3. Passe `config.yml` an

---

## ⚙️ Konfiguration

### config.yml Übersicht
```yaml
settings:
  shards-per-kill: 3          # Shards pro Kill
  sidebar-update-ticks: 20    # Sidebar Update-Intervall
  
worlds:
  spawn: world                 # Spawn-Welt
  nether: world_nether        # Nether
  end: world_the_end          # End
  farm: world_farm            # Farm-Welt

crates:
  common:
    tier: basic
    display: "§a★ Gewöhnliche Kiste"
    guaranteed:
      - "DIAMOND:1"
    pool:
      - "IRON_INGOT:5:10"     # Item:Menge:Gewicht

worth:
  DIAMOND: 100.0
  EMERALD: 75.0
  # ... weitere Items

worth-enchant-multipliers:
  SHARPNESS: 1.5
  PROTECTION: 1.4
  # ... weitere Verzauberungen
```

---

## 📜 Befehle

| Befehl | Beschreibung | Permission |
|--------|--------------|------------|
| `/menu` | Öffnet Hauptmenü (Zugang zu allem!) | - |
| `/balance` | Zeigt Kontostand | - |
| `/pay <spieler> <betrag>` | Geld senden | - |
| `/sell` | Inventar verkaufen | - |
| `/sell hand` | Item in Hand verkaufen | - |
| `/worth` | Zeigt Item-Wert | - |
| `/rtp <welt>` | Random Teleport | - |
| `/shop` | Öffnet Donut Shop | - |
| `/slayshop` | Öffnet Slay Shop | - |
| `/crate` | Öffnet Kisten-Übersicht | - |
| `/ah` | Öffnet Auktionshaus | - |
| `/order` | Zeigt Orders | - |
| `/crateadmin` | Kisten-Administration | `donut.admin` |

**💡 Tipp**: Nutze einfach `/menu` für alles!

---

## 🎨 GUIs

Alle GUIs haben:
- ✅ Schöne Borders
- ✅ Navigation (Vor/Zurück/Schließen)
- ✅ Farbige Kategorien mit Icons
- ✅ Informative Lore-Texte
- ✅ Sound-Feedback
- ✅ **Vollständige UI-Integration** - keine Commands nötig!

### 🎯 Hauptmenü Features
- **Shop**: Items mit Geld kaufen
- **Slay Shop**: PvP-Items mit Shards
- **Auktionshaus**: Items verkaufen & kaufen (alles in UI!)
- **Orders**: Kaufanfragen erstellen & erfüllen (alles in UI!)
- **Kisten**: Keys kaufen (Geld/Shards) & Kisten öffnen
- **Quick Actions**: Sell, Worth direkt aufrufbar

---

## 📁 Dateistruktur

```
plugins/DonutCore/
├── config.yml      # Hauptkonfiguration
├── data.yml        # Spielerdaten (auto-generiert)
└── auctions.yml    # Auktionsdaten (auto-generiert)
```

---

## 🔧 Entwicklung

### Projekt-Struktur
```
src/main/java/de/coolemod/donut/
├── DonutPlugin.java           # Hauptklasse
├── commands/                  # Alle Befehle
├── gui/                       # GUI-Klassen
├── listeners/                 # Event-Handler
├── managers/                  # Datenmanager
└── utils/                     # Hilfsfunktionen
```

### Abhängigkeiten
- Spigot API 1.21.5
- Keine externen Dependencies!

---

## 📝 Changelog

Siehe [CHANGELOG.md](CHANGELOG.md)

---

## 📄 Lizenz

Privates Projekt - Alle Rechte vorbehalten.

---

**Made with ❤️ for Minecraft**
