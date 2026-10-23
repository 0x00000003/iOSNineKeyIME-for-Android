-- SPDX-License-Identifier: GPL-3.0-or-later
-- Test-only marker. No file, process, log or database access.
return function(input, env)
  env.engine.context:set_property("ime_probe_lua", "executed")
  for candidate in input:iter() do
    yield(candidate)
  end
end
