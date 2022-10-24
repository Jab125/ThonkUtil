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
package com.jab125.limeappleboat.thonkutil.axolotl.v1.impl;

import net.fabricmc.api.ModInitializer;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.util.registry.Registry;

import java.util.ArrayList;
import java.util.HashMap;

public class ThonkUtilAxolotlApi implements ModInitializer {
    private static HashMap<String, Block> registry = new HashMap<>();
    @Override
    public void onInitialize() {
        var s = new String[]{"a", "b", "c"};
        for (String s1 : s) {
            register(s1, Registry.register(Registry.BLOCK, s1, new Block(AbstractBlock.Settings.of(Material.METAL))));
        }
    }

    private void register(String d, Block block) {
        registry.put(d, block);
    }
    public static Block getBlock(String s) {
        return registry.get(s);
    }
}