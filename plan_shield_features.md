# Shield Feature Implementation Plan

## 1. Shield HP / Breakability
**File: ShieldGeneratorBlockEntity.java**
- Add `maxHp=20`, `hp`, `regenCooldown` fields
- NBT: `MH`, `HP`, `RC`
- On hit: `hp--`, `regenCooldown=60`, deactivate if hp<=0
- Server tick: regen when cooldown expires (1hp/10ticks)
- Modulate shieldAlpha/strength by `hp/maxHp`

## 2. Physical Ripple (Vertex Displacement)
**File: ShieldGeneratorBlockEntity.java**
- Add `hitDirX/Y/Z` (unit direction of impact), `hitTick`
- NBT: `HX`, `HY`, `HZ`, `HT`
- Store on projectile hit

**File: ShieldGeneratorBlockEntityRenderer.java**
- Replace glow-only ripple with vertex displacement
- For each vertex, compute angle from hit direction
- `displace = amplitude * sin(progress*PI) * exp(-angle^2*4)` along vertex normal
- Duration: 15 ticks, amplitude: 2.0 blocks
- Keep the additive glow as secondary effect

## 3. Color GUI
**New: ShieldColorMenu.java** - extends AbstractContainerMenu
- No slots, holds BlockPos
- `clickMenuButton(int id)` decodes `(r<<16)|(g<<8)|b`, calls `be.setShieldColor()`
- Client constructor reads from FriendlyByteBuf

**New: ShieldColorScreen.java** - extends Screen
- Three sliders (R/G/B 0-255), current color from synced BE
- On slider change: packs color int, calls `gameMode.handleInventoryButtonClick()`
- "Save" button closes screen

**Modify: ShieldGeneratorBlock.java**
- `useWithoutItem` opens menu via `player.openMenu(be)` if holding nothing

**Modify: ShieldGeneratorBlockEntity.java**
- Implement `MenuProvider`, return `SimpleMenuProvider`

**Modify: Thrusted.java**
- Register `DeferredRegister<MenuType<?>> MENUS`
- Register `SHIELD_COLOR_MENU` menu type

**Modify: ClientModEvents.java** (in Thrusted.java)
- Register screen factory for ShieldColorMenu

## Verification
1. Build: `./gradlew build`
2. Run: `./gradlew runClient`
3. Place shield generator, power with redstone, see shield form
4. Shoot arrow at shield → shield HP decreases, vertices bulge, color GUI accessible
5. Right-click with empty hand → color GUI opens, sliders change color
6. Let shield run out of HP → deactivates, regenerates later
