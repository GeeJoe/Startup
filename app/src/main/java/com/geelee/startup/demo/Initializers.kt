package com.geelee.startup.demo

import com.geelee.startup.annotation.Initializer

/**
 * Created by zhiyueli on 11/13/23 17:52.
 */
@Initializer(dependencies = [BInitializer::class])
class AInitializer : BaseLogInitializer()

@Initializer(dependencies = [CInitializer::class])
class BInitializer : BaseLogInitializer()

@Initializer
class CInitializer : BaseLogInitializer()
