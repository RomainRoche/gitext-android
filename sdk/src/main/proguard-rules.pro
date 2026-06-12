# --------------------------------------------------------------------------
# Gitext SDK — consumer ProGuard / R8 rules
#
# These rules are automatically applied to any app that depends on this SDK.
# --------------------------------------------------------------------------

# Public facade: the Gitext singleton and its entire public API.
-keep public class io.gitext.sdk.sdk.Gitext { public *; }

# Configuration type passed to Gitext.configure().
-keep public class io.gitext.sdk.sdk.GitextConfig { *; }

# Namespace accessor returned by Gitext.scoped().
-keep public class io.gitext.sdk.sdk.GitextNamespace { public *; }

# Strings helper returned by rememberGitextStrings().
-keep public class io.gitext.sdk.sdk.GitextStrings { public *; }

# Sealed error and event hierarchies.
# Subclasses are instantiated inside the SDK, so R8 may consider them
# unreachable from the app side and remove them — breaking app-side
# when() expressions and instanceof checks.
# Constructors are kept explicitly so R8 does not strip them from data-class
# subclasses (RateLimited, NetworkError, ParseError, FetchSucceeded, FetchFailed).
-keep class io.gitext.sdk.sdk.GitextError { *; }
-keep class io.gitext.sdk.sdk.GitextError$* { *; <init>(...); }
-keep class io.gitext.sdk.sdk.GitextEvent { *; }
-keep class io.gitext.sdk.sdk.GitextEvent$* { *; <init>(...); }

# Top-level Compose integration functions (rememberGitextString,
# rememberGitextStrings) compile to static methods on GitextKt.
-keep class io.gitext.sdk.sdk.GitextKt { public *; }

# WorkManager instantiates GitextSyncWorker by class name via reflection.
-keep class io.gitext.sdk.sdk.GitextSyncWorker { *; }
