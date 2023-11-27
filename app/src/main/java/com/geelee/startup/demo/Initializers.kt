package com.geelee.startup.demo

import com.geelee.startup.annotation.Config
import com.geelee.startup.annotation.model.ComponentInfo

/**
 * Created by zhiyueli on 11/13/23 17:52.
 */
@Config(
    dependencies = [BInitializer::class],
    threadMode = ComponentInfo.ThreadMode.WorkThread
)
class AInitializer : BaseLogInitializer()

@Config(dependencies = [CInitializer::class])
class BInitializer : BaseLogInitializer()

@Config
class CInitializer : BaseLogInitializer()