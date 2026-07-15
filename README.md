<div align="center">

![Mod Banner](https://i.imgur.com/WLCTnCK.png)

# Female Gender Mod

![Cloud sync player count badge](https://img.shields.io/badge/dynamic/json?url=https%3A%2F%2Fwfgm.celestialfault.dev%2Fstats&query=synced_users&label=Cloud%20synced%20players)

</div>

This mod adds extra customization options to the player model by adding breasts for a more feminine appearance.

The five-part appearance editor supports expansion from subtle proportions through 13.33× the default, with independent
root span, end fullness, width, height, projection, balance, placement, and lift/drop controls. Six generated
profiles include Anime Volume and Teardrop; optional skin-sampled nipple detail and a deterministic secondary-motion
rig complete the editor. Auto-fit Front, ¾, and Side previews make even extreme proportions editable. Skin, jacket,
armor, trim, and glint geometry all follow the selected silhouette.

This mod is primarily client-sided, but your settings will also be automatically synced with other connected players
if the mod is also installed on the server.

The mod also features cloud sync support (as of 4.0 on 1.21.2+) to allow for syncing your customization settings
to other players on servers that don't have the mod installed.

## Default Controls
H - Open Customization Settings

[Pre 1.21.6] G - Open Customization Settings

## Appearance Presets

Open **Character Personalization → Placement → Presets** to save and apply named appearance presets.
Presets are portable JSON files stored in **config/FemaleGenderMod/presets**. Each file records its preset
schema, mod version, and Minecraft version. Preset schemas 1 and 2 migrate safely to schema 3; presets from
another mod or Minecraft version can be applied when their schema is compatible, while unsupported future
schemas are blocked.

## License

The Female Gender Mod is licensed under the GNU LGPLv3, a free and open-source license. For more information,
please see the [license file](./LICENSE).

## Frequently Asked Questions

<details>
<summary>1) Can you include a male bulge?</summary>

This mod is centered around female characters, so a male bulge won't be added.

</details>

<details>
<summary>2) If I were to play on a realm with my friends, and we all used this mod, would they see my character with my settings?</summary>

If everyone has **Cloud Sync** enabled, then yes. There is a delay in updating gender settings though, for performance reasons. We are going to actively monitor it to see if we can reduce or even possibly remove the delay in the future.

</details>

<details>
<summary>3) What about adding female butts?</summary>

No.

</details>

<details>
<summary>4) Any plans for additional breast models?</summary>

Six generated profiles are available now: Classic, Anime Volume, Round, Natural, Teardrop, and Bell. Each profile
changes the actual broad-root mesh silhouette, and armor layers follow it.

</details>

<details>
<summary>5) Can we have larger breasts?</summary>

Yes. Overall expansion reaches 13.33× the default value, with additional permissive controls for root span,
end fullness, width, height, projection, placement, lift/drop, and optional left/right asymmetry.

</details>

<details>
<summary>6) Will you port to past versions?</summary>

I am not providing support for past versions of Minecraft. The mod will only be developed on the most recent version of the game.

</details>

<details>
<summary>7) Is this mod compatible with Minecraft: Bedrock Edition (Mobile)?</summary>

Female Gender Mod is not available for Bedrock Edition and there are no plans to support it. However, [Extended Character Appearance](https://zazakrizpycreations.blogspot.com/2026/04/ExtendedCharacterAppearance.html) offers a very similar character model for Bedrock Edition players.

</details>

<details>
<summary>8) Why when I press 'H' ('G' in versions before 1.21.6) it opens the customization screen, and not the player list?</summary>

The option to edit other players' characters has been removed and will not be re-added. Modifying other players' characters without their consent is not recommended, and many people found it wrong to be able to do that. It was never intended to be a feature, it was just inherited from the UI design.

</details>

<details>
<summary>9) Could you change the player model's height or give it more curvy looking hips?</summary>

No, I don't want to edit the base game features. The breast model is an added layer to the default model. If I were to do a waist/torso thing, it would be editing the player model. It would also probably break compatibility with a lot more mods as well.

</details>
