/*
* Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
*/

kotlin.sourceSets {
    val commonTest by getting {
        dependencies {
            api(project(":ktor-client:ktor-client-plugins:ktor-client-logging"))
        }
    }
}
