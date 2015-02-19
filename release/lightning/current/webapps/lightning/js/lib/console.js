
window.console = window.console || {};

var names = ["log", "debug", "info", "warn", "error",
    "assert", "dir", "dirxml", "group", "groupEnd", "time",
    "timeEnd", "count", "trace", "profile", "profileEnd"];

var i = 0;
var l = names.length;
for( i = 0; i < l; i++ )
{
  window.console[names[i]] = window.console[names[i]] || function() {};
}
