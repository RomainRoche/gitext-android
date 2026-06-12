# --------------------------------------------------------------------------
# Gitrad SDK — consumer ProGuard / R8 rules
#
# These rules are automatically applied to any app that depends on this SDK.
# --------------------------------------------------------------------------

# Public facade: the Gitrad singleton and its entire public API.
-keep public class io.gitrad.sdk.sdk.Gitrad { public *; }

# Configuration type passed to Gitrad.configure().
-keep public class io.gitrad.sdk.sdk.GitradConfig { *; }

# Namespace accessor returned by Gitrad.scoped().
-keep public class io.gitrad.sdk.sdk.GitradNamespace { public *; }

# Strings helper returned by rememberGitradStrings().
-keep public class io.gitrad.sdk.sdk.GitradStrings { public *; }

# Sealed error and event hierarchies.
# Subclasses are instantiated inside the SDK, so R8 may consider them
# unreachable from the app side and remove them — breaking app-side
# when() expressions and instanceof checks.
# Constructors are kept explicitly so R8 does not strip them from data-class
# subclasses (RateLimited, NetworkError, ParseError, FetchSucceeded, FetchFailed).
-keep class io.gitrad.sdk.sdk.GitradError { *; }
-keep class io.gitrad.sdk.sdk.GitradError$* { *; <init>(...); }
-keep class io.gitrad.sdk.sdk.GitradEvent { *; }
-keep class io.gitrad.sdk.sdk.GitradEvent$* { *; <init>(...); }

# Top-level Compose integration functions (rememberGitradString,
# rememberGitradStrings) compile to static methods on GitradKt.
-keep class io.gitrad.sdk.sdk.GitradKt { public *; }

# WorkManager instantiates GitradSyncWorker by class name via reflection.
-keep class io.gitrad.sdk.sdk.GitradSyncWorker { *; }
