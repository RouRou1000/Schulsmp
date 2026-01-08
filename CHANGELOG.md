# 📝 Changelog

Alle bemerkenswerten Änderungen am DonutCore Plugin.

---

## [1.0.0] - 2025

### ✨ Neu hinzugefügt

#### 💰 Economy-System
- Vollständiges Geld-System mit `/balance`, `/pay`, `/sell`
- Shards-System als PvP-Währung
- Worth-Manager mit konfigurierbaren Item-Werten (30+ Items)
- Verzauberungs-Multiplikatoren für Worth (18 Enchants)
- Named-Item-Multiplikatoren (benannte Items mehr wert)
- `/sell hand` zum Verkaufen des Items in der Hand

#### ⚔️ PvP-Progression
- Kill/Death-Tracking
- Automatische Shard-Belohnung bei Kills
- K/D-Ratio Berechnung
- Statistik-Anzeige im Scoreboard

#### 🎰 Kisten-System
- 3 Tier-System: Basic, Rare, Legendary
- Garantierte Items pro Kiste (immer beim Öffnen)
- Gewichtete Pool-Items (zufällig)
- Bundle-Belohnungen (mehrere Items auf einmal)
- Animierte Kisten-Öffnung mit Sound
- Key-Kauf für Geld direkt im GUI
- Admin-Befehle: `give`, `givekey`, `addguaranteed`, `addbundle`, `settier`, `test`, `info`

#### 🛒 Shops
- **Donut Shop**: 30+ Items in 5 Kategorien
  - Ressourcen, Nahrung, Werkzeuge, Spawner, Spezial
- **Slay Shop**: 20+ PvP-Items für Shards
  - Kampf, Tränke, Spezial-Items
- Dynamische Preisanzeige via PersistentDataContainer
- Sound-Feedback bei Kauf/Fehlkauf

#### 📦 Auktionshaus
- Items zum Verkauf anbieten (`/auction sell <preis>`)
- Durchsuchen und Kaufen im GUI
- Worth-Integration (zeigt Item-Wert in Lore)
- Eigene Auktionen verwalten

#### 📋 Orders-System
- Aufträge erstellen
- Aufträge von anderen Spielern erfüllen
- Automatische Bezahlung bei Lieferung

#### 🌍 Welten
- 4 Welten-System: Spawn, Nether, End, Farm
- `/warp` Befehl mit allen Welten
- Portal-Partikel bei Teleport
- Enderman-Teleport-Sound

#### 🎨 GUIs
- Moderne, schöne Interfaces mit Borders
- Navigation mit Vor/Zurück-Pfeilen
- Kategorien mit Icons
- Informative Lore-Texte mit Emojis
- Sound-Feedback bei Aktionen

#### 📊 Sidebar/Scoreboard
- Echtzeit-Anzeige auf der rechten Seite
- Zeigt: Geld, Shards, Kills, Deaths, K/D
- Synchrone Updates (kein Flackern)

### 🔧 Technisch
- Spigot/Paper 1.21.5 kompatibel
- Java 17+ erforderlich
- PersistentDataContainer für GUI-Aktionen
- Maven Wrapper für einfachen Build
- Keine externen Dependencies!

---

## 📋 Geplant

- [ ] Leaderboard-System (Top Kills, Top Geld)
- [ ] Clan/Team-System
- [ ] Tägliche Belohnungen
- [ ] Quest-System
- [ ] VIP-Ränge mit Boni
- [ ] MySQL-Support für Multi-Server
- [ ] PlaceholderAPI-Integration
- [ ] Kopfgeld-System

---

**Made with ❤️ for Minecraft**
