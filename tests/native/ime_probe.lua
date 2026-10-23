-- SPDX-License-Identifier: GPL-3.0-or-later
-- Test-only marker. No file, process, log or database access.
return function(input, env)
  env.engine.context:set_property("ime_probe_lua", "executed")
  local denied = not pcall(require, "untrusted_test_module")
  local restricted = io == nil and os == nil and package == nil and debug == nil
    and dofile == nil and loadfile == nil and print == nil and warn == nil and denied
  env.engine.context:set_property("ime_probe_policy", restricted and "restricted" or "unsafe")
  for candidate in input:iter() do
    yield(candidate)
  end
end
