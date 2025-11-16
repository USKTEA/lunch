package com.usktea.lunch.entity.common

import jakarta.annotation.PostConstruct
import jakarta.persistence.EntityManagerFactory
import org.hibernate.event.internal.DefaultFlushEntityEventListener
import org.hibernate.event.service.spi.EventListenerRegistry
import org.hibernate.event.spi.EventType
import org.hibernate.event.spi.FlushEntityEvent
import org.hibernate.internal.SessionFactoryImpl
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

@Component
class DirtyCheckCustomizer(
    private val entityManagerFactory: EntityManagerFactory,
) {
    @PostConstruct
    fun customize() {
        entityManagerFactory
            .unwrap(SessionFactoryImpl::class.java)
            .serviceRegistry
            .getService(EventListenerRegistry::class.java)
            ?.apply {
                getEventListenerGroup(EventType.FLUSH_ENTITY).clearListeners()
                getEventListenerGroup(EventType.FLUSH_ENTITY).appendListener(CustomFlushEntityEventListener)
            }
    }
}

object CustomFlushEntityEventListener : DefaultFlushEntityEventListener() {
    private val DIRTY_CHECK_IGNORE_MAP = ConcurrentHashMap<KClass<out Any>, Set<String>>()

    override fun dirtyCheck(event: FlushEntityEvent) {
        super.dirtyCheck(event)

        removeIgnoredDirtyCheckProperties(event)
    }

    private fun removeIgnoredDirtyCheckProperties(event: FlushEntityEvent) {
        val propertyNames = event.entityEntry.persister.propertyNames
        val dirtyPropertyIndexes =
            event.dirtyProperties
                ?: return

        val dirtyCheckIgnoreFieldNames = getDirtyCheckIgnoreFields(event)
        if (dirtyCheckIgnoreFieldNames.isEmpty()) {
            return
        }

        val notIgnoredDirtyPropertySize =
            dirtyPropertyIndexes
                .filterNot { dirtyCheckIgnoreFieldNames.contains(propertyNames[it]) }
                .size

        if (notIgnoredDirtyPropertySize == 0) {
            event.dirtyProperties = null
        }
    }

    private fun getDirtyCheckIgnoreFields(event: FlushEntityEvent): Set<String> {
        var cachedDirtyCheckIgnoreFieldNames = DIRTY_CHECK_IGNORE_MAP[event.entity::class]
        if (cachedDirtyCheckIgnoreFieldNames != null) {
            return cachedDirtyCheckIgnoreFieldNames
        }

        synchronized(event.entity::class) {
            cachedDirtyCheckIgnoreFieldNames = DIRTY_CHECK_IGNORE_MAP[event.entity::class]
            if (cachedDirtyCheckIgnoreFieldNames != null) {
                return cachedDirtyCheckIgnoreFieldNames!!
            }

            val dirtyCheckIgnoreFieldNames =
                event.entity::class.memberProperties
                    .filter { it.javaField?.isAnnotationPresent(DirtyCheckIgnore::class.java) ?: false }
                    .map { it.name }
                    .toSet()

            DIRTY_CHECK_IGNORE_MAP[event.entity::class] = dirtyCheckIgnoreFieldNames

            return dirtyCheckIgnoreFieldNames
        }
    }
}

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class DirtyCheckIgnore
