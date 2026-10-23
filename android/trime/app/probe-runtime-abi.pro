# Stable Kotlin ABI used by the separate engine protocol APK. These selected class
# families were derived from its compiled constant pools, not arbitrary keep-all.
# Apply identically to ordinary and locally signed release. R8/native optimization
# remain enabled. Recheck artifact size/legacy roots; this is a WIP validation ABI.
-keep,allowoptimization class kotlin.Pair { public protected *; }
-keep,allowoptimization class kotlin.TuplesKt* { public protected *; }
-keep,allowoptimization class kotlin.Unit { public protected *; }
-keep,allowoptimization class kotlin.collections.ArraysKt* { public protected *; }
-keep,allowoptimization class kotlin.collections.CollectionsKt* { public protected *; }
-keep,allowoptimization class kotlin.collections.MapsKt* { public protected *; }
-keep,allowoptimization class kotlin.io.ByteStreamsKt* { public protected *; }
-keep,allowoptimization class kotlin.io.CloseableKt* { public protected *; }
-keep,allowoptimization class kotlin.io.FilesKt* { public protected *; }
-keep,allowoptimization class kotlin.jvm.internal.Intrinsics { public protected *; }
-keep,allowoptimization class kotlin.jvm.internal.Ref$* { public protected *; }
-keep,allowoptimization interface kotlin.sequences.Sequence { public protected *; }
-keep,allowoptimization class kotlin.text.Charsets { public protected *; }
-keep,allowoptimization class kotlin.text.Regex { public protected *; }
-keep,allowoptimization class kotlin.text.StringsKt* { public protected *; }
