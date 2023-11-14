package com.geelee.startup.demo

import com.geelee.startup.annotation.AppInitializer

/**
 * Created by zhiyueli on 11/13/23 17:52.
 */
@AppInitializer(dependencies = [BInitializer::class])
class AInitializer : BaseLogInitializer()

@AppInitializer(dependencies = [CInitializer::class])
class BInitializer : BaseLogInitializer()

@AppInitializer
class CInitializer : BaseLogInitializer()
