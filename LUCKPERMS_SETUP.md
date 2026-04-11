# LuckPerms Ränge - Setup Befehle für Schul-SMP
# ================================================
# Führe diese Befehle in der Server-Konsole oder als OP in-game aus.
# Reihenfolge ist wichtig!

# ============================================
# 1. GRUPPEN ERSTELLEN
# ============================================
lp creategroup owner
lp creategroup admin
lp creategroup mod
lp creategroup builder
lp creategroup spieler

# ============================================
# 2. GEWICHTE SETZEN (höher = wichtiger)
# ============================================
lp group owner setweight 100
lp group builder setweight 90
lp group admin setweight 80
lp group mod setweight 70
lp group spieler setweight 0

# ============================================
# 3. PREFIXE SETZEN (Farb-Codes im Chat/Tab)
# ============================================
# Format: prefix.<gewicht>.<text>
# & wird als § interpretiert von LuckPerms
lp group owner meta setprefix 100 "&4&lOwner"
lp group builder meta setprefix 90 "&a&lBuilder"
lp group admin meta setprefix 80 "&c&lAdmin"
lp group mod meta setprefix 70 "&c&lMod"
lp group spieler meta setprefix 0 "&7Spieler"

# ============================================
# 4. SUFFIXE SETZEN (Namensfarbe)
# ============================================
lp group owner meta setsuffix 100 "&4"
lp group builder meta setsuffix 90 "&a"
lp group admin meta setsuffix 80 "&c"
lp group mod meta setsuffix 70 "&c"
lp group spieler meta setsuffix 0 "&7"

# ============================================
# 5. VERERBUNG (Inheritance)
# ============================================
# Owner erbt alles von Builder
lp group owner parent add builder

# Builder erbt alles von Admin
lp group builder parent add admin

# Admin erbt alles von Mod
lp group admin parent add mod

# Mod erbt von Spieler
lp group mod parent add spieler

# ============================================
# 6. PERMISSIONS - OWNER
# ============================================
# Owner bekommt OP-Rechte (alle Permissions)
lp group owner permission set * true

# ============================================
# 7. PERMISSIONS - ADMIN
# ============================================
lp group admin permission set donut.admin true
lp group admin permission set donut.crate.admin true
lp group admin permission set donut.balance.others true
lp group admin permission set minecraft.command.gamemode true
lp group admin permission set minecraft.command.teleport true
lp group admin permission set minecraft.command.kick true
lp group admin permission set minecraft.command.ban true
lp group admin permission set minecraft.command.op true
lp group admin permission set minecraft.command.give true
lp group admin permission set minecraft.command.time true
lp group admin permission set minecraft.command.weather true
lp group admin permission set bukkit.command.gamemode true

# ============================================
# 8. PERMISSIONS - MOD
# ============================================
lp group mod permission set donut.admin true
lp group mod permission set donut.crate.admin true
lp group mod permission set donut.balance.others true
lp group mod permission set minecraft.command.kick true
lp group mod permission set minecraft.command.ban true
lp group mod permission set minecraft.command.teleport true
lp group mod permission set minecraft.command.gamemode true

# ============================================
# 9. PERMISSIONS - BUILDER
# ============================================
lp group builder permission set minecraft.command.gamemode true
lp group builder permission set minecraft.command.teleport true

# ============================================
# 10. PERMISSIONS - SPIELER (Standard)
# ============================================
# Spieler haben nur die Basis-Plugin-Befehle
# (Shop, Sell, Balance, Worth, AH, Order, etc. brauchen keine Permission)

# ============================================
# 11. SPIELER EINEM RANG ZUWEISEN
# ============================================
# Beispiel: Dich selbst als Owner setzen
# lp user rourou1000 parent set owner

# Beispiel: Anderen Spieler als Mod setzen
# lp user <spielername> parent set mod

# Spieler zurück auf Standard setzen
# lp user <spielername> parent set spieler

# ============================================
# FERTIG! Änderungen werden sofort angewendet.
# ============================================
