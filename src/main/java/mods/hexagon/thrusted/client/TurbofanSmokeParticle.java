package mods.hexagon.thrusted.client;
import mods.hexagon.thrusted.Thrusted;
import mods.hexagon.thrusted.client.render.ThrustedRenderTypes;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.Nullable;

public class TurbofanSmokeParticle extends TextureSheetParticle {
    
    private final boolean isRaptor;

    protected TurbofanSmokeParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.setSprite(sprites.get(level.random));
        
        // Use speed magnitude to determine if it's high-speed Raptor exhaust
        double speed = Math.sqrt(xSpeed * xSpeed + ySpeed * ySpeed + zSpeed * zSpeed);
        this.isRaptor = speed > 0.05;

        this.quadSize = 0.1F;
        this.gravity = 0.0F;
        
        if (isRaptor) {
            // Raptor: Dark grey/black
            this.rCol = 0.08F;
            this.gCol = 0.08F;
            this.bCol = 0.08F;
        } else {
            // Turbofan: White
            this.rCol = 0.95F;
            this.gCol = 0.95F;
            this.bCol = 0.95F;
        }
        
        this.lifetime = 12000;
        this.alpha = 0.7F;
        
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ThrustedRenderTypes.NO_CLIPPING_TRANSLUCENT;
    }
    
    @Override
    public void tick() {
        super.tick();
        
        if (this.quadSize < 4.0F) {
            this.quadSize += 0.04F;
        }

        if (this.age > this.lifetime - 200) {
            this.alpha -= 0.004F;
        }
    }
    
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        
        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }
        
        @Nullable
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, 
                                      double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new TurbofanSmokeParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites);
        }
    }
}
