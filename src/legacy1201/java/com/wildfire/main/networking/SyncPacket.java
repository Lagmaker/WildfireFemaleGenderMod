/*
    Wildfire's Female Gender Mod is a female gender mod created for Minecraft.
    Copyright (C) 2023 WildfireRomeo

    This program is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 3 of the License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package com.wildfire.main.networking;

import com.wildfire.main.entitydata.Breasts;
import com.wildfire.main.entitydata.PlayerConfig;
import com.wildfire.main.Gender;
import com.wildfire.main.config.BreastShape;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.minecraft.network.PacketByteBuf;

import java.util.UUID;

abstract class SyncPacket implements FabricPacket {
    protected final UUID uuid;
    private final Gender gender;
    private final float bust_size;

    //physics variables
    private final boolean breast_physics;
    private final boolean show_in_armor;
    private final float bounceMultiplier;
    private final float floppyMultiplier;
    private final boolean wobbleEnabled;
    private final float wobbleIntensity;
    private final float wobbleSpeed;

    private final float xOffset, yOffset, zOffset;
    private final boolean uniboob;
    private final float cleavage;
    private final float width, height, projection, balance, nippleSize;
    private final BreastShape shape;
    private final boolean nipples;

    private final boolean hurtSounds;

    protected SyncPacket(PlayerConfig plr) {
        this.uuid = plr.uuid;
        this.gender = plr.getGender();
        this.bust_size = plr.getBustSize();
        this.hurtSounds = plr.hasHurtSounds();

        //physics variables
        this.breast_physics = plr.hasBreastPhysics();
        this.show_in_armor = plr.showBreastsInArmor();
        this.bounceMultiplier = plr.getBounceMultiplier();
        this.floppyMultiplier = plr.getFloppiness();
        this.wobbleEnabled = plr.hasWobble();
        this.wobbleIntensity = plr.getWobbleIntensity();
        this.wobbleSpeed = plr.getWobbleSpeed();

        Breasts breasts = plr.getBreasts();
        this.xOffset = breasts.getXOffset();
        this.yOffset = breasts.getYOffset();
        this.zOffset = breasts.getZOffset();

        this.uniboob = breasts.isUniboob();
        this.cleavage = breasts.getCleavage();
        this.width = breasts.getWidth();
        this.height = breasts.getHeight();
        this.projection = breasts.getProjection();
        this.balance = breasts.getBalance();
        this.shape = breasts.getShape();
        this.nipples = breasts.hasNipples();
        this.nippleSize = breasts.getNippleSize();
    }

    protected SyncPacket(PacketByteBuf buffer) {
        this.uuid = buffer.readUuid();
        this.gender = buffer.readEnumConstant(Gender.class);
        this.bust_size = buffer.readFloat();
        this.hurtSounds = buffer.readBoolean();

        //physics variables
        this.breast_physics = buffer.readBoolean();
        this.show_in_armor = buffer.readBoolean();
        this.bounceMultiplier = buffer.readFloat();
        this.floppyMultiplier = buffer.readFloat();
        this.wobbleEnabled = buffer.readBoolean();
        this.wobbleIntensity = buffer.readFloat();
        this.wobbleSpeed = buffer.readFloat();

        this.xOffset = buffer.readFloat();
        this.yOffset = buffer.readFloat();
        this.zOffset = buffer.readFloat();
        this.uniboob = buffer.readBoolean();
        this.cleavage = buffer.readFloat();
        this.width = buffer.readFloat();
        this.height = buffer.readFloat();
        this.projection = buffer.readFloat();
        this.balance = buffer.readFloat();
        this.shape = buffer.readEnumConstant(BreastShape.class);
        this.nipples = buffer.readBoolean();
        this.nippleSize = buffer.readFloat();
    }

    @Override
    public void write(PacketByteBuf buffer) {
        buffer.writeUuid(this.uuid);
        buffer.writeEnumConstant(this.gender);
        buffer.writeFloat(this.bust_size);
        buffer.writeBoolean(this.hurtSounds);
        buffer.writeBoolean(this.breast_physics);
        buffer.writeBoolean(this.show_in_armor);
        buffer.writeFloat(this.bounceMultiplier);
        buffer.writeFloat(this.floppyMultiplier);
        buffer.writeBoolean(this.wobbleEnabled);
        buffer.writeFloat(this.wobbleIntensity);
        buffer.writeFloat(this.wobbleSpeed);

        buffer.writeFloat(this.xOffset);
        buffer.writeFloat(this.yOffset);
        buffer.writeFloat(this.zOffset);
        buffer.writeBoolean(this.uniboob);
        buffer.writeFloat(this.cleavage);
        buffer.writeFloat(this.width);
        buffer.writeFloat(this.height);
        buffer.writeFloat(this.projection);
        buffer.writeFloat(this.balance);
        buffer.writeEnumConstant(this.shape);
        buffer.writeBoolean(this.nipples);
        buffer.writeFloat(this.nippleSize);
    }

    protected void updatePlayerFromPacket(PlayerConfig plr) {
        plr.updateGender(gender);
        plr.updateBustSize(bust_size);
        plr.updateHurtSounds(hurtSounds);

        //physics
        plr.updateBreastPhysics(breast_physics);
        plr.updateShowBreastsInArmor(show_in_armor);
        plr.updateBounceMultiplier(bounceMultiplier);
        plr.updateFloppiness(floppyMultiplier);
        plr.updateWobble(wobbleEnabled);
        plr.updateWobbleIntensity(wobbleIntensity);
        plr.updateWobbleSpeed(wobbleSpeed);
        //System.out.println(plr.username + " - " + plr.gender);

        Breasts breasts = plr.getBreasts();
        breasts.updateXOffset(xOffset);
        breasts.updateYOffset(yOffset);
        breasts.updateZOffset(zOffset);
        breasts.updateUniboob(uniboob);
        breasts.updateCleavage(cleavage);
        breasts.updateWidth(width);
        breasts.updateHeight(height);
        breasts.updateProjection(projection);
        breasts.updateBalance(balance);
        breasts.updateShape(shape);
        breasts.updateNipples(nipples);
        breasts.updateNippleSize(nippleSize);
    }
}
