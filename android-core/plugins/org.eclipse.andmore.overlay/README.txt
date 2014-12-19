This plugin provides a minor extension to the ADT plugin to support
XML formatting via the "Source > Format" action on files.

The reason the plugin package name starts with the word "overlay"
instead of the normal "com.android.eclipse" package is that the plugin
name *must* be alphabetically later than "org.eclipse". The reasons
for this is detailed in issue
   http://code.google.com/p/android/issues/detail?id=20450
but essentially the plugin registration to override the
formatProcessor is processed in the alphabetical order of the plugin
names, so the org.eclipse plugin would clobber the com.android.eclipse
plugin. To work around this, the specific registration code was moved
out to a separate plugin, but the formatter itself continues to live
in ADT.
