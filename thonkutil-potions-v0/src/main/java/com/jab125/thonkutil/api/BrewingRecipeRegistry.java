/*
 * Copyright (c) 2021, 2022 Jab125 & LimeAppleBoat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jab125.thonkutil.api;

import net.minecraft.item.Item;
import net.minecraft.potion.Potion;

@Deprecated
public class BrewingRecipeRegistry {
    private BrewingRecipeRegistry() {
        throw new RuntimeException("This Util class is not allowed to be instantiated!");
    }

    public static void registerPotionRecipe(Potion input, Item ingredient, Potion output) {
        BrewingHelper.registerPotionRecipe(input, ingredient, output);
    }

    public static void registerItemRecipe(Item input, Item ingredient, Item output) {
        BrewingHelper.registerItemRecipe(input, ingredient, output);
    }
}