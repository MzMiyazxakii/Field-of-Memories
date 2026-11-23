package com.fieldofmemories.client.model;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;

// Animations supplied with the replacement scarecrow model.
public class ScarecrowAnimation {
    public static final AnimationDefinition idle = AnimationDefinition.Builder.withLength(0.0F)
            .build();

    public static final AnimationDefinition idle2 = AnimationDefinition.Builder.withLength(0.0F)
            .addAnimation("Head", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(-15.0F, 0.0F, 0.0F),
                            AnimationChannel.Interpolations.LINEAR)))
            .addAnimation("Head", new AnimationChannel(AnimationChannel.Targets.POSITION,
                    new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, -0.5F),
                            AnimationChannel.Interpolations.LINEAR)))
            .build();

    public static final AnimationDefinition idle3 = AnimationDefinition.Builder.withLength(0.0F)
            .addAnimation("Head", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(-2.7059F, 22.4774F, -1.035F),
                            AnimationChannel.Interpolations.LINEAR)))
            .build();
}
