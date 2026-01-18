# Economy - Simple Economy Plugin for Hytale

## For Developers:
Please see the [EconomyAPI](https://www.github.com/Ender57/EconomyAPI) repository/page for information on developing with this plugin's API.

## Installing
Installing Economy is as simple as copying the provided jar into your server's mods folder, then starting the server.
Configuration files will be generated automatically on first launch.

## Features
* Lightweight economy system with a clean API
* Player balances with configurable starting balance
* Offline payments (optional)
* Built-in commands:
  - `/pay` to send money to other players
  - `/eco give|take|set` for admin balance management
  - `/balance` to view your balance or another player's balance
  - `/baltop` to view the top balances leaderboard
* Fully configurable messages (color codes supported)

## Permissions
* `economy.pay`
  - Allows using `/pay`
* `economy.eco`
  - Allows using `/eco give|take|set`
* `economy.balance`
  - Allows using `/balance` (self)
* `economy.balance.others`
  - Allows using `/balance <player>`
* `economy.baltop`
  - Allows using `/baltop`

## Configuration
The plugin generates:
- `config.yml` (currency settings, starting balance, leaderboard page size, etc.)
- `messages.yml` (all command messages)

## License
Copyright (C) 2026 Ender57

Economy is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

Economy is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License with Economy. If not, see http://www.gnu.org/licenses/.