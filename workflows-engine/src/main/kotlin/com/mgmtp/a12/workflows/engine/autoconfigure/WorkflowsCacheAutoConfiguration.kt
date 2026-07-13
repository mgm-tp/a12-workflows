/*
 * SPDX-License-Identifier: EUPL-1.2 OR LicenseRef-commercial
 *
 * Copyright (c) 2012-2026 mgm technology partners GmbH
 *
 * Dual License
 * ------------
 * This source file is part of the mgm A12 Platform and available under
 * a choice of two different licenses:
 *
 * 1. Open-Source License – EUPL v1.2
 *    You may redistribute and/or modify this file under the terms of the
 *    European Union Public License, version 1.2 - see https://eupl.eu/.
 *
 * 2. Commercial License
 *    Alternatively, you may obtain a commercial license from
 *    mgm technology partners GmbH, that permits use of this software
 *    under different terms (including support and maintenance services).
 *
 *    Please contact a12-license@mgm-tp.com for more information.
 *
 * You must select and comply with exactly one of the above license options.
 *
 * Warranty Disclaimer (applies to either option)
 * ----------------------------------------------
 * THIS SOFTWARE IS PROVIDED "AS IS" AND WITHOUT WARRANTY OF ANY KIND,
 * WHETHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NON-INFRINGEMENT, EXCEPT WHERE SUCH DISCLAIMERS ARE HELD TO BE
 * LEGALLY INVALID. SEE THE RESPECTIVE LICENSE TEXT FOR DETAILS.
 */

package com.mgmtp.a12.workflows.engine.autoconfigure

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.cache.CacheManager
import org.springframework.cache.support.NoOpCacheManager
import org.springframework.context.annotation.Bean

/**
 * Supplies a fallback [CacheManager] so the application context can start when Spring caching is
 * enabled but no cache provider is configured.
 *
 * uaa-authentication's `JwtTokenConfig` is annotated with `@EnableCaching`, which activates the
 * caching infrastructure and therefore requires a [CacheManager] bean. Spring Boot 4 moved its
 * cache auto-configuration into the optional `spring-boot-cache` module, which is not on the
 * engine's classpath, so no [CacheManager] would otherwise be provided.
 *
 * The bean is guarded by [ConditionalOnMissingBean]: downstream projects that build on
 * workflows-engine can declare their own [CacheManager] (e.g. a clustered one for replicated token
 * storage) and it will take precedence over this no-op default.
 */
@AutoConfiguration
class WorkflowsCacheAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(CacheManager::class)
    fun cacheManager(): CacheManager = NoOpCacheManager()
}
