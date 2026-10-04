# 2.0.0 — early experimental release (resource-metadata fix)

AI-generated addon code and documentation, with requirements and in-game testing by the human maintainer. Mark this release as **pre-release**.

Adds station-local payment caps, profession/individual rules and NBT-aware trade grouping. Includes the corrected root `pack.mcmeta`. The metadata fix does not change trading logic.

Requires Minecraft 1.19.2, Forge 43.x and IC2 Classic 1.19.2-2.1.2.1. Install on client and server. Default cap is 1 item in the first payment slot. Prices can still rise; expensive trades are skipped.

The graphical interface and trading were tested by the human maintainer in their setup. Multiplayer compatibility and performance are not fully validated. Broader-scope rules do not erase narrower exceptions.
