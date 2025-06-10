# Converter Mod between Create and MineColonies
### Important information: only works for very specific Versions of the mods
###### I will probably add support for other versions of the mod sometime later.

## Description
This mod adds functionality to easily convert the needed items for a building in a Resource Scroll from MineColonies to a 
Materials Checklist from the Create Mod, so automating the process is easier and pulling all items from your storage system at once works.

This mod fully supports the Domum Ornamentum Blocks and all of their variants. 

## Usage
Hold your Resource Scroll, which should obviously be linked to a builder who is already requesting items, in your _Right Hand_, and a Clipboard from Create (doesn't matter if empty or not - just beware that all content in the Clipboard will be overwritten) in your _Left Hand_. Then _right click_ and the message `Successfully copied to Clipboard` should appear. Now you can take your Clipboard into the right hand and look at the required Resources in your Clipboard.

To make the most use of it, you can automate all building blocks for your Colony with Create and use the Clipboard to quickly order all required Items for a build from your storage system.


## Installation
Don't know what you all need specifically. I included the exact mod maven snippets below, so you can look it up on curseforge and download the exactly right version. 


__Requirements:__
```# Dependencies
    minecraft_version = 1.20.1
    create_version = 6.0.4-79
    ponder_version = 1.0.52
    flywheel_version = 1.0.2
    registrate_version = MC1.20-1.3.3
```
```
    // Create 6.0.4 for 1.20.1
    implementation(fg.deobf("com.simibubi.create:create-${minecraft_version}:${create_version}:slim") { transitive = false })
    implementation(fg.deobf("net.createmod.ponder:Ponder-Forge-${minecraft_version}:${ponder_version}"))
    compileOnly(fg.deobf("dev.engine-room.flywheel:flywheel-forge-api-${minecraft_version}:${flywheel_version}"))
    runtimeOnly(fg.deobf("dev.engine-room.flywheel:flywheel-forge-${minecraft_version}:${flywheel_version}"))
    implementation(fg.deobf("com.tterrag.registrate:Registrate:${registrate_version}"))
    compileOnly(annotationProcessor("io.github.llamalad7:mixinextras-common:0.4.1"))
    implementation("io.github.llamalad7:mixinextras-forge:0.4.1")

    // Minecolonies 1.1.873-alpha for 1.20.1
    implementation fg.deobf("curse.maven:minecolonies-245506:6443501")
    implementation fg.deobf("curse.maven:domum-ornamentum-527361:5281660")
    implementation fg.deobf("curse.maven:blockui-522992:6368392")
    implementation fg.deobf("curse.maven:structurize-298744:6515173")
```